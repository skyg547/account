# discovery local run

## 1. 전제 조건

- JDK 17
- IntelliJ IDEA의 Gradle JVM도 JDK 17
- 저장소 루트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기
- 포트 `8761`이 비어 있어야 함

Discovery는 H2/PostgreSQL을 사용하지 않습니다. 실행을 위해 DB나 Flyway를 준비할 필요가 없습니다.

## 2. 실행 모드

| 목적 | IntelliJ 설정 | Config Server | 외부 Logstash/Zipkin |
|---|---|---|---|
| 단독 기동/readiness 확인 | `Discovery standalone bootRun` | 불필요 | 비활성 |
| 중앙 설정 통합 확인 | `Discovery bootRun` | 먼저 실행 | 설정에 따라 사용 |
| 컨테이너 통합 | 루트 `docker compose` | Compose 서비스 | 컨테이너 이름으로 연결 |

## 3. IntelliJ standalone 실행

1. `File > Project Structure`에서 Project SDK를 JDK 17로 설정합니다.
2. `Settings > Build Tools > Gradle`의 Gradle JVM도 JDK 17로 맞춥니다.
3. Run Configuration에서 `Discovery standalone bootRun`을 실행합니다.
4. 다음 URL을 순서대로 확인합니다.

```powershell
Invoke-RestMethod http://localhost:8761/actuator/health/readiness
Invoke-WebRequest http://localhost:8761
Invoke-RestMethod http://localhost:8761/eureka/apps -Headers @{ Accept = 'application/json' }
```

예상 결과:

- readiness `status`: `UP`
- Dashboard: HTTP 200
- 서비스가 아직 없으면 registry application 목록이 비어 있음

터미널에서 같은 실행을 재현:

```powershell
.\gradlew :discovery:bootRun --args="--spring.profiles.active=local --server.port=8761 --spring.cloud.config.enabled=false --eureka.client.register-with-eureka=false --eureka.client.fetch-registry=false --management.tracing.enabled=false" --console=plain --no-daemon
```

## 4. Config Server 통합 실행

별도 터미널 또는 IntelliJ Run Configuration에서 순서대로 실행합니다.

```text
Config Server bootRun -> Discovery bootRun
```

```powershell
.\gradlew :config-server:bootRun --console=plain
.\gradlew :discovery:bootRun --console=plain
```

설정 조회:

```powershell
Invoke-RestMethod http://localhost:8888/discovery-service/default
Invoke-RestMethod http://localhost:8761/actuator/health/readiness
```

Config 응답과 실제 서버가 모두 8761인지, `register-with-eureka=false`, `fetch-registry=false`인지 확인합니다.

## 5. 다른 서비스 등록 확인

Discovery가 readiness `UP`인 뒤 Auth나 업무 API를 실행합니다.

```text
Config Server -> Discovery -> Auth/Master Data/Journal Ledger -> Gateway
```

Dashboard에서 다음을 확인합니다.

1. 서비스 ID가 `spring.application.name`과 일치하는가?
2. instance status가 `UP`인가?
3. 동일 서비스를 두 개 띄웠을 때 instance가 둘로 보이는가?
4. 정상 종료 후 instance가 cancel되는가?

테스트 코드의 `DiscoveryApplicationTests`는 임시 instance를 register -> lookup -> cancel하고 반드시 정리합니다.

## 6. 테스트와 JAR 빌드

```powershell
.\gradlew :discovery:test :discovery:bootJar --console=plain --max-workers=1 --no-daemon
```

검증 범위:

- Spring/Eureka server context
- actuator readiness
- server 자체 register/fetch 비활성
- registry register/lookup/cancel 생명주기
- local/config YAML 포트와 정책
- root/module Compose 주소·build context·`service_healthy`
- Dockerfile JDK 17/bootJar/readiness healthcheck

## 7. Docker 실행

저장소 루트에서 실행합니다.

```powershell
docker compose up --build discovery
```

루트 Compose의 Discovery 환경:

- `SERVER_PORT=8761`
- `EUREKA_INSTANCE_HOSTNAME=discovery`
- Config Server: `http://config-server:8888/`
- Logstash: `logstash:5000`
- Zipkin: `http://zipkin:9411/api/v2/spans`

`discovery/docker-compose.yml`은 이미 `account-network`와 외부 Config/Logstash/Zipkin이 있을 때 Discovery만 추가하는 보조 파일입니다.

## 8. 자주 발생하는 문제

### 8761 대신 8080으로 실행됨

현재 `application.yml` 기본 포트는 8761입니다. 오래된 build resource 또는 다른 `SERVER_PORT` 환경변수를 확인하고 Gradle refresh 후 다시 실행합니다.

### 자기 자신을 등록하려고 함

`eureka.client.register-with-eureka=false`, `fetch-registry=false`가 local과 Config 모두 적용됐는지 확인합니다.

### readiness endpoint가 404

actuator 의존성과 `management.endpoint.health.probes.enabled=true`가 포함된 최신 빌드인지 확인합니다.

### Dashboard에 종료된 서비스가 남아 있음

정상 cancel 여부, heartbeat/lease 만료 시간, self-preservation 경고와 client cache를 확인합니다. 해결을 위해 무조건 self-preservation을 끄지 않습니다.

### Docker 의존 서비스가 시작되지 않음

Discovery container health와 `/actuator/health/readiness`를 확인합니다. Discovery가 `healthy`가 되어야 `service_healthy` 의존 서비스가 시작됩니다.

## 9. 종료와 메모리 정리

실행 터미널에서 `Ctrl+C`로 Discovery를 종료한 뒤 Gradle 데몬을 정리합니다.

```powershell
.\gradlew --stop
jps -lv
```

`DiscoveryApplication`, Gradle daemon/worker가 없고 IntelliJ/SonarLint만 남으면 실행 프로세스가 정리된 상태입니다.
