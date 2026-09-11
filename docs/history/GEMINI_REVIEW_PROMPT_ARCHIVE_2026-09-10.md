# Gemini Review Prompt — 비활성 역사 보관

이 문서는 **비활성 역사 자료**다. 아래 모든 요청·명령·Issue·base·날짜·결과는 당시 원문이며 현재 지시, 읽기/실행 승인 또는 현재 검증 증거가 아니다.
현재 검수는 [루트 재사용 템플릿](../../GEMINI_REVIEW_PROMPT.md)에서 새 입력으로 시작한다. 이 archive를 프롬프트에 붙이거나 자동으로 따라 읽지 않는다.
과거 결과를 재실행·재인증하지 않았다. 원문의 상대 링크는 당시 저장소 루트 기준이며 보존을 위해 고치지 않는다.

- 보존 출처: `20bb8e48b1a4d34bc6a3ba0ee12202614c385590:GEMINI_REVIEW_PROMPT.md` (선행 안전 범위·경로 교정 반영 후).
- 파일명의 날짜는 Issue에서 지정한 역사 묶음 식별자이며 실제 snapshot 기준일은 2026-09-11이다.
- 원문 바이트 수: 89333; SHA256: `5ef3b720e8397b70f5f34fed02c571a375cb8d0106e99b1b970060c90ace35bf`.
- 아래 fenced text 내부만 원문이며 UTF-8 바이트/줄바꿈/순서를 그대로 보존한다. 바깥 설명은 보관 경계다.

<!-- BEGIN VERBATIM ORIGINAL -->
``````text
# Review Content Read Scope

이 계약을 아래 모든 리뷰 요청보다 먼저 적용한다. 과거 날짜·모듈 목록·검증 명령은 현재의 읽기/실행 승인이 아니다.
과거 기준·결과·날짜는 그대로 보존하며 역사 분리는 #675의 별도 범위다. 현재 Issue/부모가 승인한 작업만 수행한다.

## 1. 파일명부터 확인 (filename-first)

전제: 부모가 확인한 저장소·Issue 전용 branch/worktree에서 시작한다. 내용 조회 전에 아래 이름/상태 목록만 확인한다.

```powershell
git status --short --branch
git diff --name-only
git diff --cached --name-only
git ls-files --others --exclude-standard
```

staged와 unstaged는 따로 기록하고 untracked는 이름만 수집한다. 상태/파일명 목록은 콘텐츠 읽기 승인이 아니다.
PR/base 비교가 필요하면 부모가 확인한 base와 비교 방식으로 파일명만 먼저 받는다. 임의 base나 raw patch를 가져오지 않는다.

## 2. 현재의 명시적 범위로 허용 판단

현재 Issue/부모의 명시적 콘텐츠 review allowlist ∩ 실제 변경목록에서 비민감한 정확한 저장소 상대 파일 경로를 확정한다.
allowlist 또는 교집합이 없거나 비어 있으면 내용을 읽지 않고 부모에게 확인한다.
민감 가능 경로는 allowlist에 있어도 내용 조회 전에 제외한다. 예: `.env*`, `.claude/settings.local.json`, credential/key·token·password·인증서·개인 설정 파일.
범위 밖·절대경로·상위경로 이동·링크/경로 정체 불명확은 읽지 않고 부모에게 확인한다.
wildcard·폴더 전체·Git pathspec magic·자동 신규파일 전체 열람·무범위 raw diff fallback은 금지한다.
안전한 일반 파일인지와 경로 정체가 확인되지 않으면 내용을 열거나 링크를 따라가서 확인하지 않는다. 부모에게 안전한 확인 근거를 요청한다.
allowlist는 기존 민감정보 금지나 승인 범위를 넓히지 않는다. 필수 프로젝트 규칙 읽기도 적용 계약이 명시적으로 요구하는 비민감 정확 파일만 대상으로 한다.
모듈 문서·연관 파일이 더 필요하면 정확한 경로와 이유를 부모에게 요청하며, 과거 목록이나 폴더/glob을 새 승인으로 사용하지 않는다.

## 3. 승인된 한 파일만 조회 또는 보류

아래 `<exact-path>`는 명령을 실행하기 전에 2단계에서 확정한 비어 있지 않은 정확한 경로 하나로 바꾼다.
임의 입력을 shell 명령으로 조립하지 않는다. literal pathspec과 외부 diff/textconv 비활성화는 한 파일의 diff가 별도 변환기를 실행하거나 범위 패턴으로 해석되는 것을 피한다.
tracked unstaged, staged, untracked 중 해당 상태의 명령만 사용한다. 동일 파일이 양쪽 상태이면 두 diff를 구별해서 검토한다.

```powershell
git --literal-pathspecs diff --no-ext-diff --no-textconv -- '<exact-path>'
git --literal-pathspecs diff --cached --no-ext-diff --no-textconv -- '<exact-path>'
Get-Content -LiteralPath '<exact-path>'
```

untracked에도 동일한 승인/제외/정체 확인을 적용한 뒤 그 파일 하나만 읽는다. 범위나 상태가 바뀌면 파일명 조사부터 다시 판단한다.
빈 인자/누락 범위를 전체 diff로 대체하지 않는다. 보류한 파일은 본문 없이 이유와 필요한 부모 확인만 보고한다.

초보자 예시: 입력이 가짜 변경명 `docs/review.md`, 승인목록에 같은 정확 경로, 일반 파일이라는 확인이라면 → 비민감 교집합 허용 → 해당 상태의 한 파일만 조회한다.
승인목록이 비거나 파일이 밖에 있거나 민감/불명확하면 → 허용하지 않음 → 본문 조회 없이 부모 확인을 기다린다. 이름을 봤다는 이유만으로 파일을 열지 않는 것이 핵심이다.

## 로컬 회귀 검사와 한계

전제: 저장소 루트와 기존 Node.js의 내장 `node:test`. 설치·다운로드·네트워크·업무 DB는 필요 없다.

```powershell
node --test tools/ci/review-scope-contract.test.cjs
```

공백 검사도 변경 행 본문을 출력할 수 있으므로 위 filename-first/승인/제외 게이트를 동일하게 적용한다.
현재 비어 있지 않은 exact allowlist ∩ 해당 상태의 변경목록에서 확정한 한 파일만 검사한다. 범위 누락·빈 교집합·범위 밖·민감/불명확 경로는 보류한다.
아래 `<exact-path>`는 그 정확 경로 하나로 바꾼다. 첫째는 tracked unstaged, 둘째는 staged용이며 양쪽 상태라면 각각 검사한다.
untracked는 이 Git 공백 검사의 대상이 아니므로 자동 전체 검사로 대체하지 않고 보류한다. 승인된 개별 내용 조회는 위 3단계를 따른다.

```powershell
git --literal-pathspecs diff --check --no-ext-diff --no-textconv -- '<exact-path>'
git --literal-pathspecs diff --check --cached --no-ext-diff --no-textconv -- '<exact-path>'
```

초보자 설명: 공백 오류를 알려주는 검사도 잘못된 줄 자체를 보여줄 수 있다. 따라서 이름만 보는 검사가 아니며 승인 밖 파일의 줄을 출력해서는 안 된다.
Windows의 설치 경로를 직접 쓰려면 `& 'C:/Program Files/nodejs/node.exe' --test tools/ci/review-scope-contract.test.cjs`로 실행한다.
기대 결과는 모든 테스트 통과/실패 0/종료 코드 0, 공백 오류 없는 diff check의 종료 코드 0이다. 실제 실행 수·시간·결과는 실행자가 별도 기록한다.
검사는 승인된 이 문서 하나를 디스크 입력으로 삼고 필수 순서/범위 절과 위험한 diff·신규파일 fallback 회귀를 확인한다.
합성 경로·변경 상태·일반 파일 정체 입력 → 문서 계약 확인 → exact 조회 계획 또는 보류 이유를 만든다. 문서 규칙 삭제/위험 fallback 삽입은 인메모리 mutation으로 거부 여부를 확인한다.
합성 공백 진단은 승인 밖 가짜 변경 행이 전체 선택에서는 진단에 포함되지만 scoped 계획에서는 제외되는지를 메모리에서 비교한다. 실제 Git 실행 재현이나 전체 공백 규칙 구현은 아니다.
가짜 경로의 존재·본문·실제 secret을 조사하지 않으며 계획의 명령을 실행하지 않는다. 같은 입력으로 재실행해도 파일이나 외부 상태를 쓰지 않는다.
보수적인 가짜 경로 검사와 문서 정적 회귀는 AI의 실제 파일 접근을 강제하는 sandbox가 아니다. 모든 경로/비밀 탐지나 실제 노출 여부를 보장하지 않고 독립 의미 리뷰가 필요하다.
CI 미연결 상태이며 연결은 이 변경의 범위 밖이다. 기존 CI 성공을 신규 검사 실행 증거로 사용하지 않는다.

---

# 2026-07-29 Issue #20 최신 main 구조 충돌·Closing 누적 변경 리뷰

Branch: `agent/20-closing-consistency` for Issue #20. Review only; do not modify code. Report findings first, ordered by severity, with exact file/line evidence. Base is `f3d33eae085a527fa1ae1905df6d0fa8dbaeb6bc`, equal to the latest `origin/main`; review the cumulative issue-scoped commit and Draft PR.

## Required Review Focus

1. Issue #29가 도입한 `internal-audit:core/api/batch` 구조와 이동된 shared-kernel 감사 패키지를 보존하고, 삭제된 standalone `InternalAuditApplication`/context test를 PR이 되살리지 않는지 확인해 주세요.
2. `settings.gradle`이 새 internal-audit 세 프로젝트를 유지하면서 source-less phantom `:app`은 다시 추가하지 않는지 검토해 주세요.
3. `shared-kernel/build.gradle`이 upstream servlet compileOnly와 로컬 Jackson BOM 정렬을 모두 보존하고 conflict marker나 단독 버전 pin을 남기지 않는지 확인해 주세요.
4. `AccountSubjectRef`와 `MonolithMasterDataQueryAdapter` 자동 병합이 upstream 패키지 이동과 Closing이 필요한 normal balance/category 계약을 모두 보존하는지 확인해 주세요.
5. 새 internal-audit 세 모듈의 API/Batch production source와 모든 test가 `NO-SOURCE`인 상태를 이번 Closing PR이 완료로 오표기하지 않는지, Issue #29 후속 위험으로 명확히 분리했는지 검토해 주세요.
6. 두 번의 stash 적용과 손상된 외부 worktree 복구 과정에서 원본 Closing/Foundation 82개 경로가 누락·중복되지 않았는지 diff와 conflict log를 확인해 주세요.
7. 아래 Foundation/Closing 검수 항목을 최신 main 기준으로 다시 검토하고, 특히 금융 정합성·재실행·대량 처리 위험을 findings-first로 보고해 주세요.

## Verification Evidence

