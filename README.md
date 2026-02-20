# 基于联盟链的网络安全设备运维审计系统

本项目实现了一个基于 Hyperledger Fabric、Spring Boot 和 Vue.js 的网络运维操作审计系统。

## 项目结构

- `application/backend`: Spring Boot 后端应用
- `application/web`: Vue.js 前端应用
- `blockchain/chaincode`: 用于审计日志的 Go 语言链码
- `database`: MySQL 数据库脚本

## 前置要求

- Java 8 (推荐使用 Java 8 以确保兼容性)
- Node.js 16+
- Docker & Docker Compose
- Hyperledger Fabric 2.4+ (推荐使用 Test Network)
- MySQL 8.0

## 安装指南

### 1. 数据库设置

使用提供的 SQL 脚本创建数据库和表：

```bash
mysql -u root -p < database/schema.sql
```

### 2. 区块链设置

您需要部署一个 Hyperledger Fabric 网络。可以使用 Fabric samples 提供的 `test-network`。

1.  启动网络：`./network.sh up createChannel`
2.  打包并部署位于 `blockchain/chaincode/audit` 的链码。
    - 链码名称：`audit`
    - 语言：`go`
3.  更新 `application/backend/src/main/resources/application.properties` 中的路径，使其指向您的网络配置：
    - `fabric.certPath`：您的用户签名证书路径（例如 `User1@org1.example.com-cert.pem`）。
    - `fabric.keyPath`：您的用户私钥路径（例如 `priv_sk`）。
    - `fabric.tlsCertPath`：Peer 节点的 TLS CA 证书路径。
    - `fabric.peerEndpoint`：Peer 节点的 gRPC 地址（例如 `localhost:7051`）。

### 3. 后端设置

进入 `application/backend` 目录：

```bash
cd application/backend
./mvnw clean install
./mvnw spring-boot:run
```

后端将在 `http://localhost:8080` 启动。
它会每 5 秒自动同步一次 `audit.log.path` 配置指定的日志文件（默认为项目根目录下的 `ssh_commands.log`，生产环境建议配置为 `/var/log/ssh_commands.log`）。

### 4. 前端设置

进入 `application/web` 目录：

```bash
cd application/web
npm install
npm run dev
```

前端访问地址：`http://localhost:3000`。

### 5. 使用方法

1.  使用默认凭据登录：
    - **管理员 (Admin)**: `admin` / `admin`
    - **运维人员 (Ops)**: `ops` / `ops`
2.  查看分配的服务器。
3.  查看审计日志。系统会自动检测敏感命令（例如 `rm`、`sudo`、`cat /etc/shadow`）。

## 功能特性

- **用户管理**：管理员将服务器分配给运维人员。
- **日志采集**：读取本地 SSH 日志（模拟远程采集）。
- **区块链存储**：对日志进行哈希计算并存储在 Hyperledger Fabric 上。
- **审计**：智能合约自动检测敏感操作。
- **可视化**：Vue 前端提供直观的监控界面。
