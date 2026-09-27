package com.infnet.studentsapi.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StudentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(StudentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public StudentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(StudentEvent event) {
        try {
            rabbitTemplate.convertAndSend(MessagingConfig.STUDENTS_EXCHANGE, event.type().routingKey(), event,
                    message -> {
                        message.getMessageProperties().setMessageId(event.eventId().toString());
                        return message;
                    });
            log.info("Evento {} publicado: aluno {} (versao {})",
                    event.type().routingKey(), event.studentId(), event.version());
        } catch (AmqpException e) {
            log.error("Falha ao publicar {} do aluno {}: {}",
                    event.type().routingKey(), event.studentId(), e.getMessage());
        }
    }
}