- Issue #20 branch base와 최신 `origin/main`은 모두 `f3d33ea`; unmerged path는 0건입니다.
- 두 이름 있는 stash가 유지되며 transfer stash의 82개 경로를 root issue branch에 재적용했습니다. 별도 나타난 `create-deep-issues.ps1`, `create-financial-issues.ps1`은 범위 밖이라 PR에서 제외합니다.
- 최종 영향 범위 176 tests(Shared Kernel 6, Contracts 4, Master Data 74, Journal Ledger Core 23, Closing Core 44, Closing API 1, Closing Batch 12, ECL API 3, Config Server 9)가 failures/errors/skips 0건으로 통과했습니다.
- `internal-audit:core/api/batch` compile/test task는 성공했지만 실제 test는 모두 `NO-SOURCE`이고 API/Batch production source도 `NO-SOURCE`입니다.
- Closing API, Closing Batch, Config Server `bootJar`가 통과했습니다. 로컬 JDK 17은 `-Porg.gradle.java.installations.paths=C:\Java\jdk17`로 명시했습니다.
- commit, push, Draft PR은 사용자 승인에 따라 이 검수 후 진행하며 merge와 Issue close는 수행하지 않습니다.

---

# 2026-07-28 Foundation 보류 검증·공통 의존성·phantom app 리뷰

Branch: `agent/20-closing-consistency`. Review only; do not modify code. Report findings first, ordered by severity, with exact file/line evidence. This Foundation residual scope is part of the cumulative Issue #20 Draft PR above.

## Required Review Focus

1. `shared-kernel/build.gradle`에서 Jackson databind가 Spring Boot 3.2.5 BOM을 따르며 core/annotations와 동일한 2.15.4 family로 해석되는지 확인해 주세요.
2. ECL consumer 실패 전파 테스트가 `JobLauncher.run`이 실제 선언한 checked exception을 사용하면서 duplicate-complete만 정상 멱등으로 처리하고 다른 실패는 Kafka listener에 전파한다는 의도를 유지하는지 검토해 주세요.
3. `settings.gradle`에서 제거한 `:app`에 추적 소스·의존 소비처·실행 역할이 정말 없고, 다른 Gradle 프로젝트나 IntelliJ 실행 설정이 이를 전제로 하지 않는지 확인해 주세요.
4. README, beginner guide, local development 문서가 통합 app이 아닌 서비스별 `@SpringBootApplication` 실행 구조를 일관되게 설명하는지 검토해 주세요.
5. 과거 Config/contracts/shared-kernel pending 상태가 실제 재검증 결과와 일치하는지, shared-kernel 인프라 전이 의존성과 allowance 타입 분리를 완료로 과장하지 않는지 확인해 주세요.

## Verification Evidence

- Config repository health target passed with `--rerun-tasks`.
- Shared Kernel 6 tests and ECL consumer 3 tests each passed with forced reruns.
- Final affected command passed: Shared Kernel 6 + Contracts 4 + Master Data 74 + Closing Core 44 + ECL API 3 + Config Server 9 = 140 tests in 40 suites, failures/errors/skips 0; Config Server bootJar passed.
- Gradle `projects` output no longer contains `:app` and retains the remaining modules.
- Live Docker/Compose/PostgreSQL were not executed. No commit, push, merge, or PR action was performed.

---

# 2026-07-28 Closing 상태·FX/ECL·재실행·런타임 경계 리뷰

Branch: `agent/20-closing-consistency`. Review only; do not modify code. Report findings first, ordered by severity, with exact file/line evidence. The original pre-sync base was `a06ebd6`; final PR base is latest main `f3d33ea` and the work is linked to Issue #20.

## Required Review Focus

1. Missing Fiscal Period가 Closing과 Journal validation 양쪽에서 fail-closed인지, `@Primary` 어댑터가 이를 우회하지 않는지 확인해 주세요.
2. Calendar/Task/Gate/Reopen 전이가 유효 상태만 허용하고, mandatory task/gate 정의 부재·JSON evidence 미구현·자기 승인·중복 PENDING 요청을 통과시키지 않는지 확인해 주세요.
3. Master Fiscal Period와 Closing Calendar 변경 순서/트랜잭션, 감사 로그 실패 전파, unlock 행 삭제 TODO가 실제 정합성 위험을 정확히 다루는지 검토해 주세요.
4. API valuation/provision 실행 이력이 전표 실패에도 RUNNING/FAILED를 보존하고 DRAFT를 PENDING_APPROVAL로 기록하는지, status 갱신 실패 orphan DRAFT와 실행키 unique/outbox TODO를 평가해 주세요.
5. FX signed balance가 debit positive/credit negative로 실제 POSTED `journal_entries/details`의 amount/base_amount를 집계하고 자산·부채/비정상 잔액의 차대 방향이 맞는지 확인해 주세요.
6. FX 파티셔너가 metadata를 grid-size 이하로 제한하고 Cursor/chunk/checkpoint를 사용하며 한 항목 실패 시 chunk 전체를 rollback하는지, 1억 건 전 journal scan read-model TODO가 충분한지 검토해 주세요.
7. Exchange Rate와 Account Subject 조회가 provider Repository/entity를 core에 노출하지 않고 기준일 최신 환율/SCD2 계정을 사용하며 null/비양수/비 ISO 값을 fail-closed 처리하는지 확인해 주세요.
8. ECL JDBC 선집계와 core 재검증이 하나의 run/model/legal entity/date만 허용하고, 목표액을 합산한 뒤 `gl_balances`의 debit-credit 부호를 positive credit로 바꿔 그룹당 한 번만 차감하는지 검토해 주세요.
9. ECL empty summary, null/negative target/stage/source, mixed mapping, 여러 법인이 성공/no-op으로 숨지 않는지 확인해 주세요.
10. Closing journal amount/baseAmount 차대일치, 20자 explicit slip, account+currency lineage, 동일 fingerprint retry reuse와 DRAFT/APPROVED/POSTED 자동전기 멱등성이 충돌을 숨기지 않는지 확인해 주세요.
11. Annual Closing이 POSTED·baseAmount·REVENUE/EXPENSES만 사용하고 안정 정렬/retained account slip identity를 가지는지, 현재 N+1 및 기존 lineage 제외 TODO를 지적해 주세요.
12. API/Batch가 `com.ho.account` 전체나 provider Application 설정을 스캔하지 않고 필요한 adapter/entity/repository만 로드하는지, 배치 전용 서비스와 `closing-service`/`closing-batch` 이름이 격리되는지 확인해 주세요.
13. Contracts record component 추가의 호환성, 로컬 모놀리스 compile dependency와 원격 MSA adapter 완료 조건을 검토해 주세요.
14. 문서의 필수 Job parameter, DRAFT/PENDING_APPROVAL, 원장 원천, ECL 단일 snapshot, 알려진 schema/runtime 위험이 코드와 일치하는지 확인해 주세요.

## Verification Evidence

- Final command passed: Contracts 4 + Master Data 74 + Journal Ledger Core 23 + Closing Core 44 + Closing API 1 + Closing Batch 12 = 158 tests in 52 suites, failures/errors/skips 0.
- Closing API and Batch bootJars passed. Both H2 ApplicationContext tests passed with their own application names and jobs disabled for Batch.
- Final static checks will be rerun after harness synchronization.
- Live PostgreSQL/Flyway and Docker/Compose were not executed.
- No commit, push, merge, or PR action was performed.

---

# 2026-07-27 Loan 헥사고날·DDD·회계 흐름 리뷰

Branch: `agent/asset-lease-split`. Review only; do not modify code. Report findings first, ordered by severity, with exact file/line evidence. The working tree contains the prior uncommitted Master Data follow-up plus the new Loan pass; review overlapping Master Data dated-port and root Compose changes cumulatively.

## Changed Scope

- `loan/core`: inbound/outbound ports, aggregate value references, lifecycle/concurrency rules, BigDecimal EIR, unified schedule, accrual pipeline, Journal/Master adapters, persistence, V33, tests.
- `loan/api`: DTO ownership and Bean Validation, `LoanUseCase` controller dependency, event-result response, exception mapping, tests.
- `loan/batch`: required-date paging/chunk orchestration with core pipeline.
- `master-data`: dated account/currency query methods required by the Loan adapter.
- `loan/Dockerfile`, module/root Compose, Loan README/docs and common worklogs/handoff.

## Required Review Focus

1. Do Loan and DeferredItemType own only stable IDs/codes, with Master Data entities confined to the local infrastructure adapter?
2. Are `PENDING_DISBURSEMENT`, one full disbursal, pessimistic lookup, optimistic version and unique index sufficient and ordered before state mutation/journal calls?
3. Does recalculation preserve original principal, validate date/maturity/current balance, calculate proposed EIR before mutation, and retain event/journal lineage?
4. Are DEFAULT/RECOVERY/OTHER handled without invalid `RecalculationReason.valueOf` conversion or accidental recalculation fields?
5. Is the BigDecimal EIR solver's decimal-rate unit, convergence policy, cash-flow signs, scale and fail-closed behavior correct for financial use?
6. Does `EIRAmortizationSchedule.generateMonthly` fully amortize principal/deferred amounts, handle final maturity, and avoid deleting old rows before successful calculation?
7. Does accrual lock the Loan, skip only SUCCESS, retry FAILED, use the unified EIR schedule, save failure details, and make the Step fail after processing the chunk?
8. Does `LoanJournalAdapter` REQUIRES_NEW isolation achieve failure-log durability without hiding the larger Loan/Journal distributed consistency gap?
9. Do Journal commands balance, use the accounting date as slip date, normalize ISO currency, and use correct debit/credit directions for disbursal, deferred recognition, repayment and accrual?
10. Are API DTOs truly outside core, validations/HTTP mappings complete, and state-event responses useful without leaking raw JPA entities?
11. Is V33 safe as a forward migration, and are duplicate preflight plus legacy schedule-table retirement conditions adequate?
12. Do Java 17 Docker, exact API bootJar COPY, build context, 8088 Compose port, and docs align? Recheck that no unrelated root Compose service changed.
13. Identify any remaining skeleton/dead paths, missing domain collaboration, transaction-order issue, N+1/load risk, or beginner documentation claim that does not match code.

## Verification Evidence

- Final affected command passed: Master Data 73, Governance 25, Closing Core 19, Journal Ledger Core 19, Loan Core 30, Loan API 3 = 169 tests; failures/errors/skips 0.
- Loan API and Batch bootJars passed; exact artifacts were generated.
- `git diff --check`, conflict markers, core boundary imports, Batch business logic, legacy schedule runtime consumers and current Markdown link checks passed.
- Docker CLI and local YAML parser are unavailable; image build, Compose execution and automated Compose parsing remain unverified.
- No commit/push/merge has been performed for this cumulative local pass.

---

# 2026-07-27 master-data 변경 승인·SCD2 조회·회계기간 경계 리뷰

