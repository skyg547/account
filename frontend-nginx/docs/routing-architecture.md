# Nginx 단일 진입점 라우팅 및 헤더 아키텍처 (Routing & Header Architecture)

## 1. 개요
Nginx는 모든 인바운드 HTTP/WebSocket 연결을 받아 백엔드 컨테이너의 포트로 프록시하는 Layer 7 리버스 프록시입니다.

## 2. 서브패스별 프록시 헤더 구성

### 2.1 Next.js (`/`, `/_next/static/`)
```nginx
location /_next/static/ {
    proxy_pass http://frontend:3000;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_cache_bypass $http_upgrade;
    expires 365d;
    access_log off;
}

location / {
    proxy_pass http://frontend:3000;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_cache_bypass $http_upgrade;
}
```

### 2.2 Spring Cloud Gateway (`/api/`)
```nginx
location /api/ {
    proxy_pass http://gateway:8000;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Prefix /api;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
}
```

### 2.3 Grafana (`/grafana/`)
```nginx
location /grafana/ {
    proxy_pass http://grafana:3000;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
}
```

### 2.4 Zipkin (`/zipkin/`)
```nginx
location /zipkin/ {
    proxy_pass http://zipkin:9411/zipkin/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### 2.5 Kibana (`/kibana/`)
```nginx
location /kibana/ {
    proxy_pass http://kibana:5601;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Prefix /kibana;
}
```

### 2.6 pgAdmin (`/pgadmin/`)
```nginx
location /pgadmin/ {
    proxy_pass http://pgadmin:80/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Script-Name /pgadmin;
    proxy_redirect off;
}
```