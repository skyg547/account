# gateway local run

## 1. 전제 조건

- JDK 17
- IntelliJ IDEA의 Gradle JVM도 JDK 17
- 현재 checkout의 저장소 루트를 Gradle 프로젝트로 열기
- 기본 Gateway 포트 `8000`이 비어 있어야 함

Gateway는 H2나 PostgreSQL을 직접 사용하지 않습니다. 사용자와 역할 데이터는 Auth가 소유하므로 Gateway 실행을 위해 DB 프로필을 선택할 필요가 없습니다.

## 2. 실행 시점 JWT 입력

Gateway는 JWT secret, 공개키 또는 JWKS URI 중 하나가 없으면 fail-closed 합니다. standalone local smoke에서는 저장소에 값을 남기지 않고 현재 PowerShell 프로세스에 32바이트 이상 임시 secret을 생성합니다.

```powershell
$bytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($bytes)
    $env:AUTH_JWT_SECRET = [Convert]::ToBase64String($bytes)
} finally {
    $random.Dispose()
}
```

값을 출력하거나 문서, 명령 인자, Run Configuration 파일, Issue/PR에 저장하지 않습니다. IntelliJ에서는 현재 세션에서 만든 값을 `Gateway standalone bootRun`의 환경변수로 연결합니다.

## 3. 실행 모드 선택

| 목적 | 실행 설정 | 외부 모듈 | 실제 라우팅 |
|---|---|---|---|
| 컴파일/컨텍스트/포트 확인 | `Gateway standalone bootRun` | 불필요 | 없음 |
| 로그인과 보호 API 통합 확인 | `Gateway bootRun` | Config, Discovery, Auth, 대상 API | 있음 |
| 컨테이너 통합 확인 | 루트 `docker compose` | Compose 서비스 | 있음 |

### 중요한 차이

standalone `local` 프로파일은 token-version 원격 검증과 Config/Discovery/Eureka를 끕니다. Gateway가 단독 Spring Boot API 서버로 뜨는지만 확인하는 모드입니다. 이 설정을 운영이나 실제 인증 테스트에 사용하면 안 됩니다.

## 4. IntelliJ standalone smoke

1. `File > Project Structure > Project SDK`에서 JDK 17을 선택합니다.
2. `Settings > Build Tools > Gradle > Gradle JVM`도 JDK 17로 맞춥니다.
3. 우측 상단 Run Configuration에서 `Gateway standalone bootRun`을 선택합니다.
4. 실행 로그에서 Netty가 포트 8000으로 시작했는지 확인합니다.
5. PowerShell에서 health를 호출합니다.

```powershell
Invoke-RestMethod http://localhost:8000/actuator/health
```

예상 핵심 값은 `status = UP`입니다. 이 모드는 라우트가 없으므로 업무 API 404는 정상입니다.

같은 실행을 터미널에서 재현:

```powershell
.\gradlew :gateway:bootRun --args="--spring.profiles.active=local --server.port=8000" --console=plain --no-daemon
```

## 5. executable JAR smoke

```powershell
.\gradlew :gateway:test :gateway:bootJar --offline --console=plain --max-workers=1

$jar = Get-ChildItem gateway/build/libs -Filter '*.jar' |
    Where-Object { $_.Name -notlike '*-plain.jar' } |
    Select-Object -First 1

java -jar $jar.FullName --spring.profiles.active=local --server.port=8000
```

입력을 제거하고 같은 JAR을 실행하면 `JWT verification requires either ...` 오류로 시작에 실패해야 합니다.

```powershell
Remove-Item Env:AUTH_JWT_SECRET -ErrorAction SilentlyContinue
java -jar $jar.FullName --spring.profiles.active=local --server.port=8000
```

## 6. 전체 라우팅 실행

각 명령은 별도 IntelliJ Run Configuration이나 별도 터미널에서 실행합니다.

1. Config Server (`8888`)
2. Discovery (`8761`)
3. Auth (`8084`)
4. 호출할 업무 API 서비스
5. Gateway (`8000`)

```powershell
.\gradlew :config-server:bootRun --console=plain
.\gradlew :discovery:bootRun --console=plain
.\gradlew :auth:api:bootRun --console=plain
.\gradlew :gateway:bootRun --console=plain
```

Auth와 Gateway 터미널에는 같은 `AUTH_JWT_SECRET`과 `AUTH_JWT_ISSUER`를 안전한 로컬 환경변수로 주입합니다. 실제 값은 문서, Git, 명령 기록, 로그에 남기지 않습니다.

### Config 확인

