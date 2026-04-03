package com.oms.audit.controller;

import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.ServerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/servers")
@CrossOrigin(origins = "*")
public class ServerController {

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    @GetMapping
    public List<ServerInfo> getAllServers() {
        return serverInfoRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<ServerInfo> getServersByUserId(@PathVariable Long userId) {
        return serverInfoRepository.findByAssignedUserIdsContaining(userId);
    }

    @PostMapping
    public ServerInfo addServer(@RequestBody ServerInfo server) {
        return serverInfoRepository.save(server);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateServer(@PathVariable Long id, @RequestBody ServerInfo serverDetails) {
        return serverInfoRepository.findById(id).map(server -> {
            server.setIp(serverDetails.getIp());
            server.setSshPort(serverDetails.getSshPort());
            server.setSshUser(serverDetails.getSshUser());
            server.setSshPassword(serverDetails.getSshPassword());
            return ResponseEntity.ok(serverInfoRepository.save(server));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteServer(@PathVariable Long id) {
        return serverInfoRepository.findById(id).map(server -> {
            serverInfoRepository.delete(server);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignServer(@PathVariable Long id, @RequestBody Set<Long> userIds) {
        return serverInfoRepository.findById(id).map(server -> {
            server.setAssignedUserIds(userIds == null ? new HashSet<>() : userIds);
            return ResponseEntity.ok(serverInfoRepository.save(server));
        }).orElse(ResponseEntity.notFound().build());
    }
}
