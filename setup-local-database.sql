-- Local PostgreSQL Database Setup Script
-- Run this after installing PostgreSQL

-- Connect to PostgreSQL:
-- psql -U postgres -p 5433

-- Create database
CREATE DATABASE posdb;

-- Connect to the new database
\c posdb

-- Verify connection
SELECT current_database(), current_user, version();

-- Note: Tables will be created automatically by DatabaseInitializer
-- Or run: mvn compile exec:java -Dexec.mainClass="com.example.pos.db.DatabaseInitializer"

-- Show all databases
\l

-- Exit
\q
