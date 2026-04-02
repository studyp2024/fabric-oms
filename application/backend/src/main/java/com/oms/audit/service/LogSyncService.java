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

/**
 * 日志同步服务类
 * 负责定时从目标服务器拉取增量 SSH 命令日志，并将其保存到数据库和区块链中
 */
@Service
public class LogSyncService {

    // 目标服务器上日志文件的绝对路径，从配置文件中读取
    @Value("${audit.log.path}")
    private String logFilePath; // /var/log/ssh_commands.log

    @Autowired
    private FabricService fabricService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ServerInfoRepository serverInfoRepository;

    /**
     * 定时任务，每 10 秒执行一次
     * 遍历所有配置的服务器，拉取它们的增量日志
     */
    @Scheduled(fixedRate = 10000) 
    public void syncLogs() {
        List<ServerInfo> servers = serverInfoRepository.findAll();
        for (ServerInfo server : servers) {
            syncServerLogs(server);
        }
    }

    /**
     * 连接指定的服务器并同步日志
     * @param server 目标服务器信息
     */
    private void syncServerLogs(ServerInfo server) {
        // 如果服务器信息不完整，则跳过
        if (server.getIp() == null || server.getSshUser() == null || server.getSshPassword() == null) {
            return;
        }

        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;

        try {
            // 建立 SSH 会话
            session = jsch.getSession(server.getSshUser(), server.getIp(), 22);
            session.setPassword(server.getSshPassword());
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
            session.connect(5000); // 设置连接超时时间为 5000 毫秒

            // 获取上次读取的字节偏移量
            long offset = server.getLastLogOffset();
            // 构造 tail 命令，从指定的字节位置开始读取新增内容
            String command = "tail -c +" + (offset + 1) + " " + logFilePath;

            // 打开执行命令的通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            long bytesRead = 0;

            // 逐行读取命令输出（新增的日志行）
            while ((line = reader.readLine()) != null) {
                // tail 命令输出按行返回。我们需要计算读取的字节数以更新偏移量。
                // 注意：这里加上了换行符 "\n" 的长度。对于 UTF-8 编码的普通日志来说，这是准确的。
                byte[] lineBytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
                bytesRead += lineBytes.length;
                
                // 处理单行日志
                processLogLine(line, server.getIp());
            }

            // 如果读取到了新数据，更新数据库中的偏移量记录
            if (bytesRead > 0) {
                server.setLastLogOffset(offset + bytesRead);
                serverInfoRepository.save(server);
            }

        } catch (Exception e) {
            System.err.println("同步服务器 " + server.getIp() + " 日志时发生错误: " + e.getMessage());
        } finally {
            // 释放资源，断开连接
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    /**
     * 解析单行日志内容，并进行持久化和上链操作
     * @param line 原始日志文本行
     * @param serverIp 日志来源服务器 IP
     */
    private void processLogLine(String line, String serverIp) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        // 定义正则表达式解析日志格式
        // 期望格式: 2026-01-27 10:10:00 | SSH_IP:192.168.1.100 | USER:ubuntu | PWD:/home/ubuntu | COMMAND:ls
        String regex = "^(.*?)\\s\\|\\sSSH_IP:(.*?)\\s\\|\\sUSER:(.*?)\\s\\|\\sPWD:(.*?)\\s\\|\\sCOMMAND:(.*)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            // 提取各个字段信息
            String timestamp = matcher.group(1);
            String clientIp = matcher.group(2); // 客户端 IP
            String user = matcher.group(3);
            String pwd = matcher.group(4);
            String command = matcher.group(5);

            AuditLog log = new AuditLog();
            // 计算日志哈希值，包含 serverIp 以保证跨服务器日志的唯一性
            String hash = calculateHash(line + serverIp); 
            log.setId(hash);
            log.setTimestamp(timestamp);
            log.setIp(clientIp);
            log.setServerIp(serverIp);
            log.setUser(user);
            log.setPwd(pwd);
            log.setCommand(command);
            log.setHash(hash);
            log.setSensitive(false); // 默认非敏感，将由链码或 Mock 逻辑进行判定

            try {
                // 1. 提交到 Fabric 联盟链存证
                AuditLog processedLog = fabricService.uploadLog(log);
                // 2. 存入 MySQL 数据库以供快速查询
                auditLogRepository.save(processedLog);
            } catch (Exception e) {
                System.err.println("处理日志行发生错误: " + line + " 错误信息: " + e.getMessage());
            }
        }
    }

    /**
     * 计算字符串的 SHA-256 哈希值
     * @param content 待计算的字符串内容
     * @return 16进制的哈希字符串
     */
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
