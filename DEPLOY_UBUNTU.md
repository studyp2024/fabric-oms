# 网络安全设备运维审计系统 - 部署与运行手册

本手册提供了在 Ubuntu 22.04 服务器上部署和运行基于联盟链的网络安全设备运维审计系统的详细分步指南。

**项目目录说明**:
假设项目代码位于 `/fabric-oms` 根目录下。
*   整体项目: `/fabric-oms`
*   Fabric 网络脚本: `/fabric-oms/blockchain/fabric-samples`
*   后端代码: `/fabric-oms/application/backend`
*   前端代码: `/fabric-oms/application/web`

---

## 1. 系统要求

*   **操作系统**: Ubuntu 22.04 LTS
*   **硬件配置**: 至少 4GB 内存, 2 核 CPU, 20GB 磁盘空间
*   **用户权限**: Root 用户或具有 sudo 权限的用户

---

## 2. 环境准备

你可以运行提供的 `deploy.sh` 脚本来安装大部分依赖，或者按照以下步骤手动安装。建议手动安装以确保每一步都正确无误。

### 2.1 更新系统
```bash
sudo apt-get update && sudo apt-get upgrade -y
```

### 2.2 安装基础工具
```bash
sudo apt-get install -y curl git build-essential unzip jq
```

### 2.3 安装 Docker & Docker Compose
Hyperledger Fabric 运行必需。
```bash
sudo apt-get install -y docker.io docker-compose
sudo usermod -aG docker $USER
# 注意：执行完上述命令后，请注销并重新登录，或重启服务器以使用户组变更生效
```
*验证*: `docker run hello-world`

### 2.4 安装 Go 语言环境
链码开发必需。

```bash
# 1. 下载 Go (请根据最新版本调整)
wget https://golang.google.cn/dl/go1.26.0.linux-amd64.tar.gz

# 2. 将下载的二进制包解压至 /usr/local 目录
sudo tar -C /usr/local -xzf go1.26.0.linux-amd64.tar.gz

# 3. 创建工作目录
mkdir -p $HOME/go

# 4. 配置环境变量 (追加到 ~/.bashrc)
echo 'export GOPATH=$HOME/go' >> ~/.bashrc
echo 'export GOROOT=/usr/local/go' >> ~/.bashrc
echo 'export PATH=$GOROOT/bin:$PATH' >> ~/.bashrc
echo 'export PATH=$GOPATH/bin:$PATH' >> ~/.bashrc

# 5. 使环境变量生效
source ~/.bashrc

# 6. 设置 Go 代理 (加速下载)
go env -w GO111MODULE=on
go env -w GOPROXY=https://goproxy.cn,direct
```
*验证*: `go version`

### 2.5 安装 Java 8 & Maven
后端服务必需。
```bash
sudo apt-get install -y openjdk-8-jdk maven
```
*验证*: `java -version` (应输出 1.8.x), `mvn -version`

### 2.6 安装 Node.js 18+
前端构建必需。
> **注意**: 必须安装 Node.js 18 或更高版本，否则 Vite 构建会失败。
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```
*验证*: `node -v` (应输出 v18.x.x)

### 2.7 安装 MySQL
```bash
sudo apt-get install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

---

## 3. 数据库初始化

1.  **配置 MySQL 安全性及创建数据库**:
    ```bash
    sudo mysql
    ```
    在 MySQL 命令行中执行:
    ```sql
    -- 设置 root 密码 (如果是新安装)
    ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
    FLUSH PRIVILEGES;
    CREATE DATABASE IF NOT EXISTS oms_db;
    EXIT;
    ```

2.  **初始化表结构和默认数据**:
    ```bash
    # 假设你在 /fabric-oms 目录下
    cd /fabric-oms
    mysql -u root -p123456 oms_db < database/schema.sql
    ```

3.  **默认账号说明**:
    执行上述脚本后，系统会自动创建以下默认账号：
    *   **管理员**: `admin` / `admin`
    *   **运维人员**: `ops` / `ops`

---

## 4. Hyperledger Fabric 区块链网络搭建

我们将使用项目自带的 `fabric-samples` (位于 `/fabric-oms/blockchain/fabric-samples`)。

### 4.1 准备环境
我们将使用项目自带的 `fabric-samples` (位于 `/fabric-oms/blockchain/fabric-samples`)。如果你还没有下载 Fabric 的相关文件，请使用以下命令：

```bash
cd /fabric-oms/blockchain
# 下载 Fabric 安装脚本
curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && chmod +x install-fabric.sh

# 运行脚本下载 Docker 镜像、Samples 和 二进制文件
./install-fabric.sh docker samples binary
```

*验证*: 检查 `bin` 目录是否存在且包含 `peer`, `orderer` 等文件。

### 4.2 启动测试网络
```bash
cd /fabric-oms/blockchain/fabric-samples/test-network
./network.sh down
./network.sh up createChannel -c mychannel -ca
```
*验证*: `docker ps` 应看到 peer0.org1, peer0.org2, orderer 等容器。

### 4.3 部署链码 (Chaincode)
我们将部署名为 `audit` 的链码。

