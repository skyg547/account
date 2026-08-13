# Runtime Execution Matrix

이 문서는 GitHub Issue `#227` 기준으로 `origin/main@c0fb871b`의 실행 계약을 다시 확인한 결과입니다. 과거 worklog의 성공 기록이 아니라 현재 checkout에서 실행한 명령과 정적 설정을 근거로 합니다.

> Issue #231 후속 변경에서는 `:internal-audit:api`를 local H2/Flyway 및 dev/prod PostgreSQL을 갖춘 실행형 JAR로 복구했습니다. 실제 Job/Step이 없는 `:internal-audit:batch`와 `:auth:batch`는 Gradle·image·Compose 실행 대상에서 제거했습니다. 아래 #227 표는 당시 감사 snapshot이며, 현재 실행 인벤토리는 API 17개, Batch 15개, 인프라 서버 3개입니다.
>
> Issue #66 후속 변경은 root 개발 Compose에 manifest의 35개 Java target과 Frontend를 모두 매핑하고, API 17개·Batch 15개, self-contained/external-dev PostgreSQL gate, 개발용 `next dev` 컨테이너를 정의했습니다. 아래 Module Contract/Container Result 표는 #227 당시 snapshot이며 현재 Compose 구현 상태를 나타내지 않습니다. 실제 Docker/Podman Compose render/build/up 증거는 provider가 있는 환경의 close gate로 남습니다.

## Environment Contract

| Environment | Launch method | Database contract | Configuration rule |
| --- | --- | --- | --- |
| `local` | Gradle `bootRun`, executable JAR, frontend NPM | DB 사용 모듈은 H2, Config/Discovery/Gateway는 DB 없음 | 외부 Config/Eureka/Vault 없이 단독 smoke가 가능해야 함 |
| `dev` self-contained | Docker Compose | Compose가 관리하는 PostgreSQL | 안전한 example 값과 service health dependency 사용 |
| `dev` shared | Docker Compose external-dev override | 기존 공유 개발 PostgreSQL | host, JDBC URL, user, password는 환경변수로만 주입하고 저장소에 기록하지 않음 |
| `prod` | immutable image + production Compose override | 외부 PostgreSQL | 기본 자격증명/H2 fallback/build 금지, 환경 또는 secret provider로만 주입 |

사용자가 제공한 공유 개발 환경의 pgAdmin/PostgreSQL 포트는 2026-07-30에 TCP 도달 가능했습니다. 로그인, DB metadata, schema, credential, 컨테이너 목록은 조회하지 않았고 정확한 host는 저장소와 GitHub Issue에 기록하지 않았습니다.

## Inventory

Gradle `projects`는 root 외에 70개 subproject를 구성합니다. 분류는 `tools/runtime-smoke.ps1`로 재생성할 수 있습니다.

| Role | Count | Projects |
| --- | ---: | --- |
| API | 16 | `:account-mart:mart-api`, `:asset-lease:api`, `:auth:api`, `:closing:api`, `:deposit:api`, `:ecl:ecl-api`, `:expenditure-resolution:api`, `:internal-audit:api`, `:journal-ledger:api`, `:loan:api`, `:master-data:api`, `:payable:api`, `:receivable:api`, `:reconciliation:api`, `:reporting:api`, `:tax:api` |
| Batch | 16 | `:account-mart:mart-batch`, `:asset-lease:batch`, `:auth:batch`, `:closing:batch`, `:deposit:batch`, `:ecl:ecl-batch`, `:expenditure-resolution:batch`, `:internal-audit:batch`, `:journal-ledger:batch`, `:loan:batch`, `:master-data:batch`, `:payable:batch`, `:receivable:batch`, `:reconciliation:batch`, `:reporting:batch`, `:tax:batch` |
| Infra server | 3 | `:config-server`, `:discovery`, `:gateway` |
| Library | 18 | `:account-mart:mart-core`, `:asset-lease:core`, `:auth:core`, `:closing:core`, `:contracts`, `:deposit:core`, `:ecl:ecl-core`, `:expenditure-resolution:core`, `:internal-audit:core`, `:journal-ledger:core`, `:loan:core`, `:master-data:core`, `:payable:core`, `:receivable:core`, `:reconciliation:core`, `:reporting:core`, `:shared-kernel`, `:tax:core` |
| Aggregator/compatibility | 16 | `:account-mart`, `:asset-lease`, `:auth`, `:closing`, `:deposit`, `:ecl`, `:expenditure-resolution`, `:internal-audit`, `:journal-ledger`, `:loan`, `:master-data`, `:payable`, `:receivable`, `:reconciliation`, `:reporting`, `:tax` |
| Phantom | 1 | `:app` |

