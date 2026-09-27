package com.infnet.libraryapi.service;

import com.infnet.libraryapi.dto.LoanRequest;
import com.infnet.libraryapi.exception.BusinessException;
import com.infnet.libraryapi.messaging.StudentEvent;
import com.infnet.libraryapi.model.AuditAction;
import com.infnet.libraryapi.model.Book;
import com.infnet.libraryapi.model.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Transactional
class LoanStudentValidationTest {

    private static final Long STUDENT_ID = 42L;

    @Autowired
    private LoanService loanService;

    @Autowired
    private StudentReplicaService studentReplicaService;

    @Autowired
    private BookService bookService;

    @Autowired
    private AuditService auditService;

    private Book book;

    @BeforeEach
    void setUp() {
        var newBook = new Book();
        newBook.setTitle("Clean Code");
        newBook.setAuthor("Robert C. Martin");
        book = bookService.save(newBook);
    }

    private void studentArrives(String type, long version, String status) {
        studentReplicaService.apply(new StudentEvent(UUID.randomUUID(), type, Instant.now(), STUDENT_ID, version,
                "Maria Silva", "maria@infnet.edu.br", "2026001", status, 1L, "Engenharia de Software"));
    }

    private LoanRequest request() {
        return new LoanRequest(book.getId(), STUDENT_ID, null, null);
    }

    @Test
    void shouldCreateLoanUsingTheLocalCopyOfTheStudent() {
        studentArrives("CREATED", 0, "ATIVO");

        var response = loanService.create(request());

        assertThat(response.id()).isNotNull();
        assertThat(response.bookTitle()).isEqualTo("Clean Code");
        assertThat(response.studentName()).isEqualTo("Maria Silva");
        assertThat(response.studentEnrollmentNumber()).isEqualTo("2026001");
        assertThat(response.studentCourseName()).isEqualTo("Engenharia de Software");
        assertThat(response.studentDataAvailable()).isTrue();
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.dueDate()).isEqualTo(response.loanDate().plusDays(14));
    }

    @Test
    void shouldRejectLoanWhenTheStudentIsUnknown() {
        assertThatThrownBy(() -> loanService.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estudante 42 nao encontrado");
    }

    @Test
    void shouldRejectLoanAfterTheStudentIsLockedByAnEvent() {
        studentArrives("CREATED", 0, "ATIVO");
        studentArrives("UPDATED", 1, "TRANCADO");

        assertThatThrownBy(() -> loanService.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TRANCADO");
    }

    @Test
    void shouldRejectLoanAfterTheStudentIsDeleted() {
        studentArrives("CREATED", 0, "ATIVO");
        studentArrives("DELETED", 1, "ATIVO");

        assertThatThrownBy(() -> loanService.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    void shouldRejectLoanWhenBookDoesNotExist() {
        studentArrives("CREATED", 0, "ATIVO");

        assertThatThrownBy(() -> loanService.create(new LoanRequest(999_999L, STUDENT_ID, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Livro");
    }

    @Test
    void shouldReflectStudentChangesInExistingLoans() {
        studentArrives("CREATED", 0, "ATIVO");
        loanService.create(request());
        loanService.create(request());

        studentReplicaService.apply(new StudentEvent(UUID.randomUUID(), "UPDATED", Instant.now(), STUDENT_ID, 1L,
                "Maria Silva Santos", "maria@infnet.edu.br", "2026001", "ATIVO", 1L, "Engenharia de Software"));

        var loans = loanService.enrich(loanService.findByStudent(STUDENT_ID));

        assertThat(loans).hasSize(2).allMatch(loan -> loan.studentName().equals("Maria Silva Santos"));
    }

    @Test
    void shouldFallBackToTheCopiedNameWhenTheStudentIsNotReplicated() {
        studentArrives("CREATED", 0, "ATIVO");
        var created = loanService.create(request());

        // emprestimo de um aluno que nao esta na copia local
        var loan = loanService.findById(created.id()).orElseThrow();
        loan.setStudentId(7_777L);

        var response = loanService.enrich(loan).orElseThrow();

        assertThat(response.studentDataAvailable()).isFalse();
        assertThat(response.studentName()).isEqualTo("Maria Silva");
        assertThat(response.studentEnrollmentNumber()).isNull();
    }

    @Test
    void shouldRecordLoanHistoryWithTheStudentName() {
        studentArrives("CREATED", 0, "ATIVO");

        var created = loanService.create(request());
        loanService.returnLoan(created.id());

        var history = auditService.findByEntityAndId("LOAN", created.id());

        assertThat(history)
                .extracting(log -> log.getAction())
                .containsExactlyInAnyOrder(AuditAction.CREATE, AuditAction.UPDATE);
        assertThat(history)
                .filteredOn(log -> log.getAction() == AuditAction.CREATE)
                .first()
                .satisfies(log -> assertThat(log.getDetails()).contains("Maria Silva").contains("Clean Code"));
    }
}
