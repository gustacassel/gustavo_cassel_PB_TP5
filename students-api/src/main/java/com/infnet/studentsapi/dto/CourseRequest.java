package com.infnet.studentsapi.dto;

import com.infnet.studentsapi.model.DegreeLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "O nome do curso e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String name,

        @NotBlank(message = "O codigo do curso e obrigatorio")
        @Size(max = 20, message = "O codigo deve ter no maximo 20 caracteres")
        String code,

        @NotNull(message = "O nivel do curso e obrigatorio")
        DegreeLevel degreeLevel,

        @NotNull(message = "A duracao em semestres e obrigatoria")
        @Min(value = 1, message = "A duracao minima e de 1 semestre")
        @Max(value = 20, message = "A duracao maxima e de 20 semestres")
        Integer durationSemesters,

        @Size(max = 100, message = "O departamento deve ter no maximo 100 caracteres")
        String department
) {
}
