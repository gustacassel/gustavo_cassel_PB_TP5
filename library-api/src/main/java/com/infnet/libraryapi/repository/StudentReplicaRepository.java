package com.infnet.libraryapi.repository;

import com.infnet.libraryapi.model.StudentReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentReplicaRepository extends JpaRepository<StudentReplica, Long> {

    Optional<StudentReplica> findByIdAndDeletedFalse(Long id);

    List<StudentReplica> findByDeletedFalseOrderByNameAsc();

    long countByDeletedFalse();

    Optional<StudentReplica> findFirstByOrderByLastEventAtDesc();
}
