# GH-762 검증 근거

## 감사 버전의 실행 가능한 RED

- 감사 소스: `a97d10ab6efc2570a88f83d630242cd748f8be57`.
- 별도 검증 브랜치/워크트리: `agent/762-audited-proof`, `/tmp/account-762-audited-proof`.
- [`AuditedPeriodPostingCutoffRegressionTest.java`](AuditedPeriodPostingCutoffRegressionTest.java)를
  해당 버전의 `journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/ledger/`에
  복사하여 실행했습니다. 이 파일은 의도적으로 실패하는 역사적 probe이므로 현재 테스트 소스에는
  넣지 않습니다. 두 복사본 SHA-256은 모두
  `e5f52d7a55040d64a7a54caf82aaa16ab3cc8f2771f76b1f92c549d5580d8d51`입니다.
- 감사 워크트리의 `git diff --exit-code a97d10ab -- journal-ledger/core/src/main`은 0입니다.
  감사 대상 production 코드를 수정하지 않았습니다.

```bash
./gradlew :journal-ledger:core:test \
  --tests '*AuditedPeriodPostingCutoffRegressionTest' \
  --offline --no-daemon --console=plain --max-workers=1
```

2026-10-23 실행 결과: **1 test, 1 expected assertion failure, 0 errors/skips** (35초).
`A committed final-close snapshot must not miss a later posting`: expected **0**, actual **4**.

실제 감사 버전 PostingService·도메인·기간 필터가 OPEN을 읽고, JDBC transaction 안에서 합성
header/GL/SL/balance 행을 쓴 뒤 latch에서 멈춥니다. 다른 transaction은 CLOSED와 빈 snapshot을
커밋합니다. 원래 전기를 재개하면 네 효과가 이후 커밋되어 불변식 assertion이 실패합니다.
이는 static schedule을 실제 transaction interleaving으로 재현한 증거입니다.

범위: 실제 H2 commit 순서와 감사 서비스 경로이며, outbound persistence는 합성 테이블에 쓰는
대역입니다. 실제 Closing 서비스, PostgreSQL, 실제 GL/SL schema의 검증으로 해석하지 않습니다.
현재 버전의 GREEN은 별도 실제 admission/PostingService/Closing 복구 테스트로 검증합니다.

## 현재 구현의 검증 범위

- Contracts: HTTP JSON 명령·receipt 왕복 및 입력 경계 11 tests PASS.
- Journal: 실제 전기·외부 transaction commit barrier, 직접 잔액 변경, 역분개, DB gate와 migration.
- Closing: 영속 취소·재오픈 단계 및 재시작 후 복구, 증빙 checkpoint 바인딩, 엄격한 HTTP ACK.
- Cross-module: 실제 Journal SQL adapter와 Closing HTTP client를 loopback 서버로 연결합니다.
  합성 movement table은 protocol commit 검증용이며 실제 전표 검증은 Journal suite가 담당합니다.

운영 데이터, 실 배포, 운영 부하 시험은 이 로컬 검증의 범위가 아닙니다.

## 최종 통합 검증과 독립 리뷰

```bash
./gradlew :journal-ledger:test :closing:test :contracts:test
```

**PASS**, exit 0, 1분 36초, 54 tasks (2 executed, 52 up-to-date).
마지막 집중 실행으로 교체되었던 Journal API/Core는 전체 실행으로 갱신했고, 변경되지 않은
Closing·Contracts·Journal Batch의 이전 성공 결과는 Gradle up-to-date로 재사용했습니다.
최종 XML을 `/tmp/account-762-evidence/final-aggregate/`에 보관하고 부모가 직접 집계했습니다.

| Suite | 발견 | PASS | 실패/오류 | skip |
| --- | ---: | ---: | --- | ---: |
| Journal core | 353 | 353 | 0/0 | 0 |
| Journal API | 86 | 86 | 0/0 | 0 |
| Journal Batch | 38 | 38 | 0/0 | 0 |
| Closing core | 448 | 447 | 0/0 | 1 |
| Closing API | 187 | 187 | 0/0 | 0 |
| Closing Batch | 133 | 133 | 0/0 | 0 |
| Contracts | 11 | 11 | 0/0 | 0 |
| 합계 | **1,256** | **1,255** | **0/0** | **1** |

skip은 opt-in `JdbcAnnualJournalReadAdapterPostgresTest` 한 건이며 아래의 실제 PostgreSQL
집중 실행에서는 PASS했습니다. 전체 로그는 `/tmp/account-762-final-aggregate.log`입니다.
Closing 자체 전체 실행도 exit 0, 55초이며 API 187/Batch 133/Core 447 PASS + 동일 optional skip 1입니다.
최종 코드 수정 뒤 재실행되지 않은 필수 테스트는 없습니다. 초기 compile/fixture 실패는 위 결과로
대체했으며, 감사 소스의 의도된 RED는 성공 결과에 포함하지 않습니다.

