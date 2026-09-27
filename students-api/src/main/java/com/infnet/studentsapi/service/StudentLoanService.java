package com.infnet.studentsapi.service;

import com.infnet.studentsapi.messaging.LoanEvent;
import com.infnet.studentsapi.model.StudentLoan;
import com.infnet.studentsapi.model.StudentLoanStatus;
import com.infnet.studentsapi.repository.StudentLoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class StudentLoanService {
    private static final Logger log = LoggerFactory.getLogger(StudentLoanService.class);

    private final StudentLoanRepository repository;

    public StudentLoanService(StudentLoanRepository repository) {
        this.repository = repository;
    }

    public long countActive(Long studentId) {
        return repository.countByStudentIdAndStatus(studentId, StudentLoanStatus.ACTIVE);
    }

    public List<StudentLoan> findActive(Long studentId) {
        return repository.findByStudentIdAndStatus(studentId, StudentLoanStatus.ACTIVE);
    }

    @Transactional
    public boolean apply(LoanEvent event) {
        var target = event.resultingStatus();
        var loan = repository.findById(event.loanId()).orElseGet(StudentLoan::new);

        // evento repetido ou atrasado nao faz o emprestimo voltar de estado
        if (loan.getStatus() != null && target.compareTo(loan.getStatus()) <= 0) {
            log.info("Evento {} ignorado: emprestimo {} ja esta {}", event.type(), event.loanId(), loan.getStatus());
            return false;
        }

        loan.setLoanId(event.loanId());
        loan.setStudentId(event.studentId());
        loan.setBookTitle(event.bookTitle());
        loan.setDueDate(event.dueDate());
        loan.setStatus(target);
        loan.setLastEventAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
        repository.save(loan);

        log.info("Evento {} aplicado: emprestimo {} do aluno {} agora {}",
                event.type(), event.loanId(), event.studentId(), target);
        return true;
    }
}
