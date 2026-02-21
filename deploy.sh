#!/bin/bash

# Network Operation Audit System - Full Deployment Script for Ubuntu 22.04
# Author: Trae AI
# Reference: DEPLOY_UBUNTU.md

set -e

echo "=========================================================="
echo "   Network Operation Audit System - Deployment Script     "
echo "=========================================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (e.g., sudo ./deploy.sh)"
  exit 1
fi

PROJECT_ROOT=$(pwd)
DB_NAME="oms_db"
DB_USER="root"
DB_PASS="123456" # CHANGE THIS IN PRODUCTION

# 1. Update System & Install Base Tools
echo "[1/8] Updating system and installing base tools..."
apt-get update && apt-get upgrade -y
apt-get install -y curl git build-essential unzip jq

# 2. Install Docker & Docker Compose
echo "[2/8] Installing Docker..."
if ! command -v docker &> /dev/null; then
    apt-get install -y docker.io docker-compose
    usermod -aG docker $USER
    # Note: Changes to group membership require re-login or newgrp to take effect for the current user
else
    echo "Docker is already installed."
fi

# 3. Install Go (User specified method)
echo "[3/8] Installing Go..."
if ! command -v go &> /dev/null; then
    wget https://golang.google.cn/dl/go1.26.0.linux-amd64.tar.gz
    tar -C /usr/local -xzf go1.26.0.linux-amd64.tar.gz
    rm go1.26.0.linux-amd64.tar.gz
    
    mkdir -p $HOME/go
    
    # Add to .bashrc if not present
    if ! grep -q "export GOROOT=/usr/local/go" ~/.bashrc; then
        echo 'export GOPATH=$HOME/go' >> ~/.bashrc
        echo 'export GOROOT=/usr/local/go' >> ~/.bashrc
        echo 'export PATH=$GOROOT/bin:$PATH' >> ~/.bashrc
        echo 'export PATH=$GOPATH/bin:$PATH' >> ~/.bashrc
    fi
    
    # Export for current session
    export GOPATH=$HOME/go
    export GOROOT=/usr/local/go
    export PATH=$GOROOT/bin:$PATH:$GOPATH/bin
    
    go env -w GO111MODULE=on
    go env -w GOPROXY=https://goproxy.cn,direct
else
    echo "Go is already installed."
    # Ensure env vars are set for this script
    export GOPATH=$HOME/go
    export GOROOT=/usr/local/go
    export PATH=$GOROOT/bin:$PATH:$GOPATH/bin
fi
go version

# 4. Install Java 8 & Maven
echo "[4/8] Installing Java 8 & Maven..."
apt-get install -y openjdk-8-jdk maven
java -version
mvn -version

# 5. Install Node.js 18.x
echo "[5/8] Installing Node.js 18.x..."
if ! node -v | grep -q "v18"; then
    echo "Current Node version is not 18.x. Installing/Updating..."
    apt-get remove -y nodejs || true
    apt-get autoremove -y || true
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
else
    echo "Node.js 18.x is already installed."
fi
node -v

# 6. Setup MySQL
echo "[6/8] Setting up MySQL..."
systemctl start mysql
systemctl enable mysql

echo "Configuring database..."
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASS';" || true
mysql -u$DB_USER -p$DB_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
mysql -u$DB_USER -p$DB_PASS $DB_NAME < $PROJECT_ROOT/database/schema.sql

echo "Database initialized."

# 7. Setup Hyperledger Fabric
echo "[7/8] Setting up Hyperledger Fabric..."

FABRIC_DIR="$PROJECT_ROOT/blockchain"
SAMPLES_DIR="$FABRIC_DIR/fabric-samples"

if [ ! -d "$SAMPLES_DIR/bin" ]; then
    echo "Downloading Fabric binaries and samples..."
    cd $FABRIC_DIR
    curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && chmod +x install-fabric.sh
    ./install-fabric.sh docker samples binary
else
    echo "Fabric binaries already exist."
fi

# 8. Build Projects
echo "[8/8] Building Backend & Frontend..."

# Build Backend
if [ -d "$PROJECT_ROOT/application/backend" ]; then
    echo "Building Backend..."
    cd $PROJECT_ROOT/application/backend
    mvn clean package -DskipTests
else
    echo "Error: Backend directory not found!"
fi

# Build Frontend
if [ -d "$PROJECT_ROOT/application/web" ]; then
    echo "Building Frontend..."
    cd $PROJECT_ROOT/application/web
    npm install
    npm run build
else
    echo "Error: Frontend directory not found!"
fi

# Set permissions for Nginx/Web
chmod -R 755 $PROJECT_ROOT

echo ""
echo "=========================================================="
echo "   Environment Setup Complete!                            "
echo "=========================================================="
echo ""
echo "Please follow DEPLOY_UBUNTU.md to:"
echo "1. Start the network: cd blockchain/fabric-samples/test-network && ./network.sh up createChannel -c mychannel -ca"
echo "2. Deploy chaincode: ./network.sh deployCC -ccn audit -ccp ../chaincode/audit -ccl go -ccv 1.0 -ccs 1"
echo "3. Configure application.properties with new certificate paths."
echo "4. Run the application."
echo ""