Library와 aggregator는 `jar`/`test` 대상입니다. 서버용 `bootRun`/`bootJar`/Docker/Compose를 강제하지 않습니다. `:app`은 source와 build file이 없는 phantom project이므로 성공한 library로 간주하지 않습니다.

## Current Packaging Result

35개 실행 후보에 대한 `bootJar --dry-run --offline`은 task path 존재 여부만 확인합니다. `tools/runtime-smoke.ps1 -Mode Packaging`으로 각 후보의 실제 offline packaging을 별도로 실행한 결과는 다음과 같습니다.

| Result | Projects | Evidence / issue |
| --- | --- | --- |
| PASS | 33개 API/Batch/infra executable JAR | 프로젝트별 실제 `bootJar --offline` 성공과 JAR artifact 확인 |
| NON_EXECUTABLE_COMPILE_FAILED | `:internal-audit:api` | source file의 UTF-8 BOM 때문에 `compileJava` 실패하고 `bootJar`도 비활성, existing `#73` |
| NON_EXECUTABLE | `:internal-audit:batch` | `compileJava`는 성공하지만 `bootJar { enabled = false }`; 실제 Job/Step도 없음, existing `#74` and coordination `#231` |

Master Data API의 중복 Application 제거가 반영된 최신 main에서는 API와 Batch 모두 packaging 및 local/H2 JAR context 기동을 통과했습니다.

18개 library와 16개 aggregator/compatibility project는 프로젝트별 실제 `test jar --offline`을 모두 통과했습니다. Phantom `:app`은 이 수치에서 제외했습니다.

패키징 결과를 재사용해 각 executable JAR을 local/H2 및 외부 인프라 비활성 인자로 기동한 결과는 다음과 같습니다. Batch는 컨텍스트만 기동했고 실제 업무 Job은 실행하지 않았습니다.

| Local JAR result | Count | Projects / evidence |
| --- | ---: | --- |
| PASS_STARTED | 4 | `:config-server`, `:discovery`, `:ecl:ecl-api`, `:gateway`; 시작 marker 확인 후 제한시간 종료 |
| PASS_EXITED | 21 | local smoke 컨텍스트가 `Started` marker 후 정상 종료한 API/Batch |
| FAIL | 8 | Asset Lease API/Batch, Journal Ledger Batch, Loan API/Batch: `BusinessPartnerJpaEntity` entity scan 누락; Closing API: H2 runtime driver 없음; Closing Batch: `BusinessPartnerPersistencePort` bean 누락; Journal Ledger API: repository 중복 등록 |
| BLOCKED_BY_PACKAGING | 2 | `:internal-audit:api`, `:internal-audit:batch` |

성공 25개는 “local 컨텍스트 기동” 계약에 대한 결과입니다. 대표 Batch 업무 Job의 입력·출력·재시작 검증은 포함하지 않았으므로 각 Batch Issue에서 별도로 수행해야 합니다.

### Latest-main follow-up

Draft PR 준비 전 `origin/main@36a1be4f`로 rebase한 뒤 원래 실패한 8개 target을 다시 `bootJar` 패키징하고 local JAR로 기동했습니다. 8개 패키징은 모두 PASS였고 Closing Batch는 Issue #43의 composition 보완으로 `PASS_EXITED`가 됐습니다. 나머지 7개 실패 원인은 원래 감사와 동일했습니다.

