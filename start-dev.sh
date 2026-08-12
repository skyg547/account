#!/bin/bash
# MSA 개발 환경 통합 실행 스크립트 (모듈별 Dockerfile 환경)

echo "MSA 개발 환경 통합 구동 (Podman / Docker Compose)"
echo "사용할 인프라 프로파일을 선택하거나, 기본 프로파일(platform,apis)로 구동합니다."

PROFILES=${1:-"platform,apis,self-contained"}

# Windows 환경 호환성을 위해 COMPOSE_PROFILES 환경변수를 명시적으로 내보냅니다.
export COMPOSE_PROFILES=$PROFILES

echo "활성화된 프로파일: $COMPOSE_PROFILES"
echo "docker-compose up --build -d"

# 백그라운드로 서비스 기동
docker-compose up --build -d

echo ""
echo "=== 기동 상태 확인 ==="
docker-compose ps
