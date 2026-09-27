package com.infnet.studentsapi.model;

/** Somente estudantes ATIVO podem tomar livros emprestados na library-api. */
public enum StudentStatus {
    ATIVO,
    TRANCADO,
    FORMADO,
    DESLIGADO
}
