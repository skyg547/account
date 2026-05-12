# 📚 Repository Docs Hub (전사 문서 허브)

이 디렉터리는 시스템 전체의 구조와 비즈니스 흐름을 파악하기 위한 중앙 문서 저장소입니다.
최근 문서 고도화 작업을 통해 각 모듈 하위의 `docs/` 폴더(`beginner-guide.md`, `process-flow.md`, `schema.md`)가 최신 헥사고날 아키텍처 및 도커 환경에 맞게 일제히 업데이트되었습니다. 특히 모든 `beginner-guide.md`에는 초보자를 위한 친절한 비유와 개념 설명이 추가되어 학습 곡선을 크게 낮췄습니다. 이곳 전사 허브에서는 개별 모듈이 아닌 전체 시스템 아키텍처와 통합 비즈니스 흐름을 다룹니다.

---

## 1. 🐣 처음 오신 분들을 위한 입문 경로

모든 문서를 처음부터 다 읽을 필요는 없습니다. 아래 순서대로 파악하는 것을 권장합니다.

1. **[../BEGINNER_GUIDE.md](../BEGINNER_GUIDE.md):** 프로젝트 최상단에 있는 왕초보용 통합 가이드. 시스템의 큰 그림을 잡아줍니다.
2. **[infrastructure-guide.md](./infrastructure-guide.md):** 유레카, 게이트웨이, 도커 환경 등 인프라가 어떻게 구성되어 있는지 비유로 설명합니다.
3. **[business_workflow.md](./business_workflow.md):** 지출결의부터 전표 생성, 마감까지 실제 돈이 어떻게 흘러가는지 업무 시나리오를 다룹니다.
4. **[domain-catalog.md](./domain-catalog.md):** 우리 시스템에 어떤 도메인(업무)들이 있는지 한눈에 보는 카탈로그입니다.

---

## 2. 🏛️ 아키텍처 및 원칙 문서

시스템의 설계 원칙과 MSA 구조에 대해 심도 있게 알고 싶다면 아래 문서를 참고하세요.

- **[principles_and_policies.md](./principles_and_policies.md):** 헥사고날 아키텍처 원칙, SCD2 적용 기준 등 전사 개발 정책.
- **[service-discovery-model.md](./service-discovery-model.md):** MSA 간 서비스 디스커버리 연동 모델.
- **[msa-execution-and-work-plan.md](./msa-execution-and-work-plan.md):** MSA 전환 로드맵 및 운영 적용 순서.

---

## 3. 📦 모듈별 상세 문서 학습 가이드

각 모듈의 세부적인 기능과 도커 실행 방법은 **해당 모듈 디렉토리의 `README.md`**를 열어보세요. (모든 모듈의 `README.md`에 초보자 설명이 추가되어 있습니다.)

### 3.1 코어 및 기준 정보 (기초)
- `shared-kernel/README.md`: 공통 타입과 유틸리티
- `contracts/README.md`: 모듈 간 통신(포트, DTO) 서식지
- `master-data/README.md`: 계정, 부서, 거래처 등 기준 정보 센터

### 3.2 업무 서브레저 (현업 부서)
- `expenditure-resolution/README.md`: 지출 결의 및 예산 통제
- `payable/README.md` & `receivable/README.md`: 매입채무/매출채권 관리
- `tax/README.md`: 세금계산서 관리
- `asset-lease/README.md` & `loan/README.md`: IFRS16 리스 및 대출

### 3.3 핵심 회계 엔진 (결산 부서)
- `journal-ledger/README.md`: 전표 기록 및 원장(GL/SL) 관리
- `closing/README.md`: 월마감 및 자동 분개
- `reconciliation/README.md`: 외부 데이터와의 자동 대사
- `reporting/README.md`: B/S, I/S 재무제표 생성

---

## 4. 🗄️ 아카이브 (Archive)

과거 MSA 분리 이전의 히스토리나 완료된 작업 마일스톤 문서는 `archive/` 폴더 내에 보관되어 있습니다. (예: `msa-modularization-*.md`, `dependency-split-status.md` 등)
과거 아키텍처의 의사결정 과정이 궁금할 때 열어보세요.

---

## 5. 🤖 AI Agent 연동 규칙

- **[GEMINI.md](./GEMINI.md) & [GEMINI_SKILL.md](./GEMINI_SKILL.md):** Gemini/Claude 등 AI 에이전트가 코드를 작성하거나 리뷰할 때 반드시 지켜야 하는 도메인 제약사항 및 핸드오프 규칙입니다.
