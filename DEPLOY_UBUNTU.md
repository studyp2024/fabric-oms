# 网络安全设备运维审计系统 - 部署与运行手册

本手册提供了在 Ubuntu 22.04 服务器上部署和运行基于联盟链的网络安全设备运维审计系统的详细分步指南。

## 1. 系统要求

*   **操作系统**: Ubuntu 22.04 LTS
*   **硬件配置**: 至少 4GB 内存, 2 核 CPU, 20GB 磁盘空间
*   **用户权限**: Root 用户或具有 sudo 权限的用户

## 2. 环境准备

你可以运行提供的 `deploy.sh` 脚本来安装大部分依赖，或者按照以下步骤手动安装。建议手动安装以确保每一步都正确无误。

### 2.1 更新系统

1.  **执行命令**:
    ```bash
    sudo apt-get update && sudo apt-get upgrade -y
    ```
2.  **验证**: 确保命令执行无报错，且系统提示已是最新版本。

### 2.2 安装基础工具

1.  **执行命令**:
    ```bash
    sudo apt-get install -y curl git build-essential unzip
    ```
2.  **验证**:
    ```bash
    git --version
    # 预期输出: git version 2.x.x
    ```

### 2.3 安装 Docker & Docker Compose (Hyperledger Fabric 必需)

1.  **执行命令**:
    ```bash
    sudo apt-get install -y docker.io docker-compose
    sudo usermod -aG docker $USER
    # 注意：执行完上述命令后，请注销并重新登录，或重启服务器以使用户组变更生效
    ```
2.  **验证**:
    ```bash
    docker --version
    docker-compose --version
    docker run hello-world
    # 预期输出: 能够看到 "Hello from Docker!" 的欢迎信息，说明 Docker 安装并运行正常
    ```

### 2.4 安装 Go 语言环境 (链码开发必需)

1.  **执行命令**:
    ```bash
    wget https://go.dev/dl/go1.20.10.linux-amd64.tar.gz
    sudo tar -C /usr/local -xzf go1.20.10.linux-amd64.tar.gz
    echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
    source ~/.bashrc
    ```
2.  **验证**:
    ```bash
    go version
    # 预期输出: go version go1.20.10 linux/amd64
    ```

### 2.5 安装 Java 8 (后端服务必需)

1.  **执行命令**:
    ```bash
    sudo apt-get install -y openjdk-8-jdk
    ```
2.  **验证**:
    ```bash
    java -version
    # 预期输出: openjdk version "1.8.0_xxx"
    ```

### 2.6 安装 Maven (后端构建必需)

1.  **执行命令**:
    ```bash
    sudo apt-get install -y maven
    ```
2.  **验证**:
    ```bash
    mvn -version
    # 预期输出: Apache Maven 3.x.x ... Java version: 1.8.0_xxx
    ```

### 2.7 安装 Node.js & npm (前端构建必需)

> **注意**: 本项目前端构建工具 Vite 需要 Node.js 版本 **>= 18.0.0**。Ubuntu 默认源中的版本可能过旧 (v10/v12)，会导致构建失败 (`SyntaxError: Unexpected reserved word`)。请务必使用以下命令安装最新版。

1.  **清理旧版本 (如果存在)**:
    ```bash
    sudo apt-get remove -y nodejs
    sudo apt-get autoremove -y
    sudo rm -f /etc/apt/sources.list.d/nodesource.list
    ```

2.  **安装 Node.js 18.x**:
    ```bash
    curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
    sudo apt-get install -y nodejs
    ```

3.  **验证**:
    ```bash
    node -v
    # 必须输出: v18.x.x (例如 v18.19.0)
    # 如果显示 v12.x.x，说明安装未成功，请检查上述步骤是否报错。
    ```

### 2.8 安装 MySQL (数据库)

1.  **执行命令**:
    ```bash
    sudo apt-get install -y mysql-server
    sudo systemctl start mysql
    sudo systemctl enable mysql
    ```
