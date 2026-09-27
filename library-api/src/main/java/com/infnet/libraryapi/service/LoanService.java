package com.infnet.libraryapi.service;

import com.infnet.libraryapi.dto.LoanRequest;
import com.infnet.libraryapi.dto.LoanResponse;
import com.infnet.libraryapi.exception.BusinessException;
import com.infnet.libraryapi.messaging.LoanEvent;
import com.infnet.libraryapi.messaging.LoanEventType;
import com.infnet.libraryapi.model.AuditAction;
import com.infnet.libraryapi.model.Loan;
import com.infnet.libraryapi.model.LoanStatus;
import com.infnet.libraryapi.model.StudentReplica;
import com.infnet.libraryapi.repository.BookRepository;
import com.infnet.libraryapi.repository.LoanRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoanService {
    private static final String ENTITY_NAME = "LOAN";
    private static final int DEFAULT_LOAN_DAYS = 14;

    private final LoanRepository repository;
    private final BookRepository bookRepository;
    private final StudentReplicaService studentReplicaService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public LoanService(LoanRepository repository,
                       BookRepository bookRepository,
                       StudentReplicaService studentReplicaService,
                       AuditService auditService,
                       ApplicationEventPublisher events) {
        this.repository = repository;
        this.bookRepository = bookRepository;
        this.studentReplicaService = studentReplicaService;
        this.auditService = auditService;
        this.events = events;
    }

    public List<Loan> findAll() {
        return repository.findAll();
    }

    public Optional<Loan> findById(Long id) {
        return repository.findById(id);
    }

    public List<Loan> findByStatus(LoanStatus status) {
        return repository.findByStatus(status);
    }

    public List<Loan> findByStudent(Long studentId) {
        return repository.findByStudentId(studentId);
    }

    public List<Loan> findByBook(Long bookId) {
        return repository.findByBookId(bookId);
    }

    public List<Loan> findOverdue() {
        return repository.findOverdue(LocalDate.now());
    }

    public List<LoanResponse> enrich(List<Loan> loans) {
        if (loans.isEmpty()) {
            return List.of();
        }

        var studentIds = loans.stream().map(Loan::getStudentId).collect(Collectors.toSet());
        Map<Long, StudentReplica> students = studentReplicaService.indexByIds(studentIds);
        return loans.stream()
                .map(loan -> LoanResponse.of(loan, students.get(loan.getStudentId())))
                .toList();
    }

    public Optional<LoanResponse> enrich(Loan loan) {
        return Optional.of(LoanResponse.of(loan,
                studentReplicaService.indexByIds(List.of(loan.getStudentId())).get(loan.getStudentId())));
    }

    @Transactional
    public LoanResponse create(LoanRequest request) {
        var book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BusinessException("Livro %d nao encontrado".formatted(request.bookId())));

        var student = studentReplicaService.findById(request.studentId())
                .orElseThrow(() -> new BusinessException(
                        "Estudante %d nao encontrado".formatted(request.studentId())));

        if (!student.isActive()) {
            throw new BusinessException(
                    "O estudante '%s' esta com situacao %s e nao pode pegar livros emprestados"
                            .formatted(student.getName(), student.getStatus()));
        }

        var loan = new Loan();
        loan.setBook(book);
        loan.setStudentId(student.getId());
        loan.setStudentName(student.getName());
        loan.setLoanDate(request.loanDate() != null ? request.loanDate() : LocalDate.now());
        loan.setDueDate(request.dueDate() != null
                ? request.dueDate()
                : loan.getLoanDate().plusDays(DEFAULT_LOAN_DAYS));
        loan.setStatus(LoanStatus.ACTIVE);

        var saved = repository.save(loan);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE,
                "Emprestimo criado: livro '%s' para aluno '%s' (id %d), devolucao ate %s"
                        .formatted(book.getTitle(), student.getName(), student.getId(), saved.getDueDate()));
        events.publishEvent(LoanEvent.of(LoanEventType.CREATED, saved));
        return LoanResponse.of(saved, student);
    }

    @Transactional
    public Optional<Loan> returnLoan(Long id) {
        var loan = repository.findById(id);
        if (loan.isEmpty() || loan.get().getStatus() == LoanStatus.RETURNED) {
            return Optional.empty();
        }

        var returnedLoan = loan.get();
        returnedLoan.setReturnDate(LocalDate.now());
        returnedLoan.setStatus(LoanStatus.RETURNED);

        var saved = repository.save(returnedLoan);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.UPDATE,
                "Emprestimo devolvido em %s: livro '%s' (aluno '%s')"
                        .formatted(saved.getReturnDate(), saved.getBook().getTitle(), saved.getStudentName()));
        events.publishEvent(LoanEvent.of(LoanEventType.RETURNED, saved));
        return Optional.of(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        var loan = repository.findById(id);
        if (loan.isEmpty()) {
            return false;
        }

        repository.delete(loan.get());
        auditService.record(ENTITY_NAME, id, AuditAction.DELETE,
                "Emprestimo removido: livro '%s' (aluno '%s')"
                        .formatted(loan.get().getBook().getTitle(), loan.get().getStudentName()));
        events.publishEvent(LoanEvent.of(LoanEventType.DELETED, loan.get()));
        return true;
    }
}
