CREATE DATABASE IF NOT EXISTS oms_db;
USE oms_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip VARCHAR(255) NOT NULL,
    ssh_user VARCHAR(255) NOT NULL,
    ssh_password VARCHAR(255) NOT NULL,
    assigned_user_id BIGINT,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    timestamp VARCHAR(255),
    ip VARCHAR(255),
    user VARCHAR(255),
    pwd VARCHAR(255),
    command TEXT,
    is_sensitive BOOLEAN,
    hash VARCHAR(255)
);

-- Insert Sample Data
INSERT INTO users (username, password, role) VALUES ('admin', 'admin', 'ADMIN');
INSERT INTO users (username, password, role) VALUES ('ops', 'ops', 'OPS');

INSERT INTO servers (ip, ssh_user, ssh_password, assigned_user_id) VALUES ('192.168.1.100', 'ubuntu', 'password123', 2);
