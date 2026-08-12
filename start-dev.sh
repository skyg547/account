#!/usr/bin/env bash
set -e

# Default to running all necessary profiles for local development
export COMPOSE_PROFILES=${COMPOSE_PROFILES:-"self-contained,platform,apis"}

echo "Starting account MSA with profiles: $COMPOSE_PROFILES"
docker-compose up -d --build

echo "MSA environment started. Check status with 'docker-compose ps'."
