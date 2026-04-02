package com.oms.audit.controller;

import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.ServerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务器信息控制器
 * 提供纳管服务器的增删改查以及分配用户的 RESTful API 接口
 */
@RestController
@RequestMapping("/api/servers")
@CrossOrigin(origins = "*") // 允许跨域请求
public class ServerController {

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    /**
     * 获取系统中所有配置的服务器列表
     *
     * @return 服务器信息列表
     */
    @GetMapping
    public List<ServerInfo> getAllServers() {
        return serverInfoRepository.findAll();
    }

    /**
     * 根据分配的用户 ID 获取对应的服务器列表
     *
     * @param userId 用户的唯一标识 ID
     * @return 分配给该用户的服务器列表
     */
    @GetMapping("/user/{userId}")
    public List<ServerInfo> getServersByUserId(@PathVariable Long userId) {
        return serverInfoRepository.findByAssignedUserId(userId);
    }

    /**
     * 添加新的纳管服务器
     *
     * @param server 包含服务器信息（IP, SSH账户密码等）的请求体
     * @return 保存后的服务器信息对象（包含生成的 ID）
     */
    @PostMapping
    public ServerInfo addServer(@RequestBody ServerInfo server) {
        return serverInfoRepository.save(server);
    }

    /**
     * 将服务器分配给指定的用户
     * 如果 userId 为空或未提供，则可以视为解除分配（根据业务逻辑而定）
     *
     * @param id 目标服务器的 ID
     * @param userId 待分配的用户 ID
     * @return 如果服务器存在，返回更新后的服务器信息；否则返回 404 状态码
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignServer(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        return serverInfoRepository.findById(id).map(server -> {
            server.setAssignedUserId(userId);
            return ResponseEntity.ok(serverInfoRepository.save(server));
        }).orElse(ResponseEntity.notFound().build());
    }
}
