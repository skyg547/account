@echo off
echo Starting Modern Finance System Applications...

echo [1/6] Starting Discovery Service (Eureka)...
start "Discovery" cmd /c "gradlew :discovery:bootRun"
timeout /t 20

echo [2/6] Starting Config Server...
start "Config-Server" cmd /c "gradlew :config-server:bootRun"
timeout /t 20

echo [3/6] Starting Auth Service...
start "Auth-Service" cmd /c "gradlew :auth:bootRun"
timeout /t 10

echo [4/6] Starting API Gateway...
start "Gateway" cmd /c "gradlew :gateway:bootRun"
timeout /t 10

echo [5/6] Starting Master Data Service...
start "Master-Data" cmd /c "gradlew :master-data:bootRun"
timeout /t 10

echo [6/6] Starting Journal Ledger Service...
start "Journal-Ledger" cmd /c "gradlew :journal-ledger:api:bootRun"

echo All applications are requested to start. 
echo Please check the individual windows for log output.
pause