Codex가 Master Data 변경 요청의 업무 버전, 승인 잠금, 실제 반영 전략, 기준일 조회, 환율 선택과 회계기간 상태 경계를 보강했습니다. 아래 누적 변경을 독립 검수해 주세요.

## 핵심 검수 항목

1. `MasterDataChangeVersionPolicy`의 CREATE=1, UPDATE=이력 수+1, DEACTIVATE=현재 이력 수가 네 SCD2 서비스의 실제 신규 행/종료 동작과 일치하는지 확인해 주세요.
2. 요청, 승인, 반영 직전 재검증과 `findByIdForUpdate`/`@Version` 조합이 같은 요청의 중복 상태 전이를 막는지 검토해 주세요.
3. `MasterDataChangeApplierRegistry`가 중복 담당을 시작 시 실패시키고 미지원 유형을 접수 단계에서 fail-closed 처리하는지 확인해 주세요.
4. `applyDueApprovedChanges`의 단일 트랜잭션 한계와 `REQUIRES_NEW`/`SKIP LOCKED`/실행 이력 TODO가 운영 대량 반영 위험을 정확히 설명하는지 확인해 주세요.
5. DTO Bean Validation, 409 예외 응답, 요청 본문 actor 및 raw payload/직접 쓰기 API TODO가 신뢰 경계를 충분히 드러내는지 검토해 주세요.
6. Governance 승인 `sourceReference` 재시도가 같은 요청을 반환하고 다른 payload 재사용을 409로 차단하는지, APPLIED 재시도가 no-op이며 미래 시행일은 승인 후 예약 상태로 남는지 확인해 주세요.
7. V2 체크섬 보존과 V3/V4/V5 forward migration이 H2에서는 안전한지, 신규 PostgreSQL에서 V2 `CLOB`보다 먼저 필요한 vendor baseline 리스크가 정확히 문서화됐는지 확인해 주세요.
8. 8082 포트, JDK 17 단일 bootJar, Actuator DB readiness, root/module Compose와 IntelliJ 설정이 일치하는지 확인해 주세요.
9. 기존 초보자 설명이 보존되면서 요청→승인→시행일 반영의 업무/데이터 흐름과 실제 코드가 일치하는지 확인해 주세요.
10. 네 SCD2 UPDATE 서비스가 신규 `validFrom/validTo`를 검증한 뒤에만 현재 행을 종료하고, 계정과목/부서는 상위 참조 확인과 신규 버전 조립도 먼저 끝내 잘못된 입력이 기존 상태를 변경하지 않는지 확인해 주세요.
11. 거래처 현재 조회는 `useYn`과 유효기간을, 과거 조회는 유효기간을 사용하며, `Optional` 단건 조회가 겹치는 기간을 임의 선택하지 않고 fail-closed 처리하는지 검토해 주세요.
12. PostgreSQL exclusion constraint와 legacy `useYn` 상태 분리 `@todo`의 완료 조건이 충분한지, 특히 과거 `BusinessPartnerRef.active`의 현재 한계가 과장 없이 문서화됐는지 확인해 주세요.
13. 계정과목/상품 활성 목록과 거래처명 검색이 전체 메모리 필터 대신 기준일·활성 조건이 있는 DB 쿼리를 사용하고, 빈 검색어를 거부하는지 확인해 주세요.
14. 환율이 `effectiveDate <= 기준일` 중 최신 한 건을 결정적으로 선택하고 ISO 통화 코드/양수 환율을 검증하는지, 활성 통화 기간 중복은 임의 선택하지 않고 fail-closed인지 검토해 주세요.
15. 회계기간 변경이 JPA Repository 직접 접근 대신 application port와 비관적 잠금을 사용하며, 감사 actor 필수·OPEN에서 영구 마감으로 건너뛰기 금지·영구 마감 최종 상태를 도메인이 보장하는지 확인해 주세요.
16. 사용되지 않는 변경요청 전체 조회, no-op 활성 setter, 가짜 분리 Currency를 만들던 호환 메서드, 미사용 유효기간 helper 제거가 실제 사용처 검색과 컴파일 결과에 비춰 안전한지 검토해 주세요.
17. PostgreSQL 전체 baseline, 목록/검색 pagination, `TaxProfile` 소유권, Loan Core/Closing Batch의 Master Data 내부 직접 의존 TODO가 현재 코드의 남은 운영·아키텍처 위험을 정확히 설명하는지 확인해 주세요.

## Codex 검증 결과

```powershell
.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test :master-data:api:bootJar :master-data:batch:bootJar :governance:test :governance:bootJar :closing:core:test :closing:batch:compileJava :journal-ledger:core:test --console=plain --max-workers=1 --no-daemon "-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"
```

- Master Data 73개 + Governance 25개 + Closing Core 19개 + Journal Ledger Core 19개, 총 136개 테스트, 실패/오류/skip 0건
- Closing Batch 컴파일과 Master Data/Governance `bootJar` 성공
- 최종 `git diff --check`, conflict marker/placeholder·아키텍처 검색, 변경 Markdown 상대 링크 검사 성공
- 실제 PostgreSQL 및 Docker/Compose 실행은 미검증

---

# 2026-07-14 auth 인증/역할 승인 멱등 경계 리뷰

Codex가 auth 모듈의 API/core 경계와 Governance 역할 승인 재시도 정합성을 보강했습니다. 아래 항목을 독립 검수해 주세요.

## 핵심 검수 항목

1. `AuthUseCase`/`AuthService`가 더 이상 `auth.api.dto`를 참조하지 않고 Controller가 `LoginCommand`와 `AuthenticationResult`를 올바르게 매핑하는지 확인해 주세요.
2. 로그인 응답과 JWT의 roles/roleAssignments가 같은 Clock 시점 스냅샷을 사용하며 역할 만료 경계가 `[validFrom, validTo)`인지 확인해 주세요.
3. token-version 검증이 roleVersion 외에 inactive/admin-lock/no-effective-role 상태를 fail-closed 처리하는지 확인해 주세요.
4. memory 역할 교체가 dataScope/validFrom/validTo를 JPA와 동일하게 보존하는지 확인해 주세요.
5. approvalTraceId와 fingerprint 멱등성, JPA 사용자 비관적 lock, V72 apply log가 외부 응답 유실 재시도에서 roleVersion 중복 증가를 막는지 검토해 주세요.
6. 같은 trace의 다른 payload가 memory/JPA 모두 fail-closed인지, 동시 요청에서 트랜잭션/unique key rollback이 안전한지 검토해 주세요.
7. 평문 비밀번호, 최초 로그인 실패 upsert, apply-log retention `@todo`가 실제 남은 운영 리스크를 정확히 가리키는지 확인해 주세요.
8. H2/IntelliJ/PostgreSQL 실행 문서가 Gradle/Flyway 설정과 일치하는지 확인해 주세요.

## Codex 검증 결과

```powershell
.\gradlew :auth:test :auth:bootJar --console=plain --max-workers=1
.\gradlew :auth:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=true --auth.persistence.mode=jpa --auth.login-security.store=jpa" --console=plain --max-workers=1
```

최종 실행은 모두 성공했고 auth 테스트 32개, Flyway V70~V72, Hibernate schema validate를 확인했습니다. 실제 PostgreSQL 동시성 검증은 미수행입니다.

---

# 2026-07-14 governance 승인 경계/실행 구조 리뷰

Codex가 누적 reporting/deposit/master-data 변경에 이어 governance 실행 및 승인 경계를 보강했습니다. 아래 항목을 독립적으로 검수해 주세요.

## 핵심 검수 항목

1. `GovernanceApplication`의 명시적 component/entity/repository scan이 필요한 Bean만 등록하고 legacy `com.ho.account.security`를 의도대로 제외하는지 확인해 주세요.
2. 권한 회수 API가 즉시 삭제하지 않고 `AUTHORIZATION / DELETE` 승인 요청과 HTTP 202 접수 정보를 반환하는지 확인해 주세요.
3. `SystemRoleApprovalApplyAdapter`가 역할 CREATE, 권한 CREATE/DELETE 외 조합을 반드시 fail-closed 처리하는지 확인해 주세요.
4. 승인 상태 전이와 실제 apply 호출의 트랜잭션 실패 시 정합성, 외부 Auth 호출의 outbox/inbox `@todo`가 적절한지 확인해 주세요.
5. 역할/권한 생성 API가 preview 도메인을 반환하는 기존 계약과 승인 접수 DTO 전환 `@todo`의 호환성 리스크를 검토해 주세요.
6. H2 단독 실행 설정, PostgreSQL 드라이버/문서, 초보자 설명이 실제 Gradle/Spring Boot 구조와 일치하는지 확인해 주세요.

## Codex 검증 결과

