package com.infnet.studentsapi.controller;

import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.model.Student;
import com.infnet.studentsapi.model.StudentStatus;
import com.infnet.studentsapi.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public final class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<Student> getAll() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(@PathVariable Long id) {
        return studentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/name/{name}")
    public List<Student> getByName(@PathVariable String name) {
        return studentService.findByName(name);
    }

    @GetMapping("/enrollment/{enrollmentNumber}")
    public ResponseEntity<Student> getByEnrollmentNumber(@PathVariable String enrollmentNumber) {
        return studentService.findByEnrollmentNumber(enrollmentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public List<Student> getByStatus(@PathVariable StudentStatus status) {
        return studentService.findByStatus(status);
    }

    @GetMapping("/course/{courseId}")
    public List<Student> getByCourse(@PathVariable Long courseId) {
        return studentService.findByCourse(courseId);
    }

    @PostMapping
    public ResponseEntity<Student> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (studentService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
