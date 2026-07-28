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

## 폐기 가능한 PostgreSQL 로컬 검증

`master-data`에는 H2와 PostgreSQL JDBC 드라이버가 모두 포함됩니다. 이미 로컬 PostgreSQL에 빈 연습용 DB를 준비했고 접속정보를 IntelliJ 환경변수나 승인된 비밀 저장소에 설정한 경우 다음처럼 실행합니다.

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/account_master_local'
$env:SPRING_DATASOURCE_USERNAME = 'approved-local-user'
# SPRING_DATASOURCE_PASSWORD는 터미널 기록에 평문으로 남기지 말고 IntelliJ 환경변수/승인된 비밀 주입 기능으로 설정합니다.

.\gradlew :master-data:bootRun --args="--spring.profiles.active=local --server.port=8082 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=update --spring.flyway.enabled=true --spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=0" --console=plain --max-workers=1 --no-daemon
```

이 명령은 개발 중인 스키마를 확인하기 위한 폐기 가능한 smoke 경로입니다. 운영 기준은 전체 기준정보 테이블 Flyway DDL을 완성한 뒤 `ddl-auto=validate`를 사용하는 것입니다.

이미 적용되었을 수 있는 `V2`는 체크섬을 보존합니다. `payload_json`을 `TEXT`로 바꾸는
forward correction은 신규 `V4`가 담당하며 H2 PostgreSQL 모드에서 검증했습니다. 다만 새
PostgreSQL 데이터베이스는 `V2`의 `CLOB` 선언을 먼저 읽으므로 현재 공통 migration만으로는
부트스트랩 완료를 보장하지 않습니다. 운영 전 vendor별 pre-V2 baseline 또는 승인된 migration
전환 정책을 마련해야 합니다. 체크섬 불일치를 숨기기 위해 임의로 `flyway repair`하지 말고,
배포 이력과 신규 forward migration 적용 결과를 확인합니다.

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

- ACCOUNT_SUBJECT/BUSINESS_PARTNER/DEPARTMENT/PRODUCT typed change applier와 중복 소유권 차단
- CREATE/UPDATE/DEACTIVATE별 `requestedVersion`과 실제 SCD2 이력 수 정합성
- 요청 후 다른 변경이 먼저 반영된 경우 승인 단계의 오래된 버전 차단
- 미지원 targetType의 요청 접수 fail-closed
- 승인된 `effectiveDate`가 SCD2 생성/수정/비활성화에 전달되는지
- 변경 요청 JPA `lockVersion` 매핑과 Repository 초기화
- 기준일 누락 시 일일 유효성 보고서가 fail-fast 하는지
- H2에서 네 기준정보 활성 건수를 DB `COUNT`로 계산하는지

## 주요 엔드포인트

- `GET /api/basic/account-subjects`
- `GET /api/basic/departments`
- `GET /api/basic/businesspartners`
- `GET /api/basic/products`
- `POST /api/master-data/change-requests`
- `GET /api/master-data/change-requests/pending`
- `POST /api/master-data/change-requests/{requestId}/approve`
- `POST /api/master-data/change-requests/{requestId}/reject`
- `POST /api/master-data/change-requests/{requestId}/apply`
- `POST /api/master-data/change-requests/apply-due`

`requestedVersion` 예시는 다음과 같습니다.

- 아직 이력이 없는 `D-001` 생성: `CREATE / requestedVersion=1`
- 이력이 1행인 `D-001` 수정: `UPDATE / requestedVersion=2`
- 이력이 2행인 `D-001` 종료: `DEACTIVATE / requestedVersion=2`

지원하지 않는 targetType이나 DTO 형식 오류는 HTTP 400, 저장 이력과 `requestedVersion`이 달라진 낙관적 업무 충돌은 HTTP 409로 구분합니다. 예상하지 못한 내부 예외 메시지는 공통 예외 처리기가 외부에 그대로 노출하지 않습니다.

## 로컬 실행 주의사항

- 로컬 API 기본 포트는 `8082`입니다.
- `V1__init_baseline.sql`은 기존 구조를 보존한 빈 baseline이므로, H2 학습 실행은 Flyway를 끄고 Hibernate `create-drop`을 사용합니다.
- 운영에서는 `ddl-auto: update/create-drop`을 사용하지 말고 실제 PostgreSQL용 Flyway DDL을 작성해 `validate` 또는 `none`으로 검증해야 합니다.
- 일일 유효성 집계는 전체 행을 Java 메모리에 올리지 않고 JPA `COUNT` 쿼리 네 번으로 처리합니다.
- `CURRENCY`, `EXCHANGE_RATE`, `FISCAL_PERIOD` 변경 요청은 typed applier와 버전 조회 어댑터가 함께 추가되기 전까지 접수 단계에서 fail-closed 됩니다.
- 직접 CRUD 쓰기 엔드포인트는 승인 흐름과 공존하므로 운영에서는 관리자 보정 권한으로 제한해야 합니다.
- `apply-due`는 현재 최대 500건을 한 트랜잭션으로 처리합니다. 운영 대량 실행 전 요청별 재시작 경계와 `SKIP LOCKED` 파티셔닝이 필요합니다.
