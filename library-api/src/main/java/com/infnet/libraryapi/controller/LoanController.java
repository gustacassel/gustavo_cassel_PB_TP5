package com.infnet.libraryapi.controller;

import com.infnet.libraryapi.dto.LoanRequest;
import com.infnet.libraryapi.dto.LoanResponse;
import com.infnet.libraryapi.model.LoanStatus;
import com.infnet.libraryapi.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public final class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanResponse> getAll() {
        return loanService.enrich(loanService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getById(@PathVariable Long id) {
        return loanService.findById(id)
                .flatMap(loanService::enrich)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public List<LoanResponse> getByStatus(@PathVariable LoanStatus status) {
        return loanService.enrich(loanService.findByStatus(status));
    }

    @GetMapping("/student/{studentId}")
    public List<LoanResponse> getByStudent(@PathVariable Long studentId) {
        return loanService.enrich(loanService.findByStudent(studentId));
    }

    @GetMapping("/book/{bookId}")
    public List<LoanResponse> getByBook(@PathVariable Long bookId) {
        return loanService.enrich(loanService.findByBook(bookId));
    }

    @GetMapping("/overdue")
    public List<LoanResponse> getOverdue() {
        return loanService.enrich(loanService.findOverdue());
    }

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(request));
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<LoanResponse> returnLoan(@PathVariable Long id) {
        return loanService.returnLoan(id)
                .flatMap(loanService::enrich)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (loanService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
