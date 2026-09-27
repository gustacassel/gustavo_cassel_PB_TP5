package com.infnet.libraryapi.controller;

import com.infnet.libraryapi.model.AuditLog;
import com.infnet.libraryapi.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de consulta ao historico de mudancas dos dados (auditoria).
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public final class HistoryController {
    private final AuditService auditService;

    public HistoryController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLog> getAll() {
        return auditService.findAll();
    }

    @GetMapping("/{entityName}")
    public List<AuditLog> getByEntity(@PathVariable String entityName) {
        return auditService.findByEntity(entityName);
    }

    @GetMapping("/{entityName}/{entityId}")
    public List<AuditLog> getByEntityAndId(@PathVariable String entityName, @PathVariable Long entityId) {
        return auditService.findByEntityAndId(entityName, entityId);
    }
}