```bash
./gradlew :journal-ledger:api:bootJar :journal-ledger:batch:bootJar \
  :closing:api:bootJar :closing:batch:bootJar \
  --offline --no-daemon --console=plain --max-workers=2
```

패키징은 exit 0, 18초입니다. 각 Journal API/Batch bootJAR에 포함된 V20과 Closing API/Batch의
V58이 source SQL과 byte-for-byte 일치함을 ZIP 내부를 직접 읽어 확인했습니다.
`git diff --check`, 정확한 변경 파일의 conflict-marker/allowlist 검사와 unmerged-index 검사도
PASS입니다. 91개 변경 파일은 Journal 25, Closing 56, Contracts 10개이며 다른 모듈·공용 하네스는
수정하지 않았습니다. 이 수치는 빌드 산출물을 제외한 Git 변경 파일 수입니다.

독립 읽기 전용 리뷰어 `/root/journal_analysis`와 `/root/closing_analysis`는 실제 diff·문서·보관 XML을
대조했습니다. HTTP 200 판정, 역사적 마감 봉인 bootstrap, V58 NULL CHECK, 오래된 MVCC 설명 지적을
원작성자가 수정한 뒤 **남은 P0–P3 없음, Q1–Q4 PASS**로 재검토했습니다.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `JdbcJournalPeriodAdmissionAdapter.java:41`, `ClosingTransitionTransactions.java:184`; 전체 1,255 PASS, PG 22 PASS | 해당 없음: 코드 변경에 적용 | 전역 직렬화의 운영 부하 검증 | 두 reviewer, 코드·XML 대조 |
| Q2 | PASS | `journal-ledger/docs/posting-concurrency.md:374`, `closing/docs/process-flow.md:176`, `contracts/docs/process-flow.md:78` | 해당 없음: 신규 프로토콜에 적용 | primary의 새 snapshot, 기간 정의 동결 | 두 reviewer, 실행 순서 대조 |
| Q3 | PASS | 모듈 README/schema/beginner/local-run 및 이 실행 근거; `closing/README.md:212`, `closing/docs/process-flow.md:713` | 해당 없음: 배포·사용법 변경에 적용 | 역사적 CLOSED/PERMANENTLY_CLOSED 봉인 대사, TLS | 두 reviewer, 수정 후 재검토 |
| Q4 | PASS | Journal adapter의 commit/tombstone/backdating 주석, `ClosingPeriodTransitionService.java:90`, `ClosingTransitionTransactions.java:242` | 해당 없음: 비자명 상태 전이에 적용 | 후속 변경도 잠금·해제 순서 보존 | 두 reviewer, 인접 주석 대조 |

## 실행한 PostgreSQL 및 JVM 복구 검증

- Journal 실제 PostgreSQL 집중 검증: **18 PASS** (admission 11, 실제 PostingService 경합 7),
  failures/errors/skips 0. 최신 API qualifier·오류 응답 검증은 별도 **9 PASS**.
  로그 `/tmp/journal-762-pg-focused-final.log`, 보관 XML
  `/tmp/account-762-evidence/postgres-journal-and-api/`.
- Journal 전체 H2 실행은 core 353/API 86/Batch 38 = **477 PASS**였습니다.
  이후 집중 실행이 XML을 교체했으므로 최종 전체 명령의 결과는 별도로 집계합니다.
- Closing PostgreSQL 교차 모듈 protocol 3 + 기존 연간 원천 MVCC 1 = **4 PASS**, failures/errors/skips 0,
  exit 0 (38초). 실제 Journal SQL adapter와 Closing HTTP client로 외부 transaction drain,
  응답 유실 후 provider/server/client 재생성, release를 추월하는 지연 fence를 검증했습니다.
  원천 MVCC 테스트는 같은 snapshot의 두 순회와 이후 재시작의 새 전기 노출을 검증합니다.
  로그 `/tmp/account-762-closing-pg.log`, XML `/tmp/account-762-evidence/postgres-closing/`.
  첫 실행의 200ms preflight checkpoint timeout은 주입 대상이 아닌 초기 연결까지 짧은 deadline을
  사용한 fixture 문제였습니다. preflight는 3초로 분리하고 실제 fence fault-injection은 200ms와
  latch barrier를 유지한 뒤 전체 4개를 다시 실행했습니다.

