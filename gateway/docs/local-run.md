# gateway local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

## 실행 순서

라우트 검증까지 하려면 아래 순서가 가장 안정적입니다.

1. `Config Server bootRun`
2. `Discovery bootRun`
3. 뒤쪽 서비스: `Auth bootRun`, `Master Data bootRun`, `Journal Ledger API bootRun`
4. `Gateway bootRun`

Gateway만 필터 단위 테스트를 할 때는 뒤쪽 서비스를 띄우지 않아도 됩니다.

## IntelliJ에서 실행하기

상단 Run Configuration에서 `Gateway bootRun`을 선택합니다.

## PowerShell 명령

```powershell
.\gradlew :gateway:bootRun --console=plain
```

테스트만 확인:

```powershell
.\gradlew :gateway:test --console=plain --max-workers=1 --no-daemon
```

## 주요 설정

- `config-repo/gateway-service.yml`: 포트, 라우트, CORS, CircuitBreaker, Swagger aggregation.
- `gateway/src/main/resources/application.yml`: 서비스 이름과 JWT 기본 설정.
- `AUTH_JWT_SECRET`: Auth와 Gateway가 같은 값을 사용해야 합니다.
- `AUTH_JWT_ISSUER`: 기본값은 `auth-service`입니다.

## 빠른 확인

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

라우팅 API는 JWT가 필요한 경로가 있으므로 먼저 Auth에서 로그인 토큰을 받아야 합니다.
