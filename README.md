# Restaurant Point of Sale System

A comprehensive desktop-based Point of Sale application designed specifically for restaurants. Built with JavaFX for the user interface, Maven for project management, and PostgreSQL for data persistence. The system handles complete restaurant operations including billing, inventory management, table management, kitchen order tickets, staff payroll, and thermal receipt printing.

---

## Quick Start - Running the Application

**To run the application, simply double-click:**
```
START-POS.bat
```

That's it! The application window will open automatically.

**First time users:** If you haven't set up the system yet, see the Installation and Setup section below.

**Having issues?** See the Troubleshooting section or check HOW_TO_RUN.txt for detailed instructions.

---

## Project Overview

This POS system is a full-featured restaurant management application that streamlines daily operations from order taking to bill settlement. It provides an intuitive interface for managing tables, processing orders, tracking inventory, generating kitchen orders, managing staff, and printing thermal receipts. The application uses a modern tech stack with JavaFX for the frontend, Java for business logic, and PostgreSQL for reliable data storage.

---

## Technology Stack

**Frontend**
- JavaFX 17 - Desktop UI framework
- FXML - Declarative UI markup
- CSS - Custom styling and themes

**Backend**
- Java 17 - Core programming language
- Maven - Build automation and dependency management
- HikariCP - High-performance JDBC connection pooling

**Database**
- PostgreSQL 12+ - Relational database
- JDBC - Database connectivity

**Architecture**
- MVC (Model-View-Controller) pattern
- Service layer for business logic
- DAO pattern for data access
- Connection pooling for performance

**Additional Technologies**
- javax.print API - Thermal printer integration
- ESC/POS commands - Receipt formatting
- Java modules - Modular application structure

---

## Core Features

**Billing and Sales**
- Multi-tab billing interface for handling multiple orders simultaneously
- Real-time order management with add, edit, and remove item functionality
- Support for multiple order types: Dine-In, Takeaway, Online, Delivery
- Table selection and session management
- Payment method selection: Cash, Card, UPI
- Discount application (percentage or fixed amount)
- Tax calculation with CGST and SGST breakdown
- Automatic bill number generation
- Complete transaction history with search and filter
- Thermal receipt printing on 3-inch (80mm) printers
- Automatic bill settlement after printing

**Menu Management**
- Category-based menu organization
- Add, edit, and delete menu items
- Price management
- Per-item tax rate configuration
- Image support for menu items
- Search and filter functionality
- Category filtering
- Stock tracking for retail items

**Inventory Management**
- Product stock tracking
- Automatic inventory updates after sales
- Low stock alerts
- Retail item integration with menu
- Quantity management
- Rate and pricing control

**Table Management**
- Visual table layout
- Table status tracking (Available, Occupied, Reserved)
- Table session management
- Capacity configuration
- Order persistence per table
- Automatic status updates

**Kitchen Order Tickets (KOT)**
- Generate kitchen orders from billing
- KOT number generation
- Priority management
- Table and order type information
- Item-wise kitchen instructions
- Database persistence for tracking

**Restaurant Information**
- Configurable restaurant details
- Name, address, contact information
- GSTIN and FSSAI license numbers
- Information appears on printed bills
- Logo support (planned)

**Staff and Payroll**
- Staff member management
- Attendance tracking
- Salary calculation
- Payroll processing
- Staff performance tracking

**Dashboard and Analytics**
- Sales overview
- Revenue tracking
- Top-selling items
- Daily, weekly, monthly reports
- Visual charts and graphs

**Thermal Printer Integration**
- Automatic printer detection
- ESC/POS command support
- 3-inch (80mm) receipt format
- Restaurant info on receipts
- Itemized billing
- Tax breakdown
- Payment details
- Auto paper cutting

---

## System Requirements

**Required Software**
- Java JDK 17 or higher
- Apache Maven 3.6 or higher
- PostgreSQL 12 or higher
- Windows 10/11 (primary support)

**Optional Hardware**
- 3-inch (80mm) thermal printer with USB connection
- ESC/POS compatible printer (Epson, Star, XPrinter, etc.)

**System Specifications**
- Minimum 4GB RAM (8GB recommended)
- 500MB free disk space
- 1280x720 screen resolution minimum

---

