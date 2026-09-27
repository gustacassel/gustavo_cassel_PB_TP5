package com.infnet.studentsapi.messaging;

import com.infnet.studentsapi.model.StudentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private StudentEventPublisher publisher;

    private StudentEvent event(StudentEventType type) {
        return new StudentEvent(UUID.randomUUID(), type, Instant.now(), 1L, 0L,
                "Maria Silva", "maria@email.com", "2026001", StudentStatus.ATIVO, 10L, "Engenharia de Software");
    }

    @Test
    void shouldSendEachEventTypeWithItsOwnRoutingKey() {
        var created = event(StudentEventType.CREATED);
        var deleted = event(StudentEventType.DELETED);

        publisher.publish(created);
        publisher.publish(deleted);

        verify(rabbitTemplate).convertAndSend(eq("students.events"), eq("student.created"), eq(created),
                any(MessagePostProcessor.class));
        verify(rabbitTemplate).convertAndSend(eq("students.events"), eq("student.deleted"), eq(deleted),
                any(MessagePostProcessor.class));
    }

    @Test
    void shouldNotPropagateBrokerFailuresToTheCaller() {
        var created = event(StudentEventType.CREATED);
        willThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .given(rabbitTemplate)
                .convertAndSend(eq("students.events"), eq("student.created"), eq(created),
                        any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.publish(created)).doesNotThrowAnyException();
    }
}
