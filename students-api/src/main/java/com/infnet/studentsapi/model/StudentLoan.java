package com.infnet.studentsapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentLoan {

    // id do emprestimo na library-api
    @Id
    private Long loanId;

    @Column(nullable = false)
    private Long studentId;

    @Column(length = 200)
    private String bookTitle;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentLoanStatus status;

    @Column(nullable = false)
    private Instant lastEventAt;
}
