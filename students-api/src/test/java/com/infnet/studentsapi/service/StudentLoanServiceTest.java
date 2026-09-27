package com.infnet.studentsapi.service;

import com.infnet.studentsapi.dto.CourseRequest;
import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.exception.BusinessException;
import com.infnet.studentsapi.messaging.LoanEvent;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.model.StudentStatus;
import com.infnet.studentsapi.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@ActiveProfiles("test")
@Transactional
class StudentLoanServiceTest {

    @Autowired
    private StudentLoanService studentLoanService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    private Long studentId;

    @BeforeEach
    void setUp() {
        var courseId = courseService.create(new CourseRequest(
                "Engenharia de Software", "ESW-LOAN", DegreeLevel.GRADUACAO, 8, "Tecnologia")).getId();
        studentId = studentService.create(new StudentRequest("Maria Silva", "loan@email.com", "LOAN-001",
                LocalDate.of(2000, 5, 10), LocalDate.of(2024, 2, 1), StudentStatus.ATIVO, 3, courseId)).getId();
    }

    private LoanEvent event(String type, long loanId) {
        return new LoanEvent(UUID.randomUUID(), type, Instant.now(), loanId, studentId, 7L,
                "Clean Code", LocalDate.now().plusDays(14));
    }

    @Test
    void shouldCountActiveLoansAsTheyAreCreatedAndReturned() {
        studentLoanService.apply(event("CREATED", 1));
        studentLoanService.apply(event("CREATED", 2));
        assertThat(studentLoanService.countActive(studentId)).isEqualTo(2);

        studentLoanService.apply(event("RETURNED", 1));
        assertThat(studentLoanService.countActive(studentId)).isEqualTo(1);
        assertThat(studentLoanService.findActive(studentId))
                .singleElement()
                .satisfies(loan -> assertThat(loan.getLoanId()).isEqualTo(2L));
    }

    @Test
    void shouldIgnoreDuplicatedDeliveries() {
        var created = event("CREATED", 1);

        assertThat(studentLoanService.apply(created)).isTrue();
        assertThat(studentLoanService.apply(created)).isFalse();

        assertThat(studentLoanService.countActive(studentId)).isEqualTo(1);
    }

    @Test
    void shouldNotReactivateALoanWhenCreatedArrivesAfterReturned() {
        studentLoanService.apply(event("RETURNED", 1));

        assertThat(studentLoanService.apply(event("CREATED", 1))).isFalse();

        assertThat(studentLoanService.countActive(studentId)).isZero();
    }

    @Test
    void shouldExposeTheActiveLoanCountOnTheStudent() {
        studentLoanService.apply(event("CREATED", 1));
        studentLoanService.apply(event("CREATED", 2));
        entityManager.flush();
        entityManager.clear();

        assertThat(studentRepository.findById(studentId).orElseThrow().getActiveLoans()).isEqualTo(2L);
    }

    @Test
    void shouldBlockStudentRemovalWhileThereAreActiveLoans() {
        studentLoanService.apply(event("CREATED", 1));

        assertThatThrownBy(() -> studentService.delete(studentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 emprestimo(s) ativo(s)");

        studentLoanService.apply(event("RETURNED", 1));
        assertThat(studentService.delete(studentId)).isTrue();
    }
}
