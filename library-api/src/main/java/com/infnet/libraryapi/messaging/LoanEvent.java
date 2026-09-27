package com.infnet.libraryapi.messaging;

import com.infnet.libraryapi.model.Loan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanEvent(
        UUID eventId,
        LoanEventType type,
        Instant occurredAt,
        Long loanId,
        Long studentId,
        Long bookId,
        String bookTitle,
        LocalDate dueDate
) {

    public static LoanEvent of(LoanEventType type, Loan loan) {
        var book = loan.getBook();
        return new LoanEvent(
                UUID.randomUUID(),
                type,
                Instant.now(),
                loan.getId(),
                loan.getStudentId(),
                book != null ? book.getId() : null,
                book != null ? book.getTitle() : null,
                loan.getDueDate());
    }
}
