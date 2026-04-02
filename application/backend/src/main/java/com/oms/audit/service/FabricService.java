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

/**
 * Fabric 区块链服务类
 * 负责与 Hyperledger Fabric 网络进行交互，包括初始化网关连接、提交日志上链等操作
 */
@Service
public class FabricService {

    // 所在通道名称，从配置文件读取
    @Value("${fabric.channelName}")
    private String channelName;

    // 智能合约（链码）名称，从配置文件读取
    @Value("${fabric.chaincodeName}")
    private String chaincodeName;

    // 组织 MSP ID，从配置文件读取
    @Value("${fabric.mspId}")
    private String mspId;

    // 客户端证书路径，从配置文件读取
    @Value("${fabric.certPath}")
    private String certPath;

    // 客户端私钥路径，从配置文件读取
    @Value("${fabric.keyPath}")
    private String keyPath;

    // Peer 节点的 TLS 证书路径，从配置文件读取
    @Value("${fabric.tlsCertPath}")
    private String tlsCertPath;

    // Peer 节点的 endpoint 地址，从配置文件读取
    @Value("${fabric.peerEndpoint}")
    private String peerEndpoint;

    // 覆盖的认证主机名，从配置文件读取
    @Value("${fabric.overrideAuth}")
    private String overrideAuth;

    // 智能合约对象
    private Contract contract;
    // Fabric 网关对象
    private Gateway gateway;
    // gRPC 连接通道
    private ManagedChannel grpcChannel;

    // 用于 JSON 序列化和反序列化的 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 服务初始化方法，在 Bean 创建后自动执行
     * 负责加载证书和私钥，建立与 Fabric 网络的 gRPC 连接并获取合约实例
     */
    @PostConstruct
    public void init() {
        try {
            // 检查证书和私钥文件是否存在
            if (!Files.exists(Paths.get(certPath)) || !Files.exists(Paths.get(keyPath))) {
                System.out.println("未找到 Fabric 证书材料。跳过 Fabric 初始化。");
                return;
            }

            // 读取客户端身份证书
            Identity credentials = new X509Identity(mspId, readX509Certificate(Paths.get(certPath)));
            // 读取客户端私钥并创建签名者
            Signer signer = Signers.newPrivateKeySigner(Identities.readPrivateKey(Files.newBufferedReader(Paths.get(keyPath))));

            // 构建 gRPC 通道连接到 Peer 节点，配置 TLS
            grpcChannel = NettyChannelBuilder.forTarget(peerEndpoint)
                    .sslContext(GrpcSslContexts.forClient().trustManager(Paths.get(tlsCertPath).toFile()).build())
                    .overrideAuthority(overrideAuth)
                    .build();

            // 创建并连接 Fabric 网关
            gateway = Gateway.newInstance()
                    .identity(credentials)
                    .signer(signer)
                    .connection(grpcChannel)
                    .connect();

            // 获取网络和智能合约实例
            Network network = gateway.getNetwork(channelName);
            contract = network.getContract(chaincodeName);

            System.out.println("Fabric 网关初始化成功。");

        } catch (Exception e) {
            System.err.println("初始化 Fabric 网关失败: " + e.getMessage());
        }
    }

    /**
     * 将审计日志上传到 Fabric 区块链网络
     *
     * @param log 待上链的审计日志对象
     * @return 包含链码执行结果（例如是否为敏感命令）的审计日志对象
     * @throws Exception 上链过程中的异常
     */
    public AuditLog uploadLog(AuditLog log) throws Exception {
        // 如果合约未初始化（例如在本地开发或无证书环境下），使用模拟逻辑
        if (contract == null) {
            System.out.println("Fabric 合约未初始化。使用模拟方式上传日志。");
            log.setSensitive(checkSensitiveMock(log.getCommand()));
            return log;
        }

        System.out.println("正在向 Fabric 提交 CreateLog 交易...");
        // 调用智能合约的 CreateLog 方法，传入日志字段
        byte[] result = contract.submitTransaction("CreateLog",
                log.getId(),
                log.getTimestamp(),
                log.getIp(),
                log.getServerIp(),
                log.getUser(),
                log.getPwd(),
                log.getCommand(),
                log.getHash());

        // 解析智能合约返回的 JSON 数据（包含是否为敏感命令的标记）
        return objectMapper.readValue(new String(result, StandardCharsets.UTF_8), AuditLog.class);
    }

    /**
     * 模拟敏感命令检测逻辑（仅在未连接 Fabric 时使用）
     *
     * @param command 执行的命令
     * @return 是否为敏感命令
     */
    private boolean checkSensitiveMock(String command) {
        return command.contains("rm") || command.contains("sudo") || command.contains("etc");
    }

    /**
     * 从指定路径读取 X509 证书文件
     *
     * @param certificatePath 证书文件路径
     * @return X509 证书对象
     * @throws IOException IO 异常
     * @throws CertificateException 证书解析异常
     */
    private static X509Certificate readX509Certificate(final Path certificatePath) throws IOException, CertificateException {
        try (BufferedReader reader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(reader);
        }
    }
}
