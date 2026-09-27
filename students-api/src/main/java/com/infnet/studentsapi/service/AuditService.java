package com.infnet.studentsapi.service;

import com.infnet.studentsapi.model.AuditAction;
import com.infnet.studentsapi.model.AuditLog;
import com.infnet.studentsapi.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String entityName, Long entityId, AuditAction action, String details) {
        var log = new AuditLog();
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        repository.save(log);
    }

    public List<AuditLog> findAll() {
        return repository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> findByEntity(String entityName) {
        return repository.findByEntityNameIgnoreCaseOrderByTimestampDesc(entityName);
    }

    public List<AuditLog> findByEntityAndId(String entityName, Long entityId) {
        return repository.findByEntityNameIgnoreCaseAndEntityIdOrderByTimestampDesc(entityName, entityId);
    }
}
