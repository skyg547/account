#!/bin/bash
# Risk Data Mart Batch 실행 스크립트
# Podman을 사용하여 이미지를 리빌드하고 컨테이너를 실행합니다.

echo "[Run] Data Mart Batch 컨테이너를 실행합니다..."
podman-compose up -d --build --force-recreate
echo "[Run] Data Mart Batch 컨테이너 실행 명령이 전달되었습니다."
podman ps -a | grep mart-batch
