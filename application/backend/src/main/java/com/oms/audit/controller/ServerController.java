package com.oms.audit.controller;

import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.ServerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return serverInfoRepository.findByAssignedUserId(userId);
    }

    @PostMapping
    public ServerInfo addServer(@RequestBody ServerInfo server) {
        return serverInfoRepository.save(server);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignServer(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        return serverInfoRepository.findById(id).map(server -> {
            server.setAssignedUserId(userId);
            return ResponseEntity.ok(serverInfoRepository.save(server));
        }).orElse(ResponseEntity.notFound().build());
    }
}
