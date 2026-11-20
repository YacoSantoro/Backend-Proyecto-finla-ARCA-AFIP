@echo off
REM Levanta backend (8080) y frontend (5500) en dos ventanas
cd /d "%~dp0"
start "BACKEND - Spring Boot :8080" cmd /k mvnw.cmd spring-boot:run
cd frontend
if not exist node_modules call npm install
call npm run build
start "FRONTEND - http-server :5500" cmd /k npm start
