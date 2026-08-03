# PostgreSQL Migration Runbook

## Supported version

- 개발·운영 PostgreSQL의 최소 지원 버전은 **15**다. 루트 Compose는 PostgreSQL 15,
  전용 PostgreSQL Compose는 PostgreSQL 16을 사용한다.
- 승인 환경 검증은 migration 실행 전에 `SHOW server_version_num`으로 150000 이상인지
  확인한다. 버전 확인이 끝나지 않은 외부 DB에는 migration을 실행하지 않는다.
- Reconciliation 기준선은 nullable lineage의 재적재를 차단하기 위해 PostgreSQL 15의
  `UNIQUE NULLS NOT DISTINCT`를 사용한다.

## Contract

- 개발·운영 DB는 context별 PostgreSQL database와 runtime role을 분리한다.
- schema owner/migrator만 DDL 권한을 가지며 API/Batch runtime role에는 DML과 sequence 사용
  권한만 부여한다.
- API/Batch는 동일 context database와 context `flyway_schema_history`를 공유한다. Closing은
  기존 계약 때문에 `flyway_schema_history_closing`을 유지한다. Spring Batch metadata는
  domain version namespace와 충돌하지 않도록 별도 `flyway_schema_history_batch`에 기록한다.
- 애플리케이션은 `spring.jpa.hibernate.ddl-auto=validate`,
  `spring.batch.jdbc.initialize-schema=never`, startup Flyway disabled를 사용한다.
- `migration-runner`만 release gate에서 schema를 변경한다. DB 주소, 사용자, 비밀번호는
  Compose 파일이나 Git에 저장하지 않고 secret provider에서 주입한다.

## Current inventory

| Context | State | Evidence or blocker |
| --- | --- | --- |
| account-mart | READY | published V1-V5 preserved; forward-only V6 schema convergence; Issue #255 stacked Draft PR |
| asset-lease | READY | complete PostgreSQL V20 parity baseline; Issue #254 stacked Draft PR |
| auth | READY | V70-V73 |
| budget | READY | V50 |
| closing | READY | V49 clean baseline + V50 state upgrade + V51 indexes; guarded legacy baseline 49 transition; Issue #250 stacked Draft PR |
| deposit | READY | PostgreSQL-specific V40; published H2 checksum preserved |
| ecl | READY | published V1-V3 preserved; forward-only V4 schema convergence; Issue #255 stacked Draft PR |
| expenditure-resolution | READY | isolated PostgreSQL V1 baseline; Issue #252 stacked Draft PR |
| journal-ledger | READY | published V1/V10 preserved; forward-only V11 complete baseline; Issue #251 stacked Draft PR |
| loan | READY | PostgreSQL-specific V30 plus V31-V33 |
| master-data | READY | published V1-V5 preserved; forward-only V6 complete baseline; Issue #251 stacked Draft PR |
| payable | READY | isolated PostgreSQL V1 baseline; Issue #252 stacked Draft PR |
| receivable | READY | isolated PostgreSQL V1 baseline; Issue #252 stacked Draft PR |
| reconciliation | READY | isolated PostgreSQL V1 baseline; Issue #253 stacked Draft PR |
| reporting | READY | V60-V63 |
| tax | READY | isolated PostgreSQL V1 baseline; Issue #253 stacked Draft PR |

`READY`는 artifact와 H2 PostgreSQL-mode 검증이 있다는 뜻이며, 이 변경만으로 실제
PostgreSQL 검증이 끝났다는 뜻은 아니다. 승인된 PostgreSQL 검증 환경의 clean migrate +
validate 증거가 #244 완료 게이트다.

## Release sequence

1. 배포할 jar의 commit SHA와 digest를 변경 티켓에 고정한다.
2. `--list`로 context가 `READY`인지 확인한다. `BLOCKED`를 우회하지 않는다.
3. 변경 티켓에 context, canonical database 이름, target environment를 함께 고정한다.
   Runner에는 `MIGRATION_EXPECTED_DATABASE`와 `MIGRATION_TARGET_ENV`를 주입한다. 예상 이름은
   context token과 URL database path에 일치해야 하고 연결 직후 `current_database()`와 다시
   대조된다. production URL은 `sslmode=verify-full` 없이는 거부된다.
