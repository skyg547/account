# Runtime Execution Matrix

이 문서는 GitHub Issue `#227`의 최신 실행 감사 결과입니다. 기준은 `origin/main@1ae9e108`(2026-08-14)이며, 격리 worktree에서 실행한 Gradle/JAR 검사와 현재 Compose 계약을 근거로 합니다.

## 판정 기준

| 판정 | 의미 |
| --- | --- |
| `PASS` | 추적된 설정과 산출물만으로 명령이 성공했습니다. |
| `REQUIRED_LOCAL_INPUT` | 저장소에 둘 수 없는 보안 입력을 실행자가 임시로 제공해야 합니다. |
| `FAIL` | 현재 checkout의 추적된 local 구성으로 기동하지 못합니다. |
| `BLOCKED` | 필요한 도구 또는 승인된 의존성이 없어 실제 명령을 수행하지 못했습니다. |

`ProfileJar`는 datasource, H2 URL, JPA/Flyway 설정이나 secret을 주입하지 않습니다. 자식 Java 프로세스에는 OS/JDK 실행에 필요한 최소 환경변수와 격리된 빈 `user.home`만 전달합니다. 부모 셸의 Spring, datasource, proxy, control-plane, secret, JVM option, cloud-binding 값은 제거하고 증거 문자열은 비식별화합니다. 따라서 `ProfileJar` 실패는 외부 설정으로 가려지지 않은 local 계약 결과입니다.

감사 도구는 Gradle 하위 명령을 기본 600초, JAR 기동을 기본 30초로 제한하고 stdout/stderr를 동시에 수집합니다. 실패와 `BLOCKED`도 JSON을 먼저 기록한 뒤 nonzero로 종료합니다.

## 환경 계약

| 환경 | 실행 방식 | DB 계약 | 현재 검증 |
| --- | --- | --- | --- |
| `local` | Gradle, executable JAR, NPM | DB 모듈은 H2 | Java packaging/test/JAR 실제 실행; NPM은 아래 제약 참조 |
| `dev` self-contained | root Compose | Compose 관리 PostgreSQL | 정적 정책/매핑 PASS; 실제 render/build/up 미검증 |
| `dev` external | external-dev override | 공유 개발 PostgreSQL | 환경변수 계약만 검증; 외부 DB 미접속 |
| `prod` | immutable image + production Compose | 외부 PostgreSQL | 정적 image/env/TLS 정책 PASS; 실제 배포 미검증 |

외부 PostgreSQL, private host, credential, 기존 컨테이너에는 접근하지 않았습니다.

## 최신 인벤토리와 패키징

Gradle에는 root 외 72개 subproject가 있습니다.

| 역할 | 수 | 최신 결과 |
| --- | ---: | --- |
| API | 17 | 실행 JAR packaging 대상 |
| Batch | 15 | 실행 JAR packaging 대상; smoke에서는 Job 비활성 |
| 인프라 서버 | 3 | Config Server, Discovery, Gateway |
| CLI | 1 | Migration Runner |
| Library | 19 | `*:core`, Contracts, Shared Kernel |
| Aggregator | 17 | 실행 JAR이 아닌 모듈 경계 |

| 검증 | 결과 |
| --- | --- |
| 실행 target Gradle task 계약 | `PASS` |
| 36개 실행 target 실제 `bootJar --offline` 및 단일 JAR 검사 | 36/36 `PASS` |
| 19개 library + 17개 aggregator 독립 `test + jar --offline` | 36/36 `PASS` |

## Profile-only JAR 결과

36개 실행 산출물의 최종 집계는 `PASS_STARTED` 16, `PASS_EXITED` 15, `FAIL` 5입니다. 다섯 실패 가운데 Closing API/Batch는 local 구성 결함이고, Auth/Budget/Gateway는 보안 입력이 없는 fail-closed 결과입니다.

| 모듈 | API | Batch | 판정 및 후속 |
| --- | --- | --- | --- |
| Master Data | `PASS` | `PASS` | local H2 기동 |
| Closing | `FAIL` | `FAIL` | API `FiscalPeriodMapper`는 `#422`, Batch `JournalPostingPort`는 `#423` |
| Journal Ledger | `PASS` | `PASS` | local H2 기동 |
| Expenditure Resolution | `PASS` | `PASS` | local H2 기동 |
| Payable | `PASS` | `PASS` | local H2 기동 |
| Receivable | `PASS` | `PASS` | local H2 기동 |
| Tax | `PASS` | `PASS` | local H2 기동 |
| Asset Lease | `PASS` | `PASS` | local H2 기동 |
| Budget | `REQUIRED_LOCAL_INPUT` | `PASS` | API는 32-byte 이상 임시 `AUTH_JWT_SECRET` 필요; `#249` |
| Deposit | `PASS` | `PASS` | local H2 기동 |
| Loan | `PASS` | `PASS` | local H2 기동 |
| Account Mart | `PASS` | `PASS` | local H2 기동 |
| ECL | `PASS` | `PASS` | local H2 기동 |
| Reconciliation | `PASS` | `PASS` | local H2 기동 |
| Reporting | `PASS` | `PASS` | local H2 기동 |
| Auth | `REQUIRED_LOCAL_INPUT` | N/A | 추적된 `application-local` 부재와 stale 정책 설명은 `#420` |
| Internal Audit | `PASS` | N/A | local H2 기동 |

Config Server와 Discovery는 standalone JAR 기동을 통과했습니다. Gateway는 JWT secret/public-key/JWKS 중 하나를 요구해 입력 없는 엄격 smoke에서 fail-closed 했으며, 임시 local 입력과 실행 명령 계약은 `#421`로 추적합니다. Migration Runner는 `--list`로 컨텍스트 목록을 출력하고 정상 종료했습니다.

