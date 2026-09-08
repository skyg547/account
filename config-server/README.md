# Config Server (중앙 설정 서버) - "MSA 레시피 본사"

`config-server`는 각 Spring Boot 서비스가 시작할 때 필요한 포트, 인프라 주소, 기능 설정을 `config-repo`에서 찾아 HTTP로 제공합니다. 기본 포트는 `8888`입니다.

## 초보자를 위한 한 문장

여러 지점이 각자 레시피를 복사해 관리하지 않도록, 지점 이름과 운영 환경에 맞는 레시피를 나눠 주는 프랜차이즈 본사 서버입니다.

## 현재 책임과 아키텍처 경계

```text
Config Client
  -> GET /{application}/{profile}[/{label}]
  -> Spring Cloud Config inbound adapter
  -> EnvironmentRepository
  -> native config-repo adapter
  -> ordered propertySources 응답
```

- `ConfigServerApplication`은 Config Server와 repository probe를 조립하는 실행 진입점입니다.
- Spring Cloud Config가 파일 탐색, profile 병합, `Environment` HTTP 응답 생성을 담당합니다.
- `ConfigRepositoryHealthIndicator`는 대표 설정(`master-data/default`)의 property source가 실제로 존재할 때만 readiness를 UP으로 만듭니다.
- `config-repo`는 로컬/native 설정 원본입니다. Docker에서는 `/config-repo`에 읽기 전용으로 마운트합니다.
- 회계 금액, 승인, 마감 같은 업무 규칙은 이 모듈에 두지 않고 각 업무 모듈의 core/domain에 둡니다.

애플리케이션 클래스가 작은 것은 빈 스켈레톤이기 때문이 아닙니다. 검증된 Config 프레임워크를 어댑터로 사용하고, 이 모듈이 소유해야 할 저장소 가용성·실행·보안·배포 정책과 계약 테스트를 명시적으로 관리합니다.

## 설정 조회 업무 흐름

1. 서비스의 `spring.application.name`과 활성 profile이 조회 키가 됩니다.
2. Config Client가 Config Server에 설정을 요청합니다.
3. Config Server가 `config-repo/{application}.yml` 및 profile 파일을 조회합니다.
4. 우선순위가 있는 `propertySources`를 반환합니다.
5. 클라이언트가 원격 설정과 자신의 로컬/환경변수 설정을 Spring 규칙에 따라 바인딩합니다.
6. 대표 설정을 읽지 못하면 Config Server readiness가 DOWN이 되고, Compose의 15개 의존 서비스는 시작을 기다립니다.

`optional:configserver:`를 사용하는 클라이언트는 Config Server가 없어도 로컬 기본값으로 시작할 수 있습니다. 이는 장애 허용 방식이지 중앙 설정과 동일하게 실행된다는 뜻은 아닙니다.

## 실행과 검증

IntelliJ에서는 `Config Server bootRun`을 실행합니다. 루트 폴더를 기준으로 native 저장소를 읽도록 실행 인자가 포함되어 있습니다.
Config Server는 `ENCRYPT_KEY`가 없거나 빈 값·공백·앞뒤 공백이면 HTTP context 생성 전에 실패합니다.
저장소나 Run Configuration에 기본 키를 저장하지 말고 실행 프로세스마다 임시 값을 주입합니다.

```powershell
$keyBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)
$env:ENCRYPT_KEY = [Convert]::ToBase64String($keyBytes)
try {
    .\gradlew :config-server:test :config-server:bootJar --console=plain --max-workers=1 --no-daemon
    .\gradlew :config-server:bootRun --args="--spring.profiles.active=native --server.port=8888 --spring.cloud.config.server.native.search-locations=file:./config-repo" --console=plain --no-daemon
} finally {
    Remove-Item Env:ENCRYPT_KEY -ErrorAction SilentlyContinue
}
```

서버가 준비됐는지 확인합니다.

```powershell
(Invoke-WebRequest http://localhost:8888/actuator/health/readiness).StatusCode
(Invoke-WebRequest http://localhost:8888/master-data/default).StatusCode
```

설정 조회 응답에는 내부 설정이 포함될 수 있으므로 공유 로그나 이슈에 응답 본문을 붙이지 않습니다. 이 모듈은 DB를 사용하지 않으므로 H2/PostgreSQL 설정이 필요하지 않습니다.

## 암복호화 엔드포인트 보안 (Issue #436)