4. 대상 DB의 복구 지점/PITR 상태와 최근 restore rehearsal 결과를 확인한다.
5. runtime API/Batch writer를 중지하거나 변경 윈도우의 write quiescence를 확인한다.
6. 기존 DB는 `--action=validate`를 먼저 실행한다. checksum 오류나 pending migration 외
   불일치는 중단하고 조사한다. clean DB는 migration이 pending이므로 이 단계의 실패가
   예상되며 새 database임을 별도 증거로 남긴다.
7. 변경 티켓과 `MIGRATION_ALLOW_MIGRATE=true`를 주입해 한 개의 runner만
   `--action=migrate`를 실행한다.
8. 동일 artifact로 `--action=validate`를 다시 실행한다. pending migration이 하나라도
   남으면 실패다.
9. runtime role로 DDL이 거부되는지, API의 JPA validate와 Batch repository 재시작 smoke가
   성공하는지 확인한 뒤 애플리케이션을 연다.

Spring Batch metadata는 별도 history의 공통 `V1__spring_batch_metadata_v5_1.sql`로 생성된다.
Domain tables 때문에 schema가 이미 non-empty여도 Batch history만 version `0`으로 초기화한 뒤
V1을 적용한다. Context history에는 원칙적으로 baseline을 자동 생성하지 않는다. Closing만 기존
`flyway_schema_history_closing` 계약 때문에 예외다. history가 없는 비어 있지 않은 DB에서
10개 legacy V49 테이블의 전체 컬럼 타입·길이·nullability, identity, PK/FK/기간 unique가
확인되고 V50의 `state` 컬럼이 아직 없을 때만 49 baseline marker를 기록한 뒤 V50/V51을
적용한다. 불완전하거나 부분 적용된 모양과 같은 이름의 잘못된 운영 인덱스는 중단한다. 이 기준 파일은 적용 후
수정하지 않고 후속 schema 변경은 새 forward migration으로 추가한다. Job instance,
execution, step execution/context 테이블과 세 sequence를 앱 기동 전에 provision하므로
`initialize-schema=never`에서도 restart metadata가 보존된다. Runner는 적용·검증 뒤 Spring
Batch 5.1의 전체 table/column 집합과 세 sequence가 정확히 존재하는지 확인하며, 구버전이나
부분 schema는 성공으로 처리하지 않는다.

## Failure and recovery

- migration 실패 시 Flyway history와 PostgreSQL 로그를 민감정보 없이 수집하고 앱 배포를
  중단한다.
- 이미 성공한 versioned migration 파일을 수정하거나 `repair`로 checksum을 덮지 않는다.
- 데이터/DDL을 내리는 자동 rollback SQL과 `clean`은 금지한다.
- 실패가 transaction 안에서 rollback되었는지 확인한 뒤 새 version의 forward-fix
  migration을 코드 리뷰·백업·restore rehearsal와 함께 배포한다.
- 비트랜잭션 DDL이 부분 적용됐다면 DBA 승인 하에 상태를 대조하고, 원본 migration을
  재작성하지 않은 새 migration으로 수렴한다.
- 서비스 복구가 먼저 필요하면 검증된 이전 application image로 되돌리되, 이미 적용된
  schema와의 backward compatibility를 확인한다. schema 자체는 파괴적으로 되돌리지 않는다.

## Verification boundaries

- 로컬 자동 검증: runner 단위 테스트, PostgreSQL-mode H2에서 검증 가능한 context의 domain
  migration + Batch metadata migrate/validate, 32개 API/Batch executable의 PostgreSQL driver,
  executable jar `--list`.
- 승인 환경 검증: 16개 READY context 각각 clean PostgreSQL migrate + validate, 기존 schema
  upgrade, runtime least-privilege, Batch restart.
- 아직 금지된 범위: 운영 DB 접속, 운영 데이터 변경, 실제 secret 출력, 승인 없는 image pull
  또는 배포.
