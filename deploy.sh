#!/bin/bash

# Network Operation Audit System - Deployment Script for Ubuntu 22.04
# Author: Trae AI

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
echo "[1/6] Updating system and installing base tools..."
apt-get update && apt-get upgrade -y
apt-get install -y curl git build-essential openjdk-8-jdk maven mysql-server docker.io docker-compose

# 2. Install Node.js 18.x (Required for Vite)
echo "[2/6] Installing Node.js 18.x..."
if ! node -v | grep -q "v18"; then
    echo "Current Node version is not 18.x. Installing/Updating..."
    # Remove old version if exists
    apt-get remove -y nodejs || true
    apt-get autoremove -y || true
    
    # Install Node 18
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
else
    echo "Node.js 18.x is already installed."
fi
node -v
npm -v

# 3. Setup MySQL
echo "[3/6] Setting up MySQL..."
systemctl start mysql
systemctl enable mysql

# Secure MySQL installation (Automated)
echo "Configuring database..."
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASS';" || true
mysql -u$DB_USER -p$DB_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
mysql -u$DB_USER -p$DB_PASS $DB_NAME < $PROJECT_ROOT/database/schema.sql

echo "Database initialized with default users (admin/admin, ops/ops)."

# 4. Setup Fabric Network (Placeholder)
echo "[4/6] Setting up Hyperledger Fabric..."
echo "NOTE: This script prepares the environment. You MUST manually start the network."
echo "Please follow 'DEPLOY_UBUNTU.md' -> Section 4 for detailed steps."
echo "Command hint: cd blockchain/fabric-samples/test-network && ./network.sh up createChannel -c mychannel -ca"

# 5. Build Backend
echo "[5/6] Building Backend..."
if [ -d "$PROJECT_ROOT/application/backend" ]; then
    cd $PROJECT_ROOT/application/backend
    mvn clean package -DskipTests
else
    echo "Error: Backend directory not found!"
    exit 1
fi

# 6. Build Frontend
echo "[6/6] Building Frontend..."
if [ -d "$PROJECT_ROOT/application/web" ]; then
    cd $PROJECT_ROOT/application/web
    npm install
    npm run build
else
    echo "Error: Frontend directory not found!"
    exit 1
fi

# 7. Deployment Info
echo ""
echo "=========================================================="
echo "   Deployment Preparation Complete!                       "
echo "=========================================================="
echo ""
echo "Next Steps:"
echo "1. Start Fabric Network & Deploy Chaincode (See DEPLOY_UBUNTU.md)"
echo "2. Update certificates path in: application/backend/src/main/resources/application.properties"
echo "3. Run Backend:"
echo "   cd application/backend"
echo "   java -jar target/audit-0.0.1-SNAPSHOT.jar"
echo "4. Run Frontend (Production):"
echo "   Configure Nginx to serve 'application/web/dist' (See DEPLOY_UBUNTU.md)"
echo ""
echo "For more details, please read DEPLOY_UBUNTU.md"