2.  **验证**:
    ```bash
    sudo systemctl status mysql
    # 预期输出: Active: active (running)
    ```

## 3. 数据库配置

1.  **配置 MySQL 安全性及创建数据库**:
    ```bash
    sudo mysql
    ```
    在 MySQL 命令行中执行:
    ```sql
    ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
    FLUSH PRIVILEGES;
    CREATE DATABASE IF NOT EXISTS oms_db;
    EXIT;
    ```
2.  **初始化表结构**:
    ```bash
    mysql -u root -p123456 oms_db < database/schema.sql
    ```
3.  **验证**:
    ```bash
    mysql -u root -p123456 -e "USE oms_db; SHOW TABLES;"
    # 预期输出: 应该看到 audit_logs, servers, users 三张表
    ```

## 4. Hyperledger Fabric 区块链网络搭建

本系统依赖于 Fabric 网络。我们将使用 Fabric Test Network 进行快速部署。

1.  **安装 Fabric 二进制文件和示例**:
    ```bash
    mkdir -p ~/fabric
    cd ~/fabric
    # 下载 Fabric 2.5.4 和 CA 1.5.7
    curl -sSL https://bit.ly/2ysbOFE | bash -s -- 2.5.4 1.5.7
    ```
    *验证*: 检查 `bin` 目录是否存在且包含 `peer`, `orderer` 等文件。

2.  **启动测试网络**:
    ```bash
    cd ~/fabric/fabric-samples/test-network
    ./network.sh down
    ./network.sh up createChannel -c mychannel -ca
    ```
    *验证*:
    ```bash
    docker ps
    # 预期输出: 应该看到 peer0.org1.example.com, peer0.org2.example.com, orderer.example.com 等容器正在运行
    ```

3.  **部署链码 (Chaincode)**:
    
    我们将使用 Fabric 官方提供的 `network.sh` 脚本来自动化部署流程（打包 -> 安装 -> 批准 -> 提交）。

    *   **准备链码**:
        为了避免路径权限问题，建议将链码复制到 `fabric-samples` 的标准链码目录下：
        ```bash
        # 1. 清理旧的链码（如果有）
        rm -rf ~/fabric/fabric-samples/chaincode/audit

        # 2. 复制最新代码
        cp -r ~/fabric-oms/blockchain/chaincode/audit ~/fabric/fabric-samples/chaincode/
        ```

    *   **执行部署**:
        切换到测试网络目录并运行部署命令：
        ```bash
        cd ~/fabric/fabric-samples/test-network
        
        # 首次部署 (版本 1.0, 序列号 1)
        ./network.sh deployCC -ccn audit -ccp ../chaincode/audit -ccl go -ccv 1.0 -ccs 1
        ```
        *   `-ccn`: 链码名称 (chaincode name) -> `audit`
        *   `-ccp`: 链码路径 (chaincode path) -> `../chaincode/audit`
        *   `-ccl`: 语言 (language) -> `go`
        *   `-ccv`: 版本号 (label version)
        *   `-ccs`: 序列号 (sequence) - **重要**: 每次更新代码重新部署时，必须增加此数字 (如 2, 3...)

    *   **更新/重新部署链码**:
        如果你修改了 Go 代码并需要更新链码，请执行：
        ```bash
        # 1. 再次复制最新代码
        cp -r ~/fabric-oms/blockchain/chaincode/audit ~/fabric/fabric-samples/chaincode/
        
        # 2. 部署新版本 (注意 -ccs 2)
        ./network.sh deployCC -ccn audit -ccp ../chaincode/audit -ccl go -cqv 2.0 -ccs 2
        ```

    *   **验证**:
        检查链码容器是否启动：
        ```bash
        docker ps | grep dev-peer
        # 预期输出: 应该看到 dev-peer0.org1.example.com-audit-1.0... 和 dev-peer0.org2...
        ```

    *   **设置环境变量以使用 CLI 测试 (可选)**:
        如果你想在命令行直接调用链码进行测试：
        ```bash
        export PATH=${PWD}/../bin:$PATH
        export FABRIC_CFG_PATH=$PWD/../config/
        export CORE_PEER_TLS_ENABLED=true
        export CORE_PEER_LOCALMSPID="Org1MSP"
        export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
        export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
        export CORE_PEER_ADDRESS=localhost:7051
        
        # 调用 InitLedger (如果合约中有)
        peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem" -C mychannel -n audit --peerAddresses localhost:7051 --tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt" --peerAddresses localhost:9051 --tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt" -c '{"function":"InitLedger","Args":[]}'
        ```

