package com.oms.audit.service;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.ServerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class ServerStatusMonitor {
    @Autowired
    private ServerInfoRepository serverInfoRepository;
    @Scheduled(fixedRate = 60000) 
    public void checkServerStatus() {
        List<ServerInfo> servers = serverInfoRepository.findAll();
        for (ServerInfo server : servers) {
            String status = "OFFLINE";
            Session session = null;
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(server.getSshUser(), server.getIp(), server.getSshPort());
                session.setPassword(server.getSshPassword());
                session.setConfig("StrictHostKeyChecking", "no"); 
                session.connect(5000); 
                if (session.isConnected()) {
                    status = "ONLINE";
                }
            } catch (Exception e) {
                System.out.println("无法连接到服务器 " + server.getIp() + ": " + e.getMessage());
            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
            if (!status.equals(server.getStatus())) {
                server.setStatus(status);
                serverInfoRepository.save(server);
            }
        }
    }
}
