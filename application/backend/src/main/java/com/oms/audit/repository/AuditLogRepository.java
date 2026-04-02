package com.oms.audit.repository;

import com.oms.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 审计日志的数据访问层接口
 * 继承 JpaRepository，提供对 AuditLog 实体类的基本 CRUD 和分页查询功能
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    /**
     * 分页查询所有被标记为敏感命令的审计日志
     *
     * @param pageable 分页和排序参数
     * @return 包含敏感审计日志的分页对象
     */
    Page<AuditLog> findByIsSensitiveTrue(Pageable pageable);

    /**
     * 根据命令内容模糊查询相关的审计日志（分页）
     *
     * @param keyword  命令中包含的关键词
     * @param pageable 分页和排序参数
     * @return 匹配关键词的审计日志分页对象
     */
    Page<AuditLog> findByCommandContaining(String keyword, Pageable pageable);

    /**
     * 分页查询所有审计日志
     *
     * @param pageable 分页和排序参数
     * @return 包含所有审计日志的分页对象
     */
    Page<AuditLog> findAll(Pageable pageable);
}
