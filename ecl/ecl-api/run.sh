#!/bin/bash
# IFRS 9 Allowance API 실행 스크립트
# Podman을 사용하여 이미지를 리빌드하고 컨테이너를 재기동합니다.

echo "[Run] IFRS 9 Allowance API 컨테이너를 재기동합니다..."
podman-compose up -d --build --force-recreate
echo "[Run] IFRS 9 Allowance API 컨테이너가 정상적으로 기동되었습니다."
podman ps | grep allowance-api
