# master-data local run

## 전제 조건

- JDK 17
- IntelliJ IDEA의 Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`master-data`는 현재 하나의 Spring Boot API 애플리케이션입니다. `core`, `api`, `batch`는 패키지 경계이며 별도 Gradle 하위 프로젝트는 아닙니다. 일일 유효성 보고 파이프라인은 core에 있고 batch orchestrator가 이를 호출하지만, 독립 Spring Batch Job/Step 실행 모듈은 아직 없습니다.

## IntelliJ에서 H2 단독 실행

1. Gradle 창에서 프로젝트를 새로고침합니다.
2. 상단 Run Configuration에서 `Master Data bootRun`을 선택합니다.
3. 실행하면 `local` profile, 내장 H2, Hibernate `create-drop`으로 API 서버가 `8082` 포트에 올라옵니다.
4. 로그에서 `Started MasterDataApplication`을 확인합니다.
5. 테스트가 끝나면 IntelliJ의 Stop 버튼으로 프로세스를 종료합니다.

이 실행 설정은 Config Server, Discovery, Vault, 외부 tracing을 끕니다. 따라서 초보자가 인프라를 먼저 띄우지 않아도 기준정보 API와 JPA 매핑을 확인할 수 있습니다. `create-drop`이므로 프로세스를 끄면 H2 데이터도 사라집니다.

## PowerShell H2 단독 실행

```powershell
.\gradlew :master-data:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

API 서버를 열지 않고 Spring/JPA 빈 연결만 확인하려면 다음 smoke 명령을 사용합니다.

```powershell
.\gradlew :master-data:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

## 공통 인프라와 함께 실행

Config Server 설정을 확인하는 통합 모드에서는 다음 순서로 실행합니다.

1. `Config Server bootRun`
2. Eureka 등록이 필요하면 `Discovery bootRun`
3. `Master Data bootRun`에서 standalone 인자를 제거한 통합용 실행 설정

통합 모드는 `config-repo/master-data.yml`의 H2/Flyway/Eureka 설정을 사용합니다. 운영 PostgreSQL 접속정보는 저장소나 문서에 직접 기록하지 말고 승인된 환경변수/비밀 저장소로 주입해야 합니다.

## 검증 명령

```powershell
.\gradlew :master-data:test --console=plain --max-workers=1
```

테스트는 다음을 함께 확인합니다.

- ACCOUNT_SUBJECT/BUSINESS_PARTNER/DEPARTMENT/PRODUCT typed change applier
- 승인된 `effectiveDate`가 SCD2 생성/수정/비활성화에 전달되는지
- 기준일 누락 시 일일 유효성 보고서가 fail-fast 하는지
- H2에서 네 기준정보 활성 건수를 DB `COUNT`로 계산하는지

## 주요 엔드포인트

- `GET /api/basic/account-subjects`
- `GET /api/basic/departments`
- `GET /api/basic/businesspartners`
- `GET /api/basic/products`
- `GET /api/master-data/change-requests/pending`
- `POST /api/master-data/change-requests/{requestId}/apply`
- `POST /api/master-data/change-requests/apply-due`

## 로컬 실행 주의사항

- 로컬 API 기본 포트는 `8082`입니다.
- `V1__init_baseline.sql`은 기존 구조를 보존한 빈 baseline이므로, H2 학습 실행은 Flyway를 끄고 Hibernate `create-drop`을 사용합니다.
- 운영에서는 `ddl-auto: update/create-drop`을 사용하지 말고 실제 PostgreSQL용 Flyway DDL을 작성해 `validate` 또는 `none`으로 검증해야 합니다.
- 일일 유효성 집계는 전체 행을 Java 메모리에 올리지 않고 JPA `COUNT` 쿼리 네 번으로 처리합니다.
- `CURRENCY`, `EXCHANGE_RATE`, `FISCAL_PERIOD` 변경 요청은 typed applier가 추가되기 전까지 의도적으로 fail-closed 됩니다.