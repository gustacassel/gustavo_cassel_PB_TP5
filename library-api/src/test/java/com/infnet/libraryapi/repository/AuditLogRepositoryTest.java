package com.infnet.libraryapi.repository;

import com.infnet.libraryapi.config.JpaAuditingConfig;
import com.infnet.libraryapi.model.AuditAction;
import com.infnet.libraryapi.model.AuditLog;
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
        return log;
    }

    @Test
    void shouldSaveLogWithAutomaticTimestamp() {
        var saved = repository.save(newLog("BOOK", 1L, AuditAction.CREATE, "Livro cadastrado"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void shouldFindHistoryByEntityName() {
        repository.save(newLog("BOOK", 1L, AuditAction.CREATE, "Livro cadastrado"));
        repository.save(newLog("BOOK", 1L, AuditAction.UPDATE, "title alterado"));
        repository.save(newLog("STUDENT", 7L, AuditAction.CREATE, "Aluno cadastrado"));

        var bookHistory = repository.findByEntityNameIgnoreCaseOrderByTimestampDesc("book");

        assertThat(bookHistory).hasSize(2);
        assertThat(bookHistory).allMatch(log -> log.getEntityName().equals("BOOK"));
    }

    @Test
    void shouldFindHistoryOfSingleRecord() {
        repository.save(newLog("BOOK", 1L, AuditAction.CREATE, "Livro cadastrado"));
        repository.save(newLog("BOOK", 1L, AuditAction.UPDATE, "title: 'a' -> 'b'"));
        repository.save(newLog("BOOK", 2L, AuditAction.CREATE, "Outro livro"));

        var history = repository.findByEntityNameIgnoreCaseAndEntityIdOrderByTimestampDesc("BOOK", 1L);

        assertThat(history).hasSize(2);
        assertThat(history).allMatch(log -> log.getEntityId().equals(1L));
    }
}