## Installation and Setup

**Automated Setup (Recommended)**

1. Run the automated setup script as Administrator:
   - Right-click on setup-environment.bat
   - Select "Run as administrator"
   - Follow the on-screen prompts
   - The script will install Java, Maven, and PostgreSQL if missing
   - Database will be configured automatically

2. After installation completes, close and reopen command prompt

3. Run the application:
   ```
   Double-click: START-POS.bat
   ```
   
   Alternative methods:
   - Right-click run-pos.ps1 and select "Run with PowerShell"
   - Open PowerShell in project folder and run: .\run-pos.ps1
   - Open Command Prompt and run: mvn javafx:run

**Manual Setup**

1. Install Java JDK 17:
   - Download from https://adoptium.net/
   - Install and set JAVA_HOME environment variable
   - Add Java bin directory to PATH

2. Install Apache Maven:
   - Download from https://maven.apache.org/download.cgi
   - Extract to C:\Program Files\Apache\maven
   - Add Maven bin directory to PATH
   - Set M2_HOME environment variable

3. Install PostgreSQL:
   - Download from https://www.postgresql.org/download/windows/
   - Install with default settings
   - Remember the postgres user password
   - Default port: 5432

4. Create Database:
   ```
   psql -U postgres
   CREATE DATABASE pos_db;
   CREATE USER pos_user WITH PASSWORD 'pos_password';
   GRANT ALL PRIVILEGES ON DATABASE pos_db TO pos_user;
   ALTER DATABASE pos_db OWNER TO pos_user;
   ```

5. Configure Environment:
   - Copy .env.example to .env
   - Edit .env with your database credentials:
   ```
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=pos_db
   DB_USER=pos_user
   DB_PASSWORD=pos_password
   ```

6. Build the Project:
   ```
   mvn clean package
   ```

7. Run the Application:
   ```
   Double-click: START-POS.bat
   ```
   
   Or use command line:
   ```
   mvn javafx:run
   ```

**Verification**

Check if all requirements are installed:
```
Double-click: check-requirements.bat
```

Run comprehensive diagnostics:
```
system-diagnostics.bat
```

---

## Project Structure

```
gruks/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/pos/
│   │   │       ├── Main.java                    # Application entry point
│   │   │       ├── controller/                  # UI Controllers
│   │   │       │   ├── BillingController.java   # Billing and orders
│   │   │       │   ├── MenuController.java      # Menu management
│   │   │       │   ├── TablesController.java    # Table management
│   │   │       │   ├── KOTController.java       # Kitchen orders
│   │   │       │   ├── InventoryController.java # Inventory
│   │   │       │   ├── DashboardController.java # Analytics
│   │   │       │   ├── StaffPayrollController.java # Staff management
│   │   │       │   ├── RestaurantInfoController.java # Restaurant config
│   │   │       │   └── MainController.java      # Main navigation
│   │   │       ├── model/                       # Data Models
│   │   │       │   ├── MenuItem.java
│   │   │       │   ├── TableModel.java
│   │   │       │   ├── KOT.java
│   │   │       │   ├── RestaurantInfo.java
│   │   │       │   ├── StaffMember.java
│   │   │       │   └── InventoryItem.java
│   │   │       ├── service/                     # Business Logic
│   │   │       │   ├── MenuService.java
│   │   │       │   ├── TableService.java
│   │   │       │   ├── SalesService.java
│   │   │       │   ├── KOTService.java
│   │   │       │   ├── InventoryService.java
│   │   │       │   ├── PayrollService.java
│   │   │       │   └── RestaurantInfoService.java
│   │   │       ├── db/                          # Database Layer
│   │   │       │   ├── DatabaseConnection.java  # Connection management
│   │   │       │   ├── ConnectionPool.java      # HikariCP pooling
│   │   │       │   └── DatabaseInitializer.java # Schema setup
│   │   │       └── util/                        # Utilities
│   │   │           ├── ThermalPrinter.java      # Receipt printing
│   │   │           └── SimpleCache.java         # Caching
│   │   └── resources/
│   │       └── com/example/pos/
│   │           ├── view/                        # FXML UI Files
│   │           │   ├── MainLayout.fxml
│   │           │   ├── BillingView.fxml
│   │           │   ├── MenuView.fxml
│   │           │   ├── TablesView.fxml
│   │           │   ├── KOTView.fxml
│   │           │   ├── InventoryView.fxml
│   │           │   ├── DashboardView.fxml
│   │           │   ├── StaffPayrollView.fxml
│   │           │   └── RestaurantInfoView.fxml
│   │           └── styles/                      # CSS Stylesheets
│   └── test/                                    # Test Files
├── .env                                         # Database configuration
├── pom.xml                                      # Maven configuration
├── module-info.java                             # Java module descriptor
├── setup-environment.bat                        # Automated setup
├── check-requirements.bat                       # Requirements checker
├── system-diagnostics.bat                       # System diagnostics
├── run-pos.bat                                  # Application launcher
├── test-db-connection.bat                       # Database test
├── setup-local-database.sql                     # Database schema
└── README.md                                    # This file
```

