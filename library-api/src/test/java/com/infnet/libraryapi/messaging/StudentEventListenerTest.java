package com.infnet.libraryapi.messaging;

import com.infnet.libraryapi.service.StudentReplicaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentEventListenerTest {

    @Mock
    private StudentReplicaService replicaService;

    @InjectMocks
    private StudentEventListener listener;

    @Test
    void shouldDelegateTheEventToTheReplica() {
        var event = new StudentEvent(UUID.randomUUID(), "CREATED", Instant.now(), 1L, 0L,
                "Maria Silva", "maria@infnet.edu.br", "2026001", "ATIVO", 1L, "Engenharia de Software");

        listener.onStudentEvent(event);

        verify(replicaService).apply(event);
    }

    @Test
    void shouldReadTheJsonPublishedByTheStudentsApi() {
        // payload no formato exato publicado pela students-api
        var json = """
                {"eventId":"f5c33441-5c7c-41b3-bff7-3c60ddc988f0","type":"UPDATED",
                 "occurredAt":"2026-09-27T20:30:27.968107900Z","studentId":8,"version":3,
                 "name":"Joao Souza","email":"joao@infnet.edu.br","enrollmentNumber":"2026002",
                 "status":"TRANCADO","courseId":2,"courseName":"Ciencia de Dados"}
                """;
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(StudentEvent.class);
        var message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

        var event = (StudentEvent) new JacksonJsonMessageConverter().fromMessage(message);

        assertThat(event.studentId()).isEqualTo(8L);
        assertThat(event.version()).isEqualTo(3L);
        assertThat(event.status()).isEqualTo("TRANCADO");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-27T20:30:27.968107900Z"));
        assertThat(event.isDeletion()).isFalse();
    }
}
