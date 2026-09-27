package com.infnet.studentsapi.service;

import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.exception.BusinessException;
import com.infnet.studentsapi.messaging.StudentEvent;
import com.infnet.studentsapi.model.AuditAction;
import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.Student;
import com.infnet.studentsapi.model.StudentStatus;
import com.infnet.studentsapi.repository.CourseRepository;
import com.infnet.studentsapi.repository.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentService {
    private static final String ENTITY_NAME = "STUDENT";

    private final StudentRepository repository;
    private final CourseRepository courseRepository;
    private final AuditService auditService;
    private final StudentLoanService studentLoanService;
    private final ApplicationEventPublisher events;

    public StudentService(StudentRepository repository,
                          CourseRepository courseRepository,
                          AuditService auditService,
                          StudentLoanService studentLoanService,
                          ApplicationEventPublisher events) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.auditService = auditService;
        this.studentLoanService = studentLoanService;
        this.events = events;
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

    public Optional<Student> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Student> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Optional<Student> findByEnrollmentNumber(String enrollmentNumber) {
        return repository.findByEnrollmentNumber(enrollmentNumber);
    }

    public List<Student> findByName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    public List<Student> findByStatus(StudentStatus status) {
        return repository.findByStatus(status);
    }

    public List<Student> findByCourse(Long courseId) {
        return repository.findByCourseId(courseId);
    }

    @Transactional
    public Student create(StudentRequest request) {
        ensureEmailIsFree(request.email(), null);
        ensureEnrollmentIsFree(request.enrollmentNumber(), null);

        var student = new Student();
        apply(student, request);

        var saved = repository.save(student);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE,
                "Aluno cadastrado: '%s' (matricula %s, curso %s)"
                        .formatted(saved.getName(), saved.getEnrollmentNumber(), courseLabel(saved.getCourse())));
        events.publishEvent(StudentEvent.created(saved));
        return saved;
    }

    @Transactional
    public Optional<Student> update(Long id, StudentRequest request) {
        var found = repository.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        ensureEmailIsFree(request.email(), id);
        ensureEnrollmentIsFree(request.enrollmentNumber(), id);

        var student = found.get();
        var changes = new StringBuilder();
        appendChange(changes, "name", student.getName(), request.name());
        appendChange(changes, "email", student.getEmail(), request.email());
        appendChange(changes, "enrollmentNumber", student.getEnrollmentNumber(), request.enrollmentNumber());
        appendChange(changes, "birthDate", student.getBirthDate(), request.birthDate());
        appendChange(changes, "status", student.getStatus(), request.status());
        appendChange(changes, "currentSemester", student.getCurrentSemester(), request.currentSemester());
        appendChange(changes, "course", courseLabel(student.getCourse()), courseLabel(resolveCourse(request.courseId())));

        apply(student, request);

        var saved = repository.saveAndFlush(student);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.UPDATE,
                changes.isEmpty() ? "Nenhum campo alterado" : changes.toString());
        events.publishEvent(StudentEvent.updated(saved));
        return Optional.of(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        var found = repository.findById(id);
        if (found.isEmpty()) {
            return false;
        }

        var activeLoans = studentLoanService.countActive(id);
        if (activeLoans > 0) {
            throw new BusinessException(
                    "Nao e possivel remover o aluno '%s': ele possui %d emprestimo(s) ativo(s) na biblioteca"
                            .formatted(found.get().getName(), activeLoans));
        }

        repository.delete(found.get());
        auditService.record(ENTITY_NAME, id, AuditAction.DELETE,
                "Aluno removido: '%s'".formatted(found.get().getName()));
        events.publishEvent(StudentEvent.deleted(found.get()));
        return true;
    }

    private void apply(Student student, StudentRequest request) {
        student.setName(request.name());
        student.setEmail(request.email());
        student.setEnrollmentNumber(request.enrollmentNumber());
        student.setBirthDate(request.birthDate());
        student.setEnrollmentDate(request.enrollmentDate() != null ? request.enrollmentDate() : LocalDate.now());
        student.setStatus(request.status() != null ? request.status() : StudentStatus.ATIVO);
        student.setCurrentSemester(request.currentSemester() != null ? request.currentSemester() : 1);
        student.setCourse(resolveCourse(request.courseId()));
    }

    private Course resolveCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("Curso %d nao encontrado".formatted(courseId)));
    }

    private void ensureEmailIsFree(String email, Long currentId) {
        repository.findByEmail(email)
                .filter(other -> !other.getId().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException("Ja existe um estudante com o email '%s'".formatted(email));
                });
    }

    private void ensureEnrollmentIsFree(String enrollmentNumber, Long currentId) {
        repository.findByEnrollmentNumber(enrollmentNumber)
                .filter(other -> !other.getId().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException(
                            "Ja existe um estudante com a matricula '%s'".formatted(enrollmentNumber));
                });
    }

    private String courseLabel(Course course) {
        return course != null ? course.getName() : "sem curso";
    }

    private void appendChange(StringBuilder changes, String field, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            if (!changes.isEmpty()) {
                changes.append("; ");
            }
            changes.append("%s: '%s' -> '%s'".formatted(field, oldValue, newValue));
        }
    }
}
