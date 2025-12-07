# Quick Start Guide

## Running the Application

### Method 1: Using Batch File (Easiest)

Simply double-click on:
```
run-pos.bat
```

The application window will open automatically.

### Method 2: Using Command Prompt

1. Open Command Prompt
2. Navigate to project folder:
   ```
   cd E:\Projects\POSapp\gruks
   ```
3. Run:
   ```
   mvn javafx:run
   ```

## First Time Setup

If this is your first time running the application:

1. Double-click: `check-requirements.bat`
   - This will verify Java, Maven, and PostgreSQL are installed

2. If anything is missing:
   - Right-click on `setup-environment.bat`
   - Select "Run as administrator"
   - Follow the prompts

3. After setup, double-click: `run-pos.bat`

## Troubleshooting

### Maven Not Found Error

If you see "Maven is NOT installed or not in PATH":

**Solution 1: Fix Maven PATH (Easiest)**
```
Double-click: fix-maven-path.bat
Choose option 2 (User PATH)
Close and reopen Command Prompt
```

**Solution 2: Use Direct Run Script**
```
Double-click: run-pos-direct.bat
This script finds Maven automatically
```

**Solution 3: Add Maven to PATH Manually**
1. Search "Environment Variables" in Windows
2. Click "Edit the system environment variables"
3. Click "Environment Variables" button
4. Under "User variables", select "Path" and click "Edit"
5. Click "New" and add: `C:\Program Files\Apache\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin`
6. Click OK on all windows
7. Close and reopen Command Prompt

### Application doesn't start

**Check 1: Verify requirements**
```
Double-click: check-requirements.bat
```

**Check 2: Ensure PostgreSQL is running**
```
Open Command Prompt and run:
sc query postgresql-x64-16
```

If not running, start it:
```
net start postgresql-x64-16
```

**Check 3: Rebuild the project**
```
Open Command Prompt in project folder and run:
mvn clean package
```

### Build errors

If you see compilation errors:

1. Make sure all files are present
2. Run clean build:
   ```
   mvn clean compile
   ```
3. If errors persist, check the error messages

### Database connection errors

1. Check if PostgreSQL service is running
2. Verify .env file has correct credentials:
   ```
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=pos_db
   DB_USER=pos_user
   DB_PASSWORD=pos_password
   ```

## Common Issues and Solutions

**Issue: "Java is not installed"**
- Solution: Run `setup-environment.bat` as administrator

**Issue: "Maven is not installed"**
- Solution: Run `setup-environment.bat` as administrator

**Issue: "PostgreSQL is not installed"**
- Solution: Run `setup-environment.bat` as administrator

**Issue: "Cannot connect to database"**
- Solution: Start PostgreSQL service: `net start postgresql-x64-16`

**Issue: "ThermalPrinter class not found"**
- Solution: The file was missing. It has been recreated. Run: `mvn clean compile`

## What Was Fixed

The application wasn't starting because:
1. The ThermalPrinter.java file was missing from the util folder
2. This caused compilation errors when trying to run the application

Solution:
- Recreated the ThermalPrinter.java file
- Rebuilt the project with `mvn clean compile`
- Now the application runs successfully

## Next Steps

After the application starts:

1. Configure Restaurant Information
   - Click "Restaurant Info" button
   - Fill in all required fields
   - Save

2. Add Menu Items
   - Navigate to Menu view
   - Add your menu items with prices

3. Add Tables
   - Navigate to Tables view
   - Add tables for your restaurant

4. Start Taking Orders
   - Navigate to Billing view
   - Select table and order type
   - Add items and process payment

## Support

For more help:
- Run: `system-diagnostics.bat` for detailed system information
- Check: `README.md` for complete documentation
- See: `SETUP_GUIDE.md` for detailed setup instructions
