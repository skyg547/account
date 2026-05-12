import os

modules = [
    "asset-lease", "auth", "closing", "config-repo", "config-server", 
    "contracts", "discovery", "elasticsearch", "expenditure-resolution", 
    "gateway", "governance", "grafana", "journal-ledger", "kafka", 
    "kibana", "loan", "logstash", "master-data", "payable", "prometheus", 
    "receivable", "reconciliation", "redis", "reporting", "shared-kernel", 
    "tax", "vault", "zipkin"
]

infra = {
    "elasticsearch": ("elasticsearch:7.17.10", 9200),
    "grafana": ("grafana/grafana:latest", 3000),
    "kafka": ("confluentinc/cp-kafka:latest", 9092),
    "kibana": ("docker.elastic.co/kibana/kibana:7.17.10", 5601),
    "logstash": ("docker.elastic.co/logstash/logstash:7.17.10", 5044),
    "prometheus": ("prom/prometheus:latest", 9090),
    "redis": ("redis:alpine", 6379),
    "vault": ("vault:1.13.3", 8200),
    "zipkin": ("openzipkin/zipkin:latest", 9411)
}

spring_boot_ports = {
    "auth": 8084,
    "config-server": 8888,
    "discovery": 8761,
    "gateway": 8000,
    "journal-ledger": 8081,
    "asset-lease": 8083,
    "closing": 8085,
    "expenditure-resolution": 8086,
    "governance": 8087,
    "loan": 8088,
    "master-data": 8089,
    "payable": 8090,
    "receivable": 8091,
    "reconciliation": 8092,
    "reporting": 8093,
    "tax": 8094
}

dockerfile_template = """FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
"""

compose_spring_template = """version: '3.8'
services:
  {module}:
    build: .
    container_name: {module}
    ports:
      - "{port}:{port}"
    networks:
      - account-network

networks:
  account-network:
    external: true
"""

compose_infra_template = """version: '3.8'
services:
  {module}:
    image: {image}
    container_name: {module}
    ports:
      - "{port}:{port}"
    networks:
      - account-network

networks:
  account-network:
    external: true
"""

for mod in modules:
    if not os.path.exists(mod):
        os.makedirs(mod)
        
    if mod in infra:
        # For infra, usually we just need a compose file, but the user asked for Dockerfile and docker-compose.yml
        # Let's write a wrapper Dockerfile just in case
        with open(f"{mod}/Dockerfile", "w") as f:
            f.write(f"FROM {infra[mod][0]}\n")
        with open(f"{mod}/docker-compose.yml", "w") as f:
            f.write(compose_infra_template.format(module=mod, image=infra[mod][0], port=infra[mod][1]))
    elif mod in spring_boot_ports:
        # Spring Boot app
        with open(f"{mod}/Dockerfile", "w") as f:
            f.write(dockerfile_template)
        with open(f"{mod}/docker-compose.yml", "w") as f:
            f.write(compose_spring_template.format(module=mod, port=spring_boot_ports[mod]))
    else:
        # Others like config-repo, docs, contracts, shared-kernel don't need real Dockerfiles to run as apps
        # But we'll provide placeholder Dockerfiles to satisfy the prompt "각 모듈별로 도커파일..."
        with open(f"{mod}/Dockerfile", "w") as f:
            f.write("FROM alpine:latest\nCMD [\"echo\", \"Not a runnable service\"]\n")
        with open(f"{mod}/docker-compose.yml", "w") as f:
            f.write(f"version: '3.8'\nservices:\n  {mod}:\n    build: .\n")

print("Generated Dockerfiles and docker-compose.yml for all modules.")
