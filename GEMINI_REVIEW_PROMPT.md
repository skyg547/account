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

1. 먼저 아래 문서를 읽어 현재 프로젝트 규칙과 최근 작업 이력을 확인하세요.
- GEMINI.md
- docs/GEMINI.md
- docs/GEMINI_SKILL.md
- Agents.md
- docs/WORKLOG.md 최신 항목
- CODEX_WORKLOG.md 최신 항목
- MODULE_REVIEW_2026-05-06.md 최신 항목
- docs/todo.md
- 변경 대상 모듈의 README.md 및 docs/*.md

2. 현재 리뷰 대상 범위를 확인하세요.
- git status --short --branch
- git diff --stat
- git diff
- 새 파일이 있으면 해당 파일도 확인

3. 리뷰 대상 변경 범위는 Codex의 2026-05-27 "Closing ECL summary 포트 연동, ECL allowance_summary 생성 경로 추가, account-mart/ecl 루트 Gradle 편입 및 wiring 복구, allowance exposure snapshot 및 allowanceEclJob 추가"입니다.
- `closing:core`에 `EclAllowanceResultPort`, `EclAllowanceSummary` 추가
- `closing:batch`에 `JdbcEclAllowanceResultAdapter` 추가
- `EclProvisionService`에서 대출채권 계정/KRW/1% 고정 산식 제거
- `EclProvisionService`가 `EclAllowanceResultPort`의 기준일별 summary를 조회해 목표 충당금과 GL 기존 대손충당금 잔액 차이만 보충/환입 전표로 처리하도록 변경
- 통화, 충당금 계정, 비용 계정, 환입 수익 계정을 summary/설정 기반으로 resolve하도록 변경
- 환입 시 비용 계정 재사용 대신 `reversalIncomeAccountCode` 사용
- `EclProvisionServiceTest` 추가: 보충, 환입, summary 없음 케이스 검증
- `ecl:ecl-core`에 `AllowanceSummaryBuildPort`, `AllowanceSummaryService`, `AllowanceSummaryBuildResult`, `JdbcAllowanceSummaryPersistenceAdapter` 추가
- 완료된 `cr_risk_results`를 `allowance_account_mappings`와 조인해 `allowance_summary`를 SQL bulk 집계로 재생성
- mapping 누락 시 기존 summary를 보존하고 실패시키며, 성공 시 기준일 summary를 교체해 closing 중복 조회를 방지
- `ecl:ecl-batch`에 `AllowanceSummaryTasklet`, `allowanceSummaryStep`, `standaloneAllowanceSummaryJob` 추가
- `ecl:ecl-api`에 `V3__add_allowance_summary.sql` 추가
- `ecl:ecl-batch` H2 통합 스키마에 summary 테이블과 샘플 계정 매핑 추가
- `AllowanceSummaryServiceTest` 추가
- 루트 `settings.gradle`에 `risk-common`, `account-mart:mart-*`, `ecl:ecl-*` 포함
- `risk-common` 호환 모듈 추가: 기존 `com.ho.account.shared.finance` 엔티티/enum/event/DTO/예외/락 타입 제공
- `ecl`/`account-mart` Gradle 의존성, Kafka 의존성, stale import 정리
- `account-mart` KAP/조기경보/계좌금리/수익률곡선/대사이력/등급마스터 포트 adapter 추가
- `IntegratedPositionProcessor`의 KRW 환산 `marketValue` 보강
- `mart-batch` demo SQL, JPA reader projection, ODS/GL MATCH/MISMATCH 대사 이력 보강
- Kafka 없는 테스트를 위한 `mart.batch.cdm-event.enabled=false` 설정 추가
- `account-mart:mart-core`에 `AllowanceExposureSnapshotBuildPort`, `AllowanceExposureSnapshotService`, `AllowanceExposureSnapshotBuildResult`, `JdbcAllowanceExposureSnapshotPersistenceAdapter` 추가
- `allowance_exposure_snapshots` entity와 `account-mart:mart-api` `V2__add_allowance_exposure_snapshot.sql`, `account-mart/db/schema-mart.sql` 추가
- `integratedPositionEtlJob`에 `allowanceExposureSnapshotStep` 추가 및 개별 `allowanceExposureSnapshotJob` 노출
- `IntegratedPositionEtlJobTest`에서 snapshot 건수, 미사용한도, 회계 노출 계정코드 검증 추가
- `ecl:ecl-core`에 `AllowanceExposureSyncService`/port/adapter 추가: `allowance_exposure_snapshots`에서 `cr_customers`, `cr_accounts` bulk upsert
- `ecl:ecl-core`에 `AllowanceEclCompletionService`/port/adapter 추가: RWA 없는 ECL 전용 경로에서 weighted ECL 결과를 `COMPLETED`로 확정
- `ecl:ecl-batch`에 `AllowanceExposureSyncTasklet`, `AllowanceEclCompletionTasklet`, `AllowanceEclBatchConfig`, `allowanceEclJob` 추가
- `IndividualStepJobConfig`에 `standaloneAllowanceExposureSyncJob`, `standaloneAllowanceEclCompletionJob` 추가
- `JobRunner`가 `spring.batch.job.enabled=false`와 `job.name`/`spring.batch.job.name` 선택 실행을 지원하도록 보강
- `StressSimulatorService`의 깨진 baseline 식별자 복구
- `docs/allowance-ecl-refocus-plan.md`, `closing/README.md`, `closing/docs/README.md`, `ecl/README.md`, `ecl/docs/*.md`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md` 갱신

4. 리뷰 기준은 아래 순서로 우선순위를 둡니다.
- 컴파일/테스트/bootJar/smoke 기동 실패를 유발하는 결함
- 회계 금액/차대/상태 전이/마감/승인/라인리지 오류
- 헥사고날 아키텍처 위반: adapter, application, domain, infrastructure 경계 침범
- DDD 위반: 단순 setter 중심 변경, 도메인 규칙의 서비스 산재, 의미 없는 단순 위임
- 배치 규칙 위반: batch 모듈 내 비즈니스 if/for/math 구현
- API 계약 회귀: DTO, Controller, Service, Test 불일치
- Flyway migration 버전 충돌 또는 다중 모듈 런타임 classpath 리스크
- 테스트 누락 또는 기존 테스트 계약 미갱신
- 문서/WORKLOG/todo 완료 표기와 실제 코드 상태 불일치

5. 가능하면 아래 검증을 재실행하세요.
- .\gradlew test --console=plain --max-workers=1
- .\gradlew build --console=plain --max-workers=1
- npm run build (workdir: frontend)
- 실행 가능 Spring Boot JAR smoke는 외부 인프라 없이 `--server.port=0`, `--spring.cloud.config.enabled=false`, `--spring.cloud.vault.enabled=false`, `--management.tracing.enabled=false`, `--spring.batch.job.enabled=false` 기준으로 확인하세요.
- Discovery는 Eureka Server 자체가 ApplicationInfoManager를 필요로 하므로 `--eureka.client.enabled=false`를 넣지 마세요.

6. 출력 형식은 반드시 아래 순서를 따르세요.

Findings:
- Severity: Critical/High/Medium/Low 중 하나
- 파일/라인: 실제 경로와 라인 번호
- 문제: 무엇이 잘못됐는지
- 영향: 운영/회계/빌드/테스트에 어떤 위험이 있는지
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
- 루트 `WORKLOG.md`는 현재 작업트리에 없고, 추적된 최신 작업 이력은 `docs/WORKLOG.md`에 있다.
- Codex 작업 로그는 루트 `CODEX_WORKLOG.md`에 최신 항목을 추가했다.
- 최종 검증 결과:
  - `.\gradlew :closing:batch:test --console=plain`: 성공
  - `.\gradlew projects --console=plain`: 성공. 루트 프로젝트 목록에 `risk-common`, `account-mart`, `ecl` 포함
  - `.\gradlew :risk-common:compileJava :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava :ecl:ecl-core:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava --console=plain`: 성공
  - `.\gradlew :account-mart:mart-batch:test --console=plain`: 성공
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceSummaryServiceTest :account-mart:mart-core:test --tests com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessorTest --console=plain`: 성공
  - `.\gradlew :account-mart:mart-core:compileTestJava :account-mart:mart-batch:compileTestJava :ecl:ecl-core:compileTestJava :ecl:ecl-batch:compileTestJava --console=plain`: 성공
  - `.\gradlew :account-mart:mart-core:test --tests com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotServiceTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest --tests com.ho.account.ecl.core.application.service.allowance.AllowanceEclCompletionServiceTest :ecl:ecl-batch:compileJava --console=plain`: 성공
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.CreditRiskBatchIntegrationTest --console=plain`: 실패. `JobRunner` 비활성화 후 컨텍스트 로딩은 통과했으나 기존 Batch metadata table 미초기화(`BATCH_JOB_INSTANCE` 없음)로 job launch 단계 실패
  - 전체 diff check는 이번 범위 밖 기존 trailing whitespace가 섞여 있을 수 있으므로 범위 확인 필요
  - 결산 자동 승인/전기 통제는 기존 흐름을 유지하며, 별도 approval/reversal policy 포트 분리는 후속 리스크로 남음
  - `risk-common`은 임시 호환 계층이며 장기적으로 shared-kernel 또는 allowance 전용 공통 모델로 이관 필요
  - `JdbcAllowanceExposureSyncAdapter`는 PostgreSQL `ON CONFLICT` 기준이므로 H2 end-to-end 테스트에는 H2 호환 upsert 또는 fixture 보강 필요

## Review Handoff Checklist

- 리뷰 전 `git status --short --branch`로 범위를 확인한다.
- 변경 대상 모듈 문서를 읽는다.
- `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md` 최신 항목을 확인한다.
- Findings는 요약보다 먼저 작성한다.
- 각 Finding은 파일/라인 근거를 포함한다.
- Gemini는 코드를 수정하지 않는다.
