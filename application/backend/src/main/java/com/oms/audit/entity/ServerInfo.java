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
}