```powershell
.\gradlew :governance:compileJava --console=plain --max-workers=1
.\gradlew :governance:test :governance:bootJar --console=plain --max-workers=1
.\gradlew :governance:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

세 명령 모두 성공했습니다. 실제 PostgreSQL/Flyway 실행은 하지 않았으므로 해당 부분은 미검증 리스크로 남아 있습니다.

---

# 2026-07-14 master-data typed applier/대량 통계 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `master-data` 변경 요청 typed applier, SCD2 effectiveDate 정합성, core/batch 보고 모델 경계, DB COUNT 집계.
- 우선 확인:
  - `MasterDataValidityReportPipeline`이 batch 패키지/DTO를 참조하지 않고 core `MasterDataValidityReport`만 반환하는지.
  - `MasterDataValidityStatisticsPort`와 JPA 어댑터가 네 전체 테이블을 메모리에 올리지 않고 기준일 DB COUNT를 호출하는지.
  - `MasterDataBatchOrchestrator`가 core pipeline 호출과 DTO 매핑만 수행하고 if/for/math 업무 연산을 갖지 않는지.
  - ACCOUNT_SUBJECT/BUSINESS_PARTNER/DEPARTMENT/PRODUCT typed applier가 기존 use case를 재사용하는지.
  - CREATE/UPDATE payload key가 승인 targetKey와 다르면 실패하고, DEACTIVATE는 payload 없이 승인 effectiveDate를 종료일로 전달하는지.
  - Jackson `ObjectMapper`가 core application service에서 제거되고 `MasterDataChangePayloadDecoder` 출력 포트 뒤 infrastructure adapter에 있는지.
  - `MasterDataValidityPolicy.requireTerminationDate`가 기간 역전/연장을 막는지.
  - 미지원 CURRENCY/EXCHANGE_RATE/FISCAL_PERIOD가 APPLIED로 조용히 바뀌지 않고 fail-closed인지.
  - 현재 남은 `@todo`인 requestedVersion 충돌 검사와 미지원 typed applier의 우선순위가 적절한지.
- 재실행 권장:
  - `.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test --console=plain --max-workers=1`
  - `.\gradlew :master-data:core:test --console=plain --max-workers=1`
  - `rg -n "com\\.ho\\.account\\.masterdata\\.batch|MasterDataBatchReport" master-data/core/src/main/java --glob "*.java"`
  - `rg -n "\\b(if|for|while)\\s*\\(|BigDecimal|\\.stream\\(" master-data/batch/src/main/java --glob "*.java"`
- 알려진 리스크:
  - 독립 `master-data:batch` Job/Step은 실행되지만 durable 결과 이력과 관제 출력 포트는 아직 없다.
  - PostgreSQL Flyway DDL과 대량 실행 계획은 이번 H2 검증 범위 밖이다.

# 2026-07-09 deposit command/Batch 기준일 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `deposit:core` 계좌 개설 command 검증과 `deposit:batch` `asOfDate` JobParameter fail-fast 보강.
- 우선 확인:
  - `OpenAccountCommand`가 고객/상품/통화 코드 필수값, 통화 코드 정규화, 초기입금/금리 음수 방어를 수행하는지.
  - `DepositService`는 기존처럼 command -> domain 생성 -> 도메인 입금 메서드 -> persistence port -> 계정 매핑/master-data 검증 -> journal posting 순서를 유지하는지.
  - `DepositAccountIntegrityBatchConfig`가 `asOfDate` 누락 시 현재 날짜로 대체하지 않고 실패해 배치 재실행 기준일을 명확히 남기는지.
  - Batch 모듈은 Job/Step/Tasklet orchestration과 core use case 위임만 담당하고 업무 검증은 core `DepositBatchUseCase`가 수행하는지.
- 재실행 권장:
  - `.\gradlew :deposit:core:test :deposit:batch:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1`
  - `rg -n "@todo|TODO|FIXME" deposit --glob "*.java" --glob "!**/build/**"`
# 2026-07-08 reporting API response DTO 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `reporting:api`에서 core 도메인 객체를 HTTP 응답으로 직접 반환하지 않고, response DTO로 외부 JSON 계약을 고정한 변경.
- 우선 확인:
  - `ReportingController`가 `FinancialStatement`, `DisclosureNoteMart`, `RegulatoryReportSubmission`, `RegulatoryFiling`, `JournalDetailSummary`를 직접 반환하지 않는지.
  - `reporting:api/.../dto`의 response DTO가 기존 JSON 필드명과 필요한 감사/제출 정보를 유지하는지.
  - Controller는 요청 파라미터를 core command/query로 바꾸고, 보고서 합산/제출 검증/주석 분류/감독보고 매핑 업무 판단은 core에 남아 있는지.
  - `reporting:batch`는 Job/Step/Tasklet orchestration과 core 위임만 담당하는지.
- 재실행 권장:
  - `.\gradlew :reporting:api:test --console=plain --max-workers=1`
  - `rg -n "public (FinancialStatement|DisclosureNoteMart|RegulatoryReportSubmission|RegulatoryFiling)|ResponseEntity<DisclosureNoteMart|ResponseEntity<RegulatoryFiling|JournalDetailSummary>" reporting\api\src\main\java --glob "*.java"`
# 2026-07-08 reconciliation API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `reconciliation:core`에서 HTTP Controller/DTO/Web/Validation 의존을 제거하고, `reconciliation:api`가 요청 DTO/응답 DTO를 core command로 연결하도록 정리한 변경.
- 우선 확인:
  - `reconciliation:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, Controller/DTO를 더 이상 소유하지 않는지.
  - `ReconciliationService`와 `ReconciliationBatchService`가 command 기반 입력만 받는지.
  - `ReconciliationUnitCommand`, `ReconciliationRuleCommand`, `DifferenceReasonCodeCommand`, `RunReconciliationCommand`, `AssignDifferenceCommand`, `ResolveDifferenceCommand`가 API/Batch 공통 업무 입력값으로 충분한지.
  - `reconciliation:api` 요청 DTO가 Bean Validation 후 command로 변환하고, 응답 DTO가 domain/JPA 엔티티 직접 직렬화를 막는지.
  - `ReconciliationBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
  - 대사 규칙/단위/사유 코드 삭제 흐름이 물리 삭제가 아니라 `isActive=false` 논리 비활성화로 감사 이력을 보존하는지.
- 재실행 권장:
  - `.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :reconciliation:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :reconciliation:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|@RequestMapping|jakarta\.validation|com\.ho\.account\.reconciliation\.api" reconciliation\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`
# 2026-07-08 payable + receivable API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `payable:core`, `receivable:core`에서 HTTP Controller/DTO/Web/Validation 의존을 제거하고, 각 API 모듈이 요청 DTO/응답 DTO를 core command로 연결하도록 정리한 변경.
- 우선 확인:
  - `payable:core`, `receivable:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, Controller/DTO를 더 이상 소유하지 않는지.
  - `PurchaseUseCase`, `PaymentUseCase`, `SalesUseCase`, `CollectionUseCase`가 command 기반 입력만 받는지.
  - `SalesInvoiceCommand`, `CollectionCommand`, `ManualMatchingCommand`가 API/Batch 공통 업무 입력값으로 충분한지.
  - `receivable:api` 요청 DTO가 Bean Validation 후 command로 변환하고, 응답 DTO가 domain/JPA 엔티티 직접 직렬화를 막는지.
  - `SalesService`가 매출 전표 actor를 `createdBy` 기준으로 전달하고, `CollectionService`가 수납/매칭 금액 상태 전이를 기존 도메인 규칙으로 유지하는지.
  - `PayableBatchJobRegistryConfiguration`, `ReceivableBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :receivable:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :receivable:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|@RequestMapping|jakarta\.validation|com\.ho\.account\.receivable\.api" receivable\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`

# 2026-07-08 payable API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `payable:core`에서 HTTP Controller/DTO/Web/Validation 의존을 제거하고, `payable:api`가 요청 DTO/응답 DTO를 core command로 연결하도록 정리한 변경.
- 우선 확인:
  - `payable:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, Controller/DTO를 더 이상 소유하지 않는지.
  - `PurchaseUseCase`, `PaymentUseCase`가 command 기반 입력만 받는지.
  - `PurchaseInvoiceCommand`, `PaymentRunCommand`, `ExecutePaymentCommand`, `AdvancePaymentCommand`, `OffsetPayableCommand`가 API/Batch 공통 업무 입력값으로 충분한지.
  - `payable:api` 요청 DTO가 Bean Validation 후 command로 변환하고, 응답 DTO가 JPA/domain 엔티티 직접 직렬화를 막는지.
  - `PayablePaymentRunBatchConfig`가 Job/Step orchestration만 담당하고 업무 판단은 core `PaymentUseCase`에 위임하는지.
  - `PayableBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :payable:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :payable:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|@RequestMapping|jakarta\.validation|com\.ho\.account\.expenditure\.payable\.api" payable\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`

# 2026-07-08 expenditure-resolution API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: expenditure-resolution:core에서 HTTP Controller/DTO와 master-data 내부 참조를 제거하고, API DTO -> core command -> domain/service 흐름으로 정리한 변경.
- 우선 확인:
  - expenditure-resolution:core가 spring-boot-starter-web, spring-boot-starter-validation, Controller/DTO를 더 이상 소유하지 않는지.
  - ExpenditureResolutionUseCase, APPaymentUseCase가 command 기반 입력만 받는지.
  - ExpenditureResolutionService가 MasterDataQueryPort와 TaxInvoiceRef.purchase()/active()로 외부 참조를 검증하는지.
  - Budget과 Invoice가 master-data Entity 연관 대신 코드 값을 저장하는지.
  - API 통합 테스트가 expenditure-resolution:api 테스트 소스로 이동했고 기존 지출결의 -> tax -> AP 지급 시나리오가 유지되는지.
  - ExpenditureResolutionBatchJobRegistryConfiguration이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - $compile
  - $verify
  - $apiRun
  - $batchRun
  -
g -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|jakarta\.validation|com\.ho\.account\.expenditure\.resolution\.api|masterdata\.core|AccountSubjectPersistencePort|DepartmentPersistencePort|BusinessPartnerPersistencePort" expenditure-resolution\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"
# 2026-07-08 tax API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `tax:core`에서 HTTP Controller/DTO를 제거하고 `tax:api`가 API DTO를 core `TaxInvoiceCommand`로 변환하도록 정리한 변경.
- 우선 확인:
  - `tax:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, API DTO/Controller를 더 이상 소유하지 않는지.
  - `TaxInvoiceUseCase`와 `TaxInvoiceService`가 `TaxInvoiceCommand`를 기준으로 업무 흐름을 처리하는지.
  - `APInvoiceController`와 `TaxInvoiceRequestDto`가 `tax:api`에 있고, HTTP 요청 검증 후 command로 변환하는지.
  - `TaxInvoiceRef.purchase()`, `active()`, `usableForPurchaseSettlement()`가 외부 모듈이 취소/매입 정책을 명시적으로 판단하기에 충분한지.
  - `expenditure-resolution`의 지출결의/AP 지급 검증이 `TaxInvoiceRef` 계약 메서드를 사용하고 기존 SALES/CANCELLED 차단 테스트가 유지되는지.
  - `TaxBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - `.\gradlew :tax:core:test :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:test --console=plain --max-workers=1`
  - `.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :tax:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|jakarta\.validation|com\.ho\.account\.tax\.api" tax\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`
# 2026-07-07 asset-lease 감가상각 Batch 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `asset-lease` 대량 감가상각 batch에서 JPA 엔티티 mutation과 JDBC bulk update가 동시에 발생할 수 있는 경계 수정.
- 우선 확인:
  - `FixedAsset.calculateDepreciation()`이 상태를 변경하지 않고 `FixedAssetDepreciationResult`만 반환하는지.
  - 단건 API 경로의 `FixedAsset.depreciate()`는 기존처럼 도메인 상태 전이를 수행하는지.
  - `DepreciationPipeline`이 batch chunk를 결과 값 객체로 변환하고 엔티티를 변경하지 않는지.
  - `AssetJdbcAdapter.updateDepreciationBulk()`가 상각누계액, 장부가액, 상태, 최종상각일을 결과 값 기준으로 한 번만 반영하는지.
  - `AssetDepreciationBatchConfig`가 Job/Step/Reader/Writer orchestration만 담당하고 업무 계산은 core pipeline에 위임하는지.
- 재실행 권장:
  - `.\gradlew :asset-lease:core:test :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1`
  - `rg -n "org\.springframework\.batch|StepExecution|ItemProcessor|Tasklet|JobParameters|StepScope|JobScope" asset-lease\core\src\main\java asset-lease\core\src\test\java asset-lease\core\build.gradle --glob "*.java" --glob "*.gradle"`

