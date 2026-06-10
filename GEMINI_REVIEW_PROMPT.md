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

3. 리뷰 대상 변경 범위는 Codex의 2026-06-09 "잔여 TODO 최종 경계 통합"과 2026-06-10 "ECL·Journal 잔여 경계 및 문서 통합"입니다.
- Auth 로그인 성공/실패 감사, 설정 기반 임시 잠금, `LoginAttemptPort`/기본 어댑터
- Payable `PaymentExecutionPort`, 지급 멱등 키, 실패/재시도 상태, 정확한 `payableId`, master-data 내부 의존 제거
- Receivable 참조번호 우선/만기일 허용/중복 실패 폐쇄 자동 매칭과 `CollectionAllocation` 잔액 이력
- Journal/Unsettled HTTP DTO, 필수 `X-User-ID`, 미결 인바운드 포트, 반제 참조번호 멱등/감사 필드
- Loan 소유 출력 포트와 외부 전표 값 참조
- Reconciliation 표준 `Unit -> Run -> Difference` Aggregate 및 단계 결과 연결
- Allowance input JPA 쓰기 소유권의 account-mart 이동과 ECL 자체 읽기 모델
- Closing 조정 전표 기본 DRAFT 통제, Tax 논리 취소/actor/계약 포트, Asset actor 전달
- Java 코드 `@todo` 0건과 README/WORKLOG/리뷰 문서의 실제 코드 일치 여부
- ECL `EadCalculationResult`, 모델 비율 fail-closed 검증, 이름 있는 기본 CCF 정책
- ECL 모델 파라미터 기술 독립 포트와 JPA 어댑터 빈 구성
- ECL 등급·상품·LGD·담보배분·거시시나리오·전이행렬 기술 독립 포트와 JPA/캐시 어댑터
- Journal `UnsettledItemPersistencePort`, 거래처별 DB 필터, `JournalRuleQueryPort`, `JournalSide` 규칙 타입
- Journal `JournalPersistencePort`/`LedgerEntryPersistencePort`/`LedgerBalancePersistencePort` 전기·잔액 경계와 DB 조건 필터
- Journal `journal-ledger.ledger.persistence-mode=jdbc-bulk` 설정 기반 JDBC batch insert/upsert 어댑터, `YearMonthAttributeConverter`, 잔액 재집계 bulk 저장 경로
- ECL/Journal docs 인덱스, 입문·프로세스·스키마 문서의 실제 코드 일치 여부

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
- .\gradlew :auth:test :payable:test :receivable:test :asset-lease:test :tax:test --console=plain
- .\gradlew :closing:batch:test :journal-ledger:core:test :journal-ledger:api:compileJava :reconciliation:test --console=plain
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
- 이번 핸드오프의 기준은 2026-06-09 잔여 TODO 통합과 2026-06-10 ECL·Journal 추가 경계/문서 통합이다.
- 루트 `WORKLOG.md`는 현재 작업트리에 없고, 추적된 최신 작업 이력은 `docs/WORKLOG.md`에 있다.
- Codex 작업 로그는 루트 `CODEX_WORKLOG.md`에 최신 항목을 추가했다.
- 최종 검증 결과:
  - Auth/Payable/Receivable/Asset/Tax 집중 테스트 성공.
  - Closing/Journal/Reconciliation/ECL 집중 테스트 및 API 컴파일 성공.
  - Loan/Account-Mart/Allowance 소유권 경계 테스트 및 API 컴파일 성공.
  - Java 소스 `@todo` 검색 결과 0건, 변경 범위 diff check 성공.
  - ECL core/API/batch와 Journal core/API 통합 검증 성공.
  - Journal T53 구현 후 core 테스트와 H2 기반 JDBC batch insert/upsert 집중 테스트 성공.
- Docker 이미지 빌드는 실행하지 않았다.
- Auth 기본 잠금 어댑터와 Payable 로컬 지급 어댑터는 운영용 공유/외부 어댑터 교체가 필요하다.
- Journal 전기·잔액 Repository 직접 의존과 ECL 마스터 포트의 JPA 기술 누수는 제거했다.
- Journal JDBC bulk 구현은 설정 기반으로 추가했으며, 운영 DB 기준 배치 크기·인덱스·락 대기·동시 재집계 부하 검증은 남아 있다.

## Review Handoff Checklist

- 리뷰 전 `git status --short --branch`로 범위를 확인한다.
- 변경 대상 모듈 문서를 읽는다.
- `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md` 최신 항목을 확인한다.
- Findings는 요약보다 먼저 작성한다.
- 각 Finding은 파일/라인 근거를 포함한다.
- Gemini는 코드를 수정하지 않는다.