**执行部署**:

```bash
cd /fabric-oms/blockchain/fabric-samples/test-network

# 部署版本 1.0, 序列号 1
# -ccp 参数指定链码的绝对路径
./network.sh deployCC -ccn audit -ccp /fabric-oms/blockchain/chaincode/audit -ccl go -ccv 1.0 -ccs 1
```

*   `-ccn`: 链码名称
*   `-ccp`: 链码路径 (使用绝对路径)
*   `-ccs`: 序列号 (**重要**: 每次更新代码重新部署时，必须增加此数字，如 2, 3...)

---

## 5. 后端部署

### 5.1 配置证书路径
编辑 `/fabric-oms/application/backend/src/main/resources/application.properties`。

你需要修改 `fabric.certPath`, `fabric.keyPath`, `fabric.tlsCertPath` 三个参数。
这些文件位于 `/fabric-oms/blockchain/fabric-samples/test-network/organizations` 目录下。

**注意**: 每次重启网络 (`./network.sh up`)，**私钥文件名 (`_sk`) 都会改变**！请务必检查。

示例配置 (请根据实际 `ls` 结果修改):
```properties
fabric.mspId=Org1MSP
fabric.peerEndpoint=localhost:7051
fabric.overrideAuth=peer0.org1.example.com

# 证书 (通常不变)
fabric.certPath=/fabric-oms/blockchain/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/signcerts/cert.pem
# 私钥 (每次重启网络都会变，请 ls 查看目录下的文件名)
fabric.keyPath=/fabric-oms/blockchain/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/keystore/priv_sk
# TLS CA (通常不变)
fabric.tlsCertPath=/fabric-oms/blockchain/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
```

### 5.2 构建并运行
```bash
cd /fabric-oms/application/backend
mvn clean package -DskipTests
java -jar target/audit-0.0.1-SNAPSHOT.jar
```
后端将在 `http://localhost:8080` 启动。

---

## 6. 前端部署 (Nginx)

### 6.1 构建前端
```bash
cd /fabric-oms/application/web
npm install
npm run build
```
构建完成后，静态文件位于 `/fabric-oms/application/web/dist`。

### 6.2 解决权限问题 (关键)
为了防止 Nginx 报 **403 Forbidden** 或 **500 Internal Server Error**，必须赋予 Nginx 用户 (`www-data`) 读取项目文件的权限。

```bash
# 赋予整个项目目录读取和执行权限 (755)
# 这允许 www-data 进入目录读取 dist 下的文件
sudo chmod -R 755 /fabric-oms
```

### 6.3 配置 Nginx
创建配置 `/etc/nginx/sites-available/oms`:
```nginx
server {
    listen 80;
    server_name _;

    # 指向构建生成的 dist 目录
    root /fabric-oms/application/web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端 API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

启用并重启:
```bash
sudo ln -s /etc/nginx/sites-available/oms /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
```

现在可以通过浏览器访问 `http://localhost/`。

---

## 7. 目标服务器日志配置

要监控其他服务器，请在**目标服务器**上执行：
1.  上传 `scripts/setup_remote_logging.sh`。
2.  运行 `sudo ./setup_remote_logging.sh`。
3.  在系统前端 "Server List" 中添加该服务器信息。

---

## 8. 项目停止与重启 (维护指南)

### 8.1 停止项目

*   **仅停止应用 (保留数据)**:
    ```bash
    pkill -f "audit-0.0.1-SNAPSHOT.jar"
    ```
    *适用场景*: 修改了 Java/Vue 代码，或者暂时不需要 web 服务，但想保留区块链数据。

*   **完全停止 (清空数据)**:
    ```bash
    # 1. 停止应用
    pkill -f "audit-0.0.1-SNAPSHOT.jar"
    
    # 2. 销毁 Fabric 网络 (数据将丢失!)
    cd /fabric-oms/blockchain/fabric-samples/test-network
    ./network.sh down
    ```
    *适用场景*: 环境出错需要重置，或者不再使用。

### 8.2 重新启动

*   **情况 A: 网络未停止 (仅重启应用)**:
    1.  直接运行 Java 后端即可。
    2.  如果前端更新，重新 `npm run build` 即可 (Nginx 无需重启)。

*   **情况 B: 网络已停止 (完全重启)**:
    1.  **启动网络**:
        ```bash
        cd /fabric-oms/blockchain/fabric-samples/test-network
        ./network.sh up createChannel -c mychannel -ca
        ```
    2.  **部署链码**:
        ```bash
        # 注意: 如果是全新启动，序列号(-ccs)可以是 1。如果是升级，必须增加序列号。
        ./network.sh deployCC -ccn audit -ccp ../chaincode/audit -ccl go -ccv 1.0 -ccs 1
        ```
    3.  **更新证书配置 (关键)**:
        *   进入 `/fabric-oms/blockchain/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/keystore/`
        *   查看新的私钥文件名 (`ls`).
        *   更新 `application/backend/.../application.properties` 中的 `fabric.keyPath`。
    4.  **启动后端**:
        ```bash
        java -jar target/audit-0.0.1-SNAPSHOT.jar
        ```
