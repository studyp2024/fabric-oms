package com.oms.audit.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Set;
import java.util.HashSet;

@Entity
@Data
@Table(name = "servers")
public class ServerInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    @Column(columnDefinition = "int default 22")
    private int sshPort = 22;

    private String sshUser;
    private String sshPassword;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "server_users", joinColumns = @JoinColumn(name = "server_id"))
    @Column(name = "user_id")
    private Set<Long> assignedUserIds = new HashSet<>();

    @Column(columnDefinition = "bigint default 0")
    private long lastLogOffset = 0;

    private String status = "OFFLINE";
}