## 5. 后端部署

1.  **配置应用程序**:
    *   编辑配置文件: `application/backend/src/main/resources/application.properties`
    *   **关键步骤**: 更新 Fabric 证书路径。这些证书由 Test Network 生成。
    *   通常路径在 `~/fabric/fabric-samples/test-network/organizations` 下。
    *   你需要修改 `fabric.certPath`, `fabric.keyPath`, `fabric.tlsCertPath` 为你服务器上的绝对路径。
    *   **示例**:
        ```properties
        fabric.certPath=/home/ubuntu/fabric/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/signcerts/cert.pem
        fabric.keyPath=/home/ubuntu/fabric/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/keystore/priv_sk
        fabric.tlsCertPath=/home/ubuntu/fabric/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
        ```
    *验证*: 使用 `ls -l <路径>` 确保配置文件中填写的路径真实存在。

2.  **日志同步配置 (多服务器支持)**:
    *   系统现在支持通过 SSH 从多台远程服务器拉取日志。
    *   **配置**: 在 `application.properties` 中设置 `audit.log.path` 为**远程服务器上**的日志文件绝对路径（例如 `/var/log/ssh_commands.log`）。
    *   **添加服务器**: 系统启动后，管理员需要在系统界面（或直接通过数据库）添加目标服务器信息（IP、SSH用户名、SSH密码）。
    *   **原理**: 后端会定期遍历所有已添加的服务器，建立 SSH 连接，读取指定的日志文件，并增量同步到区块链。
    *验证*:
    ```bash
    touch /var/log/ssh_commands.log
    chmod 644 /var/log/ssh_commands.log
    ls -l /var/log/ssh_commands.log
    ```

3.  **构建并运行**:
    ```bash
    cd ~/fabric-oms/application/backend
    mvn clean package -DskipTests
    java -jar target/audit-0.0.1-SNAPSHOT.jar
    ```
    *验证*:
    *   观察控制台输出，应显示 "Fabric Gateway initialized successfully"。
    *   检查端口占用:
        ```bash
        sudo netstat -tuln | grep 8080
        # 预期输出: tcp6 0 0 :::8080 LISTEN
        ```

## 6. 目标服务器配置 (日志源)

为了让审计系统能够收集远程服务器的操作日志，你需要在**每一台**被监控的服务器上执行以下配置步骤。

1.  **分发配置脚本**:
    *   将项目中的 `scripts/setup_remote_logging.sh` 脚本上传到目标服务器。
    *   例如使用 `scp`:
        ```bash
        scp scripts/setup_remote_logging.sh user@target-server-ip:/tmp/
        ```

2.  **执行配置脚本**:
    *   登录目标服务器并以 sudo 权限运行脚本:
        ```bash
        ssh user@target-server-ip
        cd /tmp
        chmod +x setup_remote_logging.sh
        sudo ./setup_remote_logging.sh
        ```
    *   **脚本功能说明**:
        *   配置 `rsyslog` 将 `local6` 级别的日志写入 `/var/log/ssh_commands.log`。
        *   修改 `/etc/profile` 添加 `PROMPT_COMMAND`，用于捕获每个用户的命令操作并发送到 `rsyslog`。
        *   设置日志文件权限，允许 SSH 用户读取。

