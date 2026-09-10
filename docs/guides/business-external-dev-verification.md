# Issue #653 업무 패키지 실기동 검증

## 최종 실기동 결과

최종 확인: **2026-09-10 15:51 KST**, 13개 이미지 순차 빌드 및 전체 유지 상태의 Accounting7 → Products3 → Risk3 최종 재검증 **PASS**. 컨테이너와 기존 플랫폼을 유지했다.

| 패키지 | API | Eureka application | 이미지 | health / Eureka |
| --- | --- | --- | --- | --- |
| Accounting | `journal-ledger-api` | `JOURNAL-LEDGER-API` | PASS | UP / UP |
| Accounting | `closing-api` | `CLOSING-SERVICE` | PASS | UP / UP |
| Accounting | `payable-api` | `PAYABLE-API` | PASS | UP / UP |
| Accounting | `receivable-api` | `RECEIVABLE-API` | PASS | UP / UP |
| Accounting | `expenditure-resolution-api` | `EXPENDITURE-RESOLUTION-API` | PASS | UP / UP |
| Accounting | `tax-api` | `TAX-API` | PASS | UP / UP |
| Accounting | `reporting-api` | `REPORTING-API` | PASS | UP / UP |
| Products | `deposit-api` | `DEPOSIT-SERVICE` | PASS | UP / UP |
| Products | `loan-api` | `LOAN-API` | PASS | UP / UP |
| Products | `asset-lease-api` | `ASSET-LEASE-SERVICE` | PASS | UP / UP |
| Risk | `account-mart-api` | `ACCOUNT-MART-API` | PASS | UP / UP |
| Risk | `ecl-api` | `IFRS9-ALLOWANCE-API` | PASS | UP / UP |
| Risk | `reconciliation-api` | `RECONCILIATION-API` | PASS | UP / UP |

모든 행에서 현재 컨테이너 IP·8080 등록, running=true, restart=0, OOM=false, 실제 CPU0.50/RAM768MiB 상한을 확인했다. 최근 최대10,000줄의 ERROR/startup failure/DB failure 패턴은 각각0건이다. 세 패키지의 DB ACL prerequisite도 통과했다. Risk DB gate는 정상 Compose 경로(223초), 기존 Accounting/Products checker는 아래에 설명한 동일 설정·직접 ACL·상태 재검증 경로를 사용했다.

정규화된 실행 증거는 `/tmp/issue653-runtime-evidence.log`이며 마지막 `PASS final 13-service runtime verification`과 프로세스 exit0을 확인했다. 원본 로그·환경변수 값·DB 응답 데이터는 게시하지 않았다. 일괄 동시 기동 없이 패키지별/서비스별 순차 실행했고 각 패키지 완료 후 유지한 상태로 다음 패키지를 진행했다.

## 범위와 전제

