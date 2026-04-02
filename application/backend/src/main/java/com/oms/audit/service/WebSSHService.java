package com.oms.audit.service;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.ServerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSSH 核心服务类
 * 负责管理 WebSocket 会话与 JSch (SSH) 会话之间的桥接
 */
@Service
public class WebSSHService {

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    // 存储 WebSocket Session ID 与 SSH 连接对象的映射关系
    private Map<String, SSHConnection> sshMap = new ConcurrentHashMap<>();

    /**
     * 内部类，用于封装一个完整的 SSH 交互连接
     */
    class SSHConnection {
        Session jschSession;
        ChannelShell channel;
        InputStream is;
        OutputStream os;
    }

    /**
     * 初始化 SSH 连接并将其与 WebSocket 会话绑定
     *
     * @param session WebSocket 会话
     * @param serverId 目标服务器 ID
     */
    public void connect(WebSocketSession session, Long serverId) {
        ServerInfo server = serverInfoRepository.findById(serverId).orElse(null);
        if (server == null) {
            try {
                session.sendMessage(new TextMessage("Error: Server not found.\r\n"));
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            JSch jsch = new JSch();
            // 建立 JSch Session，使用配置的端口
            Session jschSession = jsch.getSession(server.getSshUser(), server.getIp(), server.getSshPort());
            jschSession.setPassword(server.getSshPassword());
            jschSession.setConfig("StrictHostKeyChecking", "no");
            jschSession.connect(5000);

            // 开启交互式 Shell 通道
            ChannelShell channel = (ChannelShell) jschSession.openChannel("shell");
            InputStream is = channel.getInputStream();
            OutputStream os = channel.getOutputStream();
            channel.connect(3000);

            // 封装连接并存入 Map
            SSHConnection conn = new SSHConnection();
            conn.jschSession = jschSession;
            conn.channel = channel;
            conn.is = is;
            conn.os = os;
            sshMap.put(session.getId(), conn);

            // 启动一个后台线程，持续读取 SSH 终端的输出，并通过 WebSocket 发送给前端
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                int i;
                try {
                    while ((i = is.read(buffer)) != -1) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(new String(buffer, 0, i)));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("SSH stream read ended.");
                } finally {
                    close(session);
                }
            }).start();

        } catch (Exception e) {
            try {
                session.sendMessage(new TextMessage("Error connecting to SSH: " + e.getMessage() + "\r\n"));
                session.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 处理前端通过 WebSocket 发送过来的用户输入指令
     *
     * @param session WebSocket 会话
     * @param command 用户在终端输入的字符
     */
    public void recvHandle(WebSocketSession session, String command) {
        SSHConnection conn = sshMap.get(session.getId());
        if (conn != null && conn.os != null) {
            try {
                conn.os.write(command.getBytes());
                conn.os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭 WebSocket 对应的 SSH 连接，并清理资源
     *
     * @param session WebSocket 会话
     */
    public void close(WebSocketSession session) {
        SSHConnection conn = sshMap.remove(session.getId());
        if (conn != null) {
            if (conn.channel != null) conn.channel.disconnect();
            if (conn.jschSession != null) conn.jschSession.disconnect();
        }
    }
}