3.  **验证配置**:
    *   注销并重新登录目标服务器（确保 `/etc/profile` 生效）。
    *   执行几个命令（如 `ls`, `whoami`）。
    *   检查日志文件是否生成:
        ```bash
        tail -f /var/log/ssh_commands.log
        ```
    *   **预期输出**: 应该看到类似以下的日志条目:
        ```text
        2024-05-20 10:00:00 | SSH_IP:192.168.1.5 | USER:ubuntu | PWD:/home/ubuntu | COMMAND:ls
        ```

## 7. 前端部署

1.  **安装依赖并构建**:
    ```bash
    cd ~/fabric-oms/application/web
    npm install
    npm run build
    ```
    *验证*: 检查 `dist` 目录是否生成。
    ```bash
    ls -l dist
    ```

2.  **使用 Nginx 部署 (推荐)**:
    *   安装 Nginx: `sudo apt-get install -y nginx`
    *   创建配置: `sudo nano /etc/nginx/sites-available/oms`
    ```nginx
    server {
        listen 80;
        server_name _;

        location / {
            root /home/ubuntu/fabric-oms/application/web/dist;
            try_files $uri $uri/ /index.html;
        }

        location /api {
            proxy_pass http://localhost:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
    ```
    *   启用站点:
    ```bash
    sudo ln -s /etc/nginx/sites-available/oms /etc/nginx/sites-enabled/
    sudo rm /etc/nginx/sites-enabled/default
    sudo nginx -t
    sudo systemctl restart nginx
    ```
    *验证*:
    ```bash
    sudo nginx -t
    # 预期输出: syntax is ok, test is successful
    curl localhost
    # 预期输出: 返回 HTML 内容
    ```

## 7. 系统操作指南

### 7.1 用户管理
*   **管理员账号**: 系统初始化时没有用户，需要手动插入或注册。
*   **添加管理员 SQL**:
    ```sql
    INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'ADMIN');
    INSERT INTO users (username, password, role) VALUES ('op1', 'op123', 'OPERATOR');
    ```
*   *验证*: 登录 MySQL 查询 `users` 表确认数据已插入。

### 7.2 服务器管理
1.  使用管理员账号登录系统。
2.  进入 **Server Management** (服务器管理) 页面。
3.  添加服务器信息 (IP, SSH账号, 密码)。
4.  将服务器分配给操作员 (例如 'op1')。
*   *验证*: 在列表中能看到新添加的服务器。

### 7.3 运维操作流程
1.  **操作员登录**: 使用 'op1' 登录。
2.  **获取凭证**: 查看分配的服务器，获取 SSH IP/账号/密码。
3.  **执行运维**: 使用终端工具 SSH 连接到目标服务器并执行命令。
    *   *注意*: 确保目标服务器会将命令记录到 `/var/log/ssh_commands.log`。
4.  **自动审计**: 后端系统会自动读取日志，计算哈希，检查敏感命令 (如 `rm`, `sudo`)，并上链。
5.  **查看日志**: 进入 **Audit Logs** (审计日志) 页面。
    *   敏感操作会以**红色**高亮显示。
    *   日志数据来自区块链/数据库。

### 7.4 日志模拟 (演示用)
如果你没有配置 `bashrc` 钩子，可以通过追加文件内容来模拟日志：
```bash
echo "2026-02-19 12:00:00 | SSH_IP:192.168.1.10 | USER:ubuntu | PWD:/home | COMMAND:sudo rm -rf /" >> /var/log/ssh_commands.log
```
*验证*: 刷新前端页面，应该在 5 秒内看到新的敏感操作日志。

## 8. 常见问题排查 (Troubleshooting)

*   **Fabric 连接错误**:
    *   检查 `application.properties` 路径是否正确。
    *   注意: 私钥文件名通常是 `priv_sk` 或者一串哈希值，请务必检查实际文件名。
*   **链码错误**:
    *   确保链码已安装并提交 (Committed)。
    *   使用 `docker logs` 查看链码容器日志。
*   **前端 404**:
    *   检查 Nginx 配置中的 `root` 路径是否正确指向 `dist` 目录。
    *   检查 `proxy_pass` 是否正确指向后端端口 (8080)。
