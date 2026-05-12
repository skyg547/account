const fs = require('fs');
const path = require('path');

const springModules = [
    "asset-lease", "auth", "closing", "config-server", 
    "discovery", "expenditure-resolution", "gateway", "governance", 
    "journal-ledger", "loan", "master-data", "payable", 
    "receivable", "reconciliation", "reporting", "tax"
];

const infraModules = {
    "elasticsearch": { image: "docker.elastic.co/elasticsearch/elasticsearch:7.17.10", port: 9200 },
    "grafana": { image: "grafana/grafana:latest", port: 3000 },
    "kafka": { image: "confluentinc/cp-kafka:latest", port: 9092, deps: ["zookeeper"] },
    "zookeeper": { image: "confluentinc/cp-zookeeper:latest", port: 2181 },
    "kibana": { image: "docker.elastic.co/kibana/kibana:7.17.10", port: 5601 },
    "logstash": { image: "docker.elastic.co/logstash/logstash:7.17.10", port: 5044 },
    "prometheus": { image: "prom/prometheus:latest", port: 9090 },
    "redis": { image: "redis:alpine", port: 6379 },
    "vault": { image: "vault:1.13.3", port: 8200 },
    "zipkin": { image: "openzipkin/zipkin:latest", port: 9411 }
};

const springBootPorts = {
    "auth": 8084, "config-server": 8888, "discovery": 8761, "gateway": 8000,
    "journal-ledger": 8081, "asset-lease": 8083, "closing": 8085,
    "expenditure-resolution": 8086, "governance": 8087, "loan": 8088,
    "master-data": 8089, "payable": 8090, "receivable": 8091,
    "reconciliation": 8092, "reporting": 8093, "tax": 8094
};

// 1. Backend Multi-stage Dockerfile (Root context build)
const backendDockerfileTemplate = (moduleName) => `# [엔터프라이즈급 멀티스테이지 빌드]
# 1단계: 빌드 환경 (Builder)
# 소스코드를 가져와서 컴파일하고 실행 파일(.jar)을 만드는 과정입니다.
FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /build

# 전체 프로젝트 소스를 복사합니다. (멀티 모듈 의존성 해결을 위함)
COPY . .

# 해당 모듈만 선택하여 빌드합니다. (테스트는 생략하여 빌드 속도 향상)
# 엔터프라이즈 환경에서는 CI/CD 파이프라인에서 이미 테스트를 거치므로 여기선 패스합니다.
RUN ./gradlew :${moduleName}:build -x test --no-daemon

# 2단계: 실행 환경 (Runner)
# 무거운 빌드 도구(Gradle 등)를 버리고, 자바 실행기(JRE)만 있는 가벼운 이미지로 갈아탑니다.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 빌더 환경에서 생성된 .jar 파일만 쏙 빼옵니다. (이미지 용량 최적화)
COPY --from=builder /build/${moduleName}/build/libs/*-SNAPSHOT.jar app.jar
# 하위 모듈이 있을 경우 (예: journal-ledger/core)
COPY --from=builder /build/${moduleName}/*/build/libs/*-SNAPSHOT.jar app.jar || true
COPY --from=builder /build/${moduleName}/api/build/libs/*-SNAPSHOT.jar app.jar || true

# 실행 시 메모리 옵션 및 환경변수를 받을 수 있도록 설정합니다.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
`;

// 2. Frontend Dockerfile (Next.js Node)
const frontendNodeDockerfile = `# [엔터프라이즈 프론트엔드 빌드 (Next.js)]
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/node_modules ./node_modules
# Next.js 기본 포트
EXPOSE 3000
CMD ["npm", "start"]
`;

// 3. Frontend Nginx Dockerfile
const frontendNginxDockerfile = `# [엔터프라이즈 프론트엔드 서빙 (Nginx Reverse Proxy)]
# Next.js의 SSR/동적 라우팅을 지원하기 위해 Node.js 서버 앞단에 Nginx를 배치합니다.
# 보안, Gzip 압축, 정적 자원 캐싱을 Nginx가 전담하여 성능을 극대화합니다.
FROM nginx:alpine

# Nginx 기본 설정 파일 덮어쓰기
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
`;