| Current result | Projects |
| --- | --- |
| PASS_EXITED | `:closing:batch` |
| FAIL | `:asset-lease:api`, `:asset-lease:batch`, `:closing:api`, `:journal-ledger:api`, `:journal-ledger:batch`, `:loan:api`, `:loan:batch` |

따라서 위의 8건 표는 `c0fb871b` 감사 snapshot 증거이고, 최신 main에서 실제로 남은 local 실행 실패는 7건입니다. Closing Batch의 대표 업무 Job 및 PostgreSQL 재시작 검증은 이 context smoke에 포함되지 않습니다.

## Module Contract Matrix

`PASS`는 현재 checkout에서 실제 offline packaging 또는 local JAR 컨텍스트 기동으로 확인된 항목이고, `GAP`은 후속 Issue가 필요한 항목입니다. dev/prod와 image 항목은 Docker가 없는 이 호스트에서 정적 감사만 수행했습니다.

| Module | Direct Gradle/JAR | Local DB | dev/prod PostgreSQL | Image/Compose | Follow-up |
| --- | --- | --- | --- | --- | --- |
| Config Server / Discovery / Gateway | specialized local JAR smoke PASS | DB 없음 | DB 없음 | module 정의 있음, 실제 image/root integration 미검증 | `#66`, `#228` |
| Contracts / Shared Kernel / `*:core` | library JAR PASS | N/A | N/A | 서비스 이미지 불필요 | library로 유지 |
| Master Data | API/Batch local JAR PASS | local smoke H2 PASS | driver 있음, 서비스 설정 상충 | API image만 있고 root integration GAP | `#72`, `#228`, `#66` |
| Journal Ledger | API/Batch package PASS, local JAR FAIL | repository 중복/API, entity scan 누락/Batch | driver/profile GAP | Docker context/artifact 및 Batch image GAP | `#75`, `#76`, `#228` |
| Closing | API/Batch package PASS; API local FAIL, Batch local PASS | API H2 driver 없음; Batch H2 context 기동 확인 | driver/profile GAP | Docker context/artifact 및 Batch image GAP | `#77`, `#78`, `#228` |
| Loan | API/Batch package PASS, local JAR FAIL | Master Data entity scan 누락 | driver/profile GAP | API JAR filename과 Batch image GAP | `#79`, `#80`, `#228` |
| Deposit | API/Batch specialized local JAR PASS | H2 dependency/config | driver/profile GAP | API/Batch Dockerfile·Compose 없음 | `#228` |
| Asset Lease | API/Batch package PASS, local JAR FAIL | Master Data entity scan 누락 | driver/profile GAP | API artifact/context와 Batch image GAP | `#89`, `#90`, `#228` |
| Payable / Receivable / Reconciliation / Tax / Expenditure Resolution | API/Batch local JAR PASS | local smoke H2 PASS | PostgreSQL runtime/profile GAP | aggregator JAR Dockerfile와 단일 API Compose GAP | `#60`, `#228`, `#66` |
| Reporting | API/Batch specialized local JAR PASS | local smoke memory/H2 mode PASS | driver/profile GAP | aggregator JAR Dockerfile와 단일 API Compose GAP | `#60`, `#228`, `#66` |
| Account Mart | API/Batch local JAR PASS | local smoke H2 PASS | `postgres/docker`만 있고 dev/prod GAP | API image 일부 정의, Batch Dockerfile/통합 Compose GAP | `#97`, `#98`, `#228` |
| ECL | API/Batch local JAR PASS | local smoke H2 PASS | `postgres/docker`만 있고 dev/prod GAP | API/Batch image 정의, root integration GAP | `#99`, `#100`, `#66` |
| Auth | API/Batch local JAR PASS | local smoke H2 PASS | PostgreSQL driver 있음 | Dockerfile/Run Configuration이 잘못된 aggregator task 사용 | `#60`, `#228`, `#231` |
| Internal Audit | API `bootJar` 및 local H2 JAR PASS, Batch 대상 없음 | H2 PostgreSQL mode + Flyway V60 + JPA validate | PostgreSQL driver, 환경변수 전용 dev/prod, prod TLS guard | API canonical image와 dev/prod Compose 계약 | 실제 PostgreSQL/Compose 기동 gate |
| Frontend | scripts/lockfile PASS | N/A | API URL은 env 주입 대상 | dev Compose가 `next dev`가 아니며 image 정의가 이중화 | `#228`, `#66` |