# 2026-07-07 account-mart 담보 DQ/LGD 상세 연결 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `account-mart`의 아파트/부동산 담보 상세(`OdsApartCollDetail`)를 실제 DQ/LGD 선행 흐름에 연결.
- 우선 확인:
  - `CollateralDataQualityInspectionService`가 application service로서 `OdsApartCollDetailRepository` port 조회와 domain processor 호출 순서만 조정하는지.
  - `CollateralDataQualityProcessor`가 DB/JPA를 알지 않고 담보 마스터 평가액, 부동산/아파트 상세 존재 여부, KB 시세/지역/전용면적 필수값만 판단하는지.
  - `OdsApartCollDetailPersistenceAdapter`와 `JpaOdsApartCollDetailRepository`가 infrastructure adapter 경계에 머무르는지.
  - `CollateralDataQualityItemProcessor`가 Spring Batch adapter로서 기준일과 row 전달만 담당하는지.
  - `DataPopulator`가 부동산 담보 seed 생성 시 `ods_apart_coll_detail` 상세를 함께 만들어 demo DQ 흐름을 깨지 않는지.
  - `V5__add_ods_apart_coll_detail.sql`이 PostgreSQL/H2 호환 DDL로 충분한지.
- 재실행 권장:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1`
  - `rg -n "@todo|TODO|FIXME" account-mart --glob "*.java" --glob "!**/build/**"`

# Latest Review Target - 2026-07-03 Loan

Gemini는 이번 Codex 변경에서 `loan` 모듈을 우선 검토하세요.

1. Architecture / DDD
- `InterestAccrualService`가 journal-ledger `JournalUseCase`/`JournalEntry`를 직접 알지 않고 `LoanJournalPort` 명령만 생성하는지 확인하세요.
- `LoanAccrualLog`, `LoanEvent`, `EIRAmortizationSchedule`이 전표 엔티티 연관 대신 전표 ID/전표번호 값 참조만 보관하는지 확인하세요.
- `LoanEventDto`가 전표번호를 null로 버리지 않고 이벤트 이력의 값 참조를 그대로 반환하는지 확인하세요.

2. Batch / Local Execution
- `LoanInterestAccrualBatchConfig`가 Reader/Processor/Writer/Chunk 실행 책임만 갖고 이자 금액, 중복 처리, 전표 명령 생성은 core `InterestAccrualService`에 위임하는지 확인하세요.
- `LoanBatchJobRegistryConfiguration`이 Spring Batch 인프라 초기화 순서만 조정하고 업무 로직을 포함하지 않는지 확인하세요.
- Loan README/local-run 문서와 `.run` 설정의 `spring.application.name`, Redis repository 비활성화, H2/local 옵션이 실제 bootRun 명령과 일치하는지 확인하세요.

3. Codex 검증 결과
- `.\gradlew :loan:core:test --console=plain --max-workers=1 --rerun-tasks` 성공.
- `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1` 성공.
- `:loan:api:bootRun` local/H2 context smoke 성공, 로그 app name `loan-api` 확인, Redis repository 스캔 로그 미재현.
- `:loan:batch:bootRun` local/H2 context smoke 성공, 로그 app name `loan-batch` 확인, Redis repository 스캔 로그 및 Batch JobRegistry 경고 미재현.

4. 알려진 리스크
- PostgreSQL migration 적용과 대량 ACTIVE 대출 이자 발생 Job은 아직 seeded 데이터로 검증하지 않았습니다.
- `spring.data.redis.repositories.enabled=false`는 local smoke 옵션입니다. 향후 loan에 Redis repository가 실제로 추가되면 문서 옵션을 재검토해야 합니다.

아래 기존 리뷰 지침도 함께 따르세요.

---
# Latest Review Target - 2026-07-03 Closing

Gemini는 이번 Codex 변경에서 `closing` 모듈을 우선 검토하세요.

1. Architecture / DDD
- `closing:core/application/service/FxValuationService`가 FX 평가 금액, 정상잔액 방향, 차대변 판단을 core 업무 로직으로 적절히 소유하는지 확인하세요.
- `closing:core/application/service/EclProvisionService`가 확정 `allowance_summary`와 기존 GL 충당금 잔액 차이만 처리하고 Stage/PD/LGD/EAD를 재계산하지 않는지 확인하세요.
- `closing:core/application/port/out`의 `FxExchangeRateLookupPort`, `AllowanceBalanceLookupPort`, `ClosingJournalEntryPort`가 기술 독립 포트로 충분한지 확인하세요.

2. Batch Boundary
- `closing:batch`가 Spring Batch Job/Step/Reader/Tasklet, 외부 Repository/JournalUseCase adapter 책임만 갖고 업무 산식과 차대변 판단을 직접 구현하지 않는지 확인하세요.
- `FxValuationBatchConfig`의 partition/reader/writer 흐름이 core DTO 변환과 위임만 수행하는지 확인하세요.
- `EclProvisionBatchConfig` Tasklet이 기준일/batch ID를 core 서비스에 전달하는 실행 어댑터로 충분한지 확인하세요.

3. Codex 검증 결과
- `.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1` 성공.
- `.\gradlew :closing:api:compileJava --console=plain --max-workers=1` 성공.
- `.\gradlew :closing:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false" --console=plain --max-workers=1` 성공.
- closing API/BATCH local logback XML 파싱 성공.
- closing core Spring Batch 타입 직접 참조 검색 결과 없음.
- closing Java/문서 TODO 및 깨진문자 검색 결과 없음.

4. 알려진 리스크
- PostgreSQL 대량 GL 잔액/allowance_summary 기준 성능, skip/retry, 중복 전표 감지 운영 검증은 아직 수행하지 않았습니다.
- FX valuation writer는 현재 계정별 실패를 로깅하고 계속 진행합니다. 운영 정책상 fail-fast 또는 skip-limit/reporting이 더 적절한지 검토가 필요합니다.

아래 기존 리뷰 지침도 함께 따르세요.

---
# Latest Review Target - 2026-07-03 ECL + Journal Ledger

Gemini는 이번 Codex 변경에서 아래 범위를 우선 검토하세요.

1. ECL
- `ecl-core/application/pipeline`의 `StagingCalculationPipeline`, `EadCrmCalculationPipeline`, `ForwardLookingEclCalculationPipeline`이 IFRS 9 Stage/PD, EAD/LGD, 미래전망 ECL 산출 순서를 올바르게 core에 두는지 확인하세요.
- `ecl-batch` processor가 Spring Batch adapter 책임만 갖고 업무 산식/순서를 직접 구현하지 않는지 확인하세요.
- `AllowanceCalculationService`와 Batch가 같은 core pipeline을 공유하면서 API/단건 산출과 Batch 산출의 업무 순서가 어긋나지 않는지 확인하세요.

2. Journal Ledger
- `dailyBalanceReaggregationJob`이 Job/Step wiring, Tasklet adapter, core `LedgerService` 호출로 책임이 나뉘어 있는지 확인하세요.
- `BatchDateRangeParameterUtils`의 `startDate/endDate`, `fromDate/toDate`, `baseDate`, `targetDate` 해석과 기간 역전 검증이 재실행/운영 파라미터 관점에서 충분한지 확인하세요.
- `journal-ledger/batch/application.yml`의 H2 local datasource/JPA/Batch 계층과 `spring.batch.job.enabled=false` 기본값이 로컬/운영 실행에 문제를 만들지 않는지 확인하세요.
- `journal-ledger` docs와 `.run/Journal Ledger Batch Reaggregation.run.xml`의 명령이 실제 Gradle 실행 경로와 일치하는지 확인하세요.

3. Codex 검증 결과
- `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1` 성공.
- `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1` 성공.
- `.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1` 성공, Job status `COMPLETED`.
- ECL/Journal Ledger Java TODO 및 깨진문자 검색 결과 없음.
- `git diff --check` 오류 없음(CRLF 변환 경고만 출력).

4. 알려진 리스크
- PostgreSQL 대량 seed 기준 성능/락/재실행 검증은 아직 수행하지 않았습니다.
- Journal Ledger Batch bootRun에서 Spring Cloud/Batch BeanPostProcessor WARN은 남아 있으나 Job 실패는 유발하지 않았습니다.

아래 기존 리뷰 지침도 함께 따르세요.

---
# 2026-07-03 ecl core pipeline boundary review

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `ecl`의 batch processor 업무 산출 순서를 `ecl-core/application/pipeline`으로 이동하고, `ecl-batch` processor를 Spring Batch adapter로 축소한 변경.
- 우선 확인:
  - `ecl-batch`의 `StagingProcessor`, `EadCrmProcessor`, `EclProcessor`가 산식/상태 판단/BigDecimal 계산을 직접 수행하지 않는지.
  - `StagingCalculationPipeline`, `EadCrmCalculationPipeline`, `ForwardLookingEclCalculationPipeline`이 DDD/헥사고날 기준으로 core 업무 순서를 적절히 소유하는지.
  - `AllowanceCalculationService`와 batch가 같은 core pipeline을 재사용해 API/Batch 산출 순서가 불일치하지 않는지.
  - ecl README/docs와 batch config 주석이 실제 코드 흐름과 맞는지.
- 재실행 권장:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1`
  - `rg -n "AllowanceParameterService|PdCalculationService|LgdCalculationService|CcfCalculationService|EadCrmCalculationService|LifetimePdService|ForwardLookingEclService|BigDecimal|resolveMaturityYears|AllowanceEclResult\.builder" ecl\ecl-batch\src\main\java\com\ho\account\ecl\batch\processor`

기존 장기 리뷰 프롬프트는 아래 내용을 참고하세요.
# 2026-07-02 account-mart core/batch boundary review

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `account-mart`의 `mart-core` Spring Batch 의존 제거, `mart-batch` processor adapter 추가, 미사용 JPA-leaking application port 삭제, ODS-GL 대사 합계 조회 보정, account-mart 문서 보강.
- 우선 확인:
  - `mart-core`가 Spring Batch 타입을 import/구현하지 않는지.
  - `mart-batch`가 Job/Step/Reader/Writer/Chunk/adapter 책임만 갖고 업무 판단을 core에 위임하는지.
  - `OdsGeneralLedgerPersistenceAdapter.getBalanceSummaryByBaseDate()`가 기준일+계정+통화 합계 기준으로 대사에 충분한지.
  - `OdsApartCollDetail`에 남긴 `@todo`가 실제 후속 LGD/DQ 연결 리스크를 정확히 설명하는지.
  - account-mart README/docs 설명이 실제 Gradle 모듈과 실행 흐름에 맞는지.
- 재실행 권장:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-batch:test --console=plain --max-workers=1`
  - `rg -n "org\.springframework\.batch|StepExecution|ItemProcessor|StepExecutionListener|ExitStatus|StepScope" account-mart\mart-core\src\main\java account-mart\mart-core\src\test\java account-mart\mart-core\build.gradle`

기존 장기 리뷰 프롬프트는 아래 내용을 참고하세요.
# Gemini Review Prompt

이 파일은 Codex가 구현을 맡고 Gemini가 독립 리뷰를 맡는 표준 핸드오프 프롬프트다.
Gemini에게 리뷰를 요청할 때 아래 프롬프트를 그대로 전달한다.

