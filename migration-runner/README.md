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

`--list`는 자격증명 없이 16개 context의 `READY`/`BLOCKED` 상태를 출력한다. 현재 이 stack은
Expenditure Resolution, Payable, Receivable의 분리된 V1 baseline까지 포함해
`READY 12 / BLOCKED 4`다. baseline이
불완전한 context는 해당 GitHub Issue가 해결될 때까지 DB 접속 전에 종료 코드 `3`으로
차단된다.

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
