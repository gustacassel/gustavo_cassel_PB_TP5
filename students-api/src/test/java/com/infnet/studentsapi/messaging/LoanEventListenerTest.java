package com.infnet.studentsapi.messaging;

import com.infnet.studentsapi.model.StudentLoanStatus;
import com.infnet.studentsapi.service.StudentLoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanEventListenerTest {

    @Mock
    private StudentLoanService studentLoanService;

    @InjectMocks
    private LoanEventListener listener;

    @Test
    void shouldReadTheJsonPublishedByTheLibraryApiAndDelegate() {
        // payload no formato publicado pela library-api
        var json = """
                {"eventId":"0b8f5a52-2f8e-4c35-9d6c-7c4f1f0b1a11","type":"RETURNED",
                 "occurredAt":"2026-09-27T21:00:00Z","loanId":5,"studentId":2,"bookId":1,
                 "bookTitle":"Clean Architecture","dueDate":"2026-10-11"}
                """;
        var properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(LoanEvent.class);

        var event = (LoanEvent) new JacksonJsonMessageConverter()
                .fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), properties));
        listener.onLoanEvent(event);

        assertThat(event.loanId()).isEqualTo(5L);
        assertThat(event.dueDate()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(event.resultingStatus()).isEqualTo(StudentLoanStatus.RETURNED);
        verify(studentLoanService).apply(event);
    }
}
