package com.infnet.libraryapi.repository;

import com.infnet.libraryapi.config.JpaAuditingConfig;
import com.infnet.libraryapi.model.Book;
import com.infnet.libraryapi.model.Loan;
import com.infnet.libraryapi.model.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class LoanRepositoryTest {

    /** O aluno vive no microsservico: aqui e so um id, sem tabela nem FK. */
    private static final Long STUDENT_ID = 42L;
    private static final String STUDENT_NAME = "Maria Silva";

    @Autowired
    private LoanRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        entityManager.persist(book);
    }

    private Loan newLoan(LoanStatus status, LocalDate loanDate, LocalDate dueDate) {
        return newLoan(status, loanDate, dueDate, STUDENT_ID);
    }

    private Loan newLoan(LoanStatus status, LocalDate loanDate, LocalDate dueDate, Long studentId) {
        var loan = new Loan();
        loan.setBook(book);
        loan.setStudentId(studentId);
        loan.setStudentName(STUDENT_NAME);
        loan.setLoanDate(loanDate);
        loan.setDueDate(dueDate);
        loan.setStatus(status);
        return loan;
    }

    @Test
    void shouldSaveLoanReferencingRemoteStudentById() {
        var saved = repository.save(newLoan(LoanStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(14)));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBook().getTitle()).isEqualTo("Clean Code");
        assertThat(found.get().getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(found.get().getStudentName()).isEqualTo(STUDENT_NAME);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldPersistLoanForAStudentThatOnlyExistsInTheMicroservice() {
        // sem FK, qualquer id e aceito aqui: quem valida e o LoanService pela copia local
        var saved = repository.save(newLoan(LoanStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(14), 9_999L));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByStudentId(9_999L)).hasSize(1);
    }

    @Test
    void shouldFindByStatus() {
        repository.save(newLoan(LoanStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(14)));
        repository.save(newLoan(LoanStatus.RETURNED, LocalDate.now().minusDays(30), LocalDate.now().minusDays(16)));

        assertThat(repository.findByStatus(LoanStatus.ACTIVE)).hasSize(1);
        assertThat(repository.findByStatus(LoanStatus.RETURNED)).hasSize(1);
    }

    @Test
    void shouldFindByStudentIdAndBookId() {
        repository.save(newLoan(LoanStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(14)));

        assertThat(repository.findByStudentId(STUDENT_ID)).hasSize(1);
        assertThat(repository.findByBookId(book.getId())).hasSize(1);
        assertThat(repository.findByStudentId(999L)).isEmpty();
    }

    @Test
    void shouldFindOverdueLoansWithCustomQuery() {
        // ativo e vencido ha 5 dias -> deve aparecer
        repository.save(newLoan(LoanStatus.ACTIVE, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5)));
        // ativo mas dentro do prazo -> nao deve aparecer
        repository.save(newLoan(LoanStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(14)));
        // vencido porem ja devolvido -> nao deve aparecer
        repository.save(newLoan(LoanStatus.RETURNED, LocalDate.now().minusDays(30), LocalDate.now().minusDays(10)));

        var overdue = repository.findOverdue(LocalDate.now());

        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getDueDate()).isBefore(LocalDate.now());
        assertThat(overdue.get(0).getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }
}