## Frontend Result

`frontend/package.json`에는 `dev`, `build`, `start`가 있고 lockfile도 있습니다. 이 외부 worktree에는 `node_modules`가 없으며 repository policy상 승인 없이 package installation을 하지 않았기 때문에 `npm ci`와 실제 build/start smoke는 실행하지 않았습니다. 기존 사용자 checkout의 dependency directory를 복사하거나 공유하지 않습니다.

## Container Result

현재 호스트에는 Docker CLI가 없어 image build와 `docker compose config/up`을 실행하지 못했습니다. 정적 감사에서는 다음 공통 결함을 확인했습니다.

- root Compose의 활성 서비스는 PostgreSQL, Redis, Kafka, frontend뿐이고 backend는 주석 예시입니다.
- 다수 module Compose가 module directory를 build context로 사용하지만 Dockerfile은 repository root의 wrapper/settings를 요구합니다.
- 다수 Dockerfile이 `:<module>:api:bootJar`가 아니라 aggregator `:<module>:build`를 실행하고 plain/wildcard JAR을 복사합니다.
- API/Batch가 모두 별도 image/service로 표현된 도메인은 Account Mart와 ECL뿐입니다.
- Java 17 repository contract와 Java 21 base image가 혼재합니다.
- production Compose/override는 없습니다.

Image 교정은 `#228`, root development orchestration은 기존 `#66`, development PostgreSQL ownership/bootstrap은 `#229`, production overlay는 `#230`이 소유합니다.

## Reproduce

PowerShell 실행 정책의 사용자 profile 오류를 피하기 위해 별도 프로세스로 실행합니다. `TaskContract`는 task path 확인일 뿐이고, 실제 검증은 아래 모드를 각각 사용합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Inventory
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Packaging -OutputPath C:\tmp\account-packaging.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Libraries -OutputPath C:\tmp\account-libraries.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode LocalJar -PackagingResultPath C:\tmp\account-packaging.json -OutputPath C:\tmp\account-local-jar.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Frontend
```

실패 프로젝트만 재현하려면 `-Project`를 사용합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode LocalJar -PackagingResultPath C:\tmp\account-packaging.json -Project :closing:api,:journal-ledger:api,:journal-ledger:batch,:loan:api,:loan:batch
```

Gradle은 외부 다운로드 없이 확인합니다.

```powershell
.\gradlew projects --offline --console=plain --no-daemon
```

공유 개발 인프라의 도달성을 확인할 때 host를 repository에 쓰지 말고 현재 shell 환경변수로만 전달합니다. 로그인, credential, DB metadata 조회는 이 smoke 범위가 아닙니다.

## Rollback And Safety

- 이 Issue의 rollback은 `README.md`, `docs/local-development.md`, 이 문서, `tools/runtime-smoke.ps1` 및 이 Issue가 추가한 AI harness 기록을 revert합니다.
- DB migration, Compose volume, 원격 컨테이너를 생성·수정·삭제하지 않습니다.
- 개발/운영 host, username, password, token, Config Server response body를 문서·JSON·worklog에 기록하지 않습니다.
- Named volume 삭제와 `down -v`는 별도 사람 승인 없이는 수행하지 않습니다.
