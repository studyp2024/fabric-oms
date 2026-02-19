package com.oms.audit.service;

import com.oms.audit.entity.AuditLog;
import com.oms.audit.repository.AuditLogRepository;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
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

    private long lastFilePointer = 0;

    private static final String POINTER_FILE = "log_pointer.dat";

    @PostConstruct
    public void init() {
        try {
            if (Files.exists(Paths.get(POINTER_FILE))) {
                try (RandomAccessFile pointerFile = new RandomAccessFile(POINTER_FILE, "r")) {
                    lastFilePointer = pointerFile.readLong();
                }
            } else {
                // If no pointer file, start from end of log file to avoid re-processing
                if (Files.exists(Paths.get(logFilePath))) {
                    try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
                        lastFilePointer = file.length();
                        savePointer(lastFilePointer);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void syncLogs() {
        if (!Files.exists(Paths.get(logFilePath))) {
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
            long fileLength = file.length();
            if (fileLength < lastFilePointer) {
                // File was truncated
                lastFilePointer = 0;
            }

            if (fileLength > lastFilePointer) {
                file.seek(lastFilePointer);
                String line;
                while ((line = file.readLine()) != null) {
                    // RandomAccessFile reads bytes, need to decode
                    String decodedLine = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    processLogLine(decodedLine);
                }
                lastFilePointer = file.getFilePointer();
                savePointer(lastFilePointer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePointer(long pointer) {
        try (RandomAccessFile pointerFile = new RandomAccessFile(POINTER_FILE, "rw")) {
            pointerFile.seek(0);
            pointerFile.writeLong(pointer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processLogLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        // Format: 2026-01-27 10:10:00 | SSH_IP:192.168.1.100 | USER:ubuntu | PWD:/home/ubuntu | COMMAND:ls
        // Regex to parse
        String regex = "^(.*?)\\s\\|\\sSSH_IP:(.*?)\\s\\|\\sUSER:(.*?)\\s\\|\\sPWD:(.*?)\\s\\|\\sCOMMAND:(.*)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String ip = matcher.group(2);
            String user = matcher.group(3);
            String pwd = matcher.group(4);
            String command = matcher.group(5);

            AuditLog log = new AuditLog();
            String hash = calculateHash(line);
            log.setId(hash); // Use hash as ID to prevent duplicates
            log.setTimestamp(timestamp);
            log.setIp(ip);
            log.setUser(user);
            log.setPwd(pwd);
            log.setCommand(command);
            log.setHash(hash);
            
            // Default sensitive to false, let Fabric decide
            log.setSensitive(false);

            try {
                // Upload to Fabric (simulated or real)
                // FabricService should return the updated log with IsSensitive status from chaincode
                AuditLog processedLog = fabricService.uploadLog(log);
                
                // Save to local DB
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
