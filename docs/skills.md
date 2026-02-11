# 🔄 차세대 재무 시스템 개발 워크플로우

본 워크플로우는 금융 데이터의 무결성과 최신 모던 웹 기술 스택의 조화를 목적으로 합니다. 모든 에이전트는 단계별로 성호 엔지니어의 컨펌을 받은 후 다음 단계로 진행합니다.

## 🏁 Phase 1: 요구사항 정의 및 기술 조사 (PM)
- **목표**: 비즈니스 로직 구체화 및 2026년 최신 기술 트렌드 반영.
- **활동**:
    - `Brave Search MCP`를 통해 IFRS 등 관련 회계 기준 조사.
    - Next.js 15 및 React 19 기반의 최적 UI 라이브러리 선정.
- **산출물**: `docs/requirements.md`

## 🏗️ Phase 2: 데이터 모델링 (DA)
- **목표**: Oracle/PostgreSQL 기반의 견고한 스키마 설계.
- **활동**:
    - 테이블 명세서 및 DDL 작성 (`docs/db/schema.sql`).
    - 재무 데이터 정합성을 위한 제약 조건(FK, Check) 및 인덱스 전략 수립.
- **산출물**: `docs/db/table_spec.md`, `schema.sql`

## 💻 Phase 3: 백엔드 & API 설계 (Backend)
- **목표**: Java 17/Spring Boot 기반의 안정적인 API 구축.
- **활동**:
    - JPA Entity 및 QueryDSL 구조 설계.
    - DB 연결 전, Mock 데이터 기반의 컨트롤러 우선 구현.
- **산출물**: Spring Boot API 코드, Swagger 명세.

## 🎨 Phase 4: 모던 프론트엔드 구현 (Frontend)
- **목표**: 고성능 재무 대시보드 UI 구축.
- **활동**:
    - Tailwind CSS와 Shadcn/UI를 활용한 컴포넌트 개발.
    - `TanStack Query`를 이용한 비동기 데이터 처리 및 상태 관리.

## 🧪 Phase 5: 통합 검증 (QA)
- **목표**: 로직 오차 제로(Zero) 검증.
- **활동**:
    - `Sequential Thinking MCP`를 활용한 복합 결산 시나리오 테스트.
    - JUnit 및 Playwright를 이용한 자동화 테스트 실행.