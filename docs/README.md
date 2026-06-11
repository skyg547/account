# 📚 Repository Docs Hub (전사 문서 허브)

이 디렉터리는 시스템 전체의 구조와 비즈니스 흐름을 파악하기 위한 중앙 문서 저장소입니다.
최근 문서 고도화 작업을 통해 각 모듈 하위의 `docs/` 폴더(`beginner-guide.md`, `process-flow.md`, `schema.md`)가 최신 헥사고날 아키텍처 및 도커 환경에 맞게 일제히 업데이트되었습니다. 특히 모든 `beginner-guide.md`에는 초보자를 위한 친절한 비유와 개념 설명이 추가되어 학습 곡선을 크게 낮췄습니다. 이곳 전사 허브에서는 개별 모듈이 아닌 전체 시스템 아키텍처와 통합 비즈니스 흐름을 다룹니다.

---

## 1. 🐣 처음 오신 분들을 위한 입문 경로

모든 문서를 처음부터 다 읽을 필요는 없습니다. 아래 순서대로 파악하는 것을 권장합니다.

1. **[beginner_guide.md](./beginner_guide.md):** 프로젝트 통합 가이드. 시스템의 큰 그림과 문서 읽는 순서를 잡아줍니다.
2. **[local-development.md](./local-development.md):** IntelliJ IDEA, JDK 17, Gradle, 로컬 Spring Boot 실행 방법을 설명합니다.
3. **[infrastructure_runbook.md](./infrastructure_runbook.md):** 유레카, 게이트웨이, 도커 환경 등 인프라가 어떻게 구성되어 있는지 비유로 설명합니다.
4. **[business_workflow.md](./business_workflow.md):** 지출결의부터 전표 생성, 마감까지 실제 돈이 어떻게 흘러가는지 업무 시나리오를 다룹니다.
5. **[domain-catalog.md](./domain-catalog.md):** 우리 시스템에 어떤 도메인(업무)들이 있는지 한눈에 보는 카탈로그입니다.
6. **[module-documentation-sequence.md](./module-documentation-sequence.md):** 모듈별 문서 통합 진행 순서와 완료/대기 상태.

---

## 2. 🏛️ 아키텍처 및 원칙 문서

시스템의 설계 원칙과 MSA 구조에 대해 심도 있게 알고 싶다면 아래 문서를 참고하세요.

- **[architecture.md](./architecture.md):** 백엔드(헥사고날/MSA) 및 프론트엔드(Next.js) 통합 아키텍처 명세.
- **[principles_and_policies.md](./principles_and_policies.md):** 헥사고날 아키텍처 원칙, SCD2 적용 기준 등 전사 개발 정책.
- **[msa_roadmap.md](./msa_roadmap.md):** MSA 전환 로드맵 및 운영 적용 순서.

---

## 3. 📦 모듈별 상세 문서 학습 가이드

각 모듈의 세부 기능, IntelliJ 로컬 실행, Gradle 검증 방법은 **해당 모듈 디렉토리의 `README.md`**를 열어보세요. standalone Boot 앱이 아닌 library 모듈은 `bootRun` 대신 테스트/컴파일 중심으로 검증합니다.