```bash
JOURNAL_POSTING_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:17622/account_762 \
./gradlew :journal-ledger:core:test --tests '*JournalPeriodAdmissionIntegrationTest' \
  --tests '*PostingConcurrencyIntegrationTest.fenceDrainsPostingUntilOuterCommitThenRejectsStaleValidation' \
  --tests '*PostingConcurrencyIntegrationTest.committedFenceRejects*' \
  --tests '*PostingConcurrencyIntegrationTest.directSingleAndBulkMovementAndReaggregationCannotChangeFencedSuccessors' \
  :journal-ledger:api:test --tests '*JournalPeriodControllerTest' \
  --tests '*JournalLedgerApiLocalProfileTest' --offline --no-daemon --console=plain --max-workers=2

JOURNAL_POSTING_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:17622/account_762 \
CLOSING_ANNUAL_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:17622/account_762 \
./gradlew :closing:core:test --tests '*JournalPeriodAdmissionIntegrationTest' \
  --tests '*JdbcAnnualJournalReadAdapterPostgresTest' \
  --offline --no-daemon --console=plain --max-workers=2
```

- 다음 명령은 실제 API bootJAR과 PostgreSQL 16.13으로 **PASS**, exit 0입니다.

```bash
./gradlew :journal-ledger:api:bootJar --offline --no-daemon --console=plain --max-workers=2
python3 journal-ledger/docs/issue-762/runtime-restart-smoke.py
```

첫 JVM에서 HTTP ACTIVE receipt를 받은 뒤 프로세스를 강제 종료했습니다. 두 번째 JVM은 같은
DB schema에서 정확히 같은 receipt를 복구하고, release를 커밋한 뒤 지연 fence 재시도를 HTTP 409로
거절했습니다. revision 증가도 확인했습니다. 합성 서비스 credential은 각 프로세스 환경과 HTTP
header에만 전달하며 로그·저장소에는 기록하지 않습니다. 증거 로그는
`/tmp/account-762-runtime-restart.log`, `/tmp/account-762-api-restart-{1,2}.log`입니다.

이 검증은 **ACK 후 실제 JVM 종료·재기동의 내구성**을 입증합니다. 진행 중인 전기 중간에 JVM을
종료한 시험은 아닙니다. 커밋 전 경합과 네트워크 응답 유실은 별도의 barrier/loopback 테스트로
확인합니다. 테스트용 HTTP는 loopback이며 운영 서비스 간 TLS 검증을 대신하지 않습니다.

## 기존 컴파일·픽스처 불일치 보정

기준 `ed539846`에도 있던 Closing 도메인/영속성 분리 후 누락 메서드와 테스트 API 불일치가
요청된 전체 검증을 막았습니다. 현재 port에 맞는 mapper hydration setter·saveAndFlush·현재 cycle 및
execution-key 조회를 연결하고, 제거된 ECL API·JPA domain/entity 타입을 사용하는 테스트를
현재 계약에 맞췄습니다. 기존 locking·unique key·재시도·금융 금액 assertion은 유지합니다.
API/Batch 구성은 실제 Master 감사 adapter를 등록하며, 선택적 내장 Journal이 있을 때만 현재
저장소 경로와 XML 매핑을 연결합니다. 독립 Closing의 Journal runtime dependency는 추가하지 않습니다.
연간 분류 수 제한 테스트는 생성한 계정을 실제 날짜별 Master 조회에 제공하도록 보정하고,
0원 ECL 테스트는 이미 구현된 lineage 사전 확인과 snapshot 기록을 인정하면서 금액 쓰기 없음은
계속 검증합니다. 신규 production 금융 계산이나 승인 정책은 이 보정에 추가하지 않습니다.

기준 HEAD의 `closing/README.md`에 남아 있던 실제 conflict marker 세 줄도 발견했습니다.
부모 Integrator 판단으로 두 쪽의 유효한 lineage·재오픈 안내를 보존하고 실제 V49–V58 resource에
맞춰 migration 순서를 정리합니다. 이 해결 기록은 사용자 allowlist에 따라 모듈 문서·하네스에만
남기며 공용 `conflict-log.md`는 수정하지 않습니다.

## PostgreSQL 환경

사용자가 승인한 실기동 검증을 위해 기존 이미지 `postgres:16-alpine`으로 독립적인 합성 DB를
생성했습니다. 컨테이너는 `account-762-period-admission-pg`, DB는 `account_762`, loopback port는
`17622`입니다. 운영 DB에는 연결하지 않습니다. PostgreSQL 16.13의 `fsync`와
`synchronous_commit`은 모두 **on**입니다.

첫 Journal 집중 실행은 반복 전체 schema migration 중 `DataFileImmediateSync` I/O 대기가
길어져 해당 테스트 worker만 중단했습니다(exit143). 테스트 통과로 집계하지 않습니다.
이후 새 gate fixture는 V15/V20만 검증하고 실제 posting fixture는 전체 migration을 유지합니다.