---

## Database Schema

The application uses PostgreSQL with the following main tables:

**menu_items** - Menu products with prices and tax rates
**restaurant_tables** - Table information and capacity
**table_sessions** - Active table orders and sessions
**sales** - Completed transactions
**sale_items** - Individual items in each sale
**inventory** - Stock tracking for retail items
**kot** - Kitchen order tickets
**kot_items** - Items in each KOT
**staff_members** - Employee information
**staff_attendance** - Attendance records
**payroll** - Salary and payment records
**restaurant_info** - Restaurant configuration

Database initialization happens automatically on first run. Sample data can be loaded using the provided SQL scripts.

---

## How to Run the Application

**Easiest Method (Recommended)**

Simply double-click on:
```
START-POS.bat
```

The application window will open automatically.

**Alternative Methods**

Method 1: PowerShell Script
- Right-click on run-pos.ps1
- Select "Run with PowerShell"

Method 2: Command Line
- Open Command Prompt in project folder
- Run: mvn javafx:run

Method 3: PowerShell Command Line
- Open PowerShell in project folder
- Run: .\run-pos.ps1

**Note:** If you encounter "Maven not found" errors with batch files, use START-POS.bat which automatically handles Maven path issues.

---

## Usage Guide

**First Time Setup**

1. Launch the application using START-POS.bat
2. Configure restaurant information:
   - Click "Restaurant Info" button in top navigation
   - Fill in all required fields (marked with asterisk)
   - Save the configuration
3. Add menu items:
   - Navigate to Menu view
   - Click "Add Item" button
   - Enter item details, price, category, and tax rate
   - Save the item
4. Add tables:
   - Navigate to Tables view
   - Click "Add Table" button
   - Enter table name and capacity
   - Save the table
5. Configure thermal printer (optional):
   - Connect USB thermal printer
   - Windows will install drivers automatically
   - Verify printer in Control Panel > Devices and Printers

**Creating an Order**

1. Navigate to Billing view
2. Select order type (Dine-In, Takeaway, Online, Delivery)
3. Select table (for Dine-In orders)
4. Click menu items to add to order
5. Adjust quantities using +/- buttons
6. Apply discount if needed
7. Select payment method
8. Click "Print" button to print bill and settle

**Managing Tables**

1. Navigate to Tables view
2. View all tables with current status
3. Click on occupied table to view/edit order
4. Orders are automatically saved per table
5. Tables become available after bill settlement

**Kitchen Orders**

1. In Billing view, add items to order
2. Click "KOT" button to send to kitchen
3. KOT is saved with unique number
4. View all KOTs in KOT view
5. Track order status and priority

**Inventory Management**

1. Navigate to Inventory view
2. Add retail items that are sold directly
3. Set stock quantities and rates
4. Items automatically appear in menu under "Retail" category
5. Stock updates automatically after sales

**Staff Management**

1. Navigate to Staff & Payroll view
2. Add staff members with details
3. Mark attendance daily
4. Process payroll monthly
5. View salary reports

**Reports and Analytics**

1. Navigate to Dashboard view
2. View sales summary
3. Check top-selling items
4. Analyze revenue trends
5. Export reports (planned feature)

---

## Thermal Printer Setup

**Connecting Printer**

1. Connect 3-inch thermal printer via USB
2. Windows installs drivers automatically
3. Verify in Control Panel > Devices and Printers
4. Test print from Windows

