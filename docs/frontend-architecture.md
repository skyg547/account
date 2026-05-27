# 🎨 프론트엔드 아키텍처 설계서 (Next.js 15)

본 문서는 '모던 재무 시스템'의 사용자 인터페이스(UI)를 구축하기 위한 기술 스택, 디렉토리 구조 및 설계 원칙을 정의합니다.

## 1. 기술 스택 (Tech Stack)

| 구분 | 기술 | 이유 |
| :--- | :--- | :--- |
| **Framework** | **Next.js 15 (App Router)** | 최신 리액트 기능(Server Components) 및 최적화된 라우팅 제공 |
| **Language** | **TypeScript** | 금융 데이터의 타입 안정성 확보 및 개발 생산성 향상 |
| **Styling** | **Vanilla CSS (CSS Modules)** | 도구 의존성을 최소화하고 브라우저 성능 최적화 및 커스텀 디자인 자유도 확보 |
| **State Management** | **Zustand** | 가볍고 직관적인 상태 관리 라이브러리 (전역 테마, 사용자 세션 등) |
| **Data Fetching** | **TanStack Query (React Query)** | 서버 데이터 캐싱, 실시간 동기화 및 낙관적 업데이트 지원 |
| **Icons** | **Lucide React** | 깔끔하고 일관된 아이콘 세트 제공 |

---

## 2. 핵심 설계 원칙

1.  **Rich Aesthetics (프리미엄 디자인):**
    *   금융 시스템 특유의 딱딱함을 탈피하고, 다크 모드 기반의 **글래스모피즘(Glassmorphism)**과 부드러운 그라데이션을 사용합니다.
    *   마이크로 애니메이션(Framer Motion 등 활용 가능)을 통해 사용자 상호작용 피드백을 강화합니다.
2.  **Domain-Driven UI (도메인 기반 UI):**
    *   MSA 구조에 맞게 각 서비스(Master Data, Journal, Ledger 등)별로 모듈화된 폴더 구조를 가집니다.
3.  **Stability (안정성):**
    *   모든 통화 데이터는 정밀도 손실 없이 표시되어야 하며, 입력 폼에는 엄격한 유효성 검사를 적용합니다.
4.  **Responsive (반응형):**
    *   데스크탑 대시보드뿐만 아니라 모바일에서도 결산 현황을 확인할 수 있는 반응형 레이아웃을 제공합니다.

---

## 3. 디렉토리 구조 및 아키텍처 다이어그램 (Architecture Diagram)

프론트엔드의 컴포넌트 및 레이어 간의 의존성은 다음과 같습니다. Page 기반의 라우팅에서 공통 Layout과 전역 상태(Zustand)를 참조하고, 데이터 패칭은 React Query와 Service Layer를 거쳐 백엔드(Gateway)로 향합니다.

```mermaid
flowchart TD
    subgraph "Next.js App Router (UI Layer)"
        LAYOUT[app/layout.tsx\n(Global Shell)]
        PAGE[app/**/page.tsx\n(Route Segments)]
        
        LAYOUT --> PAGE
    end

    subgraph "Components Layer"
        COMMON[components/common/\n(UI Primitives)]
        DOMAIN[components/domain/\n(Business Widgets)]
        
        PAGE --> COMMON
        PAGE --> DOMAIN
        DOMAIN --> COMMON
    end

    subgraph "State & Logic Layer"
        STORE[(store/\nZustand)]
        HOOKS[hooks/\nReact Query & Custom Hooks]
        
        PAGE --> STORE
        PAGE --> HOOKS
        DOMAIN --> HOOKS
    end

    subgraph "Service Layer"
        SVC[services/\n(Axios / Fetch)]
        
        HOOKS --> SVC
        PAGE --> SVC
    end

    subgraph "Backend API (Gateway)"
        API[Gateway (Port: 8080)]
        SVC -.->|REST API / HTTPS| API
    end

    style STORE fill:#f9f,stroke:#333,stroke-width:2px
    style SVC fill:#bbf,stroke:#333,stroke-width:2px
```

### 상세 디렉토리 설명
```text
frontend/
├── src/
│   ├── app/                # Next.js App Router (Pages, Layouts)
│   ├── components/         # 재사용 가능한 UI 컴포넌트
│   │   ├── common/         # Button, Input, Modal 등 기초 컴포넌트
│   │   ├── layout/         # Navbar, Sidebar, Footer
│   │   └── domain/         # 특정 도메인(Account, Journal) 종속적 컴포넌트
│   ├── hooks/              # 커스텀 훅 (useAuth, useFetch 등)
│   ├── services/           # API 연동 서비스 (Axios/Fetch 관리)
│   ├── store/              # 전역 상태 관리 (Zustand)
│   ├── styles/             # 전역 CSS 및 디자인 토큰
│   ├── types/              # TypeScript 인터페이스 정의
│   └── utils/              # 유틸리티 함수 (금액 포맷팅 등)
├── public/                 # 정적 자산 (이미지, 로고)
└── docs/                   # 프론트엔드 전용 문서
```

---

## 4. 초보자를 위한 개념 설명
*   **Next.js 15:** 웹사이트의 뼈대를 만드는 프레임워크입니다. 예전에는 페이지를 이동할 때마다 새로고침이 필요했지만, Next.js를 쓰면 마치 스마트폰 앱처럼 부드럽게 화면이 전환됩니다.
*   **App Router:** 집의 구조도 같은 것입니다. `/dashboard`라고 주소창에 치면 어떤 방(페이지)을 보여줄지 결정하는 최신 방식입니다.
*   **TypeScript:** 코드를 짤 때 실수를 미리 잡아주는 조수입니다. 예를 들어, 숫자가 들어가야 할 금액 칸에 글자를 넣으려고 하면 "안 돼요!"라고 미리 알려줍니다.

---

**담당자: [프론트]**
*작성일: 2026-04-22*
