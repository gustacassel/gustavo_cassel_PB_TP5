package com.infnet.libraryapi.repository;

import com.infnet.libraryapi.config.JpaAuditingConfig;
import com.infnet.libraryapi.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BookRepositoryTest {

    @Autowired
    private BookRepository repository;

    private Book newBook(String title, String author, String isbn) {
        var book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        return book;
    }

    @Test
    void shouldSaveAndFindBookById() {
        var saved = repository.save(newBook("Clean Code", "Robert C. Martin", "9780132350884"));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
        assertThat(found.get().getAuthor()).isEqualTo("Robert C. Martin");
    }

    @Test
    void shouldPopulateAuditingFieldsOnSave() {
        var saved = repository.save(newBook("Domain-Driven Design", "Eric Evans", "9780321125217"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByTitleContainingIgnoreCase() {
        repository.save(newBook("Clean Code", "Robert C. Martin", "9780132350884"));
        repository.save(newBook("Clean Architecture", "Robert C. Martin", "9780134494166"));
        repository.save(newBook("Refactoring", "Martin Fowler", "9780134757599"));

        var found = repository.findByTitleContainingIgnoreCase("clean");

        assertThat(found).hasSize(2);
    }

    @Test
    void shouldFindByAuthorContainingIgnoreCase() {
        repository.save(newBook("Refactoring", "Martin Fowler", "9780134757599"));

        var found = repository.findByAuthorContainingIgnoreCase("fowler");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTitle()).isEqualTo("Refactoring");
    }

    @Test
    void shouldFindByIsbn() {
        repository.save(newBook("Clean Code", "Robert C. Martin", "9780132350884"));

        var found = repository.findByIsbn("9780132350884");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void shouldDeleteBook() {
        var saved = repository.save(newBook("Clean Code", "Robert C. Martin", "9780132350884"));

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
