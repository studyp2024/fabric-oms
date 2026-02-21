# 网络安全设备运维审计系统 (Network Operation Audit System)

本项目实现了一个基于 Hyperledger Fabric 联盟链、Spring Boot 后端和 Vue.js 前端的网络安全设备运维审计系统。它能够记录运维人员对服务器的操作日志，并将其不可篡改地存储在区块链上，支持审计和敏感命令检测。

## 📚 部署文档

**详细的部署和运行指南，请务必阅读 [DEPLOY_UBUNTU.md](DEPLOY_UBUNTU.md)。**

该文档包含了从环境准备、区块链网络搭建、后端配置到前端部署的全流程。

## 🏗 项目结构

- `application/backend`: Spring Boot 后端应用 (Java 8)
- `application/web`: Vue.js 前端应用 (Node.js 18+)
- `blockchain/chaincode`: 审计日志链码 (Go)
- `blockchain/fabric-samples`: Hyperledger Fabric 网络脚本
- `database`: MySQL 数据库脚本
- `scripts`: 远程日志采集脚本

## 🛠 前置要求

- **OS**: Ubuntu 22.04 LTS (推荐)
- **Java**: JDK 8
- **Node.js**: 18.0+ (必需，用于 Vite 构建)
- **Docker**: 最新版
- **Hyperledger Fabric**: 2.5.4 (集成在本项目中)
- **MySQL**: 8.0

## ✨ 功能特性

### 1. 核心功能
- **不可篡改审计**: 所有运维日志均上链存储，防止恶意删除或修改。
- **敏感命令检测**: 智能合约自动识别高危命令 (如 `rm -rf`, `sudo` 等) 并标记。
- **远程日志采集**: 支持通过 SSH 协议从多台目标服务器实时采集操作日志。

### 2. 用户与权限管理
- **管理员 (Admin)**:
    - 添加/删除运维人员 (Ops)。
    - 添加/管理受控服务器。
    - 将服务器分配给指定的运维人员。
    - 查看所有服务器的在线状态。
- **运维人员 (Ops)**:
    - 仅能查看分配给自己的服务器状态。
    - 查看自己负责范围内的审计日志。

### 3. 系统监控
- **服务器状态监控**: 系统每分钟自动检测服务器 SSH 连接状态 (Online/Offline)。
- **分页查询**: 支持海量日志的分页浏览和检索。

## 🚀 快速开始

请参考 [DEPLOY_UBUNTU.md](DEPLOY_UBUNTU.md) 进行完整部署。以下是简要步骤：

1.  **环境准备**: 运行 `./deploy.sh` 或手动安装依赖。
2.  **启动区块链**:
    ```bash
    cd blockchain/fabric-samples/test-network
    ./network.sh up createChannel -c mychannel -ca
    ./network.sh deployCC -ccn audit -ccp ../chaincode/audit -ccl go -ccv 1.0 -ccs 1
    ```
3.  **配置后端**: 修改 `application.properties` 中的证书路径。
4.  **运行后端**: `mvn spring-boot:run`
5.  **运行前端**: `npm run dev` (开发) 或 Nginx 部署 (生产)。

## 👤 默认账号

- **管理员**: `admin` / `admin`
- **运维人员**: `ops` / `ops`
