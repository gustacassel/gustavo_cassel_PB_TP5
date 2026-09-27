package com.infnet.libraryapi.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LoanRequest(
        @NotNull(message = "O id do livro e obrigatorio")
        Long bookId,

        @NotNull(message = "O id do estudante e obrigatorio")
        Long studentId,

        LocalDate loanDate,

        LocalDate dueDate
) {
}