- 기본값 `spring.cloud.config.server.encrypt.enabled=false`에서는 HTTP 보안 필터가 `/encrypt`, `/decrypt`와 하위 경로를 404로 거부합니다. 이 Spring 설정만으로 endpoint가 미등록되는 것은 아닙니다. MVC가 선택한 실제 `EncryptionController`에도 같은 인증 검사를 적용하여 인코딩·matrix parameter·context/servlet/Config prefix로 우회되지 않게 합니다. 해당 controller의 `/encrypt/status`, `/key` 계열도 같은 정책을 따릅니다.
- 승인된 내부 운영 프로세스에서만 `SPRING_CLOUD_CONFIG_SERVER_ENCRYPT_ENABLED=true`와 별도의 `CONFIG_CRYPTO_ENDPOINT_TOKEN`을 외부 secret provider 또는 실행 시 생성한 임시 값으로 주입합니다. 토큰은 암호화 키와 별개이며 저장소 기본값이나 자동 생성 계정은 없습니다. 기존 `config-server.internal-crypto-token`은 호환용 대체 property입니다. 첫 property가 설정되어 있으면 우선하며, 빈 값이더라도 대체값으로 우회하지 않습니다.
- 토큰이 없거나 빈 값·공백·비 ASCII·쉼표를 포함하면 403으로 차단합니다. 토큰을 임의로 trim하지 않습니다. 충분한 엔트로피의 토큰(예: 32 random bytes의 Base64)을 사용합니다.
- 요청은 `Authorization: Bearer <token>`, `X-Config-Token`, `X-Config-Internal-Token` 중 **하나의 헤더 하나**로 인증합니다. 누락·잘못된 값·중복/혼합 헤더는 401입니다. 토큰을 URL, query string, 명령행 인자, Config repository, 추적되는 env 파일에 넣지 않습니다. 헤더·본문·응답을 출력하지 않는 승인된 내부 클라이언트를 사용합니다.
- 거부 응답은 입력을 반영하지 않는 빈 본문이며 crypto 응답은 `Cache-Control: no-store`입니다. Spring Cloud Config의 `EncryptionController`는 실패한 복호화 입력을 ERROR로 기록하므로 해당 로거는 `OFF`로 유지합니다. HTTP payload/header debug·trace 및 프록시 요청 본문 기록을 활성화하지 않습니다.
- `/actuator/health`, readiness, info, prometheus와 일반 Config 조회 인증 계약은 유지됩니다. 키 fail-closed 정책은 #435의 소유이며 이 작업은 실제 키·암호문·credential을 다루지 않습니다.

## 내부 운영 경로와 Compose 노출 계약

기본 루트·모듈·production·minimal external-dev Compose는 Config Server host port를 게시하지 않습니다. `compose.prod.yml`은 crypto enable flag도 명시적으로 false로 고정합니다. 공용 Gateway/Frontend ingress에는 Config Server route를 추가하지 않습니다. Compose 네트워크에 연결된 것만으로 운영 권한이 부여되지는 않습니다.

운영자는 승인된 내부 네트워크의 클라이언트만 사용하고, 환경에 맞는 TLS/mTLS 또는 승인된 암호화 터널을 구성해야 합니다. Basic HTTP 내부 통신 자체가 전송 암호화를 제공하지는 않습니다. 이 PR은 외부 배포·방화벽·TLS 설정을 변경하지 않습니다.

임시 운영 창구가 필요하면 추적하지 않는 승인된 Compose override에서 **config-server 서비스에만** 아래 입력을 연결합니다. 기본 Compose나 공용 ingress에 포트를 추가하지 않습니다.

```yaml
services:
  config-server:
    environment:
      SPRING_CLOUD_CONFIG_SERVER_ENCRYPT_ENABLED: "true"
      CONFIG_CRYPTO_ENDPOINT_TOKEN: ${CONFIG_CRYPTO_ENDPOINT_TOKEN:?inject approved ephemeral operation token}
```

작업 종료 후 override와 임시 입력을 제거하고 기본 Compose로 해당 서비스만 재생성하여 비활성 정책을 복원합니다. 실제 credential 발급·회전은 별도 승인 절차입니다. 로컬 디버깅을 위해 포트가 필요할 때만 별도 로컬 override로 `127.0.0.1:8888:8888`을 사용하고 인증 정책을 유지합니다.

검증: Windows에서는 `gradlew.bat :config-server:test :config-server:bootJar --offline`, Linux에서는 동일 Gradle 8.7/JDK 17 task를 실행합니다. HTTP 허용/거부, encoded/prefixed 경로, 오류·로그 미노출, health/config 무회귀와 Compose 정책을 테스트합니다.

## 문서 읽기 순서

1. [docs/beginner-guide.md](./docs/beginner-guide.md)
2. [docs/process-flow.md](./docs/process-flow.md)
3. [docs/local-run.md](./docs/local-run.md)
4. [docs/README.md](./docs/README.md)
5. [../config-repo/README.md](../config-repo/README.md)

## 남은 운영 과제

코드의 `@todo` 두 건이 운영 전 필수 결정 사항입니다.

- Config 조회 API를 private network와 mTLS 또는 서비스 인증으로 보호합니다.
- native 폴더를 승인된 Git backend/고정 label로 전환하고 refresh, rollback, 다중 노드 동기화를 검증합니다.

현재 Docker CLI가 없는 개발 환경에서는 이미지와 전체 Compose 실행을 별도로 검증해야 합니다.
