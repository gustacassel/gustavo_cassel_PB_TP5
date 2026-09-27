package com.infnet.libraryapi.service;

import com.infnet.libraryapi.messaging.StudentEvent;
import com.infnet.libraryapi.repository.StudentReplicaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Transactional
class StudentReplicaServiceTest {

    private static final Long STUDENT_ID = 42L;

    @Autowired
    private StudentReplicaService service;

    @Autowired
    private StudentReplicaRepository repository;

    private StudentEvent event(String type, long version, String status, String courseName) {
        return new StudentEvent(UUID.randomUUID(), type, Instant.now(), STUDENT_ID, version,
                "Maria Silva", "maria@infnet.edu.br", "2026001", status, 1L, courseName);
    }

    @Test
    void shouldInsertTheStudentOnCreatedEvent() {
        assertThat(service.apply(event("CREATED", 0, "ATIVO", "Engenharia de Software"))).isTrue();

        var replica = service.findById(STUDENT_ID);
        assertThat(replica).isPresent();
        assertThat(replica.get().getName()).isEqualTo("Maria Silva");
        assertThat(replica.get().getCourseName()).isEqualTo("Engenharia de Software");
        assertThat(replica.get().isActive()).isTrue();
        assertThat(replica.get().getLastEventAt()).isNotNull();
    }

    @Test
    void shouldApplyNewerVersions() {
        service.apply(event("CREATED", 0, "ATIVO", "Engenharia de Software"));
        service.apply(event("UPDATED", 1, "TRANCADO", "Engenharia de Software"));

        var replica = service.findById(STUDENT_ID).orElseThrow();
        assertThat(replica.getVersion()).isEqualTo(1L);
        assertThat(replica.getStatus()).isEqualTo("TRANCADO");
        assertThat(replica.isActive()).isFalse();
    }

    @Test
    void shouldIgnoreEventsThatArriveOutOfOrder() {
        service.apply(event("UPDATED", 2, "TRANCADO", "Engenharia de Software"));

        assertThat(service.apply(event("UPDATED", 1, "ATIVO", "Engenharia de Software"))).isFalse();

        assertThat(service.findById(STUDENT_ID).orElseThrow().getStatus()).isEqualTo("TRANCADO");
    }

    @Test
    void shouldBeIdempotentWhenTheSameEventIsDeliveredTwice() {
        var created = event("CREATED", 0, "ATIVO", "Engenharia de Software");

        service.apply(created);
        service.apply(created);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(service.findById(STUDENT_ID).orElseThrow().getVersion()).isZero();
    }

    @Test
    void shouldAcceptTheSameVersionWhenTheCourseIsRenamed() {
        service.apply(event("CREATED", 0, "ATIVO", "Engenharia de Software"));

        service.apply(event("UPDATED", 0, "ATIVO", "Engenharia de Software Moderna"));

        assertThat(service.findById(STUDENT_ID).orElseThrow().getCourseName())
                .isEqualTo("Engenharia de Software Moderna");
    }

    @Test
    void shouldKeepATombstoneOnDeleteSoLateEventsCannotRecreateTheStudent() {
        service.apply(event("CREATED", 0, "ATIVO", "Engenharia de Software"));
        service.apply(event("DELETED", 2, "ATIVO", "Engenharia de Software"));

        assertThat(service.findById(STUDENT_ID)).isEmpty();
        assertThat(service.findAll()).isEmpty();
        assertThat(service.count()).isZero();

        // um UPDATED v1 atrasado chega depois do DELETED v2
        assertThat(service.apply(event("UPDATED", 1, "ATIVO", "Engenharia de Software"))).isFalse();
        assertThat(service.findById(STUDENT_ID)).isEmpty();
        // o nome continua disponivel para o historico dos emprestimos
        assertThat(service.indexByIds(List.of(STUDENT_ID))).containsKey(STUDENT_ID);
    }
}
