package com.oms.audit.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oms.audit.entity.AuditLog;
import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.AuditLogRepository;
import com.oms.audit.repository.ServerInfoRepository;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogSyncService {

    @Value("${audit.log.path}")
    private String logFilePath; // Remote path, e.g., /var/log/ssh_commands.log

    @Autowired
    private FabricService fabricService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void syncLogs() {
        List<ServerInfo> servers = serverInfoRepository.findAll();
        for (ServerInfo server : servers) {
            syncServerLogs(server);
        }
    }

    private void syncServerLogs(ServerInfo server) {
        if (server.getIp() == null || server.getSshUser() == null || server.getSshPassword() == null) {
            return;
        }

        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;

        try {
            session = jsch.getSession(server.getSshUser(), server.getIp(), 22);
            session.setPassword(server.getSshPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);

            // Command to read from the last offset
            // tail -c +<offset+1> file
            long offset = server.getLastLogOffset();
            String command = "tail -c +" + (offset + 1) + " " + logFilePath;

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            long bytesRead = 0;

            while ((line = reader.readLine()) != null) {
                // tail outputs lines. We need to estimate bytes read to update offset.
                // Note: This is an approximation if encoding varies, but for UTF-8/ASCII log lines it's generally close.
                // Ideally, we'd count bytes directly from stream, but readLine consumes them.
                // We add line length + 1 (for newline).
                byte[] lineBytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
                bytesRead += lineBytes.length;
                
                processLogLine(line, server.getIp());
            }

            if (bytesRead > 0) {
                server.setLastLogOffset(offset + bytesRead);
                serverInfoRepository.save(server);
            }

        } catch (Exception e) {
            System.err.println("Error syncing logs from " + server.getIp() + ": " + e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    private void processLogLine(String line, String serverIp) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        // Format: 2026-01-27 10:10:00 | SSH_IP:192.168.1.100 | USER:ubuntu | PWD:/home/ubuntu | COMMAND:ls
        String regex = "^(.*?)\\s\\|\\sSSH_IP:(.*?)\\s\\|\\sUSER:(.*?)\\s\\|\\sPWD:(.*?)\\s\\|\\sCOMMAND:(.*)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String clientIp = matcher.group(2); // Client IP
            String user = matcher.group(3);
            String pwd = matcher.group(4);
            String command = matcher.group(5);

            AuditLog log = new AuditLog();
            String hash = calculateHash(line + serverIp); // Include serverIp in hash for uniqueness across servers
            log.setId(hash);
            log.setTimestamp(timestamp);
            log.setIp(clientIp);
            log.setServerIp(serverIp);
            log.setUser(user);
            log.setPwd(pwd);
            log.setCommand(command);
            log.setHash(hash);
            log.setSensitive(false);

            try {
                AuditLog processedLog = fabricService.uploadLog(log);
                auditLogRepository.save(processedLog);
            } catch (Exception e) {
                System.err.println("Error processing log line: " + line + " Error: " + e.getMessage());
            }
        }
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
