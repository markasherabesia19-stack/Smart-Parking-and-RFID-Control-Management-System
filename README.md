# SPARCS — Smart Parking and RFID Control Management System

> A Java desktop application that automates campus-level parking operations through RFID-based vehicle identification, real-time slot tracking, and a role-based management interface backed by a MySQL relational database.

Built for **CMSC 127 – File Processing and Database Systems**, Second Semester AY 2025–2026
University of the Philippines Tacloban College

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Build & Distribution](#build--distribution)
- [Getting Started](#getting-started)
  - [Option A — End Users (Pre-built)](#option-a--end-users-pre-built)
  - [Option B — Developers (Build from Source)](#option-b--developers-build-from-source)
- [Default Credentials](#default-credentials)
- [Usage](#usage)
  - [Admin Portal](#admin-portal)
  - [User Portal](#user-portal)
- [Database Schema](#database-schema)
- [Sample Queries](#sample-queries)
- [Troubleshooting](#troubleshooting)
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

### Admin Portal
- Real-time dashboard with slot counts, revenue, and pending fees
- Interactive slot map across zones, color-coded by availability
- Simulated RFID entry/exit processing via ZXing barcodes
- Vehicle and owner registration and management
- Fee schedule configuration and outstanding fee tracking
- Revenue reports (today / weekly) with charts
- Complete, read-only audit log of all system events
- Account management (create, deactivate, reset passwords)
- Import/export in CSV formats

### User Portal
- Personalized dashboard: current slot, duration, estimated fee, wallet balance
- Slot availability view by zone
- Full parking transaction history
- Fee schedule viewer with billing explanation
- Scannable RFID barcode card

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| GUI | Java Swing + JavaFX |
| Database | MySQL 8.0 |
| DB Connectivity | JDBC (`mysql-connector-j-9.6.0`) |
| RFID Simulation | ZXing (`core-3.5.2`, `javase-3.5.2`) |
| Architecture | MVC with DAO layer, CardLayout navigation |


---

## Project Structure

```
SPARCS/
├── src/                 # Java source files
├── lib/                 # External JAR dependencies
│   ├── core-3.5.2.jar
│   ├── javase-3.5.2.jar
│   └── mysql-connector-j-9.6.0.jar
├── assets/              # Icons, images, and UI resources
├── db/
│   └── sparcs_db.sql    # Database schema and sample data
└── out/                 # Compiled .class files (generated)

```

> `out/` is generated during the build and is gitignored — do not commit it.

---

## Build & Distribution

```
build.bat                # Automates compilation and packaging

SPARCS_Submit/
├── SPARCS.jar           # Runnable fat JAR with bundled dependencies
├── run.bat              # Launch script
├── SETUP_FIRST.bat      # First-time database setup/import script
└── db/                  # SQL dump copy for deployment

```

> Distribution Notes
- build.bat is an internal build automation script used to compile and package the application.
- SPARCS_Submit/ contains the generated deployment-ready version of the system.
- The distributed package is intended for Windows environments and includes all required dependencies.

---


## Getting Started

### Option A — End Users (Pre-built)

> ✅ Recommended if you just want to **run the application** without setting up a Java development environment.

**Requirements:**
- Windows operating system
- [MySQL Server 8.0+](https://dev.mysql.com/downloads/mysql/) installed and running
- Java 21+ installed ([Download JDK 21](https://www.oracle.com/java/technologies/downloads/#java21))

**Steps:**

1. Download the pre-built distribution from Google Drive:
    **[SPARCS Google Drive](https://drive.google.com/drive/folders/1ui39P6RkPBnmqWNa50Dlg5hVeQNWolFN)**

2. Extract the downloaded zip to any folder.

3. Open the extracted folder and run **`SETUP_FIRST.bat`** to import the database:
   - It will prompt for your MySQL username and password.
   - This creates the `sparcs_db` database and all required tables automatically.

4. Open `db.properties` and confirm your MySQL credentials match:
   ```properties
   db.url=jdbc:mysql://localhost:3306/sparcs_db
   db.username=your_mysql_username
   db.password=your_mysql_password
   db.driver=com.mysql.cj.jdbc.Driver
   ```

5. Double-click **`SPARCS.exe`** (or run **`run.bat`**) to launch the application.

> ⚠️ Always run the app from **within the extracted folder**, not from inside the zip archive.

---

### Option B — Developers (Build from Source)

> ✅ Recommended if you want to **modify, contribute, or build** the project yourself.

**Requirements:**
- Windows operating system
- [JDK 21+](https://www.oracle.com/java/technologies/downloads/#java21) installed and on your `PATH`
- [MySQL Server 8.0+](https://dev.mysql.com/downloads/mysql/) installed and running
- Git

**Steps:**

1. Clone the repository:
   ```bash
   git clone https://github.com/markasherabesia19-stack/smart-parking-and-rfid-con.git
   cd smart-parking-and-rfid-con
   ```

2. Create a `db.properties` file in the **project root** (this file is gitignored — do not commit it):
   ```properties
   db.url=jdbc:mysql://localhost:3306/sparcs_db
   db.username=your_mysql_username
   db.password=your_mysql_password
   db.driver=com.mysql.cj.jdbc.Driver
   ```

3. Import the database:
   ```bash
   mysql -u root -p sparcs_db < db/sparcs_db.sql
   ```

4. Run the build script:
   ```bat
   build.bat
   ```
   This will:
   - Clean and recompile all source files
   - Merge all dependencies into a fat JAR
   - Copy assets and `db.properties`
   - Produce a ready-to-run **`SPARCS_Submit/`** folder containing:
     - `SPARCS.jar` — the application
     - `run.bat` — launch script
     - `SETUP_FIRST.bat` — database import helper
     - `db/` — SQL dump

5. Launch the application:
   ```bat
   cd SPARCS_Submit
   java -jar SPARCS.jar
   ```
   Or simply double-click **`run.bat`** inside `SPARCS_Submit/`.

> 💡 To produce a `SPARCS.exe`, wrap `SPARCS.jar` using [Launch4j](https://launch4j.sourceforge.net/) and point it to the JAR as the entry point.

---

## Default Credentials

After the first successful database setup, log in using the embedded administrator account:

| Field | Value |
|---|---|
| Username | `admin1` |
| Password | `cue` |

> ⚠️ It is **strongly recommended** to change the default password immediately after the first login via the **Manage Accounts** screen.

---

## Usage

### Admin Portal

| Screen | Description |
|---|---|
| Dashboard | Real-time counts of available/occupied slots, today's revenue, and pending fees |
| Slot Map | 40-slot interactive grid across Zones A–E; green = available, red = occupied, orange = reserved |
| Entry / Exit | Enter or scan a vehicle's RFID/barcode to process entry or exit automatically |
| Vehicles | Searchable table of all registered vehicles; supports edit |
| Register Vehicle | Add a new owner and vehicle simultaneously |
| Fee Management | Configure fee schedules; view outstanding unpaid fees |
| Reports | Revenue summaries (today/week/month), weekly chart, average duration, top users |
| Audit Log | Read-only chronological feed of all system events |
| Manage Accounts | Create, deactivate, and reset passwords for all user accounts |
| Import / Export | Backup and restore data in CSV, JSON, or SQL format |

### User Portal

| Screen | Description |
|---|---|
| My Status | Current slot, parking duration, estimated fee, wallet balance, total visits |
| Slot View | Available and occupied slot counts by zone |
| History | Chronological log of all past parking sessions |
| Fee Schedule | Current rates with a billing logic explainer |
| RFID Card | Scannable barcode for the registered vehicle (used by admin for entry/exit simulation) |

New users may register directly from the Login Screen. Self-registered accounts are automatically assigned the **USER** role. Administrator accounts can only be created through the **Manage Accounts** screen by an existing admin.

---


## Troubleshooting

| Problem | Solution |
|---|---|
| App won't launch | Confirm Java 21+ is installed (`java --version`) and that you're running from inside the extracted folder |
| DB connection error | Confirm MySQL is running and that `db.properties` credentials are correct |
| Missing tables | Re-run `SETUP_FIRST.bat` or re-import `db/sparcs_db.sql` manually |
| Default admin not working | Open MySQL Workbench, connect to `sparcs_db`, and confirm a row with `role = 'ADMIN'` exists in `user_account` |
| `build.bat` fails | Confirm JDK 21+ is on your `PATH` and that all JARs are present in `lib/` |

---

## Contributors

-  **Abesia, Mark Asher G.** - BSCS 2
-  **Adona, Cueshe Alyannah E.** - BSCS 2
-  **Balano, Marriane A.** - BSCS 2

**CMSC 127 – File Processing and Database Systems**  
University of the Philippines Tacloban College  
Second Semester AY 2025–2026