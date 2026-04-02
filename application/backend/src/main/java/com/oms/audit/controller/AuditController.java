package com.oms.audit.controller;

import com.oms.audit.entity.AuditLog;
import com.oms.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志控制器
 * 提供审计日志的查询、分页和搜索等 RESTful API 接口
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*") // 允许跨域请求
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * 获取所有审计日志（分页）
     * 默认按时间戳降序排列
     *
     * @param page 当前页码，从 0 开始（默认 0）
     * @param size 每页记录数（默认 10）
     * @return 包含审计日志数据的分页对象
     */
    @GetMapping
    public Page<AuditLog> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findAll(pageable);
    }

    /**
     * 获取所有被标记为敏感命令的审计日志（分页）
     * 默认按时间戳降序排列
     *
     * @param page 当前页码，从 0 开始（默认 0）
     * @param size 每页记录数（默认 10）
     * @return 包含敏感审计日志数据的分页对象
     */
    @GetMapping("/sensitive")
    public Page<AuditLog> getSensitiveLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByIsSensitiveTrue(pageable);
    }

    /**
     * 根据命令内容搜索相关的审计日志（分页）
     * 默认按时间戳降序排列
     *
     * @param q 搜索关键词（命令中包含的内容）
     * @param page 当前页码，从 0 开始（默认 0）
     * @param size 每页记录数（默认 10）
     * @return 匹配的审计日志分页对象
     */
    @GetMapping("/search")
    public Page<AuditLog> searchLogs(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByCommandContaining(q, pageable);
    }
}
