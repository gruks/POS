========================================
  RESTAURANT POS SYSTEM
  READ THIS FIRST!
========================================

TO RUN THE APPLICATION:

    Double-click: START-POS.bat

That's all you need to do!


FIRST TIME SETUP:

If this is your first time:

1. Make sure Java, Maven, and PostgreSQL are installed
   - Run: check-requirements.bat to verify

2. If anything is missing:
   - Right-click: setup-environment.bat
   - Select "Run as administrator"
   - Follow the prompts

3. After setup, double-click: START-POS.bat


WHAT EACH FILE DOES:

START-POS.bat          - Main launcher (USE THIS!)
run-pos.ps1            - PowerShell version
check-requirements.bat - Check if everything is installed
setup-environment.bat  - Install Java, Maven, PostgreSQL
fix-maven-path.bat     - Fix Maven PATH issues
HOW_TO_RUN.txt         - Detailed running instructions
README.md              - Complete documentation


TROUBLESHOOTING:

Problem: Application doesn't start
Solution: Make sure you're using START-POS.bat

Problem: "Maven not found" error
Solution: Use START-POS.bat (it handles this automatically)

Problem: Database connection error
Solution: Make sure PostgreSQL is running
         Run: net start postgresql-x64-16

Problem: Need more help
Solution: Read HOW_TO_RUN.txt or README.md


AFTER FIRST RUN:

1. Click "Restaurant Info" button
2. Fill in your restaurant details
3. Add menu items in Menu view
4. Add tables in Tables view
5. Start taking orders!


SUPPORT FILES:

- HOW_TO_RUN.txt - Simple instructions
- README.md - Complete documentation
- QUICK_START.md - Quick start guide
- SETUP_GUIDE.md - Detailed setup
- MAVEN_PATH_FIX.md - Maven troubleshooting


REMEMBER:

Always use START-POS.bat to run the application.
It's the easiest and most reliable method!


For complete documentation, see README.md