## Local 정적 계약

DB 실행 target 32개 모두 H2/PostgreSQL runtime dependency와 dev/prod profile을 갖습니다. 31개는 명시 local profile을 가지며 Auth API만 추적된 local resource가 없습니다. 명시 local H2 URL은 28개이며 Asset Lease API, Auth API, Journal Ledger API, Loan Batch는 shared resource 또는 embedded-database 구성으로 실제 H2 기동에 성공하거나(Auth 제외) 별도 보안 입력 단계에서 중단됩니다.

H2를 사용한 실제 기동 성공과 각 실행 모듈에 중복된 datasource 파일을 두는 것은 같은 요구가 아닙니다. 이 매트릭스는 실제 독립 기동 결과와 정적 resource 위치를 구분해 기록합니다.

## Frontend NPM 결과

`package.json`, lockfile, `dev`/`build`/`start` script와 Node 20 계약은 추적되어 있고 PR `#398`에서 clean lifecycle 검증 후 병합됐습니다. 이번 격리 worktree에는 `node_modules`가 없고 저장소 정책상 사용자 승인 없는 package install이 금지되어 현재 세션에서는 실제 NPM lifecycle을 재실행하지 않았습니다. `Frontend` 모드는 이 상태를 `BLOCKED`와 nonzero로 정확히 기록했습니다. 이는 새 제품 결함으로 중복 발행하지 않습니다.

## Dev/Prod Compose 결과

현재 inventory에서 Java 실행 대상은 root development Compose에 36/36, production Compose에 서버형 Java 대상 35/35가 매핑됩니다. Frontend와 개발 인프라 서비스는 별도 Compose entry로 관리됩니다. DB app 32개는 dev/prod에서 PostgreSQL driver와 profile을 사용하며 Compose에 H2 URL을 두지 않습니다.

정적 Compose/image policy는 Config Server의 `DevelopmentComposePolicyTest`, `ProductionComposePolicyTest`, `ContainerImagePolicyTest`가 소유합니다. 최신 실행 21개 중 20개는 통과했고, production test 하나가 이동 전 `docs/production-compose.md`를 참조하는 경로 회귀로 실패해 `#424`로 분리했습니다. 실제 canonical runbook은 `docs/guides/production-compose.md`입니다. 이 매트릭스의 2026-08-14 감사 호스트에는 Podman CLI만 있고 Compose provider가 없어 실제 `compose config`, image build, `up`, health, PostgreSQL 연결을 수행하지 않았습니다.

2026-08-28의 Issue #520은 전체 17-context external-dev와 별도로, Auth/Master Data
외부 DB와 Frontend 로그인 경로만 선택하는 저자원 Compose를 `tools/**`에 추가합니다.
Config Server, Discovery, Auth, Master Data, Gateway, Frontend와 2-context DB gate만 정의하고
Java image는 Gradle worker 1로 순차 빌드합니다. 이 개발 서버의 Podman은 Docker Compose
v5.4 provider를 사용하며, 값 비노출 validator self-test와 generated fixture
`config --quiet`은 통과했습니다. 2026-08-30에 병합된 PR #576/#597로 7개 최소 컨테이너 헬스,
/login HTTP 200, Frontend→Gateway→Auth HTTP 400 smoke 검증이 완료되었습니다.
Issue #521은 이 실기동 검증 결과를 바탕으로 초보자용 Podman 최소 이미지 빌드·실행·안전 롤백
가이드를 [development-compose.md](development-compose.md)에 정합화했습니다.

중복 없이 기존 Issue를 계속 사용합니다.

- `#66`: 실제 development Compose와 PostgreSQL 기동 gate
- `#228`: canonical image 실제 build gate
- `#230`: production immutable image/external PostgreSQL runtime gate
- `#249`: Budget runtime/Compose/PostgreSQL gate
- `#420`: Auth local H2/JWT 입력 계약
- `#421`: Gateway standalone local JWT 입력 계약
- `#422`: Closing API local mapper composition
- `#423`: Closing Batch local port composition
- `#424`: Production Compose policy test의 canonical runbook 경로

## 재현 명령

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Inventory -OutputPath C:\tmp\account-inventory.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode TaskContract -OutputPath C:\tmp\account-task-contract.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Packaging -OutputPath C:\tmp\account-packaging.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Libraries -OutputPath C:\tmp\account-libraries.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode ProfileJar -PackagingResultPath C:\tmp\account-packaging.json -OutputPath C:\tmp\account-profile-jar.json
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\runtime-smoke.ps1 -Mode Frontend -OutputPath C:\tmp\account-frontend.json
```

`LocalJar`는 문제 원인 분리를 위해 H2/JPA/Flyway 값을 주입하는 진단 모드입니다. `LocalJar` 성공만으로 추적된 local profile 완료를 주장하지 않습니다. 특정 target은 `-Project ':closing:api,:closing:batch'`처럼 제한할 수 있습니다.

## 안전 및 rollback

- DB migration, remote schema, Compose volume, 외부 컨테이너를 생성·수정·삭제하지 않았습니다.
- host, username, password, token 또는 private URL을 결과와 저장소에 기록하지 않습니다.
- timeout 시 감사 도구가 자신이 생성한 process tree만 종료합니다.
- 이 Issue의 tool/document/harness 변경만 검토된 revert로 되돌릴 수 있습니다.
- volume 삭제나 `down -v`는 별도 사람 승인 없이는 수행하지 않습니다.