## Role Split

- Codex: 구현, 수정, 테스트 보강, 문서/워크로그 갱신, 최종 커밋 담당.
- Gemini: 독립 코드 리뷰 담당.
- Gemini는 사용자가 명시적으로 구현을 요청하지 않는 한 코드를 수정하지 않는다.
- Gemini 리뷰 결과는 Codex가 다시 검토한 뒤 실제 수정 여부를 판단한다.

## Gemini에게 전달할 프롬프트

```text
당신은 account 저장소의 독립 코드 리뷰어입니다.
이번 리뷰에서는 코드를 직접 수정하지 말고, Codex가 작업한 변경분을 Findings 중심으로 검수하세요.
아래 3–5번의 과거 범위·검증 명령은 현재의 읽기/실행 승인이 아니다. 날짜·모듈·결과는 참고 이력으로 보존하고 현재 Issue/부모의 승인만 따른다.
이 복붙 프롬프트만 전달받아도 다음 filename-first 게이트를 생략하지 않는다.

0. 내용 조회 전에 부모가 확인한 저장소·Issue branch/worktree에서 이름/상태만 확인한다.
- git status --short --branch
- git diff --name-only
- git diff --cached --name-only
- git ls-files --others --exclude-standard
staged/unstaged/untracked를 구별한다. 상태/파일명 목록은 콘텐츠 읽기 승인이 아니다.
PR/base 비교는 부모가 확인한 base/비교 방식의 파일명부터 받으며 임의 raw patch를 읽지 않는다.

1. 현재 Issue/부모의 명시적 콘텐츠 review allowlist ∩ 실제 변경목록에서 비민감 exact 상대 파일 경로를 확정한다.
allowlist 또는 교집합이 없거나 비어 있으면 내용을 읽지 않고 부모에게 확인한다.
민감 가능 경로는 allowlist에 있어도 내용 조회 전에 제외한다. 예: .env*, .claude/settings.local.json, credential/key·token·password·인증서·개인 설정 파일.
범위 밖·절대경로·상위경로 이동·링크/경로 정체 불명확은 읽지 않고 부모에게 확인한다.
wildcard·폴더 전체·Git pathspec magic·자동 신규파일 전체 열람·무범위 raw diff fallback은 금지한다.
링크나 불명확한 파일을 열어서 정체를 확인하지 않는다. 부모에게 안전한 일반 파일 확인 근거를 요청한다.
allowlist로 기존 보안 금지나 승인 범위를 넓힐 수 없다. 필수 규칙 읽기는 적용 계약이 명시적으로 요구하는 비민감 정확 파일만 허용한다.

2. 확정된 비어 있지 않은 <exact-path> 하나로 아래 인자를 바꾼 뒤 해당 상태의 조회만 수행한다. 임의 입력을 shell 명령으로 조립하지 않는다.
- git --literal-pathspecs diff --no-ext-diff --no-textconv -- '<exact-path>'
- git --literal-pathspecs diff --cached --no-ext-diff --no-textconv -- '<exact-path>'
- Get-Content -LiteralPath '<exact-path>'
첫째는 tracked unstaged, 둘째는 staged, 셋째는 같은 승인 절차를 통과한 untracked 한 파일용이다. 양쪽 상태인 파일은 두 diff를 구별한다.
공백 검사도 변경 행 본문을 출력할 수 있으므로 위 filename-first/승인/제외 게이트를 동일하게 적용한다.
현재 비어 있지 않은 exact allowlist ∩ 해당 상태의 변경목록에서 확정한 한 파일만 검사한다. 범위 누락·빈 교집합·범위 밖·민감/불명확 경로는 보류한다.
- git --literal-pathspecs diff --check --no-ext-diff --no-textconv -- '<exact-path>'
- git --literal-pathspecs diff --check --cached --no-ext-diff --no-textconv -- '<exact-path>'
공백 검사의 첫째는 tracked unstaged, 둘째는 staged용이다. untracked를 전체 diff 공백 검사로 대체하지 않고 보류한다.
상태나 범위가 바뀌면 0번부터 다시 판단한다. 빈 인자/누락 범위를 전체 diff로 대체하지 않고 보류 이유와 부모 확인만 보고한다.

다음은 과거 요청의 문서/모듈 참고 목록이다. 현재 콘텐츠 승인 목록이 아니며 필요한 문서는 정확 경로와 이유를 부모에게 요청한다.
이번 추가 리뷰 범위에는 2026-06-19 Codex의 업무 모듈 API/BATCH 실행 구조 재점검과 누락 Batch Job 보강이 포함됩니다. 특히 `deposit`, `payable`, `receivable`, `reconciliation`, `tax`, `expenditure-resolution`에 추가한 Spring Batch `Job/Step`, core batch use case, local-run 문서, `deposit:batch` Boot Batch auto-run 설정을 우선 검수하세요.

과거 요청의 문서 목록(현재의 필수 규칙/명시적 읽기 승인을 통과한 정확 파일만 조회):
- GEMINI.md
- docs/GEMINI.md
- docs/GEMINI_SKILL.md
- AGENTS.md
- docs/WORKLOG.md 최신 항목
- CODEX_WORKLOG.md 최신 항목
- MODULE_REVIEW_2026-05-06.md 최신 항목
- docs/todo.md
- 변경 대상 모듈의 README.md 및 docs/*.md

3. 리뷰 대상 변경 범위는 Codex의 2026-06-19 "업무 모듈 API/BATCH 실행 구조 재점검 및 누락 Spring Batch Job 보강"입니다. 이전 누적 변경도 워킹트리에 섞여 있을 수 있으나, 우선순위는 아래 신규 파일/수정 파일입니다.
- `deposit:core/batch`: `DepositBatchUseCase`, `DepositBatchService`, `depositAccountIntegrityJob`, batch `application.yml`, `DepositBatchApplication`
- `payable:batch`: `payablePaymentRunJob`
- `receivable:core/batch`: `ReceivableBatchUseCase`, `ReceivableBatchService`, `CollectionPersistencePort` 후보 조회, `receivableAutoMatchingJob`
- `reconciliation:core/batch`: `ReconciliationBatchUseCase`, `ReconciliationBatchService`, `reconciliationDailyJob`
- `tax:core/batch`: `TaxInvoiceBatchUseCase`, `TaxInvoiceBatchService`, `taxInvoiceValidationJob`
- `expenditure-resolution:core/batch`: `ExpenditureResolutionBatchUseCase`, `ExpenditureResolutionBatchService`, `expenditureResolutionApprovalJob`
- 각 모듈 README 및 `docs/local-run.md`의 Job 실행 명령

이전 누적 검토 참고 범위는 Codex의 2026-06-09 "잔여 TODO 최종 경계 통합"부터 2026-06-19 "전체 API/BATCH bootRun smoke 및 build 검증 반영"까지입니다.
- Auth 로그인 성공/실패 감사, 설정 기반 임시 잠금, `LoginAttemptPort`/기본 어댑터
- Payable `PaymentExecutionPort`, 지급 멱등 키, 실패/재시도 상태, 정확한 `payableId`, master-data 내부 의존 제거
- Receivable 참조번호 우선/만기일 허용/중복 실패 폐쇄 자동 매칭과 `CollectionAllocation` 잔액 이력
- Journal/Unsettled HTTP DTO, 필수 `X-User-ID`, 미결 인바운드 포트, 반제 참조번호 멱등/감사 필드
- Loan 소유 출력 포트와 외부 전표 값 참조
- Reconciliation 표준 `Unit -> Run -> Difference` Aggregate 및 단계 결과 연결
- Allowance input JPA 쓰기 소유권의 account-mart 이동과 ECL 자체 읽기 모델
- Closing 조정 전표 기본 DRAFT 통제, Tax 논리 취소/actor/계약 포트, Asset actor 전달
- 2026-06-09 구현 종료 시점의 Java 코드 `@todo` 0건 기록과, 이후 문서 통합에서 의도적으로 추가한 운영 개선용 `@todo`의 실제 리스크 일치 여부
- ECL `EadCalculationResult`, 모델 비율 fail-closed 검증, 이름 있는 기본 CCF 정책
- ECL 모델 파라미터 기술 독립 포트와 JPA 어댑터 빈 구성
- ECL 등급·상품·LGD·담보배분·거시시나리오·전이행렬 기술 독립 포트와 JPA/캐시 어댑터
- Journal `UnsettledItemPersistencePort`, 거래처별 DB 필터, `JournalRuleQueryPort`, `JournalSide` 규칙 타입
- Journal `JournalPersistencePort`/`LedgerEntryPersistencePort`/`LedgerBalancePersistencePort` 전기·잔액 경계와 DB 조건 필터
- Journal `journal-ledger.ledger.persistence-mode=jdbc-bulk` 설정 기반 JDBC batch insert/upsert 어댑터, `YearMonthAttributeConverter`, 잔액 재집계 bulk 저장 경로
- ECL/Journal docs 인덱스, 입문·프로세스·스키마 문서의 실제 코드 일치 여부
- 공통 docs `beginner_guide.md`, `local-development.md`, `module-documentation-sequence.md`가 실제 settings.gradle/실행 클래스/Gradle 명령과 맞는지
- account-mart 문서 인덱스, batch/API/core README, Batch Job 목록, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- ECL README/docs/API/batch/core 문서, account-mart 선행 데이터 조건, demo profile 설명, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Journal Ledger README/docs/layer-guide, legacy README archive 이동, JDBC bulk 실행 설정, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Closing README/docs, legacy README archive 이동, FX/ECL Batch 실행 조건, IntelliJ `.run` Gradle 설정, 깨진 한글 주석 복구가 실제 코드와 맞는지
- Closing에 새로 남긴 `@todo` 2건이 실제 운영 리스크(대량 FX Reader, 부채 계정 차대변 판정)를 정확히 가리키는지
- Loan README/docs, legacy README archive 이동, EIR/일일 이자 Batch 실행 조건, IntelliJ `.run` Gradle 설정, 깨진 한글 주석 복구가 실제 코드와 맞는지
- Loan에 새로 남긴 `@todo` 1건이 실제 운영 리스크(이연 수수료/비용 부호 정책)를 정확히 가리키는지
- Payable README/docs, legacy README archive 이동, 매입채무/지급 런/지급 실행/선급금/상계 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Receivable README/docs, legacy README archive 이동, 매출채권/수납/자동·수동 매칭/부분 매칭 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Payable/Receivable이 현재 standalone Boot 앱이 아니라 `java-library` 모듈이라는 문서 설명이 `build.gradle` 및 소스 구조와 맞는지
- Payable에 새로 남긴 `@todo` 4건이 실제 고도화 리스크(인바운드 DTO/Bean Validation 분리)를 정확히 가리키는지
- Asset-Lease README/docs, legacy README archive 이동, 고정자산/감가상각 Batch/IFRS 16 리스/이벤트/지급결의 포트 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Asset-Lease가 `core`/`api`/`batch`로 분리되어 있고 API/BATCH 실행 문서와 Config/Eureka/Batch 비활성화 실행 인자가 실제 `build.gradle`, `AssetLeaseApiApplication`, `AssetLeaseBatchApplication`, `application.yml`과 맞는지
- Asset-Lease에 새로 남긴 `@todo` 4건이 실제 운영 리스크(Batch targetDate/파이프라인 분리, 리스 actor 감사, 리스 계정 매핑 포트 분리)를 정확히 가리키는지
- Tax README/docs, legacy README archive 이동, AP 세금계산서/금액 검증/논리 취소/외부 조회 포트 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Tax가 현재 standalone Boot 앱이 아니라 `java-library` 모듈이라는 문서 설명이 `build.gradle` 및 소스 구조와 맞는지
- Tax에 새로 남긴 `@todo` 1건이 실제 운영 리스크(취소 증빙 외부 조회 정책)를 정확히 가리키는지
- Reconciliation README/docs, legacy README archive 이동, 대사 단위/규칙/실행/차이/사유 코드/조정 전표 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Reconciliation이 현재 standalone Boot 앱이 아니라 `java-library` 모듈이라는 문서 설명이 `build.gradle` 및 소스 구조와 맞는지
- Reconciliation에 새로 남긴 `@todo` 2건이 실제 운영 리스크(규칙 물리 삭제, 조정 전표 멱등 키)를 정확히 가리키는지
- Reporting README/docs, 재무제표 생성, 제출본 버전, 주석 마트, 감독보고 제출, batch adapter 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Reporting의 `core`는 `java-library`, `api`/`batch`는 standalone Boot 앱이라는 문서 설명이 `build.gradle`, Application 클래스, `.run` 설정과 맞는지
- Deposit의 `core`는 library, `api`/`batch`는 standalone Boot 앱이라는 문서 설명과 로컬 어댑터 설정이 실제 코드와 맞는지
- `asset-lease`, `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`에 잘못된 미추적 API/BATCH Application 후보가 남아 있지 않은지
- local profile에서 logback `LOGSTASH` appender가 생성/참조되지 않고, 일반 profile에서는 기존 logstash 전송 구조가 유지되는지
- Eureka/Gateway/OpenFeign 실행 모듈에 `com.github.ben-manes.caffeine:caffeine` 의존성이 누락 없이 추가되어 Spring Cloud LoadBalancer 기본 캐시 경고를 제거하는지
- 로컬 H2/메모리/로컬 어댑터 실행 명령에서 `spring.cloud.discovery.enabled=false`와 `spring.cloud.loadbalancer.enabled=false`가 함께 적용되어 불필요한 LoadBalancer 자동 구성을 피하는지
- Deposit/Reporting Batch가 `JobRegistrySmartInitializingSingleton`으로 Batch Job 등록 시점을 늦추면서 `jobRegistryBeanPostProcessor` 조기 초기화 경고를 제거하고, Batch 앱에 업무 if/for/math 로직을 추가하지 않았는지
- Reporting에 새로 남긴 `@todo` 2건이 실제 운영 리스크(Spring Batch Job/Step 전환, 랜덤 반려 설정화)를 정확히 가리키는지
- Contracts/Shared-Kernel README/docs/local-run이 실제 `java-library` build.gradle 및 컴파일 검증 흐름과 맞는지
- Master-Data README/docs, SCD2 기준정보, 변경 요청 승인/반영, 포트/어댑터 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Governance README/docs, 감사 로그, 승인, SOD, Auth 역할 반영, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Auth README/docs, 로그인, JWT, roleVersion, 내부 역할 반영 API, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Config-Server/Config-Repo README/docs, native `config-repo` 설정 조회, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Discovery README/docs, legacy corrupt concept archive 이동, 새 Eureka concept/local-run이 실제 코드와 맞는지
- Gateway README/docs, `config-repo/gateway-service.yml` 라우트, JWT filter, fallback, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Foundation/Infra에 새로 남긴 `@todo` 5건이 실제 운영 리스크(Auth 잠금 공유, Gateway roleVersion 검증, Master-Data 실제 반영/chunk, Governance fail-closed)를 정확히 가리키는지

4. 리뷰 기준은 아래 순서로 우선순위를 둡니다.
- 컴파일/테스트/bootJar/smoke 기동 실패를 유발하는 결함
- IFRS 9 Stage, PD, LGD, EAD, ECL, summary 금액 오류
- 회계 금액/차대/상태 전이/마감/승인/라인리지 오류
- 헥사고날 아키텍처 위반: adapter, application, domain, infrastructure 경계 침범
- DDD 위반: 단순 setter 중심 변경, 도메인 규칙의 서비스 산재, 의미 없는 단순 위임
- 배치 규칙 위반: batch 모듈 내 비즈니스 if/for/math 구현
- API 계약 회귀: DTO, Controller, Service, Test 불일치
- Flyway migration 버전 충돌 또는 다중 모듈 런타임 classpath 확인사항
- 테스트 누락 또는 기존 테스트 계약 미갱신
- 문서/WORKLOG/todo 완료 표기와 실제 코드 상태 불일치

5. 가능하면 아래 검증을 재실행하세요.
- .\gradlew :deposit:core:test :payable:core:test :receivable:core:test :reconciliation:core:test :tax:core:test :expenditure-resolution:core:test :deposit:batch:compileJava :payable:batch:compileJava :receivable:batch:compileJava :reconciliation:batch:compileJava :tax:batch:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1
- .\gradlew build --console=plain --max-workers=1
- .\gradlew :deposit:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=depositAccountIntegrityJob asOfDate=2026-06-19" --console=plain --max-workers=1
- .\gradlew :payable:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=payablePaymentRunJob runDate=2026-06-19 createdBy=SMOKE description=Smoke" --console=plain --max-workers=1
- .\gradlew :receivable:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=receivableAutoMatchingJob" --console=plain --max-workers=1
- .\gradlew :reconciliation:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=reconciliationDailyJob reconciliationDate=2026-06-19 runBy=SMOKE deepMode=false" --console=plain --max-workers=1
- .\gradlew :tax:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=taxInvoiceValidationJob startDate=2026-06-19 endDate=2026-06-19" --console=plain --max-workers=1
- .\gradlew :expenditure-resolution:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=expenditureResolutionApprovalJob startDate=2026-06-19 endDate=2026-06-19 paymentDueDate=2026-06-19" --console=plain --max-workers=1
- rg -n "\bJob\s+\w+\s*\(" --glob "*.java" --glob "!**/build/**" account-mart\mart-batch asset-lease\batch closing\batch deposit\batch ecl\ecl-batch expenditure-resolution\batch journal-ledger\batch loan\batch payable\batch receivable\batch reconciliation\batch reporting\batch tax\batch
- .\gradlew :auth:test :payable:test :receivable:test :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :tax:test --console=plain
- .\gradlew :closing:batch:test :journal-ledger:core:test :journal-ledger:api:compileJava :reconciliation:test --console=plain
- .\gradlew :reconciliation:test :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain
- .\gradlew :contracts:compileJava :shared-kernel:compileJava :master-data:core:test :master-data:api:test :master-data:batch:test :governance:test :auth:test :gateway:test :discovery:test :config-server:assemble --console=plain
- .\gradlew :loan:core:test :loan:api:compileJava :shared-kernel:compileJava :account-mart:mart-core:test :account-mart:mart-batch:test :ecl:ecl-core:test :ecl:ecl-api:compileJava --console=plain
- .\gradlew :ecl:ecl-core:test :ecl:ecl-api:compileJava :ecl:ecl-batch:test :journal-ledger:core:test :journal-ledger:api:test --console=plain
- .\gradlew :journal-ledger:core:test :journal-ledger:api:test :loan:core:test --console=plain
- rg -ni "@todo" --glob "*.java" .

6. 출력 형식은 반드시 아래 순서를 따르세요.

Findings:
- Severity: Critical/High/Medium/Low 중 하나
- 파일/라인: 실제 경로와 라인 번호
- 문제: 무엇이 잘못됐는지
- 영향: 운영/회계/빌드/테스트에 어떤 문제가 생기는지
- 제안: Codex가 수행할 수정 방향

Verification:
- 실행한 명령
- 성공/실패 결과
- 실패 시 핵심 에러 요약

Open Questions:
- 정책 결정이 필요한 항목만 작성

Notes:
- 이미 WORKLOG 또는 MODULE_REVIEW에 기록된 알려진 이슈는 새 증거가 있을 때만 중복 보고하세요.
- 리뷰는 한국어로 작성하세요.
- 코드 수정은 하지 마세요.
```

