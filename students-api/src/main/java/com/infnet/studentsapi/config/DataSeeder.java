package com.infnet.studentsapi.config;

import com.infnet.studentsapi.dto.CourseRequest;
import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.model.StudentStatus;
import com.infnet.studentsapi.repository.CourseRepository;
import com.infnet.studentsapi.service.CourseService;
import com.infnet.studentsapi.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** O banco H2 e em memoria: sem isso a aplicacao sobe vazia a cada restart. */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final StudentService studentService;

    public DataSeeder(CourseRepository courseRepository,
                      CourseService courseService,
                      StudentService studentService) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.studentService = studentService;
    }

    @Override
    public void run(String... args) {
        if (courseRepository.count() > 0) {
            return;
        }

        var engenharia = courseService.create(new CourseRequest(
                "Engenharia de Software", "ESW", DegreeLevel.GRADUACAO, 8, "Tecnologia"));
        var dados = courseService.create(new CourseRequest(
                "Ciencia de Dados", "CDD", DegreeLevel.GRADUACAO, 8, "Tecnologia"));
        var arquitetura = courseService.create(new CourseRequest(
                "Arquitetura de Sistemas Distribuidos", "ASD", DegreeLevel.POS_GRADUACAO, 3, "Tecnologia"));

        studentService.create(new StudentRequest(
                "Maria Silva", "maria.silva@infnet.edu.br", "2026001",
                LocalDate.of(2001, 3, 12), LocalDate.of(2024, 2, 1),
                StudentStatus.ATIVO, 5, engenharia.getId()));
        studentService.create(new StudentRequest(
                "Joao Souza", "joao.souza@infnet.edu.br", "2026002",
                LocalDate.of(2000, 8, 30), LocalDate.of(2023, 8, 1),
                StudentStatus.ATIVO, 6, dados.getId()));
        studentService.create(new StudentRequest(
                "Ana Ferreira", "ana.ferreira@infnet.edu.br", "2026003",
                LocalDate.of(1995, 11, 5), LocalDate.of(2025, 2, 1),
                StudentStatus.ATIVO, 2, arquitetura.getId()));
        studentService.create(new StudentRequest(
                "Pedro Santos", "pedro.santos@infnet.edu.br", "2026004",
                LocalDate.of(1999, 1, 20), LocalDate.of(2022, 2, 1),
                StudentStatus.TRANCADO, 4, engenharia.getId()));
    }
}
