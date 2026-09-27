package com.infnet.studentsapi.controller;

import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.exception.BusinessException;
import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.model.Student;
import com.infnet.studentsapi.model.StudentStatus;
import com.infnet.studentsapi.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    private Student sampleStudent() {
        var course = new Course();
        course.setId(1L);
        course.setName("Engenharia de Software");
        course.setCode("ESW");
        course.setDegreeLevel(DegreeLevel.GRADUACAO);
        course.setDurationSemesters(8);

        var student = new Student();
        student.setId(10L);
        student.setName("Maria Silva");
        student.setEmail("maria@infnet.edu.br");
        student.setEnrollmentNumber("2026001");
        student.setBirthDate(LocalDate.of(2001, 3, 12));
        student.setEnrollmentDate(LocalDate.of(2024, 2, 1));
        student.setStatus(StudentStatus.ATIVO);
        student.setCurrentSemester(5);
        student.setCourse(course);
        return student;
    }

    private StudentRequest sampleRequest() {
        return new StudentRequest("Maria Silva", "maria@infnet.edu.br", "2026001",
                LocalDate.of(2001, 3, 12), LocalDate.of(2024, 2, 1), StudentStatus.ATIVO, 5, 1L);
    }

    @Test
    void shouldListStudentsWithNestedCourse() throws Exception {
        given(studentService.findAll()).willReturn(List.of(sampleStudent()));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$[0].status").value("ATIVO"))
                .andExpect(jsonPath("$[0].course.code").value("ESW"));
    }

    @Test
    void shouldReturn404WhenStudentDoesNotExist() throws Exception {
        given(studentService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/students/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFilterByStatus() throws Exception {
        given(studentService.findByStatus(StudentStatus.ATIVO)).willReturn(List.of(sampleStudent()));

        mockMvc.perform(get("/api/students/status/ATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldFilterByCourse() throws Exception {
        given(studentService.findByCourse(1L)).willReturn(List.of(sampleStudent()));

        mockMvc.perform(get("/api/students/course/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].course.id").value(1));
    }

    @Test
    void shouldCreateStudentAndReturn201() throws Exception {
        given(studentService.create(any(StudentRequest.class))).willReturn(sampleStudent());

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        var invalid = new StudentRequest("", "nao-e-email", "", null, null, null, null, null);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn409WhenBusinessRuleIsViolated() throws Exception {
        given(studentService.create(any(StudentRequest.class)))
                .willThrow(new BusinessException("Ja existe um estudante com o email 'maria@infnet.edu.br'"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ja existe um estudante com o email 'maria@infnet.edu.br'"));
    }

    @Test
    void shouldDeleteStudent() throws Exception {
        given(studentService.delete(eq(10L))).willReturn(true);

        mockMvc.perform(delete("/api/students/10"))
                .andExpect(status().isNoContent());
    }
}
