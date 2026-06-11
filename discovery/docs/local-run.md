# discovery local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

## IntelliJ에서 실행하기

1. 필요하면 `Config Server bootRun`을 먼저 실행합니다.
2. 상단 Run Configuration에서 `Discovery bootRun`을 실행합니다.
3. 브라우저에서 `http://localhost:8761`을 엽니다.
4. 다른 서비스가 뜨면 Eureka 대시보드에 서비스 ID가 등록되는지 확인합니다.

## PowerShell 명령

```powershell
.\gradlew :discovery:bootRun --console=plain
```

테스트만 확인:

```powershell
.\gradlew :discovery:test --console=plain --max-workers=1 --no-daemon
```

## 실행 순서

전체 인프라 흐름에서는 `config-server` 다음에 `discovery`를 띄웁니다.

```text
config-server -> discovery -> auth/master-data/governance/journal-ledger -> gateway
```
