# 🗺️ Discovery Service (Eureka Server) - 사내 전화번호부

`discovery` 모듈은 MSA에서 서비스 인스턴스의 위치와 상태를 관리하는 Service Registry입니다. Netflix Eureka Server를 사용하며 기본 포트는 **8761**입니다.

---

## 1. 🐣 초보자를 위한 개념 설명

### 마이크로서비스에서 전화번호부가 왜 필요한가요?

서비스 인스턴스는 재시작·증설·장애 복구 때 IP와 포트가 바뀔 수 있습니다. 주소를 코드에 직접 적으면 모든 호출자를 다시 배포해야 합니다.

각 서비스는 시작할 때 Eureka에 다음 정보를 등록합니다.

- 서비스 이름: 예) `AUTH-SERVICE`, `MASTER-DATA`
- 인스턴스 ID와 주소
- 현재 상태(`UP`, `DOWN`, `OUT_OF_SERVICE`)
- 상태 확인 URL과 metadata

Gateway는 `lb://auth-service`처럼 서비스 이름으로 registry를 조회하고 현재 `UP`인 인스턴스 중 하나를 선택합니다.

---

## 2. 🔄 등록 업무 흐름

```text
서비스 시작
  -> Eureka에 register
  -> 주기적으로 heartbeat(renew)
  -> Gateway/다른 서비스가 registry fetch
  -> lb://service-id로 UP 인스턴스 선택
  -> 서비스 정상 종료 시 cancel
  -> heartbeat가 장시간 없으면 lease 만료/eviction 검토
```

Eureka의 self-preservation은 일시적인 네트워크 장애로 heartbeat가 한꺼번에 줄었을 때 대량 인스턴스 삭제를 보류하는 안전장치입니다. 이 저장소는 기본값인 `true`를 유지합니다.

상세 흐름은 [docs/process-flow.md](./docs/process-flow.md)를 확인합니다.

---

## 3. 🧱 책임과 헥사고날/DDD 경계

Discovery에는 회계 금액, 전표, 승인 같은 도메인 로직을 넣지 않습니다. 그렇다고 비어 있는 스켈레톤은 아닙니다.

- `DiscoveryApplication`: Eureka Server 역할을 선언하는 실행 진입점.
- `application.yml`: Config 없이도 동작하는 8761 단일 노드 기본 계약.
- `config-repo/discovery-service.yml`: 중앙 운영 설정과 명시적 보안/HA TODO.
- `DiscoveryApplicationTests`: readiness와 register -> lookup -> cancel 생명주기 검증.
- `DiscoveryConfigurationPolicyTest`: local/config/Compose/Dockerfile 정합성 검증.
- Docker healthcheck: 단순 프로세스 시작이 아니라 readiness `UP`을 다음 서비스 기동 조건으로 사용.

Eureka의 registry 알고리즘은 검증된 라이브러리가 담당하고, 이 모듈은 이를 안전하게 실행하기 위한 설정·관측·검증 경계를 소유합니다.

---

## 4. ⚙️ 현재 실행 정책

- 포트: `8761`
- Config Server: optional, 실제 Config Client 의존성 포함
- Eureka Server 자체 등록: `false`
- 외부 registry fetch: `false`
- Actuator: health/info/prometheus
- Readiness: `/actuator/health/readiness`
- Dashboard: `http://localhost:8761`
- Registry API: `http://localhost:8761/eureka/apps`

Discovery는 H2나 PostgreSQL을 사용하지 않습니다. registry는 메모리 lease 상태이며 서버를 재시작하면 서비스들이 heartbeat/재등록을 통해 다시 채웁니다.

---

## 5. ▶️ 실행 방법

상세 절차는 [docs/local-run.md](./docs/local-run.md)를 따릅니다.

### IntelliJ standalone

1. Gradle JVM을 JDK 17로 설정합니다.
2. `Discovery standalone bootRun`을 실행합니다.
3. `http://localhost:8761/actuator/health/readiness`의 `UP`을 확인합니다.
4. `http://localhost:8761`에서 Dashboard를 확인합니다.

이 모드는 Config Server, 외부 tracing, 자기 등록/fetch를 끈 단일 노드 smoke입니다.

### 전체 MSA 실행 순서

```text
Config Server -> Discovery -> Auth/업무 API -> Gateway
```

```powershell
.\gradlew :discovery:bootRun --console=plain
```

### 테스트와 실행 JAR

```powershell
.\gradlew :discovery:test :discovery:bootJar --console=plain --max-workers=1 --no-daemon
```

### Docker

```powershell
docker compose up --build discovery
```

Discovery image readiness가 `healthy`가 된 뒤 의존 서비스가 시작되도록 루트 Compose가 `service_healthy`를 사용합니다.

---

## 6. 🔐 보안과 운영 주의

현재 Dashboard와 등록 API에 애플리케이션 인증을 강제하지 않습니다. 따라서 운영에서는 8761 포트를 인터넷에 공개하면 안 됩니다.

- private subnet/network policy로 Eureka 접근 주체 제한
- Gateway를 통해 Eureka Dashboard/API를 외부 라우팅하지 않기
- mTLS 또는 Spring Security 기반 서비스 자격 증명 결정
- registry 변경과 비정상 instance 등록 모니터링

## 7. 남은 고도화 TODO

- 운영 외부 노출 전 Dashboard/registration API private network + mTLS/인증 적용.
- 멀티 AZ용 peer Eureka URL, 장애 전환, registry 동기화와 부하 정책 검증.

운영 정책이 정해지지 않은 상태에서 임의 계정이나 peer 주소를 코드에 넣지 않고 `config-repo/discovery-service.yml`의 `@todo`로 명시했습니다.

## 8. 문서 읽기 순서

1. [docs/README.md](./docs/README.md)
2. [docs/concept.md](./docs/concept.md)
3. [docs/process-flow.md](./docs/process-flow.md)
4. [docs/local-run.md](./docs/local-run.md)
