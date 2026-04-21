@echo off
echo ========================================================
echo MSA 공통 도커 네트워크 (account-network) 생성 스크립트
echo ========================================================

docker network inspect account-network >nul 2>&1
if %errorlevel% neq 0 (
    echo 네트워크가 존재하지 않아 새로 생성합니다...
    docker network create account-network
    echo 생성 완료!
) else (
    echo 이미 account-network 네트워크가 존재합니다.
)

echo.
echo 이제 각 모듈 폴더(elasticsearch, logstash, kibana 등)로 이동하여
echo docker-compose up -d 명령어를 실행할 수 있습니다.
pause
