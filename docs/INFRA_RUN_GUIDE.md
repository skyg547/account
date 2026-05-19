# 🌐 MSA 인프라 운영 매뉴얼 (Infra Run Guide)

본 문서는 시스템의 척추 역할을 하는 인프라 모듈(Discovery, Auth, Gateway)을 안전하게 빌드하고 실행하는 방법을 설명합니다.

---

## 1. 인프라 아키텍처 개요

우리 시스템은 4개의 핵심 인프라 서비스가 유기적으로 연결되어 작동합니다.

1.  **Config Server (8888)**: 모든 서비스의 설정값을 중앙에서 배달해주는 '설정 우체국'입니다.
2.  **Discovery Service (8761)**: 각 서비스가 어디에 있는지 알려주는 '전화번호부(Eureka)'입니다.
3.  **Auth Service (8084)**: 정당한 사용자인지 확인하고 출입증(JWT)을 끊어주는 '보안 검문소'입니다.
4.  **Gateway Service (8080)**: 외부에서 들어오는 모든 요청을 담당 부서로 연결해주는 '안내 데스크'입니다.

---

## 2. 순차적 실행 가이드 (Execution Order)

서비스 간의 의존성 때문에 반드시 아래 순서대로 실행해야 합니다.

### [Step 1] Config Server 기동
설정 파일의 절대 경로를 인식시키기 위해 아래 명령어를 사용합니다.
```bash
./gradlew :config-server:bootRun --args='--spring.profiles.active=native --server.port=8888 --spring.cloud.config.server.native.search-locations=file:///C:/Users/skyg547/IdeaProjects/account/config-repo'
```

### [Step 2] Discovery Service 기동
'전화번호부'가 먼저 켜져야 다른 서비스들이 이름을 등록할 수 있습니다.
```bash
./gradlew :discovery:bootRun --args='--server.port=8761 --eureka.client.register-with-eureka=false --eureka.client.fetch-registry=false'
```

### [Step 3] Auth Service 기동
보안 금고(Vault)가 없는 환경에서는 기능을 끄고 실행합니다.
```bash
./gradlew :auth:bootRun --args='--server.port=8084 --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ --spring.cloud.vault.enabled=false'
```

### [Step 4] Gateway Service 기동
모든 요청의 단일 진입점을 마지막으로 활성화합니다.
```bash
./gradlew :gateway:bootRun --args='--server.port=8080 --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/'
```

---

## 3. 정상 작동 확인 방법 (Verification)

### 🟢 포인트 1: Eureka 대시보드 (전체 현황)
브라우저에서 [http://localhost:8761](http://localhost:8761) 접속
- **확인 사항**: `Instances currently registered with Eureka` 섹션에 `AUTH-SERVICE`와 `GATEWAY-SERVICE`가 나타나야 합니다.

### 🟢 포인트 2: 개별 서비스 헬스 체크
각 서비스가 살아있는지 API로 확인합니다. (응답: `{"status":"UP"}`)
- **Auth**: [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)
- **Gateway**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 🟢 포인트 3: 로그인 테스트 (관문 통과)
Gateway를 거쳐 인증 서비스에 도달하는지 확인합니다.
- **URL**: `POST http://localhost:8080/api/auth/login`
- **Body**: `{ "username": "admin", "password": "1234" }`
- **결과**: JWT 토큰이 발급되면 성공입니다.

---

## 4. 트러블슈팅 (Troubleshooting)

| 현상 | 원인 | 해결책 |
| :--- | :--- | :--- |
| **Port 8080 already in use** | 다른 서비스가 이미 실행 중 | `netstat -ano \| findstr :8080`으로 PID 확인 후 `Stop-Process -Id <PID> -Force` |
| **Cannot create Auth for TOKEN** | Vault 서버 미기동 | 실행 인자에 `--spring.cloud.vault.enabled=false` 추가 |
| **Config Server 설정 조회 실패** | `config-repo` 경로 오규 | `search-locations` 인자에 실제 폴더의 **절대 경로**를 `file:///` 형식으로 입력 |

---

### 💡 초보자를 위한 팁
> **MSA 환경에서는 왜 이렇게 복잡하게 켜야 하나요?**  
> 개별 서비스들이 서로를 자동으로 찾고 설정을 공유하기 때문에, 처음 기동할 때는 순서가 중요합니다. 한 번 자리를 잡으면 그다음부터는 어떤 서비스가 죽었다 살아나도 자동으로 서로를 다시 찾아내기 때문에 훨씬 유연한 운영이 가능해집니다.
