# reconciliation local run

## 실행 프로파일 계약

- 기본 `local` 프로파일은 H2 PostgreSQL mode, 전용 Flyway V1, Hibernate `validate`를 사용한다.
- `dev`/`prod`는 PostgreSQL 15+ 전용이며 런타임 Flyway/DDL/SQL init/Batch metadata init을 금지한다. 배포 전에 서버 버전이 15 이상인지 확인하고 `migration-runner --context=reconciliation`을 실행한다.
- `prod`는 `sslmode=verify-full`을 강제하고 DB 비밀번호는 환경 변수로만 주입한다.
- 실제 PostgreSQL 검증은 승인 환경에서 별도로 필요하다. Journal/Ledger 원격 어댑터는 Issue #266에서 구현하므로 그 전까지 완전한 dev/prod 업무 플로우는 준비되지 않았다.

개발 환경은 `DEV_DB_HOST`, `DEV_DB_PORT`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD`, 운영은 대응하는 `PROD_DB_*` 변수를 secret injection으로 제공하고 `--spring.profiles.active=dev|prod`로 시작한다. 사설 호스트나 자격증명을 저장소에 기록하지 않는다.

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 저장소 루트를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`reconciliation`은 이제 `core/api/batch` 하위 Gradle 모듈로 분리되어 있다.

- `reconciliation:core`: command, 대사 단위, 실행, 차이, 조정 정책, persistence adapter, outbound port를 담는다.
- `reconciliation:api`: Spring Boot HTTP 실행 진입점. Controller/DTO/Bean Validation을 소유하고 DTO를 core command로 변환한다.
- `reconciliation:batch`: Spring Boot Batch 실행 진입점. Job/Step 제어와 파라미터 전달만 담당하고 비교 규칙은 core를 참조한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > reconciliation > core > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Reconciliation Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.
5. HTTP API를 직접 띄울 때는 Gradle task `:reconciliation:api:bootRun`을 실행한다.
6. Batch 컨텍스트만 확인할 때는 Gradle task `:reconciliation:batch:bootRun`을 실행한다.

추가된 실행 구성:

- `.run/Reconciliation Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :reconciliation:core:compileJava :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1
```

API 서버 실행:

```powershell
.\gradlew :reconciliation:api:bootRun --console=plain --max-workers=1
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :reconciliation:batch:bootRun --console=plain --max-workers=1
```

실행 JAR 생성 및 직접 실행:

```powershell
.\gradlew :reconciliation:api:bootJar :reconciliation:batch:bootJar --console=plain --max-workers=1
java -jar reconciliation\api\build\libs\account-reconciliation-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -jar reconciliation\batch\build\libs\account-reconciliation-batch-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

실제 Batch Job 실행:

```powershell
.\gradlew :reconciliation:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=reconciliationDailyJob reconciliationDate=2026-06-19 runBy=LOCAL deepMode=false" --console=plain --max-workers=1
```

`reconciliationDailyJob`은 활성 대사 단위를 `RunReconciliationCommand`로 core 서비스에 넘긴다. `deepMode=true`를 주면 원천/인터페이스/전표/원장 단계별 Deep reconciliation을 실행한다. `ReconciliationBatchJobRegistryConfiguration`은 Batch Job 등록 시점을 늦추는 인프라 설정이며 업무 판단을 넣지 않는다.

API 스모크만 확인하고 서버를 계속 띄우지 않을 때:

> 위 API 실행 명령은 `web-application-type=none`으로 컨텍스트만 확인한다. 실제 HTTP 서버를 띄워 API를 호출하려면 해당 옵션을 빼고 `:reconciliation:api:bootRun`을 실행한다.

## 테스트 데이터 주의사항

대사 실행에는 최소한 다음 데이터가 필요하다.

- `ReconciliationUnit`과 `criteriaJson`
- `ReconciliationRule`
- `DifferenceReasonCode` 중 `GENERIC_MISMATCH`
- `RECON_EXTERNAL_STAGE_RECORD` 원천 집계 데이터
- `JournalQueryPort`가 반환할 대상 원장 집계

조정 전표 자동 생성까지 확인하려면 `JournalPostingPort` 테스트 더블 또는 journal-ledger 통합 구성이 필요하다.
