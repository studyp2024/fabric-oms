package com.oms.audit.repository;

import com.oms.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByIsSensitiveTrue();
    List<AuditLog> findByCommandContaining(String keyword);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
