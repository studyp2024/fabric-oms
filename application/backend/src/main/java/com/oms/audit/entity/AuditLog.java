package com.oms.audit.entity;

import javax.persistence.*;
import lombok.Data;

/**
 * 审计日志实体类
 * 映射数据库中的 audit_logs 表，记录所有的操作日志信息
 */
@Entity
@Data
@Table(name = "audit_logs")
public class AuditLog {
    
    // 日志唯一标识，通常使用哈希值
    @Id
    private String id;

    // 操作发生的时间戳
    private String timestamp;

    // 发起操作的客户端 IP（谁连接的）
    private String ip;

    // 执行命令的目标服务器 IP（命令在哪里运行）
    private String serverIp;

    // 执行命令的 SSH 用户名
    private String user;

    // 执行命令时的当前工作目录（Print Working Directory）
    private String pwd;

    // 执行的具体命令内容
    private String command;

    // 是否为敏感/高危命令
    private boolean isSensitive;

    // 原始日志内容的哈希值，用于防篡改校验
    private String hash;
}
