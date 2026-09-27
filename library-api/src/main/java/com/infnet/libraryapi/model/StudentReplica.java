package com.infnet.libraryapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "student_replica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentReplica {
    private static final String ACTIVE_STATUS = "ATIVO";

    // mesmo id do aluno na students-api, por isso nao e gerado aqui
    @Id
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String enrollmentNumber;

    @Column(nullable = false, length = 20)
    private String status;

    private Long courseId;

    @Column(length = 150)
    private String courseName;

    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private Instant lastEventAt;

    public boolean isActive() {
        return !deleted && ACTIVE_STATUS.equalsIgnoreCase(status);
    }
}
