package com.infnet.libraryapi.repository;

import com.infnet.libraryapi.model.Loan;
import com.infnet.libraryapi.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByStudentId(Long studentId);

    List<Loan> findByBookId(Long bookId);

    boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.status = com.infnet.libraryapi.model.LoanStatus.ACTIVE AND l.dueDate < :date")
    List<Loan> findOverdue(@Param("date") LocalDate date);
}
