package com.infnet.libraryapi.controller;

import com.infnet.libraryapi.model.StudentReplica;
import com.infnet.libraryapi.service.StudentReplicaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Somente leitura: a library-api nao e dona destes dados, apenas guarda a copia
 * que recebe pelos eventos da students-api.
 */
@RestController
@RequestMapping("/api/integration/students")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public final class StudentDirectoryController {
    private final StudentReplicaService studentReplicaService;

    public StudentDirectoryController(StudentReplicaService studentReplicaService) {
        this.studentReplicaService = studentReplicaService;
    }

    @GetMapping
    public List<StudentReplica> getAll() {
        return studentReplicaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentReplica> getById(@PathVariable Long id) {
        return studentReplicaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("source", "students.events");
        payload.put("studentCount", studentReplicaService.count());
        payload.put("lastEventAt", studentReplicaService.lastEventAt().orElse(null));
        return payload;
    }
}