```powershell
Invoke-RestMethod http://localhost:8888/gateway-service/default
Invoke-RestMethod http://localhost:8761/eureka/apps
Invoke-RestMethod http://localhost:8000/actuator/health
```

Config 응답에서 Gateway 포트가 8000인지, Eureka에 `GATEWAY-SERVICE`, `AUTH-SERVICE`와 대상 서비스가 등록됐는지 확인합니다.

### 인증 흐름 확인

1. `POST http://localhost:8000/api/auth/login`으로 로컬 테스트 계정의 JWT를 발급받습니다.
2. 보호 API에 `Authorization: Bearer <token>`을 넣습니다.
3. 토큰 없이 같은 API를 호출하면 401과 `X-Auth-Error=BEARER_TOKEN_REQUIRED`인지 확인합니다.
4. Auth를 중지한 뒤 캐시에 없는 사용자/버전으로 호출하면 503 `AUTH_VALIDATION_UNAVAILABLE`인지 확인합니다.

로그인 비밀번호나 토큰은 터미널 출력, 캡처, 문서에 붙여 넣지 않습니다.

## 7. 빌드와 테스트

```powershell
.\gradlew :gateway:test :gateway:bootJar --console=plain --max-workers=1 --no-daemon
```

주요 회귀 테스트:

- `JwtAuthenticationFilterTest`: 공개/보호/내부 경로와 신뢰 헤더 정책.
- `JjwtAccessTokenVerifierTest`: 필수 claim과 헤더 안전성.
- `AuthTokenVersionValidatorTest`: 캐시, 거절, 장애, 빈/구조 불완전 응답.
- `GatewayRouteSecurityPolicyTest`: 외부 Config의 포트/로그인 단일 공개 경로.
- `GatewayDockerConfigurationTest`: 루트/모듈 Compose 포트, 주소, build context.
- `GatewayApplicationTests`: Spring 컨텍스트.

생성 JAR:

```text
gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar
```

## 8. Docker 실행

전체 환경은 저장소 루트에서 실행합니다.

```powershell
docker compose up --build gateway
```

루트 Compose는 다음 값을 맞춥니다.

- 호스트/컨테이너 포트: `8000:8000`
- Config Server: `http://config-server:8888/`
- Discovery: `http://discovery:8761/eureka/`
- token-version Auth 주소: `http://auth:8084`

`gateway/docker-compose.yml`은 이미 `account-network`에 Config/Discovery/Auth가 떠 있을 때 Gateway만 추가하는 보조 파일입니다.

## 9. 자주 발생하는 문제

### health는 UP인데 업무 API가 404

standalone 모드에서는 Config Server 라우트를 읽지 않으므로 정상입니다. 전체 라우팅 모드로 실행합니다.

### `503 AUTH_VALIDATION_UNAVAILABLE`

- Auth 8084가 실행 중인지 확인합니다.
- `AUTH_TOKEN_VERSION_VALIDATION_BASE_URL`이 실행 환경에 맞는지 확인합니다.
- Docker 안에서는 `localhost:8084`가 아니라 `http://auth:8084`를 사용합니다.
- Auth와 Gateway의 네트워크 및 timeout 설정을 확인합니다.

### `401 ACCESS_TOKEN_INVALID`

- Auth와 Gateway의 서명키/issuer가 같은지 확인합니다.
- 토큰에 `subject`, `roles`, 양의 정수 `roleVersion`, `iat`, `exp`가 있는지 확인합니다.
- 역할/부서 코드에 공백, 쉼표, 제어 문자가 없는지 확인합니다.

### Config Server 없이 실행이 지연됨

standalone 명령처럼 `--spring.profiles.active=local`을 명시합니다. `local` 프로파일이 Config/Discovery/Eureka 비활성화 정책을 소유합니다.

### 테스트 결과 디렉터리 잠금

이전 Gradle 테스트 워커가 남았는지 먼저 확인하고 데몬을 종료합니다.

```powershell
.\gradlew --stop
jps -lv
```

IntelliJ와 SonarLint Java 프로세스는 IDE 동작에 필요하므로 일괄 종료하지 않습니다.

## 10. 종료와 메모리 정리

실행 터미널에서 `Ctrl+C`로 Gateway를 먼저 종료한 뒤 Gradle 데몬을 정리합니다.

```powershell
Remove-Item Env:AUTH_JWT_SECRET -ErrorAction SilentlyContinue
.\gradlew --stop
jps -lv
```

`jps -lv`에 `GatewayApplication`이나 Gradle daemon이 없으면 모듈 실행 프로세스가 정리된 것입니다.
