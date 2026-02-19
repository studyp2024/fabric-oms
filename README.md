# Network Security Audit System based on Consortium Blockchain

This project implements a network operation audit system using Hyperledger Fabric, Spring Boot, and Vue.js.

## Project Structure

- `application/backend`: Spring Boot Backend Application
- `application/web`: Vue.js Frontend Application
- `blockchain/chaincode`: Go Chaincode for Audit Logs
- `database`: MySQL Schema

## Prerequisites

- Java 17+
- Node.js 16+
- Docker & Docker Compose
- Hyperledger Fabric 2.4+ (Test Network recommended)
- MySQL 8.0

## Setup Instructions

### 1. Database Setup

Create the database and tables using the provided SQL script:

```bash
mysql -u root -p < database/schema.sql
```

### 2. Blockchain Setup (User Responsibility)

You need to deploy a Hyperledger Fabric network. You can use the `test-network` provided by Fabric samples.

1.  Start the network: `./network.sh up createChannel`
2.  Package and deploy the chaincode located in `blockchain/chaincode/audit`.
    - Chaincode Name: `audit`
    - Language: `go`
3.  Update `application/backend/src/main/resources/application.properties` with the correct paths to your network configuration:
    - `fabric.certPath`: Path to your user's sign certificate (e.g., `User1@org1.example.com-cert.pem`).
    - `fabric.keyPath`: Path to your user's private key (e.g., `priv_sk`).
    - `fabric.tlsCertPath`: Path to the peer's TLS CA certificate.
    - `fabric.peerEndpoint`: The peer's gRPC address (e.g., `localhost:7051`).

### 3. Backend Setup

Navigate to `application/backend`:

```bash
cd application/backend
./mvnw clean install
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`.
It will automatically sync logs from `ssh_commands.log` (in the project root) every 5 seconds.

### 4. Frontend Setup

Navigate to `application/web`:

```bash
cd application/web
npm install
npm run dev
```

Access the frontend at `http://localhost:3000`.

### 5. Usage

1.  Login with default credentials:
    - **Admin**: `admin` / `admin`
    - **Ops**: `ops` / `ops`
2.  View assigned servers.
3.  Check Audit Logs. The system automatically detects sensitive commands (e.g., `rm`, `sudo`, `cat /etc/shadow`).

## Features

- **User Management**: Admin assigns servers to Ops users.
- **Log Collection**: Reads local SSH logs (simulating remote collection).
- **Blockchain Storage**: Hashes and stores logs on Hyperledger Fabric.
- **Audit**: Smart contract detects sensitive operations.
- **Visualization**: Vue frontend for easy monitoring.
