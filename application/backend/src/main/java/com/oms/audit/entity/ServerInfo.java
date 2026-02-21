package com.oms.audit.entity;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "servers")
public class ServerInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    private String sshUser;

    private String sshPassword;

    // The user assigned to manage this server
    private Long assignedUserId;

    // Track the last read byte offset of the log file on this server
    @Column(columnDefinition = "bigint default 0")
    private long lastLogOffset = 0;

    private String status = "OFFLINE"; // ONLINE, OFFLINE
}
