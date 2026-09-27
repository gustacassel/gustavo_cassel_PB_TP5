package com.infnet.studentsapi.model;

// a ordem importa: um emprestimo so avanca (ACTIVE -> RETURNED -> DELETED)
public enum StudentLoanStatus {
    ACTIVE,
    RETURNED,
    DELETED
}
