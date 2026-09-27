package com.infnet.studentsapi.service;

import com.infnet.studentsapi.dto.CourseRequest;
import com.infnet.studentsapi.dto.CourseSummary;
import com.infnet.studentsapi.exception.BusinessException;
import com.infnet.studentsapi.messaging.StudentEvent;
import com.infnet.studentsapi.model.AuditAction;
import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.repository.CourseRepository;
import com.infnet.studentsapi.repository.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class CourseService {
    private static final String ENTITY_NAME = "COURSE";

    private final CourseRepository repository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public CourseService(CourseRepository repository,
                         StudentRepository studentRepository,
                         AuditService auditService,
                         ApplicationEventPublisher events) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.auditService = auditService;
        this.events = events;
    }

    public List<Course> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    public Optional<Course> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Course> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<Course> findByDegreeLevel(DegreeLevel degreeLevel) {
        return repository.findByDegreeLevel(degreeLevel);
    }

    /** Resolve a contagem em uma unica consulta agregada, nao um SELECT por curso. */
    public List<CourseSummary> findSummary() {
        Map<Long, Long> activeByCourse = new HashMap<>();
        for (var row : studentRepository.countActiveStudentsByCourse()) {
            activeByCourse.put((Long) row[0], (Long) row[1]);
        }

        return repository.findAllByOrderByNameAsc().stream()
                .map(course -> CourseSummary.of(course, activeByCourse.getOrDefault(course.getId(), 0L)))
                .toList();
    }

    @Transactional
    public Course create(CourseRequest request) {
        repository.findByCode(request.code()).ifPresent(existing -> {
            throw new BusinessException("Ja existe um curso com o codigo '%s'".formatted(request.code()));
        });

        var course = new Course();
        apply(course, request);

        var saved = repository.save(course);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE,
                "Curso cadastrado: '%s' (codigo %s, %s semestres)"
                        .formatted(saved.getName(), saved.getCode(), saved.getDurationSemesters()));
        return saved;
    }

    @Transactional
    public Optional<Course> update(Long id, CourseRequest request) {
        var found = repository.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        var course = found.get();
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("Ja existe um curso com o codigo '%s'".formatted(request.code()));
                });

        var nameChanged = !Objects.equals(course.getName(), request.name());
        var changes = new StringBuilder();
        appendChange(changes, "name", course.getName(), request.name());
        appendChange(changes, "code", course.getCode(), request.code());
        appendChange(changes, "degreeLevel", course.getDegreeLevel(), request.degreeLevel());
        appendChange(changes, "durationSemesters", course.getDurationSemesters(), request.durationSemesters());
        appendChange(changes, "department", course.getDepartment(), request.department());

        apply(course, request);

        var saved = repository.save(course);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.UPDATE,
                changes.isEmpty() ? "Nenhum campo alterado" : changes.toString());

        // o nome do curso vai no evento do aluno
        if (nameChanged) {
            studentRepository.findByCourseId(id)
                    .forEach(student -> events.publishEvent(StudentEvent.updated(student)));
        }
        return Optional.of(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        var found = repository.findById(id);
        if (found.isEmpty()) {
            return false;
        }

        if (studentRepository.existsByCourseId(id)) {
            throw new BusinessException(
                    "Nao e possivel remover o curso '%s': existem estudantes matriculados nele"
                            .formatted(found.get().getName()));
        }

        repository.delete(found.get());
        auditService.record(ENTITY_NAME, id, AuditAction.DELETE,
                "Curso removido: '%s'".formatted(found.get().getName()));
        return true;
    }

    private void apply(Course course, CourseRequest request) {
        course.setName(request.name());
        course.setCode(request.code());
        course.setDegreeLevel(request.degreeLevel());
        course.setDurationSemesters(request.durationSemesters());
        course.setDepartment(request.department());
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
