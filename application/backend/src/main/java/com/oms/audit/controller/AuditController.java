package com.oms.audit.controller;

import com.oms.audit.entity.AuditLog;
import com.oms.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @GetMapping("/sensitive")
    public List<AuditLog> getSensitiveLogs() {
        return auditLogRepository.findByIsSensitiveTrue();
    }

    @GetMapping("/search")
    public List<AuditLog> searchLogs(@RequestParam String q) {
        return auditLogRepository.findByCommandContaining(q);
    }
}
