package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.config.JpaAuditingConfig;
import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.model.Student;
import com.infnet.studentsapi.model.StudentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StudentRepositoryTest {

    @Autowired
    private StudentRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Course engenharia;
    private Course dados;

    @BeforeEach
    void setUp() {
        engenharia = persistCourse("Engenharia de Software", "ESW", DegreeLevel.GRADUACAO, 8);
        dados = persistCourse("Ciencia de Dados", "CDD", DegreeLevel.GRADUACAO, 8);
    }

    private Course persistCourse(String name, String code, DegreeLevel level, int semesters) {
        var course = new Course();
        course.setName(name);
        course.setCode(code);
        course.setDegreeLevel(level);
        course.setDurationSemesters(semesters);
        return entityManager.persist(course);
    }

    private Student newStudent(String name, String email, String enrollment,
                               StudentStatus status, Integer semester, Course course) {
        var student = new Student();
        student.setName(name);
        student.setEmail(email);
        student.setEnrollmentNumber(enrollment);
        student.setBirthDate(LocalDate.of(2000, 1, 15));
        student.setEnrollmentDate(LocalDate.of(2024, 2, 1));
        student.setStatus(status);
        student.setCurrentSemester(semester);
        student.setCourse(course);
        return student;
    }

    @Test
    void shouldSaveStudentWithCourseRelationship() {
        var saved = repository.save(
                newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Maria Silva");
        assertThat(found.get().getCourse().getName()).isEqualTo("Engenharia de Software");
        assertThat(found.get().getStatus()).isEqualTo(StudentStatus.ATIVO);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByEmailAndEnrollmentNumber() {
        repository.save(newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));

        assertThat(repository.findByEmail("maria@email.com")).isPresent();
        assertThat(repository.findByEnrollmentNumber("2026001")).isPresent();
        assertThat(repository.findByEmail("nao-existe@email.com")).isEmpty();
    }

    @Test
    void shouldFindByNameContainingIgnoreCase() {
        repository.save(newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));
        repository.save(newStudent("Ana Maria Souza", "ana@email.com", "2026003", StudentStatus.ATIVO, 2, dados));
        repository.save(newStudent("Pedro Santos", "pedro@email.com", "2026004", StudentStatus.ATIVO, 3, dados));

        assertThat(repository.findByNameContainingIgnoreCase("maria")).hasSize(2);
    }

    @Test
    void shouldFindByStatusAndByCourse() {
        repository.save(newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));
        repository.save(newStudent("Pedro Santos", "pedro@email.com", "2026004", StudentStatus.TRANCADO, 4, engenharia));
        repository.save(newStudent("Joao Souza", "joao@email.com", "2026002", StudentStatus.ATIVO, 6, dados));

        assertThat(repository.findByStatus(StudentStatus.ATIVO)).hasSize(2);
        assertThat(repository.findByStatus(StudentStatus.TRANCADO)).hasSize(1);
        assertThat(repository.findByCourseId(engenharia.getId())).hasSize(2);
        assertThat(repository.existsByCourseId(engenharia.getId())).isTrue();
    }

    @Test
    void shouldCountActiveStudentsGroupedByCourse() {
        repository.save(newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));
        repository.save(newStudent("Carla Dias", "carla@email.com", "2026005", StudentStatus.ATIVO, 3, engenharia));
        // trancado nao entra na contagem de ativos
        repository.save(newStudent("Pedro Santos", "pedro@email.com", "2026004", StudentStatus.TRANCADO, 4, engenharia));
        repository.save(newStudent("Joao Souza", "joao@email.com", "2026002", StudentStatus.ATIVO, 6, dados));

        var counts = repository.countActiveStudentsByCourse();

        assertThat(counts).hasSize(2);
        assertThat(counts)
                .anySatisfy(row -> {
                    assertThat(row[0]).isEqualTo(engenharia.getId());
                    assertThat(row[1]).isEqualTo(2L);
                })
                .anySatisfy(row -> {
                    assertThat(row[0]).isEqualTo(dados.getId());
                    assertThat(row[1]).isEqualTo(1L);
                });
    }

    @Test
    void shouldFindActiveStudentsFromSemesterWithCustomQuery() {
        repository.save(newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));
        repository.save(newStudent("Joao Souza", "joao@email.com", "2026002", StudentStatus.ATIVO, 6, dados));
        repository.save(newStudent("Carla Dias", "carla@email.com", "2026005", StudentStatus.ATIVO, 2, dados));
        repository.save(newStudent("Pedro Santos", "pedro@email.com", "2026004", StudentStatus.TRANCADO, 8, engenharia));

        var found = repository.findActiveFromSemester(5);

        assertThat(found).hasSize(2);
        assertThat(found).allMatch(student -> student.getStatus() == StudentStatus.ATIVO);
        assertThat(found).allMatch(student -> student.getCurrentSemester() >= 5);
    }

    @Test
    void shouldEnforceUniqueEmailAndEnrollmentConstraints() {
        repository.saveAndFlush(
                newStudent("Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 5, engenharia));

        assertThatThrownBy(() -> repository.saveAndFlush(
                newStudent("Outra Maria", "maria@email.com", "2026099", StudentStatus.ATIVO, 1, dados)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
