# Issue #653 업무 패키지 실기동 검증

## 범위와 전제

`agent/653-business-runtime`, `/tmp/account-653-business-runtime`,
최초 base는 `26f266986e8d0336675f75ae6da3b7cb7807e2a7`이며, 재개 시 fetched
`origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`로 충돌 없이 fast-forward했다.
허용 파일은 세 패키지 Compose, `tools/run-business-external-dev.py`,
`tools/test_run_business_external_dev.py`, 이 문서, AI harness의
`agent-status.md`, `worklog.md`, `handoff.md`, `docs/history/CODEX_WORKLOG.md`다.
이후 [사용자 승인 댓글](https://github.com/skyg547/account/issues/653#issuecomment-5604915412)로
ClosingApplication 및 필요한 Closing 설정/어댑터와 직접 회귀 검증 범위를 추가했다.
SQL, 스키마, 공유 계약, 다른 업무 모듈과 플랫폼 설정은 변경하지 않는다.

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
diff/marker 검사를 통과했고 현재 코드 변경의 확정 결함을 찾지 못했다.
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

## 롤백과 다음 담당자

정상 패키지는 유지한다. 실패한 이번 실행에서 생성/재생성한 API만 정확한 Compose project와
service label을 확인한 후 `podman stop <확인한-container-id>`로 중지한다. 예전부터 정상인
컨테이너, 플랫폼, DB, Redis는 중지하지 않는다. 컨테이너·이미지·네트워크·볼륨을 삭제하지 않는다.
이미지/설정 변경 전 버전 복구가 필요하면 보관된 이전 이미지와 이전 Compose 설정으로
해당 서비스만 재생성하고 동일 검증을 수행한다. 소스 롤백은 리뷰된 PR revert를 사용한다.

Integrator가 승인된 Closing 수정 및 위 순서로 13/13 실기동 검증을 진행한다.
검증 통과 후 Draft PR을 생성한다는 요청에 따라, 현재 PR은 게시하지 않고 본문을 준비한다. 이후 독립 Reviewer와 사람이 결과를 검토한다.
`Refs #653`을 유지하며 실기동 성공 전 Ready, merge, Issue close를 하지 않는다.

Implementer tier: High reasoning (difficulty:high)
Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.
