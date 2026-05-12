const fs = require('fs');
const path = require('path');

const modules = [
    "asset-lease", "auth", "closing", "config-repo", "config-server", 
    "contracts", "discovery", "elasticsearch", "expenditure-resolution", 
    "gateway", "governance", "grafana", "journal-ledger", "kafka", 
    "kibana", "loan", "logstash", "master-data", "payable", "prometheus", 
    "receivable", "reconciliation", "redis", "reporting", "shared-kernel", 
    "tax", "vault", "zipkin"
];

const infra = {
    "elasticsearch": ["docker.elastic.co/elasticsearch/elasticsearch:7.17.10", 9200],
    "grafana": ["grafana/grafana:latest", 3000],
    "kafka": ["confluentinc/cp-kafka:latest", 9092],
    "kibana": ["docker.elastic.co/kibana/kibana:7.17.10", 5601],
    "logstash": ["docker.elastic.co/logstash/logstash:7.17.10", 5044],
    "prometheus": ["prom/prometheus:latest", 9090],
    "redis": ["redis:alpine", 6379],
    "vault": ["vault:1.13.3", 8200],
    "zipkin": ["openzipkin/zipkin:latest", 9411]
};

const springBootPorts = {
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
};

const dockerfileTemplate = `# [초보자를 위한 개념 설명]
# Dockerfile은 이 애플리케이션을 실행하기 위한 '가상 컴퓨터(컨테이너)의 레시피'입니다.

# 1. 어떤 환경에서 실행할 것인가? (운영체제 및 자바 버전 설정)
# eclipse-temurin:21-jre-alpine은 가볍고(alpine) 자바 21(jre)이 미리 설치된 환경을 의미합니다.
FROM eclipse-temurin:21-jre-alpine

# 2. 가상 컴퓨터 내부에서 작업할 기본 폴더를 지정합니다.
WORKDIR /app

# 3. 로컬 컴퓨터(내 PC)에서 빌드된 애플리케이션 파일(.jar)을 가상 컴퓨터의 /app 폴더로 복사합니다.
# (주의: 이 파일을 실행하려면 먼저 터미널에서 ./gradlew build 를 통해 .jar 파일을 만들어야 합니다)
COPY build/libs/*.jar app.jar

# 4. 가상 컴퓨터가 켜질 때 이 애플리케이션을 실행하는 명령어입니다.
# 터미널에서 'java -jar app.jar'를 치는 것과 똑같습니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
`;

const composeSpringTemplate = (moduleName, port) => `# [초보자를 위한 개념 설명]
# docker-compose.yml은 복잡한 도커 실행 명령어(docker run ...)를 쉽게 설정 파일로 만들어둔 것입니다.
# 'docker-compose up -d' 명령어 하나로 아래 설정된 컨테이너를 한 번에 띄울 수 있습니다.

version: '3.8' # Docker Compose 파일의 문법 버전입니다.

services: # 실행할 서비스(컨테이너)들의 목록을 정의합니다.
  ${moduleName}:
    # build: '.' 은 현재 폴더(.)에 있는 Dockerfile을 읽어서 새로운 이미지를 굽겠다는 의미입니다.
    build: .
    
    # 컨테이너의 이름을 지정합니다. (터미널에서 docker ps 로 볼 때 나타나는 이름)
    container_name: ${moduleName}
    
    # 포트 연결 (호스트_포트:컨테이너_내부_포트)
    # 내 PC의 ${port} 포트로 접속하면, 가상 컴퓨터(컨테이너)의 ${port} 포트로 연결해달라는 뜻입니다.
    ports:
      - "${port}:${port}"
      
    # 네트워크 설정: 다른 MSA 모듈들과 통신하기 위해 공통 네트워크망에 합류합니다.
    networks:
      - account-network

# 사용할 공통 네트워크를 정의합니다.
networks:
  account-network:
    external: true # 이 네트워크는 외부(다른 곳)에서 이미 생성되어 있다는 뜻입니다. (docker network create account-network 로 미리 생성 필요)
`;

