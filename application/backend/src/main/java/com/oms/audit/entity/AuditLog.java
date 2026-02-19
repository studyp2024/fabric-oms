package com.oms.audit.entity;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    private String id; // UUID

    private String timestamp;

    private String ip;

    private String user;

    private String pwd;

    private String command;

    private boolean isSensitive;

    private String hash;
}
