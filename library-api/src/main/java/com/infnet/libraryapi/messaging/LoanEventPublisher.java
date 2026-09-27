package com.infnet.libraryapi.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LoanEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoanEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public LoanEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(LoanEvent event) {
        try {
            rabbitTemplate.convertAndSend(MessagingConfig.LIBRARY_EXCHANGE, event.type().routingKey(), event,
                    message -> {
                        message.getMessageProperties().setMessageId(event.eventId().toString());
                        return message;
                    });
            log.info("Evento {} publicado: emprestimo {} (aluno {})",
                    event.type().routingKey(), event.loanId(), event.studentId());
        } catch (AmqpException e) {
            log.error("Falha ao publicar {} do emprestimo {}: {}",
                    event.type().routingKey(), event.loanId(), e.getMessage());
        }
    }
}
