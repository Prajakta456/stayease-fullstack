-- One-time setup for a local MySQL 8 server. Run as an admin user (e.g. root in MySQL Workbench).
-- Replace CHANGE_ME with the DB_PASSWORD from airBnbApp/.env. Flyway creates the tables on first start.

CREATE DATABASE IF NOT EXISTS airbnb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'airbnb_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';

-- The app runs migrations itself, so it needs DDL rights on its own schema (and nothing else)
GRANT ALL PRIVILEGES ON airbnb.* TO 'airbnb_app'@'localhost';
FLUSH PRIVILEGES;
