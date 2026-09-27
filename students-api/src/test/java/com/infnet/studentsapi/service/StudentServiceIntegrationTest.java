package com.infnet.studentsapi.service;

import com.infnet.studentsapi.dto.CourseRequest;
import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.exception.BusinessException;
import com.infnet.studentsapi.model.AuditAction;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.model.StudentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@ActiveProfiles("test")
@Transactional
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AuditService auditService;

    private Long courseId;

    @BeforeEach
    void setUp() {
        courseId = courseService.create(new CourseRequest(
                "Engenharia de Software", "ESW-TEST", DegreeLevel.GRADUACAO, 8, "Tecnologia")).getId();
    }

    private StudentRequest request(String name, String email, String enrollment, StudentStatus status) {
        return new StudentRequest(name, email, enrollment,
                LocalDate.of(2000, 5, 10), LocalDate.of(2024, 2, 1), status, 3, courseId);
    }

    @Test
    void shouldCreateStudentLinkedToACourse() {
        var saved = studentService.create(request("Maria Silva", "maria.tp3@email.com", "TP3-001", StudentStatus.ATIVO));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCourse()).isNotNull();
        assertThat(saved.getCourse().getCode()).isEqualTo("ESW-TEST");
        assertThat(saved.getStatus()).isEqualTo(StudentStatus.ATIVO);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldApplyDefaultsWhenOptionalFieldsAreMissing() {
        var saved = studentService.create(new StudentRequest(
                "Joao Souza", "joao.tp3@email.com", "TP3-002", null, null, null, null, courseId));

        assertThat(saved.getStatus()).isEqualTo(StudentStatus.ATIVO);
        assertThat(saved.getCurrentSemester()).isEqualTo(1);
        assertThat(saved.getEnrollmentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        studentService.create(request("Maria Silva", "dup.tp3@email.com", "TP3-003", StudentStatus.ATIVO));

        assertThatThrownBy(() ->
                studentService.create(request("Outra Maria", "dup.tp3@email.com", "TP3-004", StudentStatus.ATIVO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dup.tp3@email.com");
    }

    @Test
    void shouldRejectUnknownCourse() {
        assertThatThrownBy(() -> studentService.create(new StudentRequest(
                "Sem Curso", "semcurso.tp3@email.com", "TP3-005",
                null, null, StudentStatus.ATIVO, 1, 999_999L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("999999");
    }

    @Test
    void shouldRecordCreateUpdateAndDeleteInTheHistory() {
        var saved = studentService.create(request("Maria Silva", "hist.tp3@email.com", "TP3-006", StudentStatus.ATIVO));

        studentService.update(saved.getId(), new StudentRequest(
                "Maria Silva Santos", "hist.tp3@email.com", "TP3-006",
                LocalDate.of(2000, 5, 10), LocalDate.of(2024, 2, 1), StudentStatus.TRANCADO, 4, courseId));

        studentService.delete(saved.getId());

        var history = auditService.findByEntityAndId("STUDENT", saved.getId());

        assertThat(history).hasSize(3);
        assertThat(history)
                .extracting(log -> log.getAction())
                .containsExactlyInAnyOrder(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE);
        assertThat(history)
                .filteredOn(log -> log.getAction() == AuditAction.UPDATE)
                .first()
                .satisfies(log -> assertThat(log.getDetails())
                        .contains("status")
                        .contains("TRANCADO")
                        .contains("name"));
        assertThat(history).allMatch(log -> log.getTimestamp() != null);
    }

    @Test
    void shouldBlockRemovalOfACourseThatStillHasStudents() {
        studentService.create(request("Maria Silva", "bloqueio.tp3@email.com", "TP3-007", StudentStatus.ATIVO));

        assertThatThrownBy(() -> courseService.delete(courseId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("estudantes matriculados");
    }

    @Test
    void shouldSummarizeCoursesWithActiveStudentCount() {
        studentService.create(request("Maria Silva", "sum1.tp3@email.com", "TP3-008", StudentStatus.ATIVO));
        studentService.create(request("Joao Souza", "sum2.tp3@email.com", "TP3-009", StudentStatus.ATIVO));
        studentService.create(request("Pedro Santos", "sum3.tp3@email.com", "TP3-010", StudentStatus.TRANCADO));

        var summary = courseService.findSummary();

        assertThat(summary)
                .filteredOn(item -> item.code().equals("ESW-TEST"))
                .first()
                .satisfies(item -> assertThat(item.activeStudents()).isEqualTo(2L));
    }

    @Test
    void shouldReturnEmptyWhenUpdatingAnUnknownStudent() {
        assertThat(studentService.update(999_999L,
                request("Fantasma", "fantasma.tp3@email.com", "TP3-011", StudentStatus.ATIVO))).isEmpty();
        assertThat(studentService.delete(999_999L)).isFalse();
    }
}