## Current Handoff Context

- 현재 로컬 워킹트리에는 Codex 변경 외에 사용자/Gemini가 남긴 문서 이동/삭제 및 기타 미커밋 변경이 섞여 있을 수 있다.
- 이번 핸드오프의 기준은 2026-06-09 잔여 TODO 통합부터 2026-06-15 local profile logstash 비활성화까지의 누적 변경이다.
- 루트 `WORKLOG.md`는 현재 작업트리에 없고, 추적된 최신 작업 이력은 `docs/WORKLOG.md`에 있다.
- Codex 작업 로그는 루트 `CODEX_WORKLOG.md`에 최신 항목을 추가했다.
- 최종 검증 결과:
  - Auth/Payable/Receivable/Asset/Tax 집중 테스트 성공.
  - Closing/Journal/Reconciliation/ECL 집중 테스트 및 API 컴파일 성공.
  - Loan/Account-Mart/Allowance 소유권 경계 테스트 및 API 컴파일 성공.
  - 2026-06-09 구현 종료 시점의 Java 소스 `@todo` 검색 결과는 0건이었다.
  - ECL core/API/batch와 Journal core/API 통합 검증 성공.
  - Journal T53 구현 후 core 테스트와 H2 기반 JDBC batch insert/upsert 집중 테스트 성공.
  - 문서 통합 1차 후 account-mart core/test, mart-api compileJava, mart-batch test 성공.
  - 문서 통합 2차 후 ecl core test, ecl-api compileJava, ecl-batch test 성공.
  - 문서 통합 3차 후 journal-ledger core/api test 성공.
  - 문서 통합 4차 후 closing core test, closing-api compileJava, closing-batch test 성공.
  - 문서 통합 4차에서 Closing 주석 복구와 함께 운영 개선용 Java `@todo` 2건을 의도적으로 추가했다.
  - 문서 통합 5차 후 loan core test, loan-api compileJava, loan-batch compileJava 성공.
  - 문서 통합 5차에서 Loan 주석 복구와 함께 운영 개선용 Java `@todo` 1건을 의도적으로 추가했다.
  - 문서 통합 6차 후 payable/receivable test 성공.
  - 문서 통합 6차에서 Payable 주석 복구와 함께 인바운드 DTO/Bean Validation 분리용 Java `@todo` 4건을 의도적으로 추가했다.
  - 문서 통합 7차 후 asset-lease/tax test 성공.
  - 문서 통합 7차에서 Asset-Lease 운영 개선용 Java `@todo` 4건과 Tax 외부 조회 정책 `@todo` 1건을 의도적으로 추가했다.
  - 문서 통합 8차 후 reconciliation, reporting core/api/batch test 성공.
  - 문서 통합 8차에서 Reconciliation 운영 개선용 Java `@todo` 2건과 Reporting 운영 개선용 Java `@todo` 2건을 의도적으로 추가했다.
  - 문서 통합 9차 후 contracts/shared-kernel compileJava, master-data/governance/auth/gateway/discovery test, config-server assemble 성공.
  - 문서 통합 9차에서 Auth/Gateway/Master-Data/Governance 운영 개선용 Java `@todo` 5건을 의도적으로 추가했다.
  - 2026-06-12 검수 반영 후 `asset-lease:bootJar`, `deposit:api:bootJar`, `deposit:batch:bootJar`, `reporting:api:bootJar`, `reporting:batch:bootJar`, 영향 library 모듈 compileJava 성공.
  - 2026-06-12 검수 반영 후 `deposit:api`, `deposit:batch`, `reporting:api`, `reporting:batch` 개별 bootRun 컨텍스트 스모크 성공.
  - 2026-06-12 검수 반영에서 Deposit/Reporting IntelliJ `.run` 설정과 로컬 실행 문서를 추가했다.
  - 2026-06-15 logstash 비활성화 반영 후 Deposit/Reporting API/BATCH 네 가지 bootRun 컨텍스트 스모크가 `spring.profiles.active=local`로 성공했고, `localhost:5000` logstash 연결 실패 경고가 사라졌다.
  - 2026-06-15에 모든 `logback-spring.xml` XML 파싱과 Deposit/Reporting `.run` XML 파싱, `git diff --check`를 통과했다.
