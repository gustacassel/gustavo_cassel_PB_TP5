package com.infnet.libraryapi.service;

import com.infnet.libraryapi.model.AuditAction;
import com.infnet.libraryapi.model.AuditLog;
import com.infnet.libraryapi.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servico responsavel por registrar e consultar o historico de mudancas
 * dos dados da aplicacao (auditoria/rastreabilidade).
 */
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
