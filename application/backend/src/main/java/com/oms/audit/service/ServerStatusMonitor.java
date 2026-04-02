package com.oms.audit.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.ServerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务器状态监控服务
 * 定时检测纳管服务器的连通性状态
 */
@Service
public class ServerStatusMonitor {

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    /**
     * 定时任务：每 60 秒执行一次
     * 遍历所有配置的服务器，尝试通过 SSH 建立连接，以判断服务器是否在线
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒检查一次
    public void checkServerStatus() {
        List<ServerInfo> servers = serverInfoRepository.findAll();
        for (ServerInfo server : servers) {
            // 默认状态设为离线
            String status = "OFFLINE";
            Session session = null;
            try {
                JSch jsch = new JSch();
                // 建立 SSH 会话
                session = jsch.getSession(server.getSshUser(), server.getIp(), 22);
                session.setPassword(server.getSshPassword());
                session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
                session.connect(5000); // 连接超时时间设为 5 秒
                
                // 如果成功连接，则将状态标记为在线
                if (session.isConnected()) {
                    status = "ONLINE";
                }
            } catch (Exception e) {
                // 连接失败，状态保持为 OFFLINE
                System.out.println("无法连接到服务器 " + server.getIp() + ": " + e.getMessage());
            } finally {
                // 释放连接资源
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }

            // 如果服务器状态发生变化，则更新数据库记录
            if (!status.equals(server.getStatus())) {
                server.setStatus(status);
                serverInfoRepository.save(server);
            }
        }
    }
}
