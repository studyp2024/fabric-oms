#!/bin/bash

# Network Operation Audit System - Deployment Script for Ubuntu 22.04
# Author: Trae AI

set -e

echo "=========================================================="
echo "   Network Operation Audit System - Deployment Script     "
echo "=========================================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root"
  exit 1
fi

PROJECT_ROOT=$(pwd)
DB_NAME="oms_db"
DB_USER="root"
DB_PASS="123456" # CHANGE THIS IN PRODUCTION

# 1. Update System
echo "[1/6] Updating system packages..."
apt-get update && apt-get upgrade -y
apt-get install -y curl git build-essential openjdk-8-jdk maven nodejs npm mysql-server docker.io docker-compose

# 2. Setup MySQL
echo "[2/6] Setting up MySQL..."
systemctl start mysql
systemctl enable mysql

# Secure MySQL installation (Automated)
# This is a basic setup. In production, use mysql_secure_installation manually.
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASS';"
mysql -u$DB_USER -p$DB_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
mysql -u$DB_USER -p$DB_PASS < $PROJECT_ROOT/database/schema.sql

echo "Database initialized."

# 3. Setup Fabric Network (Placeholder)
echo "[3/6] Setting up Hyperledger Fabric..."
echo "NOTE: This script assumes you will manually deploy the Fabric network."
echo "Please refer to the official Fabric documentation or samples to start a test network."
echo "Once started, update application/backend/src/main/resources/application.properties with correct paths."

# 4. Build Backend
echo "[4/6] Building Backend..."
cd $PROJECT_ROOT/application/backend
mvn clean package -DskipTests

# 5. Build Frontend
echo "[5/6] Building Frontend..."
cd $PROJECT_ROOT/application/web
# Install nvm to get a newer node version if needed, but apt installed nodejs should be sufficient for simple vue apps
# or use npm install -g n
npm install
npm run build

# 6. Deployment Info
echo "=========================================================="
echo "   Deployment Preparation Complete!                       "
echo "=========================================================="
echo ""
echo "To start the Backend:"
echo "  cd $PROJECT_ROOT/application/backend"
echo "  java -jar target/audit-0.0.1-SNAPSHOT.jar"
echo ""
echo "To serve the Frontend:"
echo "  You can use Nginx to serve the 'dist' folder generated in application/web/dist"
echo "  Or for development: cd $PROJECT_ROOT/application/web && npm run dev"
echo ""
echo "IMPORTANT: Ensure Hyperledger Fabric is running and certificates are configured in application.properties"