### 3.1 코어 및 기준 정보 (기초)
- `shared-kernel/README.md`와 `shared-kernel/docs/README.md`: 공통 타입과 유틸리티. 현재 library 모듈이라 IntelliJ/Gradle 컴파일 중심으로 검증
- `contracts/README.md`와 `contracts/docs/README.md`: 모듈 간 통신 포트와 DTO. 현재 library 모듈이라 IntelliJ/Gradle 컴파일 중심으로 검증
- `master-data/README.md`와 `master-data/docs/README.md`: 계정, 부서, 거래처, 상품 등 기준 정보와 SCD2 흐름. Spring Boot 앱으로 IntelliJ `Master Data bootRun` 제공
- `governance/README.md`와 `governance/docs/README.md`: 감사 로그, 승인, SOD, Auth 역할 반영 흐름. Spring Boot 앱으로 IntelliJ `Governance bootRun` 제공
- `auth/README.md`와 `auth/docs/README.md`: 로그인, JWT, 역할 버전, 내부 역할 반영 API. Spring Boot 앱으로 IntelliJ `Auth bootRun` 제공
- `config-server/README.md`와 `config-server/docs/README.md`: 중앙 설정 서버와 `config-repo` 로컬 설정 흐름. IntelliJ `Config Server bootRun` 제공
- `discovery/README.md`와 `discovery/docs/README.md`: Eureka 서비스 레지스트리. IntelliJ `Discovery bootRun` 제공
- `gateway/README.md`와 `gateway/docs/README.md`: API Gateway 라우팅, JWT 검증, fallback. IntelliJ `Gateway bootRun` 제공

### 3.2 업무 서브레저 (현업 부서)
- `expenditure-resolution/README.md`: 지출 결의 및 예산 통제
- `payable/README.md`와 `payable/docs/README.md`: 매입채무, 지급 런, 선급금, 상계 흐름. 현재 library 모듈이라 IntelliJ/Gradle 테스트 중심으로 검증
- `receivable/README.md`와 `receivable/docs/README.md`: 매출채권, 수납, 자동/수동 매칭 흐름. 현재 library 모듈이라 IntelliJ/Gradle 테스트 중심으로 검증
- `asset-lease/README.md`와 `asset-lease/docs/README.md`: 고정자산, 감가상각 Batch, IFRS 16 리스, 사용권자산/리스부채 흐름. 단독 Boot 실행과 IntelliJ `.run` 설정 제공
- `tax/README.md`와 `tax/docs/README.md`: 매입 세금계산서, 금액 정합성, 논리 취소 흐름. 현재 library 모듈이라 IntelliJ/Gradle 테스트 중심으로 검증
- `loan/README.md`와 `loan/docs/README.md`: 대출 실행, EIR, 일일 이자 발생 Batch

### 3.3 핵심 회계 엔진 (결산 부서)
- `journal-ledger/README.md`: 전표 기록 및 원장(GL/SL) 관리
- `closing/README.md`와 `closing/docs/README.md`: 월마감, 기간 잠금, FX/ECL 결산 배치
- `reconciliation/README.md`와 `reconciliation/docs/README.md`: 외부 데이터와의 자동 대사, 차이/조정 전표 흐름. 현재 library 모듈이라 IntelliJ/Gradle 테스트 중심으로 검증
- `reporting/README.md`와 `reporting/docs/README.md`: B/S, I/S 재무제표 생성, 주석 마트, 감독보고 제출. 현재 `core/api/batch` library 모듈이라 IntelliJ/Gradle 테스트 중심으로 검증

### 3.4 대손충당금 입력/산출
- `account-mart/README.md`와 `account-mart/docs/README.md`: ECL 입력 snapshot 생성
- `ecl/README.md`와 `ecl/docs/README.md`: IFRS 9 ECL/대손충당금 산출

---

## 4. 🗄️ 아카이브 (Archive)

과거 MSA 분리 이전의 히스토리나 완료된 작업 마일스톤 문서는 `archive/` 폴더 내에 보관되어 있습니다. (예: `msa-modularization-*.md`, `dependency-split-status.md` 등)
깨진 레거시 문서도 삭제하지 않고 `archive/`로 이동해 보존합니다.
과거 아키텍처의 의사결정 과정이 궁금할 때 열어보세요.

---

## 5. 🤖 AI Agent 연동 규칙

- 루트 **[../Agents.md](../Agents.md):** Codex 작업 규칙.
- 루트 **[../GEMINI.md](../GEMINI.md):** Gemini 작업 규칙.
- 과거 Gemini 관련 자료는 [history/](./history/)에 보관되어 있습니다.
