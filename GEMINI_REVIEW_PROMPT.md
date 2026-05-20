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

3. 리뷰 대상 변경 범위는 Codex의 2026-05-19 "모듈 순차 빌드/실행 회귀 수정"입니다.
- loan core 테스트의 JournalValidationEngine 생성자 계약 반영
- journal-ledger core의 JournalRuleCondition 컬럼명 `condition_value` 변경
- AccountingPeriodStatusPort 구현을 journal-ledger core 공통 어댑터로 이동
- reporting 테스트 fake의 LedgerQueryPort 신규 메서드 반영
- closing API 실행 진입점/bootJar 설정 추가 및 closing batch bootJar 비활성화
- asset-lease, journal-ledger batch, loan api/batch의 component/entity/repository scan 범위 축소
- loan api/batch JPA starter 추가
- asset-lease Flyway migration `V20` 조정, AssetHistoryRepository 쿼리명 수정, LeasePaymentResolutionPort 조건부 fallback 추가
- shared-kernel SpringServiceDiscoveryRegistry 중복 구현 제거
- docs/INFRA_RUN_GUIDE.md, docs/WORKLOG.md, CODEX_WORKLOG.md 갱신

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
  - `.\gradlew test --console=plain --max-workers=1`: 성공
  - `.\gradlew build --console=plain --max-workers=1`: 성공
  - `npm run build` in `frontend`: 성공, 기존 unused import warning만 남음
  - JAR smoke: `config-server`, `discovery`, `gateway`, `auth`, `master-data`, `governance`, `journal-ledger-api`, `journal-ledger-batch`, `closing-api`, `loan-api`, `loan-batch`, `asset-lease` 모두 `STARTED`

## Review Handoff Checklist

- 리뷰 전 `git status --short --branch`로 범위를 확인한다.
- 변경 대상 모듈 문서를 읽는다.
- `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md` 최신 항목을 확인한다.
- Findings는 요약보다 먼저 작성한다.
- 각 Finding은 파일/라인 근거를 포함한다.
- Gemini는 코드를 수정하지 않는다.
