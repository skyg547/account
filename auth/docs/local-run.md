# auth local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

## IntelliJ에서 실행하기

1. 필요하면 `Config Server bootRun`을 먼저 실행합니다.
2. Eureka 등록을 보고 싶으면 `Discovery bootRun`을 실행합니다.
3. 상단 Run Configuration에서 `Auth bootRun`을 실행합니다.
4. 기본 사용자는 `AUTH_DEFAULT_USERNAME`, `AUTH_DEFAULT_PASSWORD` 설정으로 바꿀 수 있습니다.

## PowerShell 명령

```powershell
.\gradlew :auth:bootRun --console=plain
```

테스트만 확인:

```powershell
.\gradlew :auth:test --console=plain --max-workers=1 --no-daemon
```

## 로컬 환경변수 예시

```powershell
$env:AUTH_PERSISTENCE_MODE='memory'
$env:AUTH_DEFAULT_USERNAME='admin'
$env:AUTH_DEFAULT_PASSWORD='1234'
$env:AUTH_INTERNAL_API_TOKEN='local-internal-auth-token'
$env:AUTH_JWT_SECRET='modern-account-system-super-secret-key-1234567890'
```

DB/Flyway까지 확인하려면 `AUTH_PERSISTENCE_MODE`를 기본값 `jpa`로 두고 H2 설정을 사용합니다.