- Docker 이미지 빌드는 실행하지 않았다.
- 운영/일반 profile에서는 기존 logstash 전송이 유지되므로 수집기(`localhost:5000`) 기동이 필요하다.
- Auth 기본 잠금 어댑터와 Payable 로컬 지급 어댑터는 운영용 공유/외부 어댑터 교체가 필요하다.
- Journal 전기·잔액 Repository 직접 의존과 ECL 마스터 포트의 JPA 기술 누수는 제거했다.
- Journal JDBC bulk 구현은 설정 기반으로 추가했으며, 운영 DB 기준 배치 크기·인덱스·락 대기·동시 재집계 부하 검증은 남아 있다.

## Review Handoff Checklist

- [Review Content Read Scope](#review-content-read-scope)를 먼저 적용한다: filename-first → 명시된 비민감 exact allowlist ∩ 변경목록 → scoped 조회/보류.
- staged/unstaged/untracked를 구별하고 민감/불명확 경로를 제외한다. 범위가 없거나 비어 있거나 불명확하면 내용은 읽지 않고 부모에게 확인한다.
- 공백 검사도 본문 출력 가능성이 있으므로 동일 게이트와 상태별 exact 경로 제한을 적용한다. 경로 없는 전체 검사 fallback은 금지한다.
- 변경 대상 모듈 문서는 현재 승인된 정확 파일만 읽는다. 과거 목록이 범위를 늘리지 않는다.
- `docs/WORKLOG.md`, `docs/history/CODEX_WORKLOG.md`, `docs/history/MODULE_REVIEW_2026-05-06.md`의 과거 참조도 현재 적용 규칙/명시적 읽기 승인에 해당하는 정확 파일만 확인한다.
- 과거 목록의 `docs/GEMINI.md`, `docs/GEMINI_SKILL.md`는 현재 `docs/history/GEMINI.md`, `docs/history/GEMINI_SKILL.md`에 있다. 현재 적용 규칙/명시적 읽기 승인을 통과한 정확 파일만 확인한다.
- Findings는 요약보다 먼저 작성한다.
- 각 Finding은 파일/라인 근거를 포함한다.
- Gemini는 코드를 수정하지 않는다.

## Current Review Request - 2026-07-20 Discovery

- Review branch: `agent/asset-lease-split`.
- Review only; do not modify code. Report findings first with severity and exact file/line evidence.
- The working tree contains the previous Gateway pass plus the new Discovery pass; root Compose overlaps both and must be reviewed cumulatively.

### Discovery Changed Scope

- `discovery`: Config/actuator/observability dependencies, 8761 server-only defaults, registry/readiness tests, JDK 17 Dockerfile, module Compose, standalone run config, and docs.
- `config-repo/discovery-service.yml`: server-only policy, readiness/Prometheus/tracing, security/HA TODOs.
- root `docker-compose.yml`: Discovery container addresses and all 14 dependent services changed to `service_healthy`.
- common local-development/module-sequence/worklog/handoff documents.

### Required Review Focus

1. Does Discovery start correctly with and without Config Server on 8761, and are `register-with-eureka`/`fetch-registry` always false for the server?
2. Are Config Client, actuator, Prometheus, Brave/Zipkin dependencies justified and aligned with configuration?
3. Does `DiscoveryApplicationTests` safely test register -> lookup -> cancel without leaking registry state or depending on test order?
4. Do readiness and `registry.shouldAllowAccess` prove the intended startup condition without masking peer-sync behavior?
5. Did root Compose change only Discovery environment and intended `depends_on.discovery.condition` values, preserving Gateway edits and all other service settings?
6. Will the Alpine image contain a compatible `wget`, and do JDK 17, bootJar selection, build context, and healthcheck work together?
7. Are self-preservation defaults and operational explanations correct? Is any recommendation likely to cause stale or prematurely evicted instances?
8. Are unauthenticated Dashboard/registration API and single-node availability adequately marked as TODOs rather than claimed production-ready?
9. Recheck the previous Gateway global-filter, JWT claim, trusted-header, 401/503, and Docker changes because they are included in the same cumulative commit.

### Verification Evidence

- `.\gradlew :discovery:test :discovery:bootJar --console=plain --max-workers=1 --no-daemon`: passed.
- XML results: 6 tests, 0 failures, 0 errors, 0 skipped.
- Registry integration: temporary instance register, lookup, cancel, cleanup passed.
- standalone local bootRun: readiness `UP`; Dashboard, registry API, Prometheus HTTP 200; JVM metric present.
- Runtime cleanup: no Discovery/Gradle process remained.
- Docker CLI is not installed; actual image build and Compose execution were not run.
- Live Config/multiple Eureka clients/heartbeat/LoadBalancer and production load were not run.

## Current Review Request - 2026-07-22 Config Server

- Review branch: `agent/asset-lease-split`.
- Review only; do not modify code. Report findings first with severity and exact file/line evidence.
- Scope: current uncommitted Config Server pass only, while checking root Compose overlap with committed Discovery changes.

### Required Review Focus

1. Does `ConfigRepositoryHealthIndicator` correctly fail closed for empty/error results without exposing property values or repository URLs?
2. Is direct use of Spring Cloud `EnvironmentRepository` appropriate for this infrastructure application, without inventing a false business-domain layer?
3. Do application/profile/label binding and ordered `propertySources` tests reflect Spring Config semantics?
4. Does readiness include the custom `configRepository` component and prove representative configuration availability?
5. Do root/module Compose mount `config-repo` read-only at the configured path, and do exactly 15 clients wait for `service_healthy`?
6. Does the JDK 17 Docker image select one non-plain bootJar, avoid embedding config-repo, require a read-only runtime mount, and use a compatible Alpine healthcheck?
7. Were unrelated root Compose service settings preserved, especially Discovery and Gateway changes?
8. Do docs avoid the incorrect claims that local settings are ignored or that file edits instantly rebind running clients?
9. Are private network+mTLS/service auth and reviewed Git backend/label/refresh/rollback TODOs sufficient for the currently unknown production policy?

### Verification Evidence

- Initial `.\gradlew :config-server:test :config-server:bootJar --rerun-tasks --console=plain --max-workers=1 --no-daemon`: passed.
- Initial XML results: 9 tests, 0 failures, 0 errors, 0 skipped.
- A later change removed repository exception messages from health details and added a sensitive-path assertion. The main/test classes compiled, but the targeted rerun stalled before producing a new report because the Windows paging file was exhausted; treat this as a verification gap, not as a passing test.
- Initial standalone JAR: readiness UP; `master-data/default` HTTP 200/property source 1; Prometheus HTTP 200/JVM metric present.
- Config values were not printed and runtime was stopped.
- Docker CLI is unavailable; actual image/Compose execution was not run.

## Current Review Request - 2026-07-22 Contracts/Shared-Kernel

- Review branch: `agent/asset-lease-split`.
- Review only; do not modify code. Report findings first with severity and exact file/line evidence.
- Scope: current uncommitted contracts/shared-kernel pass plus the directly affected master-data and ECL integration files.

### Required Review Focus

1. Do JournalEntryCommand/JournalLineCommand validations preserve valid callers while preventing mutable lists, missing dates, invalid side codes, and null amounts?
2. Does AccountSubjectRef fail closed for explicit invalid normal-balance values without breaking the legacy four-argument constructor?
3. Does MonolithMasterDataQueryAdapter use the requested effectiveDate for account, partner, and department SCD2 lookup, and can overlapping versions cause a useful fail-fast result?
4. Is `@Masked` now actually connected to Jackson, and do REG_NO/ACCOUNT/EMAIL policies avoid overexposure and fail closed for malformed values?
5. Does SpringServiceDiscoveryRegistry constructor injection avoid service-locator behavior, return deterministic immutable results, and reject duplicate service names safely?
6. Is removing the inert `@DistributedLock` usage correct, and are the remaining lock TODOs explicit enough not to imply production safety?
7. Does CdmDataReadyEvent preserve event identity over JSON, and does CdmDataReadyConsumer use eventId as Batch identity, ignore only already-completed duplicates, and propagate other failures for Kafka retry/DLT?
8. Are broad shared-kernel infrastructure dependencies and ECL/account-mart-specific types correctly identified as staged migration work rather than removed unsafely?
9. Are the archived Docker/Compose files truly unused no-op skeletons, and do docs accurately state that these modules are libraries?
10. Recheck the cumulative Config Server changes because they remain in the same uncommitted working tree.

### Verification Evidence And Gap

- `git diff --check` passed.
- `DistributedLock` has no Java usage outside its deprecated declaration.
- 15 focused tests were added.
- Gradle tests and direct javac were not completed: Windows paging-file exhaustion prevented even 64-128 MB JVM work.
- Spawned Java processes and temporary outputs were cleaned.
- Treat all JVM behavior as verification pending and rerun the commands in `docs/ai-harness/handoff.md` before merge.
``````
<!-- END VERBATIM ORIGINAL -->
