package com.oms.audit.repository;

import com.oms.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    Page<AuditLog> findByIsSensitiveTrue(Pageable pageable);
    Page<AuditLog> findByCommandContaining(String keyword, Pageable pageable);
    Page<AuditLog> findAll(Pageable pageable);
}
