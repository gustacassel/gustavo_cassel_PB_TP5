package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByEntityNameIgnoreCaseOrderByTimestampDesc(String entityName);

    List<AuditLog> findByEntityNameIgnoreCaseAndEntityIdOrderByTimestampDesc(String entityName, Long entityId);
}