`agent/653-business-runtime`, `/tmp/account-653-business-runtime`,
최초 base는 `26f266986e8d0336675f75ae6da3b7cb7807e2a7`이며, 재개 시 fetched
`origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`로 충돌 없이 fast-forward했다.
허용 파일은 세 패키지 Compose, `tools/run-business-external-dev.py`,
`tools/test_run_business_external_dev.py`, 이 문서, AI harness의
`agent-status.md`, `worklog.md`, `handoff.md`, `docs/history/CODEX_WORKLOG.md`다.
이후 [사용자 승인 댓글](https://github.com/skyg547/account/issues/653#issuecomment-5604915412)로
ClosingApplication 및 필요한 Closing 설정/어댑터와 직접 회귀 검증 범위를 추가했다.
[최종 사용자 승인](https://github.com/skyg547/account/issues/653#issuecomment-5611054436)으로
Payable/Receivable/Expenditure Resolution/Tax/Reporting/Reconciliation의 API `build.gradle`
Eureka starter 추가 및 13개 API 실기동 완료에 필요한 수정이 승인되었다.
[구체화한 Loan 파일 범위](https://github.com/skyg547/account/issues/653#issuecomment-5611100834)는
Loan API 구성, Core 의존성/외부 어댑터 및 직접 관련 회귀 테스트다.
[Reporting 범위](https://github.com/skyg547/account/issues/653#issuecomment-5611823487)와
[Deposit 범위](https://github.com/skyg547/account/issues/653#issuecomment-5611833868)는
각 Core HTTP 어댑터·의존성, 직접 관련 API/Core 테스트와 Compose의 명시적 dev 활성화다.
SQL, 스키마, 공유 계약, 제공자 HTTP API와 플랫폼 설정은 변경하지 않는다.

기존 PostgreSQL, Redis, minimal Config Server/Discovery/Auth/Master Data/Gateway/Frontend와
외부 `account-network`가 준비되어 있어야 한다. `.env.external-dev`는 승인된 경로의
private regular file이며 실행기에 경로만 전달한다. 내용을 출력하거나 shell에 source하지 않는다.
실행기는 subprocess 출력·응답 본문을 외부에 출력하지 않고 고정된 결과와 오류 개수만 출력한다.
`inspect Config.Env`, 전체 Compose render, 원본 애플리케이션 로그는 출력하지 않는다.

API는 Spring `dev`, Compose `external-dev`를 사용한다. 환경변수로 packaged profile의
Discovery/Eureka 비활성 설정을 명시적으로 켜고, 별도 application name이 없는 Reporting은
`reporting-api`를 지정한다. API별 상한은 CPU 0.50 / RAM 768 MiB이며 DB gate는
CPU 0.20 / RAM 192 MiB다. `Containerfile.minimal-auth-java`의 `--max-workers=1`을 재사용한다.

## 실행 순서

설치된 Docker Compose CLI provider를 사용한다. Python `podman-compose`는 사용하지 않는다.
빌드는 의존성과 base image가 없으면 다운로드할 수 있으므로 승인된 빌드 환경에서만 실행한다.
아래 `--env-file` 경로는 승인된 파일의 경로로 지정한다. 값 자체는 명령 인자에 넣지 않는다.

```bash
export PYTHONDONTWRITEBYTECODE=1
python3 -m unittest discover -s tools -p 'test_run_business_external_dev.py' -v

python3 tools/run-business-external-dev.py preflight --package accounting --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py build --package accounting --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py up --package accounting --env-file /home/ho/dev/account/.env.external-dev

python3 tools/run-business-external-dev.py preflight --package products --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py build --package products --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py up --package products --env-file /home/ho/dev/account/.env.external-dev

python3 tools/run-business-external-dev.py preflight --package risk --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py build --package risk --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py up --package risk --env-file /home/ho/dev/account/.env.external-dev

python3 tools/run-business-external-dev.py verify --package accounting
python3 tools/run-business-external-dev.py verify --package products
python3 tools/run-business-external-dev.py verify --package risk
```

각 명령이 성공한 경우에만 다음 명령을 실행한다. 세 패키지를 동시에 실행하지 않는다.
`build`는 각 API를 하나씩 빌드하고, `up`은 해당 패키지 DB gate를 먼저 통과한 뒤 각 API의
health/Eureka 확인이 끝나야 다음 API를 시작한다. 패키지 종료 시 모든 API를 다시 확인한다.
다음 패키지가 시작된 뒤에도 기존 패키지를 유지하며 마지막에 세 패키지 전체를 재검증한다.
`--service`는 장애 재현용 단일 API 선택이며 패키지 전체 검증을 대신하지 않는다.
기본 engine은 Podman이고 Docker 사용 시 `--engine docker`를 각 명령에 추가한다.

`up`은 지정 서비스에만 `--no-deps --no-build --pull never`를 적용한다. Compose는 이미지나
설정이 바뀐 기존 서비스를 재생성할 수 있다. 이전 이미지를 유지하는 `--no-recreate`는 쓰지 않는다.
변경 전 이미지와 시작 전 컨테이너 목록은 값이 없는 형식으로 별도 확인하여 복구에 보관한다.
메모리는 다음 API 768 MiB에 더해 2 GiB 여유가 없으면 중단한다. 이 검사는 호스트 가용 메모리이며
다른 작업의 동시 사용량을 예약하지 않으므로 담당자는 다른 빌드와 동시 실행하지 않는다.
명령 timeout은 해당 subprocess 그룹만 종료한다. 엔진이 이미 생성한 컨테이너나 서버 측 빌드는
계속 남을 수 있으므로 상태를 확인한 뒤 재시도한다. 광역 자동 롤백은 하지 않는다.

## 성공 판정

각 API에서 다음을 모두 확인한다.

- `/actuator/health` 응답 JSON의 최상위 `status`가 `UP`.
- Eureka에 해당 application이 현재 컨테이너 IP, 포트 8080, 상태 `UP`으로 등록됨.
- 실제 엔진 설정의 CPU 0.50, RAM 768 MiB와 running / restart 0 / OOM false.
- 최근 최대 10,000줄에서 ERROR, startup failure, DB connection/schema 오류 패턴 0건.

로그 검사는 제한된 패턴과 최근 로그 범위의 검사다. 모든 경고나 과거의 잘린 오류 부재를
증명하지 않는다. Eureka 등록은 업무 API의 권한·회계 처리·서비스 간 업무 연동 성공을 뜻하지 않는다.

## 이전 실행 기록 (2026-09-10 KST, Closing 수정 전)

이전 실행 시점의 13/13 실기동 수용 조건은 **미충족**이었다. 재개 세션에서 Podman 조회와 로컬 API ping이
응답했고, 회계 이미지 7개를 모두 지정 Containerfile의 `bootJar --max-workers=1`로
순차 빌드했다. 실제 env quiet Compose preflight와 회계 DB prerequisite가 통과했다.
DB gate의 healthcheck timeout은 세 패키지 모두 10초에서 30초로 늘렸다.

| 패키지 | API | 이미지 빌드 | 실기동 결과 |
| --- | --- | --- | --- |
| Accounting | journal-ledger-api | PASS | health/Eureka UP, restart 0, OOM false, CPU 0.50, RAM 768 MiB, 검사 대상 오류 0 |
| Accounting | closing-api | PASS | FAIL: `Schema-validation: missing table [account_subjects]`; restart 1, OOM false; 해당 컨테이너만 중지·보존 |
| Accounting | payable-api, receivable-api, expenditure-resolution-api, tax-api, reporting-api | PASS (5개) | Closing 실패로 순차 기동 중단; 미기동 |
| Products | deposit-api, loan-api, asset-lease-api | 미실행 | 앞 패키지 실패로 미기동 |
| Risk | account-mart-api, ecl-api, reconciliation-api | 미실행 | 앞 패키지 실패로 미기동 |

Closing 진단 시점의 원본 로그 비출력 집계는 ERROR 4, startup failure marker 0,
DB/schema failure marker 10이었다. 이 수치는 중복 stack trace를 포함한 패턴 발생 횟수이며
서로 다른 장애 수를 뜻하지 않는다. 실제 원인 exception은 Hibernate
`SchemaManagementException`이고, 애플리케이션 실패로 재시작했다. Logstash 연결 경고는
DB 장애로 분류하지 않았다.

정적 구현 근거: `closing/api/src/main/java/com/ho/account/closing/ClosingApplication.java`
25–35행은 Master Data entity/repository까지 스캔하고, 38–47행은 monolith persistence
adapter를 import한다. Master Data의 `AccountSubjectEntity`는 `account_subjects`를 요구하지만
`closing/core/src/main/resources/db/closing-migration/V49__closing_clean_baseline.sql`은
Closing 전용 테이블만 생성한다. 이는 허용된 Compose/runner 수정으로 해결할 수 있는
Eureka 또는 timeout 문제가 아니다. JPA validate 비활성화, 다른 모듈 DB 연결, 임의 DDL은
수행하지 않았다. 기존 [Issue #250](https://github.com/skyg547/account/issues/250)의
Closing PostgreSQL/JPA 부팅 검증 범위와 관련되며, 이후 승인된 Closing 수정 범위에서 재검증한다.

회귀 검증은 `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools
-p 'test_run_business_external_dev.py' -v`로 38/38 PASS다. 독립 Reviewer도 같은 테스트와
diff/marker 검사를 통과했고 당시 검토 범위의 확정 결함을 찾지 못했다.
빌드는 bootJar 패키징 검증이며 13개 모듈의 전체 업무 테스트를 실행했다는 의미는 아니다.

시작 전 minimal platform 및 회계 DB gate는 healthy였으며, 기존 Logstash와 Kibana는
unhealthy였다. 해당 관측성 서비스는 이 작업에서 수정/재시작하지 않았다. 가용 메모리는
대략 11–12 GiB였고 I/O 대기가 관측됐다. 원본 env, 응답 본문, 원본 로그는 출력하지 않았다.

## 승인된 Closing dev 구성 수정

`ClosingApplication`의 기본 엔티티·리포지토리 스캔은 Closing 소유 범위만 포함한다.
기존 외부 JPA 조합은 `ClosingMonolithConfiguration`의 `!dev` 프로파일에 보존했으므로
이번 변경은 dev 컨테이너 분리에 한정되며 prod의 스키마 분리 해결을 주장하지 않는다.
Compose는 회계기간 HTTP 어댑터를 활성화하고 Master Data와 Journal의 내부 주소를 지정한다.

Closing의 Journal HTTP 어댑터는 기간별 전표 목록, 전표번호 조회, draft 생성, 승인·전기를
실제 공개된 계약으로 호출한다. 숫자 ID 조회·상세 조회·집계는 현재 Journal REST에 대응하는
계약이 없어 `UnsupportedOperationException`으로 명시적으로 실패한다. 빈 결과나 0을
성공으로 반환하지 않는다. 따라서 조정 전표 등록, 연차 결산 등 해당 포트를 사용하는 업무는
후속 Journal HTTP 계약 확장·정합성 검증이 필요하다. health/Eureka UP은 이 업무의 완료를 뜻하지 않는다.
회계기간 HTTP 변경과 Closing DB 트랜잭션 사이의 분산 원자성도 이번 검증 범위 밖이다.

실제 `ClosingApplication`을 dev/H2 PostgreSQL mode에서 Closing V49–V51 migration과
JPA validate로 시작하는 회귀 테스트가 외부 엔티티 유입 및 포트 누락을 확인한다.
HTTP 계약 테스트는 응답 실패·미지원 조회·timeout 입력을 검증한다.
기존 core/API/batch 테스트도 실행하며, 실제 PostgreSQL 증거는 컨테이너 gate에서 수집한다.

## 이전 승인 후 실기동 기록 (6개 Eureka 의존성 승인 전, 2026-09-10 KST)

Closing 수정은 `44eb41b5`에 저장했다. JDK17 컨테이너(CPU1, RAM1536MiB,
network none)에서 `:closing:core:test :closing:api:test :closing:batch:test --offline
--console=plain --no-daemon --max-workers=1` 최종 실행이 124/124(59/49/16),
실행기 회귀 테스트가 38/38 통과했다. 독립 리뷰의 Core outbound adapter 위치 P3를
수정했고 최종 소스 리뷰에 남은 P0–P3는 없다.

| 서비스/검증 | 결과 |
| --- | --- |
| Accounting quiet Compose / DB prerequisite | PASS |
| journal-ledger-api | 반복 PASS: health/Eureka UP, restart0/OOMfalse, CPU0.50/RAM768MiB, 검사 오류0 |
| closing-api | 재빌드 및 실제 PostgreSQL PASS: health/Eureka UP, restart0/OOMfalse, CPU0.50/RAM768MiB, 검사 오류0 |
| payable-api | health UP, running/restart0/OOMfalse, 검사 오류0; Eureka 등록 조건 미충족 |
| 나머지 Accounting 4개 API | 이미지 빌드 완료, Payable gate 때문에 미기동 |
| Products / Risk 각 3개 API | 이전 패키지 gate 미충족으로 빌드·기동 미실행 |

Payable 실행 JAR 목록을 값 비출력 방식으로 확인했다. `spring-cloud-config-client`는
있지만 `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-netflix-eureka-client`,
`eureka-client`가 모두 없다. 이 때문에 health UP을 Eureka 성공으로 간주할 수 없다.
같은 누락을 독립 리뷰로 다음 여섯 API `build.gradle`과 전이 의존성에서 확인했다:
`payable`, `receivable`, `expenditure-resolution`, `tax`, `reporting`, `reconciliation`.
수정안은 각각 `implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'`
한 줄 추가이며, 기존 Spring Cloud BOM을 재사용한다. 당시 allowlist 밖이므로 소스는
변경하지 않았고 `/tmp/issue-653-eureka-dependency.patch`에 검토용 패치만 준비했다.
사용자에게 6개 빌드 파일 범위 확장을 요청했다.

Payable의 Connection refused 경고는 ERROR/DB 실패와 별도로 집계했다. 실제 진단에서
DB/schema/startup 오류는 없었으며 원본 로그·env·HTTP 응답 내용은 출력하지 않았다.
Loan의 외부 JPA 스캔과 자체 스키마 불일치도 정적 선행 점검에서 발견했으나 Loan 실기동
실패를 주장하지 않는다. 당시 Loan 수정은 승인 범위 밖이어서 실제 증거와 범위 확장을 먼저 확인했다.

`up --package accounting`은 `payable-api: health/Eureka deadline exceeded`로 exit 1 종료했고
후속 서비스를 시작하지 않았다. 이 단계에서는 Accounting 패키지 checkpoint 및 13/13 수용 조건이 미충족이었다. 정상 확인된
Journal Ledger/Closing과 진단용 Payable, 기존 인프라를 유지한다. 검증 완료 후 게시한다는
이 단계에서는 사용자 게이트에 따라 Draft PR을 생성하지 않았다.

## 최종 승인 후 재개와 Loan dev 구성

Reconciliation API의 Gradle 8.7 의존성 해석은 Eureka 추가 후 Archaius에 대한
`Corrupt serialized resolution result`로 중단됐다. API의 BOM 관리를 Core와 같은
native 방식으로 맞추되 `enforcedPlatform`으로 기존 버전 선택을 보존했다.
Payable과 비교한 Boot 3.2.5, Spring 6.1.6, Cloud Commons 4.1.2,
Netflix client integration 4.1.1, Jackson 2.15.4, Hibernate 6.4.4.Final,
Eureka client 2.0.2는 동일하며 Archaius는 양쪽 런타임에 모두 없다.
일반 `platform`만 사용했을 때의 Jackson 상승은 최종 변경에 포함하지 않았다.
Reconciliation core/API 테스트 94/3개와 위 8개 항목 비교가 통과했다.

6개 API에 기존 BOM이 관리하는 Eureka starter를 추가했다. 재빌드한 Payable의 실제
health/Eureka UP, restart0/OOMfalse, CPU0.50/RAM768MiB, 검사 오류0을 확인하여
이전 등록 누락을 해소했다. 이후 회계 → 상품 → 리스크 순서로 패키지 검증을 계속한다.

RecalculationRun의 EIR 필드는 기존 migration의 `old_eir`/`new_eir` 열에 명시적으로
매핑한다. 정밀도·계산 로직·migration은 바꾸지 않는다.

Loan API의 dev JPA 범위는 Loan 엔티티와 저장소만 포함한다. 기존 외부 모듈 스캔은
`LoanMonolithConfiguration`의 `!dev`에 보존한다. Core HTTP 어댑터는
`dev`와 `account.loan.remote.enabled=true`를 모두 요구하며 Products API Compose만
이 값을 켠다. 기본 dev Batch는 기존 내장 어댑터를 유지하고 새 HTTP URL을 요구하지 않는다.

계정과목은 실제 Master Data 유효일 조회를 사용한다. 거래처 숫자 ID 및 통화 조회의
공개 HTTP 계약은 없으므로 `requireLoanReferences`는 명시적으로 실패한다.
따라서 dev Loan 신규 계약 생성 및 이 포트를 사용하는 조회는 아직 지원되지 않는다.
Journal은 실제 create → approve → post → 조회 확인 순서로 호출하며 actor, 금액,
lineage를 보존한다. 실패 시 쓰기를 자동 재시도하지 않는다. 부분 성공과 기존 비-DRAFT
전표 응답은 담당자의 정합성 확인이 필요하며 분산 원자성·자동 복구를 보장하지 않는다.

회귀 테스트는 실제 migration-runner 구성과 같은 PostgreSQL V30 + 공통 V31–V33을
H2 PostgreSQL mode에 적용한 뒤 JPA validate와 외부 엔티티 부재를 확인한다.
어댑터 선택과 HTTP 오류·응답 불일치·금액/actor/lineage를 함께 검증한다.
H2 검증은 실제 PostgreSQL 컨테이너 health/Eureka gate를 대체하지 않는다.

## 롤백과 다음 담당자

정상 패키지는 유지한다. 실패한 이번 실행에서 생성/재생성한 API만 정확한 Compose project와
service label을 확인한 후 `podman stop <확인한-container-id>`로 중지한다. 예전부터 정상인
컨테이너, 플랫폼, DB, Redis는 중지하지 않는다. 컨테이너·이미지·네트워크·볼륨을 삭제하지 않는다.
이미지/설정 변경 전 버전 복구가 필요하면 보관된 이전 이미지와 이전 Compose 설정으로
해당 서비스만 재생성하고 동일 검증을 수행한다. 소스 롤백은 리뷰된 PR revert를 사용한다.

Integrator가 승인된 범위의 수정을 적용하고 13/13 실기동 검증을 완료했다.
다음 단계는 `Refs #653` Draft PR에서 독립 Reviewer와 사람의 검토다.
Ready 전환, main 병합, Issue close, branch/worktree 삭제는 각각 별도 승인 대상이며 이번 실행에 포함하지 않는다.

Implementer tier: High reasoning (difficulty:high)
Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## Reporting / Deposit 실기동 포트 보완

Reporting standalone dev는 기존 JPA `LedgerClientAdapter`가 필요한 `LedgerQueryPort`를
제공하지 않아 `LoadLedgerPort` 누락으로 기동에 실패했다. `account.reporting.remote.enabled=true`
에서 실제 Journal Ledger의 GL/SL 조회 포트를 연결하고, 기존 JPA 집계의 Bean 탐색 순서 의존성을
제거한다. GL/SL 금액은 `BigDecimal`이며 날짜·필터·필수 응답값을 검증한다. 제공자가 지원하지 않는
DB 집계 및 전표 ID/상세 조회는 명시적 실패이므로 주석 drill-through까지 검증된 것으로 보지 않는다.

Deposit dev는 `account.deposit.remote.enabled=true`에서 실제 Master Data 기준일 조회와
Journal 초안 생성·조회 검증을 연결한다. 기존 local sample 어댑터는 활성화하지 않는다.
Journal 재전달은 실제 기존 전표의 ID·기준일·통화·라인리지·금액·라인 내용을 검증하고 실제 상태를
유지한다. ID 기반 원격 상태 조회 계약이 없는 `approveAndPost`는 쓰기 전에 명시적으로 실패한다.
현재 Deposit outbox 초안 생성 호출에는 이 메서드가 쓰이지 않는다. 원격 저장과 outbox 완료 기록은
별도 트랜잭션이므로 동시 요청의 exactly-once나 분산 원자성을 보장한다고 주장하지 않는다.

Deposit의 기존 `deposit_outbox.payload`는 published V42의 `TEXT`에 맞춰 `@Lob`만
제거한다. 긴 Unicode payload의 flush/clear 후 JPA 재조회로 텍스트 매핑을 검증한다.
두 보완은 Controller/업무 계산/스키마/제공자 API를 바꾸지 않는다. API dev 테스트는 기존의 실제
모듈 마이그레이션 집합을 H2 PostgreSQL 모드에 적용하고 Hibernate `validate` 및 모듈 소유 엔티티만
스캔되는지 확인한다. 이 테스트는 실제 PostgreSQL/컨테이너 실기동 검증을 대신하지 않는다.

## 후속 수정 테스트 결과

JDK17 컨테이너에서 `--offline --no-daemon --max-workers=1 --console=plain`으로 실행했다.
빌드 JVM 검증 컨테이너는 CPU1/RAM1536MiB이며 API 운영 컨테이너 상한 CPU0.50/RAM768MiB와 구분한다.

실행한 Gradle task 집합은 다음과 같다. 각 행을 위 옵션과 함께 별도 순차 실행했으며
Loan/Reporting/Deposit은 수정 후 해당 Core/API/Batch 집합을 재검증했다.

```text
:closing:core:test :closing:api:test :closing:batch:test
:payable:api:test :receivable:api:test :expenditure-resolution:api:test :tax:api:test :reporting:api:test :reconciliation:api:test
:reconciliation:core:test
:loan:core:test :loan:api:test :loan:batch:test
:reporting:core:test :reporting:api:test :reporting:batch:test
:deposit:core:test :deposit:api:test :deposit:batch:test
```


| 대상 | Core | API | Batch | 합계 |
| --- | ---: | ---: | ---: | ---: |
| Closing (이전 변경 유지, XML 재확인) | 59 | 49 | 16 | 124 |
| Loan | 99 | 6 | 3 | 108 |
| Reporting | 92 | 10 | 6 | 108 |
| Deposit | 85 | 4 | 3 | 92 |
| Reconciliation | 94 | 3 | 미실행 | 97 |
| Payable / Receivable / Expenditure Resolution / Tax | 미실행 | 2 / 6 / 8 / 2 | 미실행 | 18 |

중복 제외 Java547개, failure/error/skip0이다. 이 중 이번 재개 변경의 테스트는423개이며
Closing124개는 변경되지 않은 이전 실행 결과를 독립 확인했다. Python 실행기 회귀는38/38 PASS다.
Reconciliation 및 나머지 Eureka-only API의 Batch는 변경하지 않아 추가 실행하지 않았다.
Journal Ledger/Asset Lease/Account Mart/ECL은 Java 변경이 없어 별도 단위 테스트를 추가 실행하지 않으며, 이미지 bootJar 빌드와 실제 컨테이너 검증으로 확인한다. 이 모듈의 전체 업무 테스트 통과를 주장하지 않는다.
실제 금융 거래 쓰기, 제공자에 없는 REST 계약, 분산 원자성/동시 exactly-once는 검증 범위 밖이다.

Reporting dev 컨텍스트 테스트는 Vault를 비활성화하므로 실제 Compose에서의 Vault 자동 설정
실패를 검출하지 못했다. 첫 수정 이미지의 실기동에서 `clientAuthentication`/`vaultTemplate`
생성 실패(restart1/OOMfalse, DB 오류0)를 발견했고, Vault가 없는 external-dev에 맞춰 Reporting에만
`SPRING_CLOUD_VAULT_ENABLED=false`를 추가했다. Loan/Deposit의 기본 Vault 비활성 설정은 유지한다.
컨테이너 실기동은 테스트와 별도로 실제 설정 차이까지 검증해야 한다.

## 이번 실행의 Podman 전송 경로

호스트 I/O 압력으로 일반 Podman CLI 조회가 지연되어 이미 실행 중인 **동일 사용자·동일 엔진**의
로컬 Unix API socket을 명시한 임시 Python helper를 사용했다. 엔진/컨테이너/검증 조건은 바꾸지
않았고, 새 서버나 패키지를 설치하지 않았다. 임시 helper는 `/tmp/issue653-remote-runtime.py`,
민감값 없는 실행 결과는 `/tmp/issue653-runtime-evidence.log`에 남긴다. 재개용 helper의 남은 단계 목록은 실행 중 축소했으나 이미 실행 중인 프로세스에는 영향을 주지 않았다. 완료 후 보관한 helper는 세 패키지 전체 `verify`만 수행하도록 복원했으며, 기동·복구 이력은 아래 설명과 결과 기록을 함께 확인한다.

같은 전송 경로로 단일 검증을 재현하려면 저장소 루트에서 다음을 실행한다. 기존 socket이 있어야
하며, 없으면 기본 CLI 절차를 사용한다. build/up을 실행할 때는 위의 패키지별 순서를 유지하고
승인된 env 파일의 **경로**만 전달한다.

```bash
python3 - verify --package accounting <<'PY_RUNTIME'
import importlib.util
import os
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "runtime", Path("tools/run-business-external-dev.py").resolve())
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
original_run = runtime.run
socket_url = f"unix:///run/user/{os.getuid()}/podman/podman.sock"

def same_engine_run(command, **kwargs):
    if command[0] == "podman":
        command = ["podman", "--remote", "--url", socket_url, *command[1:]]
    return original_run(command, **kwargs)

runtime.run = same_engine_run
raise SystemExit(runtime.main())
PY_RUNTIME
```

## 실기동에서 추가 확인한 실행 설정

- Products/Risk DB wrapper는 상대 경로의 공통 ACL 검사 스크립트를 source한다. 기존 Compose에 해당 파일 mount가 없어 Products gate가 exit2로 실패했다. 공통 파일을 `/postgres/runtime/check-products-risk-database.sh`에 단일 읽기 전용 mount로 연결했다. SQL/ACL/계정정보나 검사 조건은 바꾸지 않았으며 Products와 Risk DB prerequisite 재검증은 모두 통과했다.
- Deposit은 기본값으로 실행되는 outbox scheduler를 `ACCOUNT_DEPOSIT_OUTBOX_SCHEDULER_ENABLED=false`로 비활성화했다. 이번 검증에서는 실제 금융 이벤트 자동 relay를 수행하지 않는다. 업무 요청의 동기 publish 경로까지 차단하는 설정은 아니다.
- Loan 실기동에서 PostgreSQL SQLState53300과 연결 한도 초과를 확인했다. 13개 API의 Hikari `maximum-pool-size=3`, `minimum-idle=1`을 Compose에서 설정했다. 각 API가 단일 풀을 사용하면 최대 합계39개/최소 idle 목표13개이며 플랫폼·DB gate 연결은 별도다. DB 서버 한도·권한·DDL·자격증명과 Batch 풀은 변경하지 않았다.
- 연결을 확보하기 위해 검증된 Deposit을 먼저 중지했으나 용량 부족이 지속되어 Reporting도 개별 중지했다. 이후 기존 DB/up/verify 절차로 Reporting과 Deposit을 순서대로 복구해 각 검증을 통과했다. Accounting도 API별로 설정을 적용한 뒤 7개 전체를 재검증했다. 전체 동시 중지/기동은 수행하지 않았다. 작은 풀에서 업무 동시 요청이 대기하거나 timeout될 수 있으므로 health 통과를 업무 부하 검증으로 보지 않는다.

현재 환경변수 이름의 바인딩은 별도 리뷰어가 실제 사용 버전(Spring Boot3.2.5/Spring6.1.6/HikariCP5.0.1)의 Binder로 확인했다. 가상 SystemEnvironmentPropertySource에서 HikariDataSource 기본10/10이 최대3/최소1로 바뀌고 bound=true, exit0임을 확인했다. 이 프로브는 DB 연결이나 실제 환경변수 읽기를 수행하지 않으며 업무 부하 검증은 아니다.

Accounting 재적용 중 Closing은 Compose 단일 서비스 기동180초 초과로 생성 후 시작 전 상태에 머물렀다. 별도 점검에서 running=false/restart0/OOMfalse, StartedAt 미설정, 검사 로그0을 확인했다. 실행기의 일반 명령·Compose 기본·개별 API 기동 제한을300초로 늘렸다. DB gate240초/내부 wait180초, 이미지 빌드2400초, 서비스 health/Eureka deadline600초는 유지하며 Python38 회귀를 통과했다.

300초 적용 후 이미 정상인 Journal Ledger에 대한 Compose up도 엔진 단계에서 timeout됐다.
별도 점검에서 Journal Ledger의 실행 상태와 현재 Compose config hash 일치를 확인했다.
Closing은 정확한 project/service label, 현재 Compose hash, 생성 후 미기동 상태와 메모리를
확인한 뒤 동일 엔진의 `podman start <확인한 CID>`로 해당 컨테이너만 시작했다. 시작 명령은98초,
후속 기존 verify는 health/Eureka UP, restart0/OOMfalse, CPU0.50/RAM768MiB, 검사 오류0으로
통과했다. 이후 남은 회계4개는 정상 `up --service` 절차로 처리하고 회계7개 전체를 재검증했다.
이 복구는 timeout을 성공으로 간주하지 않고 실제 상태·설정 일치와 전체 검증으로 확인한 것이다.

회계 DB-check의 no-change Compose 호출도240초를 초과했지만, 실제 컨테이너는
running/restart0/OOMfalse/healthy였고 기존 ACL 검사 스크립트를 직접 실행하면 exit0이었다.
이에 임시 helper는 Accounting/Products의 **이미 존재하는 DB-check**만 정확한 label과
현재 Compose config hash 일치 확인 → 기존 read-only ACL 스크립트 실제 실행 → 정상 상태
확인으로 재검증한다. env 파일 identity 검사는 바깥 실행기에서 유지한다. 각 API 기동과
전체 health/Eureka/로그/자원 검사, Risk DB-check 생성, 이미지 빌드는 기존 실행 경로다.
독립 리뷰에서 누락된 gate는 없었다. 이 재사용 절차는 개별 명령마다 timeout을 적용하므로
전체 소요 시간이 기존 단일 Compose240초보다 길 수 있으며 timeout을 성공으로 처리하지 않는다.

Asset Lease의 기존 지급결의 연동 fallback은 호출 시 명시적으로 실패하며, Kafka 이벤트 발행도 별도 브로커 연결에 의존한다. 이번 기동 검증은 해당 지급결의·이벤트 처리의 성공을 증명하지 않는다.

## 문서 기준 통합과 로컬 계약 검증

최종 구현 커밋 `4fcf1cfd6937` 이후 `origin/main@b7c1c7c1fa45`의 문서·정책·Node 테스트를 작업 브랜치에 통합했다. 기록4파일의 충돌6곳은 양쪽 이력을 모두 보존했고 `docs/ai-harness/conflict-log.md`에 판단을 남겼다. main으로 PR을 병합한 것은 아니다. Java/빌드 구성은 이 통합에서 바뀌지 않아 기존547개 Java 증거와13개 이미지·실기동 결과를 유지한다.

```bash
node --test tools/ci/harness-pr-contract.test.cjs tools/ci/harness-quality-contract.test.cjs
```

Node v22.23.2에서64/64 PASS, failure/skip0, exit0이다. 이 로컬 검사는 CI에 연결되어 있지 않으며 문서 구조 검사 성공이 실제 코드·설명 의미의 독립 리뷰를 대신하지 않는다. Draft PR의 Module Validation, Reporting Validation, Agent Merge Guard 결과와 사람 리뷰는 별도로 확인한다.

독립 읽기 전용 Reviewer `/root/runtime_review`가 결과표·실행 증거·양쪽 이력 보존·문서 의미를 대조하고 Q1–Q4 PASS, 남은 P0–P3 없음으로 확인했다. Node64도 별도 재실행해 통과했다. 이 검수는 GitHub 사람 승인이나 Ready·merge 권한을 대신하지 않는다.
