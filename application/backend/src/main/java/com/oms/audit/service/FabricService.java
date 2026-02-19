package com.oms.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.audit.entity.AuditLog;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import javax.annotation.PostConstruct;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.security.PrivateKey;

@Service
public class FabricService {

    @Value("${fabric.channelName}")
    private String channelName;

    @Value("${fabric.chaincodeName}")
    private String chaincodeName;

    @Value("${fabric.mspId}")
    private String mspId;

    @Value("${fabric.certPath}")
    private String certPath;

    @Value("${fabric.keyPath}")
    private String keyPath;

    @Value("${fabric.tlsCertPath}")
    private String tlsCertPath;

    @Value("${fabric.peerEndpoint}")
    private String peerEndpoint;

    @Value("${fabric.overrideAuth}")
    private String overrideAuth;

    private Contract contract;
    private Gateway gateway;
    private ManagedChannel grpcChannel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(Paths.get(certPath)) || !Files.exists(Paths.get(keyPath))) {
                System.out.println("Fabric crypto materials not found. Skipping Fabric initialization.");
                return;
            }

            Identity credentials = new X509Identity(mspId, readX509Certificate(Paths.get(certPath)));
            Signer signer = Signers.newPrivateKeySigner(Identities.readPrivateKey(Files.newBufferedReader(Paths.get(keyPath))));

            grpcChannel = NettyChannelBuilder.forTarget(peerEndpoint)
                    .sslContext(GrpcSslContexts.forClient().trustManager(Paths.get(tlsCertPath).toFile()).build())
                    .overrideAuthority(overrideAuth)
                    .build();

            gateway = Gateway.newInstance()
                    .identity(credentials)
                    .signer(signer)
                    .connection(grpcChannel)
                    .connect();

            Network network = gateway.getNetwork(channelName);
            contract = network.getContract(chaincodeName);

            System.out.println("Fabric Gateway initialized successfully.");

        } catch (Exception e) {
            System.err.println("Failed to initialize Fabric Gateway: " + e.getMessage());
        }
    }

    public AuditLog uploadLog(AuditLog log) throws Exception {
        if (contract == null) {
            System.out.println("Fabric contract not initialized. Mocking upload.");
            log.setSensitive(checkSensitiveMock(log.getCommand()));
            return log;
        }

        System.out.println("Submitting transaction CreateLog to Fabric...");
        byte[] result = contract.submitTransaction("CreateLog",
                log.getId(),
                log.getTimestamp(),
                log.getIp(),
                log.getUser(),
                log.getPwd(),
                log.getCommand(),
                log.getHash());

        // Parse returned JSON to get IsSensitive
        return objectMapper.readValue(new String(result, StandardCharsets.UTF_8), AuditLog.class);
    }

    private boolean checkSensitiveMock(String command) {
        return command.contains("rm") || command.contains("sudo") || command.contains("etc");
    }

    private static X509Certificate readX509Certificate(final Path certificatePath) throws IOException, CertificateException {
        try (BufferedReader reader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(reader);
        }
    }
}
