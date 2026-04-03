package com.oms.audit.service;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oms.audit.entity.AuditLog;
import com.oms.audit.entity.ServerInfo;
import com.oms.audit.repository.AuditLogRepository;
import com.oms.audit.repository.ServerInfoRepository;
import com.oms.audit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
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
    private String logFilePath; 
    @Autowired
    private FabricService fabricService;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private ServerInfoRepository serverInfoRepository;
    @Autowired
    private UserRepository userRepository;
    @Scheduled(fixedRate = 10000) 
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
            session = jsch.getSession(server.getSshUser(), server.getIp(), server.getSshPort());
            session.setPassword(server.getSshPassword());
            session.setConfig("StrictHostKeyChecking", "no"); 
            session.connect(5000); 
            long offset = server.getLastLogOffset();
            String command = "tail -c +" + (offset + 1) + " " + logFilePath;
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            channel.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            long bytesRead = 0;
            System.out.println("开始同步服务器日志: " + server.getIp());
            while ((line = reader.readLine()) != null) {
                byte[] lineBytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
                bytesRead += lineBytes.length;
                processLogLine(line, server.getIp());
            }
            System.out.println("服务器 " + server.getIp() + " 日志同步完成，读取字节数: " + bytesRead);
            if (bytesRead > 0) {
                server.setLastLogOffset(offset + bytesRead);
                serverInfoRepository.save(server);
            }
        } catch (Exception e) {
            System.err.println("同步服务器 " + server.getIp() + " 日志时发生错误: " + e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }
    private void processLogLine(String line, String serverIp) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        String regex = "^(.*?)\\s\\|\\sSSH_IP:(.*?)\\s\\|\\sUSER:(.*?)\\s\\|\\sPWD:(.*?)\\s\\|\\sCOMMAND:(.*)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String clientIp = matcher.group(2); 
            String user = matcher.group(3);
            String pwd = matcher.group(4);
            String command = matcher.group(5);
            AuditLog log = new AuditLog();
            String hash = calculateHash(line + serverIp); 
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
                System.err.println("处理日志行发生错误: " + line + " 错误信息: " + e.getMessage());
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
