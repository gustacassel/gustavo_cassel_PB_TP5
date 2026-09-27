package com.infnet.libraryapi.service;

import com.infnet.libraryapi.model.AuditAction;
import com.infnet.libraryapi.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integracao do historico de mudancas: toda operacao de escrita
 * feita pela camada de servico deve gerar um registro consultavel de auditoria.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Transactional
class DataHistoryIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private AuditService auditService;

    private Book newBook() {
        var book = new Book();
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setIsbn("9780132350884");
        return book;
    }

    @Test
    void shouldRecordCreateUpdateAndDeleteHistory() {
        var saved = bookService.save(newBook());

        var details = new Book();
        details.setTitle("Clean Code 2nd Edition");
        details.setAuthor("Robert C. Martin");
        details.setIsbn("9780132350884");
        bookService.update(saved.getId(), details);

        bookService.delete(saved.getId());

        var history = auditService.findByEntityAndId("BOOK", saved.getId());

        assertThat(history).hasSize(3);
        assertThat(history)
                .extracting(log -> log.getAction())
                .containsExactlyInAnyOrder(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE);
        assertThat(history)
                .filteredOn(log -> log.getAction() == AuditAction.UPDATE)
                .first()
                .satisfies(log -> assertThat(log.getDetails())
                        .contains("title")
                        .contains("Clean Code 2nd Edition"));
        assertThat(history).allMatch(log -> log.getTimestamp() != null);
    }

    @Test
    void shouldPopulateCreatedAtAndUpdatedAtOnEntity() {
        var saved = bookService.save(newBook());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