**Supported Printers**

- XPrinter XP-80C (Budget option)
- Epson TM-T82 (Professional)
- Star TSP143III (Professional)
- Any ESC/POS compatible 80mm thermal printer

**Bill Format**

Printed bills include:
- Restaurant name and contact information
- GSTIN and FSSAI license numbers
- Bill number and date
- Customer name (if provided)
- Table and order type
- Itemized list with quantities and prices
- Subtotal, taxes (CGST/SGST), and total
- Payment method
- Thank you message

**Troubleshooting Printer Issues**

- Check USB connection
- Verify printer power
- Ensure paper is loaded
- Check printer appears in Windows devices
- Install printer drivers if needed
- See THERMAL_PRINTER_GUIDE.md for details

---

## Configuration

**Database Configuration (.env file)**

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=pos_db
DB_USER=pos_user
DB_PASSWORD=pos_password
```

**Application Settings**

- Tax rate: Configurable per menu item (default 5%)
- Bill number format: Timestamp-based unique numbers
- Table capacity: Configurable per table
- Currency: Indian Rupees (INR)
- Date format: DD/MM/YYYY HH:MM

---

## Troubleshooting

**Application Won't Start**

Quick Fix:
- Use START-POS.bat instead of other run scripts
- This script automatically handles Maven path issues

Detailed Checks:
- Run check-requirements.bat to verify installations
- Ensure PostgreSQL service is running
- Check database credentials in .env file
- Verify Java and Maven are installed

**Maven Not Found Error**

If you see "Maven is NOT installed or not in PATH":

Solution 1: Use START-POS.bat (Easiest)
- This script works even if Maven is not in PATH
- Simply double-click START-POS.bat

Solution 2: Fix Maven PATH
- Double-click: fix-maven-path.bat
- Choose option 2 (User PATH)
- Close and reopen Command Prompt

Solution 3: Manual PATH Fix
- See MAVEN_PATH_FIX.md for detailed instructions

For more help, see:
- HOW_TO_RUN.txt - Simple running instructions
- QUICK_START.md - Quick start guide
- MAVEN_PATH_FIX.md - Maven PATH troubleshooting

**Database Connection Failed**

- Check PostgreSQL service status
- Verify credentials in .env file
- Test connection using test-db-connection.bat
- Ensure port 5432 is not blocked

**Build Errors**

- Clean Maven cache: mvn clean
- Delete target folder
- Rebuild: mvn clean package
- Check Java version: java -version

**Printer Not Found**

- Check USB connection
- Verify printer in Windows devices
- Install printer drivers
- Restart application

**Tables Not Loading**

- Initialize database: run setup-local-database.sql
- Add tables in Tables view
- Check database connection
- See TROUBLESHOOTING_TABLES.md

For detailed troubleshooting, run system-diagnostics.bat to generate a comprehensive diagnostic report.

---

## Development

**Building from Source**

```
mvn clean package
```

**Running Tests**

```
mvn test
```

**Running Application**

Easiest way:
```
Double-click: START-POS.bat
```

Command line:
```
mvn javafx:run
```

PowerShell:
```
.\run-pos.ps1
```

**Creating Executable JAR**

```
mvn clean package
java -jar target/pos-desktop-0.1.0.jar
```

---

## Quick Reference

**Running the Application**
- START-POS.bat - Main launcher (recommended)
- run-pos.ps1 - PowerShell script
- mvn javafx:run - Direct Maven command

**Setup and Diagnostics**
- setup-environment.bat - Install all requirements (run as admin)
- check-requirements.bat - Verify installations
- system-diagnostics.bat - Detailed system check
- fix-maven-path.bat - Fix Maven PATH issues

**Database**
- test-db-connection.bat - Test database connection
- setup-local-database.sql - Database schema

**Documentation**
- README.md - This file (complete documentation)
- HOW_TO_RUN.txt - Simple running instructions
- QUICK_START.md - Quick start guide

---

## License

This project is proprietary software. All rights reserved.

---

## Support

For issues or questions:
1. Run system-diagnostics.bat for system information
2. Check documentation files in project root
3. Review console logs for error messages
4. Verify all requirements using check-requirements.bat

---

## Version

Current Version: 0.1.0
Last Updated: December 2025