package com.oms.audit.service;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oms.audit.entity.ServerInfo;
import com.oms.audit.entity.User;
import com.oms.audit.repository.ServerInfoRepository;
import com.oms.audit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSSHService {

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    @Autowired
    private UserRepository userRepository;

    private Map<String, SSHConnection> sshMap = new ConcurrentHashMap<>();

    class SSHConnection {
        Session jschSession;
        ChannelShell channel;
        InputStream is;
        OutputStream os;
    }

    public void connect(WebSocketSession session, Long serverId, Long userId) {
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

        String username = "unknown";
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                username = user.getUsername();
            }
        }

        try {
            JSch jsch = new JSch();
            Session jschSession = jsch.getSession(server.getSshUser(), server.getIp(), server.getSshPort());
            jschSession.setPassword(server.getSshPassword());
            jschSession.setConfig("StrictHostKeyChecking", "no");
            jschSession.connect(5000);

            ChannelShell channel = (ChannelShell) jschSession.openChannel("shell");
            InputStream is = channel.getInputStream();
            OutputStream os = channel.getOutputStream();
            channel.connect(3000);

            String injectCmd = "export OMS_SYS_USER='" + username + "'\r";
            os.write(injectCmd.getBytes());
            os.flush();
            os.write("clear\r".getBytes());
            os.flush();

            SSHConnection conn = new SSHConnection();
            conn.jschSession = jschSession;
            conn.channel = channel;
            conn.is = is;
            conn.os = os;
            sshMap.put(session.getId(), conn);

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

    public void close(WebSocketSession session) {
        SSHConnection conn = sshMap.remove(session.getId());
        if (conn != null) {
            if (conn.channel != null) conn.channel.disconnect();
            if (conn.jschSession != null) conn.jschSession.disconnect();
        }
    }
}
