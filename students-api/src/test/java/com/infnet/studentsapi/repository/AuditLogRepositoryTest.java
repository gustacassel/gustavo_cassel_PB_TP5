package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.config.JpaAuditingConfig;
import com.infnet.studentsapi.model.AuditAction;
import com.infnet.studentsapi.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository repository;

    private AuditLog newLog(String entityName, Long entityId, AuditAction action, String details) {
        var log = new AuditLog();
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        return repository.save(log);
    }

    @Test
    void shouldSaveLogWithGeneratedTimestamp() {
        var saved = newLog("STUDENT", 1L, AuditAction.CREATE, "Aluno cadastrado");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void shouldFindByEntityNameIgnoringCase() {
        newLog("STUDENT", 1L, AuditAction.CREATE, "Aluno cadastrado");
        newLog("COURSE", 1L, AuditAction.CREATE, "Curso cadastrado");
        newLog("STUDENT", 2L, AuditAction.UPDATE, "Aluno atualizado");

        assertThat(repository.findByEntityNameIgnoreCaseOrderByTimestampDesc("student")).hasSize(2);
        assertThat(repository.findByEntityNameIgnoreCaseOrderByTimestampDesc("COURSE")).hasSize(1);
    }

    @Test
    void shouldFindFullHistoryOfASingleEntity() {
        newLog("STUDENT", 7L, AuditAction.CREATE, "Aluno cadastrado");
        newLog("STUDENT", 7L, AuditAction.UPDATE, "status: 'ATIVO' -> 'TRANCADO'");
        newLog("STUDENT", 7L, AuditAction.DELETE, "Aluno removido");
        newLog("STUDENT", 8L, AuditAction.CREATE, "Outro aluno");

        var history = repository.findByEntityNameIgnoreCaseAndEntityIdOrderByTimestampDesc("STUDENT", 7L);

        assertThat(history).hasSize(3);
        assertThat(history)
                .extracting(AuditLog::getAction)
                .containsExactlyInAnyOrder(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE);
    }

    @Test
    void shouldListAllLogs() {
        newLog("STUDENT", 1L, AuditAction.CREATE, "Aluno cadastrado");
        newLog("COURSE", 1L, AuditAction.CREATE, "Curso cadastrado");

        assertThat(repository.findAllByOrderByTimestampDesc()).hasSize(2);
    }
}
