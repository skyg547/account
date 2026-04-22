@echo off
echo Starting Modern Finance System Infrastructure...

echo [1/4] Starting Kafka...
cd kafka
docker-compose up -d
cd ..

echo [2/4] Starting ELK Stack...
cd elasticsearch
docker-compose up -d
cd ..

echo [3/4] Starting Zipkin...
cd zipkin
docker-compose up -d
cd ..

echo [4/4] Starting Monitoring (Prometheus/Grafana)...
cd prometheus
docker-compose up -d
cd ..

echo Infrastructure is starting up. Please wait a few minutes before starting applications.
pause
