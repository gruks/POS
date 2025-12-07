# Maven PATH Fix Guide

## Problem

When you run `check-requirements.bat` or `run-pos.bat`, you see:
```
[2/3] Checking Apache Maven...
[X] Maven is NOT installed or not in PATH
```

But Maven IS installed on your system (you can see it works in PowerShell or IDE).

## Why This Happens

Maven is installed but not added to the Windows PATH environment variable. This means:
- Maven works in PowerShell (because PowerShell finds it)
- Maven works in your IDE (because IDE has its own Maven)
- Maven does NOT work in Command Prompt (CMD) batch files

## Solutions

### Solution 1: Use the Fix Script (Easiest)

1. Double-click on: `fix-maven-path.bat`
2. Choose option 2 (User PATH - No admin required)
3. Close all Command Prompt windows
4. Open a NEW Command Prompt
5. Test by running: `mvn -version`

### Solution 2: Use Direct Run Script

Instead of `run-pos.bat`, use:
```
run-pos-direct.bat
```

This script automatically finds Maven even if it's not in PATH.

### Solution 3: Manual PATH Configuration

**Step-by-step:**

1. Find your Maven installation folder:
   - Usually: `C:\Program Files\Apache\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11`
   - The `bin` folder inside contains `mvn.cmd`

2. Copy the full path to the `bin` folder:
   ```
   C:\Program Files\Apache\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin
   ```

3. Add to Windows PATH:
   - Press Windows key
   - Type "Environment Variables"
   - Click "Edit the system environment variables"
   - Click "Environment Variables" button
   - Under "User variables" (or "System variables" if admin):
     - Select "Path"
     - Click "Edit"
     - Click "New"
     - Paste the Maven bin path
     - Click OK on all windows

4. Restart Command Prompt:
   - Close ALL Command Prompt windows
   - Open a NEW Command Prompt
   - Test: `mvn -version`

### Solution 4: Reinstall with Automatic Setup

1. Right-click on: `setup-environment.bat`
2. Select "Run as administrator"
3. Let it install/configure Maven properly
4. Restart Command Prompt

## Verification

After applying any solution, verify Maven is working:

1. Open Command Prompt (NEW window)
2. Run: `mvn -version`
3. You should see:
   ```
   Apache Maven 3.9.11
   Maven home: C:\Program Files\Apache\Maven\...
   Java version: 17.0.12
   ```

## Running the Application

Once Maven is in PATH:

**Option 1: Using batch file**
```
run-pos.bat
```

**Option 2: Using direct script (works without PATH)**
```
run-pos-direct.bat
```

**Option 3: Manual command**
```
mvn javafx:run
```

## Common Mistakes

**Mistake 1: Not restarting Command Prompt**
- After adding to PATH, you MUST close and reopen Command Prompt
- Old windows won't see the new PATH

**Mistake 2: Wrong path**
- Make sure you add the `bin` folder, not just the Maven folder
- Correct: `...\apache-maven-3.9.11\bin`
- Wrong: `...\apache-maven-3.9.11`

**Mistake 3: Using PowerShell to test**
- PowerShell might find Maven even without PATH
- Always test in Command Prompt (CMD)

## Still Not Working?

If Maven still doesn't work after trying all solutions:

1. Run diagnostics:
   ```
   system-diagnostics.bat
   ```

2. Check the log file it creates

3. Verify Maven is actually installed:
   - Open File Explorer
   - Navigate to: `C:\Program Files\Apache\Maven`
   - You should see a folder like `apache-maven-3.9.11-bin`
   - Inside should be a `bin` folder with `mvn.cmd`

4. If Maven is not installed:
   - Run: `setup-environment.bat` (as Administrator)
   - Or download from: https://maven.apache.org/download.cgi

## Quick Reference

**Check if Maven is in PATH:**
```
where mvn
```

**Check Maven version:**
```
mvn -version
```

**Add Maven to PATH (current session only):**
```
set PATH=%PATH%;C:\Program Files\Apache\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin
```

**Run application without fixing PATH:**
```
run-pos-direct.bat
```

## Summary

The issue is that Maven is installed but not in the Windows PATH. You can either:
1. Fix the PATH using `fix-maven-path.bat`
2. Use `run-pos-direct.bat` which finds Maven automatically
3. Add Maven to PATH manually through Windows settings

After fixing, the application will run normally with `run-pos.bat`.
