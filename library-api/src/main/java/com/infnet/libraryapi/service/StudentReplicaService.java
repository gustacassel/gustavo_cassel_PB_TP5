package com.infnet.libraryapi.service;

import com.infnet.libraryapi.messaging.StudentEvent;
import com.infnet.libraryapi.model.StudentReplica;
import com.infnet.libraryapi.repository.StudentReplicaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentReplicaService {
    private static final Logger log = LoggerFactory.getLogger(StudentReplicaService.class);

    private final StudentReplicaRepository repository;

    public StudentReplicaService(StudentReplicaRepository repository) {
        this.repository = repository;
    }

    public List<StudentReplica> findAll() {
        return repository.findByDeletedFalseOrderByNameAsc();
    }

    public Optional<StudentReplica> findById(Long id) {
        return repository.findByIdAndDeletedFalse(id);
    }

    public Map<Long, StudentReplica> indexByIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream()
                .collect(Collectors.toMap(StudentReplica::getId, Function.identity()));
    }

    public long count() {
        return repository.countByDeletedFalse();
    }

    public Optional<Instant> lastEventAt() {
        return repository.findFirstByOrderByLastEventAtDesc().map(StudentReplica::getLastEventAt);
    }

    @Transactional
    public boolean apply(StudentEvent event) {
        var current = repository.findById(event.studentId());

        // mesma versao e aplicada de novo (idempotente); so versao menor e descartada
        if (current.isPresent() && event.version() < current.get().getVersion()) {
            log.info("Evento {} ignorado: aluno {} ja esta na versao {} (evento v{})",
                    event.type(), event.studentId(), current.get().getVersion(), event.version());
            return false;
        }

        var replica = current.orElseGet(StudentReplica::new);
        replica.setId(event.studentId());
        replica.setName(event.name());
        replica.setEmail(event.email());
        replica.setEnrollmentNumber(event.enrollmentNumber());
        replica.setStatus(event.status());
        replica.setCourseId(event.courseId());
        replica.setCourseName(event.courseName());
        replica.setVersion(event.version());
        replica.setDeleted(event.isDeletion());
        replica.setLastEventAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
        repository.save(replica);

        log.info("Evento {} aplicado: aluno {} v{}", event.type(), event.studentId(), event.version());
        return true;
    }
}
