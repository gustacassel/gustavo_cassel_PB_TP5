package com.infnet.studentsapi.messaging;

import com.infnet.studentsapi.model.StudentLoanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        Long loanId,
        Long studentId,
        Long bookId,
        String bookTitle,
        LocalDate dueDate
) {
    public StudentLoanStatus resultingStatus() {
        return switch (type) {
            case "CREATED" -> StudentLoanStatus.ACTIVE;
            case "RETURNED" -> StudentLoanStatus.RETURNED;
            case "DELETED" -> StudentLoanStatus.DELETED;
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + type);
        };
    }
}
