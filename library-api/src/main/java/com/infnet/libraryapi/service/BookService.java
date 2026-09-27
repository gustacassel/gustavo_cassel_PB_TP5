package com.infnet.libraryapi.service;

import com.infnet.libraryapi.exception.BusinessException;
import com.infnet.libraryapi.model.AuditAction;
import com.infnet.libraryapi.model.Book;
import com.infnet.libraryapi.model.LoanStatus;
import com.infnet.libraryapi.repository.BookRepository;
import com.infnet.libraryapi.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class BookService {
    private static final String ENTITY_NAME = "BOOK";

    private final BookRepository repository;
    private final LoanRepository loanRepository;
    private final AuditService auditService;

    public BookService(BookRepository repository, LoanRepository loanRepository, AuditService auditService) {
        this.repository = repository;
        this.loanRepository = loanRepository;
        this.auditService = auditService;
    }

    public List<Book> findAll() {
        return repository.findAll();
    }

    public Optional<Book> findById(Long id) {
        return repository.findById(id);
    }

    public List<Book> findByTitle(String title) {
        return repository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> findByAuthor(String author) {
        return repository.findByAuthorContainingIgnoreCase(author);
    }

    @Transactional
    public Book save(Book book) {
        var saved = repository.save(book);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE,
                "Livro cadastrado: '%s' (%s)".formatted(saved.getTitle(), saved.getAuthor()));
        return saved;
    }

    @Transactional
    public Optional<Book> update(Long id, Book bookDetails) {
        var book = repository.findById(id);

        if (book.isEmpty()) {
            return Optional.empty();
        }

        var updatedBook = book.get();
        var changes = new StringBuilder();
        appendChange(changes, "title", updatedBook.getTitle(), bookDetails.getTitle());
        appendChange(changes, "author", updatedBook.getAuthor(), bookDetails.getAuthor());
        appendChange(changes, "isbn", updatedBook.getIsbn(), bookDetails.getIsbn());
        appendChange(changes, "publicationYear", updatedBook.getPublicationYear(), bookDetails.getPublicationYear());

        updatedBook.setTitle(bookDetails.getTitle());
        updatedBook.setAuthor(bookDetails.getAuthor());
        updatedBook.setIsbn(bookDetails.getIsbn());
        updatedBook.setPublicationYear(bookDetails.getPublicationYear());

        var saved = repository.save(updatedBook);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.UPDATE,
                changes.isEmpty() ? "Nenhum campo alterado" : changes.toString());
        return Optional.of(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        var book = repository.findById(id);
        if (book.isEmpty()) {
            return false;
        }

        if (loanRepository.existsByBookIdAndStatus(id, LoanStatus.ACTIVE)) {
            throw new BusinessException(
                    "Nao e possivel remover o livro '%s': existem emprestimos ativos dele"
                            .formatted(book.get().getTitle()));
        }

        repository.delete(book.get());
        auditService.record(ENTITY_NAME, id, AuditAction.DELETE,
                "Livro removido: '%s'".formatted(book.get().getTitle()));
        return true;
    }

    private void appendChange(StringBuilder changes, String field, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            if (!changes.isEmpty()) {
                changes.append("; ");
            }
            changes.append("%s: '%s' -> '%s'".formatted(field, oldValue, newValue));
        }
    }
}