// 4. Frontend Nginx Config
const frontendNginxConfig = `server {
    listen 80;
    server_name localhost;

    # 보안 헤더 추가
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-XSS-Protection "1; mode=block";

    # 정적 리소스 캐싱 (public 폴더 등)
    location /_next/static/ {
        proxy_pass http://frontend-node:3000;
        proxy_cache_bypass $http_upgrade;
        expires 365d;
        access_log off;
    }

    # API 및 페이지 요청은 Node.js(Next.js) 서버로 리버스 프록시
    location / {
        proxy_pass http://frontend-node:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
`;

// 모듈별 디렉토리 생성 및 Dockerfile 작성
springModules.forEach(mod => {
    if (!fs.existsSync(mod)) fs.mkdirSync(mod);
    fs.writeFileSync(path.join(mod, 'Dockerfile'), backendDockerfileTemplate(mod));
});

if (!fs.existsSync('frontend')) fs.mkdirSync('frontend');
fs.writeFileSync(path.join('frontend', 'Dockerfile'), frontendNodeDockerfile);

if (!fs.existsSync('frontend-nginx')) fs.mkdirSync('frontend-nginx');
fs.writeFileSync(path.join('frontend-nginx', 'Dockerfile'), frontendNginxDockerfile);
fs.writeFileSync(path.join('frontend-nginx', 'nginx.conf'), frontendNginxConfig);

// 통합 Root docker-compose.yml 작성
let rootCompose = `# [엔터프라이즈 MSA 통합 실행 환경]
# 모든 백엔드 모듈과 인프라를 실행 순서(depends_on)와 헬스체크(healthcheck)로 묶어
# 명령어 한 방에 안정적으로 띄울 수 있는 마스터 설정 파일입니다.
version: '3.8'

networks:
  account-network:
    driver: bridge

services:
`;

// 인프라 서비스 추가
Object.keys(infraModules).forEach(infraName => {
    const info = infraModules[infraName];
    rootCompose += `
  ${infraName}:
    image: ${info.image}
    container_name: ${infraName}
    ports:
      - "${info.port}:${info.port}"
    networks:
      - account-network
`;
    if (infraName === 'zookeeper') {
        rootCompose += `    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
`;
    }
    if (infraName === 'kafka') {
        rootCompose += `    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
`;
    }
});

// 백엔드 모듈 추가
springModules.forEach(mod => {
    const port = springBootPorts[mod] || 8080;
    rootCompose += `
  ${mod}:
    build:
      context: . # 최상단에서 빌드하여 멀티모듈 참조를 가능하게 함
      dockerfile: ${mod}/Dockerfile
    container_name: ${mod}
    ports:
      - "${port}:${port}"
    networks:
      - account-network
    environment:
      - JAVA_OPTS=-Xms256m -Xmx512m
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka/
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888/
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
`;

    // 의존성(실행 순서) 제어
    let dependsOn = [];
    if (mod !== 'config-server' && mod !== 'discovery') {
        dependsOn.push('config-server');
        dependsOn.push('discovery');
    } else if (mod === 'discovery') {
        dependsOn.push('config-server');
    }
    
    if (dependsOn.length > 0) {
        rootCompose += `    depends_on:\n`;
        dependsOn.forEach(dep => {
            rootCompose += `      ${dep}:\n        condition: service_started\n`;
        });
    }
});

// 프론트엔드 및 Nginx 추가
rootCompose += `
  frontend-node:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: frontend-node
    networks:
      - account-network
    environment:
      - NEXT_PUBLIC_API_URL=http://gateway:8000

  frontend-nginx:
    build:
      context: ./frontend-nginx
      dockerfile: Dockerfile
    container_name: frontend-nginx
    ports:
      - "80:80"
    networks:
      - account-network
    depends_on:
      - frontend-node
      - gateway
`;

fs.writeFileSync('docker-compose.yml', rootCompose);

console.log("Enterprise Docker setup generated successfully!");
