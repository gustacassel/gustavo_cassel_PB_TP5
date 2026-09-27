package com.infnet.libraryapi.dto;

import com.infnet.libraryapi.model.Loan;
import com.infnet.libraryapi.model.LoanStatus;
import com.infnet.libraryapi.model.StudentReplica;

import java.time.LocalDate;

/**
 * Os dados do aluno vem da copia local (student_replica). Se o aluno ainda nao
 * chegou por evento, cai para o nome copiado no emprestimo.
 */
public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        Long studentId,
        String studentName,
        String studentEnrollmentNumber,
        String studentCourseName,
        boolean studentDataAvailable,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status
) {
    public static LoanResponse of(Loan loan, StudentReplica student) {
        var book = loan.getBook();
        return new LoanResponse(
                loan.getId(),
                book != null ? book.getId() : null,
                book != null ? book.getTitle() : null,
                book != null ? book.getAuthor() : null,
                loan.getStudentId(),
                student != null ? student.getName() : loan.getStudentName(),
                student != null ? student.getEnrollmentNumber() : null,
                student != null ? student.getCourseName() : null,
                student != null,
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus());
    }
}
