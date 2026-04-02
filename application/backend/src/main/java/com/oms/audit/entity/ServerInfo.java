package com.oms.audit.entity;

import javax.persistence.*;
import lombok.Data;

/**
 * 服务器信息实体类
 * 映射数据库中的 servers 表，存储纳管服务器的连接配置和状态
 */
@Entity
@Data
@Table(name = "servers")
public class ServerInfo {
    
    // 主键 ID，自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 目标服务器的 IP 地址
    private String ip;

    // 目标服务器的 SSH 端口
    @Column(columnDefinition = "int default 22")
    private int sshPort = 22;

    // 用于连接服务器的 SSH 用户名
    private String sshUser;

    // 用于连接服务器的 SSH 密码
    private String sshPassword;

    // 分配管理该服务器的用户 ID（关联到用户表）
    private Long assignedUserId;

    // 记录在该服务器上最后一次读取日志文件的字节偏移量，用于增量拉取
    @Column(columnDefinition = "bigint default 0")
    private long lastLogOffset = 0;

    // 服务器的在线状态：ONLINE 或 OFFLINE
    private String status = "OFFLINE"; 
}
