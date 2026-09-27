package com.infnet.libraryapi.messaging;

import com.infnet.libraryapi.dto.LoanRequest;
import com.infnet.libraryapi.exception.BusinessException;
import com.infnet.libraryapi.model.Book;
import com.infnet.libraryapi.service.BookService;
import com.infnet.libraryapi.service.LoanService;
import com.infnet.libraryapi.service.StudentReplicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@RecordApplicationEvents
@Transactional
class LoanEventPublishingTest {

    private static final Long STUDENT_ID = 42L;

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookService bookService;

    @Autowired
    private StudentReplicaService studentReplicaService;

    @Autowired
    private LoanEventPublisher publisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private Book book;

    @BeforeEach
    void setUp() {
        var newBook = new Book();
        newBook.setTitle("Domain-Driven Design");
        newBook.setAuthor("Eric Evans");
        book = bookService.save(newBook);
        studentReplicaService.apply(new StudentEvent(UUID.randomUUID(), "CREATED", Instant.now(), STUDENT_ID, 0L,
                "Maria Silva", "maria@infnet.edu.br", "2026001", "ATIVO", 1L, "Engenharia de Software"));
    }

    private List<LoanEvent> loanEvents() {
        return applicationEvents.stream(LoanEvent.class).toList();
    }

    @Test
    void shouldRegisterAnEventForEachStepOfTheLoan() {
        var created = loanService.create(new LoanRequest(book.getId(), STUDENT_ID, null, null));
        loanService.returnLoan(created.id());
        loanService.delete(created.id());

        assertThat(loanEvents())
                .extracting(LoanEvent::type)
                .containsExactly(LoanEventType.CREATED, LoanEventType.RETURNED, LoanEventType.DELETED);
        assertThat(loanEvents()).allSatisfy(event -> {
            assertThat(event.loanId()).isEqualTo(created.id());
            assertThat(event.studentId()).isEqualTo(STUDENT_ID);
            assertThat(event.bookTitle()).isEqualTo("Domain-Driven Design");
        });
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldNotRegisterAnEventWhenTheLoanIsRejected() {
        assertThatThrownBy(() -> loanService.create(new LoanRequest(book.getId(), 999L, null, null)))
                .isInstanceOf(BusinessException.class);

        assertThat(loanEvents()).isEmpty();
    }

    @Test
    void shouldBlockBookRemovalWhileItHasActiveLoans() {
        var created = loanService.create(new LoanRequest(book.getId(), STUDENT_ID, null, null));

        assertThatThrownBy(() -> bookService.delete(book.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("emprestimos ativos");

        loanService.returnLoan(created.id());
        assertThat(bookService.delete(book.getId())).isTrue();
    }

    @Test
    void shouldPublishWithTheRoutingKeyOfTheEventType() {
        var loan = loanService.create(new LoanRequest(book.getId(), STUDENT_ID, null, null));
        var event = loanEvents().getFirst();

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(eq("library.events"), eq("loan.created"), eq(event),
                any(MessagePostProcessor.class));
        assertThat(event.loanId()).isEqualTo(loan.id());
    }

    @Test
    void shouldNotPropagateBrokerFailures() {
        loanService.create(new LoanRequest(book.getId(), STUDENT_ID, null, null));
        var event = loanEvents().getFirst();
        willThrow(new AmqpConnectException(new ConnectException("refused")))
                .given(rabbitTemplate)
                .convertAndSend(eq("library.events"), eq("loan.created"), eq(event), any(MessagePostProcessor.class));

        publisher.publish(event);
    }
}
