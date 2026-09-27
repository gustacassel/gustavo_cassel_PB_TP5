package com.infnet.studentsapi.dto;

import com.infnet.studentsapi.model.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String name,

        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "Email invalido")
        @Size(max = 150, message = "O email deve ter no maximo 150 caracteres")
        String email,

        @NotBlank(message = "A matricula e obrigatoria")
        @Size(max = 30, message = "A matricula deve ter no maximo 30 caracteres")
        String enrollmentNumber,

        @Past(message = "A data de nascimento deve estar no passado")
        LocalDate birthDate,

        LocalDate enrollmentDate,

        StudentStatus status,

        @Min(value = 1, message = "O semestre atual deve ser no minimo 1")
        Integer currentSemester,

        Long courseId
) {
}
