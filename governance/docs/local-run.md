# governance local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`governance`는 Spring Boot 앱입니다. Config Server import는 optional이지만, 실제 포트와 DB 설정은 `config-repo/governance-service.yml`을 기준으로 맞추는 편이 안전합니다.

## IntelliJ에서 실행하기

1. `Config Server bootRun`을 먼저 실행합니다.
2. Eureka 등록을 확인하려면 `Discovery bootRun`을 실행합니다.
3. Auth 역할 반영까지 확인하려면 `Auth bootRun`도 실행합니다.
4. 상단 Run Configuration에서 `Governance bootRun`을 실행합니다.

## PowerShell 명령

```powershell
.\gradlew :governance:bootRun --console=plain
```

테스트만 확인:

```powershell
.\gradlew :governance:test --console=plain --max-workers=1 --no-daemon
```

## Auth 연동 환경변수

```powershell
$env:GOVERNANCE_AUTH_BASE_URL='http://localhost:8081'
$env:GOVERNANCE_AUTH_INTERNAL_TOKEN='local-internal-auth-token'
```

Auth 쪽 `AUTH_INTERNAL_API_TOKEN`과 같은 값을 사용해야 내부 역할 반영 API가 통과합니다.
