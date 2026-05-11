# SPARCS — Smart Parking and RFID Control Management System

A Java desktop application that automates campus-level parking operations through RFID-based vehicle identification, real-time slot tracking, and a role-based management interface backed by a MySQL relational database.

Built for **CMSC 127 – File Processing and Database Systems**, Second Semester AY 2025–2026  
University of the Philippines Tacloban College

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Database Setup](#database-setup)
  - [Environment Configuration](#environment-configuration)
  - [Compiling](#compiling)
  - [Running](#running)
- [Admin Account Setup](#admin-account-setup)
- [Usage](#usage)
  - [Admin Portal](#admin-portal)
  - [User Portal](#user-portal)
- [Database Schema](#database-schema)
- [Sample Queries](#sample-queries)
- [Authors](#authors)

---

## Overview

SPARCS addresses three critical failure points common in Philippine university parking systems:

- **No digital trail** — paper-based records with no audit history
- **Slot uncertainty** — no real-time confirmation of available spaces
- **Fee inconsistency** — manual computation prone to error and lack of accountability

The system provides a centralized, database-backed platform for automated entry/exit processing, real-time slot availability tracking, consistent fee calculation, and a complete audit trail of all parking events.

---

## Features

**Admin Portal**
- Real-time dashboard with slot counts, revenue, and pending fees
- Revenue reports
- Interactive slot map
- Fee schedule configuration
- Simulated RFID entry/exit processing via ZXing barcodes
- Vehicle and owner registration and management
- Complete, read-only audit log of all system events
- Import/export in CSV, JSON, and SQL formats

**User Portal**
- Personalized dashboard with current slot, duration, estimated fee, and wallet balance
- Slot availability view by zone
- Full parking transaction history
- Fee schedule viewer with billing explanation
- RFID barcode card viewer

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| GUI | Java Swing + JavaFX |
| Database | MySQL 8.0 |
| DB Connectivity | JDBC (`mysql-connector-j-9.6.0`) |
| RFID Simulation | ZXing barcode library (`core-3.5.2`, `javase-3.5.2`) |
| Architecture | MVC with DAO layer, CardLayout navigation |

---

## Project Structure

```
SPARCS/
├── src/
│   └── sparcs/          # All Java source files
├── lib/                 # Bundled JAR dependencies
│   ├── core-3.5.2.jar
│   ├── javase-3.5.2.jar
│   └── mysql-connector-j-9.6.0.jar
├── assets/              # Application assets (icons, images)
├── db/
│   └── sparcs_db.sql    # Full database dump with sample data
├── out/                 # Compiled .class files (generated)
└── db.properties        # Database credentials (NOT committed — create manually)
```

---

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- MySQL Server 8.0 or higher

Verify your Java installation:
```bash
java --version
```

### Database Setup

1. Start your MySQL server.
2. Create the database:
```sql
CREATE DATABASE sparcs_db;
```
3. Import the provided SQL dump:
```bash
mysql -u root -p sparcs_db < db/sparcs_db.sql
```
4. Confirm all tables were created: `user_account`, `vehicle_owner`, `vehicle`, `rfid_mapping`, `parking_slot`, `fee_schedule`, `parking_transaction`, `audit_log`.

The dump includes sample data: 40 pre-configured parking slots across 5 zones (A–E), one sample vehicle owner, one registered vehicle, and three sample parking transactions.

### Environment Configuration

Create a file named `db.properties` in the **project root directory** (this file is gitignored — do not commit it):

```properties
db.url=jdbc:mysql://localhost:3306/sparcs_db
db.username=root
db.password=your_mysql_password
db.driver=com.mysql.cj.jdbc.Driver
```

### Compiling

**Windows (PowerShell)**
```powershell
New-Item -ItemType Directory -Force -Path out
Get-ChildItem -Recurse -Filter "*.java" -Path src | ForEach-Object {
  javac -d out -cp "lib/*;src/sparcs;assets" $_.FullName
}
Copy-Item "db.properties" "out/db.properties" -Force
```

**Linux / macOS**
```bash
mkdir -p out
find src -name "*.java" | xargs javac -d out -cp "lib/*:src/sparcs:assets"
cp db.properties out/db.properties
```

### Running

**Windows (PowerShell)**
```powershell
java -cp "out;lib/*;assets" SPARCS
```

**Linux / macOS**
```bash
java -cp "out:lib/*:assets" SPARCS
```

The application launches with a splash screen and animation, then presents the **Role Picker** screen.

---

## Admin Account Setup

Admin accounts are not created through the GUI. Use the bundled `HashGenerator` utility to generate a secure password hash, then insert the record directly into the database.

**Step 1 — Generate a password hash**

Windows:
```powershell
javac -d out -cp "lib/*;src/sparcs;assets" src/sparcs/HashGenerator.java
java -cp "out;lib/*" HashGenerator your_password
```

Linux / macOS:
```bash
javac -d out -cp "lib/*:src/sparcs:assets" src/sparcs/HashGenerator.java
java -cp "out:lib/*" HashGenerator your_password
```

**Step 2 — Insert the admin record into MySQL**
```sql
INSERT INTO user_account (username, password_hash, role, email)
VALUES ('admin_username', '<generated_hash>', 'ADMIN', 'admin@sparcs.com');
```

---

## Usage

### Admin Portal

| Screen | Description |
|---|---|
| Dashboard | Real-time counts of available/occupied slots, today's revenue, and pending fees |
| Slot Map | 40-slot interactive grid across Zones A–E; green = available, red = occupied |
| Entry / Exit | Enter or scan a vehicle's RFID/barcode to process entry or exit automatically |
| Vehicles | Searchable table of all registered vehicles; supports edit |
| Register Vehicle | Add a new owner and vehicle simultaneously |
| Fee Management | Configure fee schedules and view outstanding unpaid fees |
| Reports | Revenue summaries (today/week/month), weekly chart, top users |
| Audit Log | Read-only chronological feed of all system events |
| Import / Export | Backup and restore data in CSV, JSON, or SQL format |

### User Portal

| Screen | Description |
|---|---|
| My Status | Current slot, parking duration, estimated fee, wallet balance, total visits |
| Slot View | Available and occupied slot counts by zone |
| History | Chronological log of all past parking sessions |
| Fee Schedule | Current rates with a billing logic explainer |
| RFID Card | Scannable barcode for the registered vehicle |

New users may register directly from the User Login screen.

---

## Database Schema

```
VEHICLE_OWNER   (owner_id PK, user_id FK, full_name, phone_number, address, ...)
VEHICLE         (vehicle_id PK, plate_number, owner_id FK, vehicle_type, color, model,
                 parking_status, is_active, ...)
RFID_MAPPING    (rfid_id PK, rfid_tag_number, vehicle_id FK, status, ...)
USER_ACCOUNT    (user_id PK, username, password_hash, role, email, wallet_balance, ...)
PARKING_SLOT    (slot_id PK, slot_code, status, current_vehicle_id, zone, ...)
FEE_SCHEDULE    (fee_id PK, fee_name, fee_per_hour, daily_rate, grace_period_minutes, ...)
PARKING_TRANS.  (transaction_id PK, vehicle_id FK, slot_id FK, entry_time, exit_time,
                 duration_minutes, calculated_fee, payment_status, status, ...)
AUDIT_LOG       (log_id PK, user_id FK, action, resource_type, resource_id,
                 changes_log, ip_address, created_at)
```

All schemas are in 3NF. The database is named `sparcs_db` and runs on MySQL 8.0.

---

## Sample Queries

**Vehicle entry sequence**
```sql
-- 1. Look up vehicle by RFID tag
SELECT v.* FROM vehicle v
JOIN rfid_mapping r ON v.vehicle_id = r.vehicle_id
WHERE r.rfid_tag_number = ? AND r.status = 'Active';

-- 2. Find an available slot
SELECT slot_id FROM parking_slot WHERE status = 'AVAILABLE' LIMIT 1;

-- 3. Record the transaction
INSERT INTO parking_transaction (vehicle_id, slot_id, entry_time, payment_status, status)
VALUES (?, ?, NOW(), 'PENDING', 'IN_PROGRESS');

-- 4. Mark slot as occupied
UPDATE parking_slot SET status = 'OCCUPIED', current_vehicle_id = ?, entry_time = NOW()
WHERE slot_id = ?;

-- 5. Update vehicle parking status
UPDATE vehicle SET parking_status = 'Parked' WHERE vehicle_id = ?;
```

**Revenue report**
```sql
SELECT DATE(entry_time) AS parking_date,
       COUNT(*) AS total_transactions,
       SUM(calculated_fee) AS total_revenue
FROM parking_transaction
WHERE status = 'COMPLETED'
GROUP BY DATE(entry_time)
ORDER BY parking_date DESC;
```

All database operations use JDBC prepared statements to prevent SQL injection.

---

## Troubleshooting

| Problem | Check |
|---|---|
| App won't launch | Confirm `java --version` returns 21+ |
| DB connection error | Confirm MySQL is running and `db.properties` credentials are correct |
| Missing tables | Re-run the SQL dump import |
| `ClassNotFoundException` | Confirm all JARs are in `lib/` and the classpath is correct |
| `out/` directory missing | Create it manually before compiling |

---

## Authors

**Abesia, Mark Asher G. · Adona, Cueshe Alyannah E. · Balano, Marriane A.**  
CMSC 127 – File Processing and Database Systems  
University of the Philippines Tacloban College  
Second Semester AY 2025–2026