const composeInfraTemplate = (moduleName, image, port) => `# [초보자를 위한 개념 설명]
# docker-compose.yml은 복잡한 도커 실행 명령어를 쉽게 설정 파일로 만들어둔 것입니다.
# 인프라스트럭처(DB, 메시지 큐 등)는 직접 코드를 빌드하지 않고 이미 만들어진 이미지를 가져와서 실행합니다.

version: '3.8' # Docker Compose 파일의 문법 버전

services:
  ${moduleName}:
    # 사용할 미리 만들어진 도커 이미지를 지정합니다. (도커 허브에서 다운로드 받습니다)
    image: ${image}
    
    # 컨테이너의 이름을 지정합니다.
    container_name: ${moduleName}
    
    # 포트 연결 (내 PC의 포트 : 컨테이너의 포트)
    # 내 PC에서 ${port} 포트로 접속하면, 이 인프라 컨테이너의 ${port} 포트로 연결됩니다.
    ports:
      - "${port}:${port}"
      
    # 공통 네트워크망에 합류하여 다른 MSA 애플리케이션들이 이 인프라를 찾을 수 있게 합니다.
    networks:
      - account-network

networks:
  account-network:
    external: true # 공통 네트워크망을 사용합니다.
`;

const infraDockerfileTemplate = (image) => `# [초보자를 위한 개념 설명]
# 인프라스트럭처는 보통 docker-compose.yml 에서 'image: ...' 방식으로 바로 띄우기 때문에 
# Dockerfile이 필수는 아닙니다. 하지만 나중에 추가적인 커스텀 설정(예: 설정 파일 복사)이 필요할 때를 대비해
# 공식 이미지를 그대로 상속받는 기본 뼈대를 만들어 두었습니다.

# 공식 이미지를 그대로 사용합니다.
FROM ${image}
`;

const emptyDockerfileTemplate = `# [초보자를 위한 개념 설명]
# 이 모듈은 단독으로 실행되는 Spring Boot 서버 애플리케이션이나 인프라가 아닙니다.
# (예: 공통 라이브러리, 인터페이스 모음, 설정 파일 저장소 등)
# 따라서 실제로 도커 컨테이너로 구동되지 않지만, 일관된 구조를 위해 뼈대만 생성해 두었습니다.

FROM alpine:latest
CMD ["echo", "This module is a library or config repo and does not run as a standalone service."]
`;

modules.forEach(mod => {
    if (!fs.existsSync(mod)) {
        fs.mkdirSync(mod);
    }
    
    if (infra[mod]) {
        fs.writeFileSync(path.join(mod, 'Dockerfile'), infraDockerfileTemplate(infra[mod][0]));
        fs.writeFileSync(path.join(mod, 'docker-compose.yml'), composeInfraTemplate(mod, infra[mod][0], infra[mod][1]));
    } else if (springBootPorts[mod]) {
        fs.writeFileSync(path.join(mod, 'Dockerfile'), dockerfileTemplate);
        fs.writeFileSync(path.join(mod, 'docker-compose.yml'), composeSpringTemplate(mod, springBootPorts[mod]));
    } else {
        fs.writeFileSync(path.join(mod, 'Dockerfile'), emptyDockerfileTemplate);
        const emptyCompose = `# [초보자를 위한 개념 설명]\n` +
                             `# 이 모듈은 단독으로 실행되는 서버가 아니므로 docker-compose 설정이 의미가 없습니다.\n` +
                             `# 구조를 맞추기 위한 빈 템플릿입니다.\n\n` +
                             `version: '3.8'\n` +
                             `services:\n` +
                             `  ${mod}:\n` +
                             `    build: .\n`;
        fs.writeFileSync(path.join(mod, 'docker-compose.yml'), emptyCompose);
    }
});

console.log("Regenerated Dockerfiles and docker-compose.yml with beginner-friendly comments.");