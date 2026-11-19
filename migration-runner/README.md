# PostgreSQL Migration Runner

`migration-runner`는 API/Batch 프로세스와 분리된 release-time Flyway 실행 artifact다. HTTP
서버나 Spring application context를 시작하지 않으며, context별 migration과 공통 Spring
Batch metadata를 하나의 실행 가능한 Boot jar에 패키징한다.

## Build and inventory

```powershell
.\gradlew.bat :migration-runner:test :migration-runner:bootJar `
  :migration-runner:verifyReadyExecutablePostgresqlDrivers
java -jar migration-runner\build\libs\account-migration-runner.jar --list
```

`--list`는 자격증명 없이 17개 context의 상태를 출력한다. Account Mart V6, ECL V4와
Internal Audit V60까지 포함한 현재 stack은 `READY 17 / BLOCKED 0`이다. 모든 context가 release artifact에 포함되어
있지만, `READY`는 승인된 실제 PostgreSQL 검증이 끝났다는 뜻이 아니다.

## Migration test engines

`MigrationExecutorH2Test`는 H2에서 지원하는 context의 신규 설치·Batch metadata와
거부 경로를 검사한다. Master Data의 전체 release history는 V9부터 PostgreSQL의
range exclusion·확장·트랜잭션 SQL을 사용하므로 H2 호환 모드로 검증하지 않는다.

`MasterDataMigrationExecutorPostgresqlTest`는 외부 접속정보 없이 폐기형 로컬
PostgreSQL을 시작하고, 테스트마다 빈 DB를 만든다. 실제 패키징된
`db/contexts/master-data`와 `MigrationExecutor`를 사용해 신규 설치, 전체 JPA 소유
테이블/정밀도/FK/index/unique, 발행된 V5에서 최신 버전으로의 순방향 업그레이드,
Batch metadata, 중복 실행, 기간 중복 거부와 음수 가격 거부를 검사한다. 운영
target을 제한하거나 release SQL을 제거하지 않는다. PostgreSQL 실행 실패는 테스트
실패이며 환경 변수를 이용한 외부 DB 대체나 조건부 skip은 없다.

```powershell
.\gradlew.bat :migration-runner:test :migration-runner:bootJar --no-daemon --console=plain --max-workers=1
```

첫 실행에는 테스트 전용 embedded-postgres 의존성과 플랫폼 실행 파일 캐시가 필요하다.
개발 PC에서는 저장소 정책에 따라 다운로드·로컬 DB 실행 승인을 먼저 받고, 기존 캐시로
실행할 때는 `--offline`을 추가한다. H2 결과만으로 PostgreSQL 검증까지 통과했다고
판단하지 않는다. 코드 되돌림은 이 테스트/build 변경의 검토된 revert로 수행하며,
발행된 migration과 업무 데이터는 보존한다.

## Commands

자격증명은 명령행 인자가 아니라 실행 환경의 secret injection으로만 전달한다.

```powershell
$env:MIGRATION_DB_URL = 'jdbc:postgresql://<host>:<port>/<context-db>?sslmode=verify-full'
$env:MIGRATION_DB_USER = '<schema-migrator-role>'
$env:MIGRATION_DB_PASSWORD = '<injected-secret>'
$env:MIGRATION_TARGET_ENV = 'production'
$env:MIGRATION_EXPECTED_DATABASE = '<context-db>'

java -jar migration-runner\build\libs\account-migration-runner.jar `
  --context=auth --action=validate
```

실제 변경은 두 개의 별도 승인 신호가 모두 있어야 실행된다.

```powershell
$env:MIGRATION_ALLOW_MIGRATE = 'true'
$env:MIGRATION_CHANGE_TICKET = '<approved-change-id>'

java -jar migration-runner\build\libs\account-migration-runner.jar `
  --context=auth --action=migrate
```

지원 action은 `validate`, `migrate`뿐이다. `clean`, `repair`는 제공하지 않는다. 일반 context의
baseline 자동 생성도 금지한다. 유일한 예외인 Closing은 과거 계약을 승계하기 위해 전용 history가
없는 비어 있지 않은 DB의 10개 V49 테이블과 전체 컬럼 타입·길이·nullability, identity,
PK/FK/기간 unique를 먼저 검사하고, 일치할 때만 version 49 baseline을 만든 뒤 V50/V51을 적용한다.
일부 테이블만 있거나 V50 컬럼이 history 없이 존재하거나 같은 이름의 잘못된 운영 인덱스가 있으면 중단한다.
`MIGRATION_TARGET_ENV`는 `development` 또는 `production`이고 production은
`sslmode=verify-full`이 필수다. 예상 DB 이름은 context의 canonical token(예:
`auth`, `auth_dev`, `auth_prod`)과 일치해야 하며 URL path와 실제 `current_database()`를 모두
대조한다. 종료 코드는 `0` 성공, `2` 인자/승인/환경 오류, `3` baseline 차단, `4` Flyway 또는
DB 오류다. URL의 `user`/`password` query parameter는 거부하며 Flyway 로그도 차단해 오류
출력에 DB 주소와 자격증명을 반복하지 않는다.

전체 운영 순서와 권한·forward-fix 정책은
[`docs/db/postgresql-migration-runbook.md`](../docs/db/postgresql-migration-runbook.md)를 따른다.
