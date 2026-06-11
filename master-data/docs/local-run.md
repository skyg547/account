# master-data local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`master-data`는 Spring Boot 앱입니다. 로컬 기본 설정은 `config-server`에서 `config-repo/master-data.yml`을 받아오는 구조이며, `application.yml`의 config import는 optional입니다.

## IntelliJ에서 실행하기

1. 먼저 필요하면 `Config Server bootRun`을 실행합니다.
2. Eureka 등록까지 확인하려면 `Discovery bootRun`도 실행합니다.
3. 상단 Run Configuration에서 `Master Data bootRun`을 실행합니다.
4. H2 메모리 DB와 Flyway 설정은 `config-repo/master-data.yml`을 기준으로 확인합니다.

## PowerShell 명령

```powershell
.\gradlew :master-data:bootRun --console=plain
```

테스트만 확인:

```powershell
.\gradlew :master-data:test --console=plain --max-workers=1 --no-daemon
```

## 주요 엔드포인트

- `GET /api/basic/account-subjects`
- `GET /api/basic/departments`
- `GET /api/basic/businesspartners`
- `GET /api/basic/products`
- `GET /api/master-data/change-requests/pending`

## 로컬 실행 주의사항

- `config-repo/master-data.yml`의 기본 포트는 `8082`입니다.
- 다른 모듈에서 master-data를 호출할 때는 가능하면 `contracts`의 포트를 거치고, HTTP 호출은 호스트 앱이나 Gateway 라우트 구성을 확인합니다.
- 운영에서는 `ddl-auto: update`를 그대로 쓰지 말고 Flyway 기준으로 검증해야 합니다.
