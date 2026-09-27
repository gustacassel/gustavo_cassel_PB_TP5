package com.infnet.studentsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microsservico de cadastro de estudantes e cursos (TP3), extraido do
 * monolito library-api - que consome estes endpoints via Spring Cloud OpenFeign.
 */
@SpringBootApplication
public class StudentsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentsApiApplication.class, args);
    }

}
