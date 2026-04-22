# 🏗️ 도메인 특화 화면 설계서 (Master Data & Journal Ledger)

본 문서는 `master-data`와 `journal-ledger` 마이크로서비스의 핵심 엔티티와 비즈니스 로직을 효율적으로 처리하기 위한 사용자 인터페이스(UI/UX) 설계를 정의합니다.

---

## 1. Master Data 관리 Portal (기준 정보 관리)

기준 정보는 시스템 전체에서 사용되는 기초 데이터로, 정확한 입력과 계층적인 조회가 중요합니다.

### ① 계정 과목 관리 (Account Subject)
*   **엔티티 연결:** `AccountSubject.java`, `AccountSubjectService.java`
*   **UI 구성:**
    *   **좌측 (TreeView):** 계정 체계(자산/부채/자본/수익/비용)를 5레벨 트리 구조로 시각화.
    *   **우측 (Detail Form):** 선택된 계정의 상세 속성(계정코드, 명칭, 사용여부, 차/대 구분) 수정.
*   **핵심 기능:** 트리 내 Drag & Drop을 통한 계정 체계 이동(선택 사항), 계정 코드 중복 체크.

### ② 거래처 관리 (Business Partner)
*   **엔티티 연결:** `BusinessPartner.java`, `BusinessPartnerService.java`
*   **UI 구성:**
    *   **상단 (Search Filter):** 거래처명, 사업자번호, 거래처 유형(매입/매출/금융) 필터.
    *   **중앙 (Data Grid):** 거래처 목록 및 상태(활성/비활성), 주요 연락처 정보.
    *   **모달 (Create/Edit):** 세금계산서 발행을 위한 필수 정보(대표자, 업태, 종목, 주소) 입력 폼.

---

## 2. Journal Ledger 관리 Portal (전표 및 원장)

회계 업무의 핵심으로, 대량의 행 데이터를 정확하게 입력하고 추적하는 기능이 핵심입니다.

### ③ 전표 입력 및 조회 (Journal Entry & Inquiry)
*   **엔티티 연결:** `JournalEntry.java`, `JournalDetail.java`
*   **UI 구성:**
    *   **Entry Form:** 마스터-디테일 구조의 전표 입력 폼.
    *   **Inquiry List:** 전표번호, 일자, 상태별 복합 조회 그리드.
*   **핵심 기능:** 대차 합계 실시간 검증, 전표 상태별 워크플로우(승인/반려).

### ④ 자동 분개 룰 설정 (Journal Rule Engine)
*   **엔티티 연결:** `JournalRule.java`, `JournalRuleCondition.java`
*   **UI 구성:**
    *   **Rule Selector:** 외부 시스템 연동 이벤트별 룰 목록.
    *   **Condition Builder:** SpEL 기반 조건을 UI 트리나 칩(Chip) 형태로 구성.
    *   **Debit/Credit Template:** 룰 만족 시 자동 생성될 차/대 분개 템플릿 정의.

---

## 3. System Admin Portal (시스템 관리)

시스템의 안정적인 운영과 보안을 위한 백오피스 관리 기능입니다.

### ⑤ 사용자 및 권한 관리 (User & Permission)
*   **엔티티 연결:** `auth` 서비스의 `User`, `Role` 관련 엔티티.
*   **UI 구성:**
    *   **User Grid:** 이름, 아이디, 소속 부서, 최근 접속일, 계정 상태(활성/잠금).
    *   **Role Mapping Modal:** 사용자에게 특정 직무(회계팀장, 일반사용자, 감사인) 권한을 부여하는 체크박스 리스트.
*   **핵심 기능:** 비밀번호 초기화 기능, 장기 미접속자 휴면 처리 UI.

### ⑥ 귀속부서 관리 (Department / Cost Center)
*   **엔티티 연결:** `Department.java`, `DepartmentService.java`
*   **UI 구성:**
    *   **Org Chart View:** 회사의 조직도를 트리 형태로 조회.
    *   **Attribution Map:** 각 부서별로 회계상 '귀속'되는 상위 부서 및 비용 센터(Cost Center) 매핑 정보 표시.
*   **핵심 기능:** 조직 개편 시 부서 이동 처리, 소속 사용자 리스트 조회.

### ⑩ 기준 정보 승인 관리 (Master Data Approval)
*   **엔티티 연결:** `AccountSubject`, `BusinessPartner`의 상태(Pending/Approved) 정보.
*   **UI 구성:**
    *   **Approval Inbox:** 승인 대기 중인 항목 리스트.
    *   **Diff Viewer:** 변경 전(Before)과 변경 후(After) 데이터를 색상별(Green/Red)로 대비하여 표시.
*   **핵심 기능:** 일괄 승인/반려 처리, 승인 이력 관리.

---

## 4. Ledger Inquiry & Financial Reporting (원장 및 보고서)

### ⑦ 로그 및 접속 기록 조회 (Audit Log)
*   **엔티티 연결:** `governance` 서비스의 `AccessLog`, `SystemLog` 엔티티.
*   **UI 구성:**
    *   **Time Series Filter:** 접속 시간대별 필터링 (최근 1시간, 1일, 1주일).
    *   **Log Viewer:** 에러 로그의 경우 stack trace를 팝업으로 상세 확인 가능.
    *   **Status Indicators:** 접속 성공/실패 여부를 컬러 아이콘으로 시각화.

---

## 4. Ledger Inquiry Portal (원장 조회)

회계 데이터의 최종 결과물을 다양한 관점에서 조회하는 리포팅 기능입니다.

### ⑧ 총계정원장 조회 (General Ledger)
*   **엔티티 연결:** `GlBalance.java`
*   **UI 구성:**
    *   **Summary Cards:** 기초잔액, 기간중 차변/대변 합계, 기말잔액 요약.
    *   **Detail Grid:** 계정별로 정렬된 상세 발생 내역.
*   **핵심 기능:** 엑셀 다운로드, 드릴다운(목록 클릭 시 전표 상세로 이동).

### ⑨ 보조원장 / 거래처원장 (Sub-ledger)
*   **엔티티 연결:** `SlBalance.java`
*   **UI 구성:**
    *   **Mapping Selector:** 특정 계정(예: 외상매입금) + 특정 거래처(또는 전체) 조합 조회.
    *   **Cross Grid:** 거래처별 잔액 현황표.
*   **핵심 기능:** 거래처별 미결(Unsettled) 내역 확인 기능.

### ⑪ 재무제표 보고서 (Financial Statements)
*   **엔티티 연결:** `ClosingService.java` (집계 데이터)
*   **UI 구성:**
    *   **Balance Sheet (BS):** 자산, 부채, 자본의 계층적 시각화 및 전년 대비 증감률 표시.
    *   **Profit & Loss (PL):** 매출, 매출원가, 영업이익 등을 단계별로 집계하여 수익성 분석 제공.
*   **핵심 기능:** 연도/분기별 비교 조회, PDF/Excel 리포트 출력.

---

## 5. Period-End Closing Portal (결산 관리)

회계 기기별 마감 및 이월 작업을 수행하고 상태를 관리합니다.

### ⑫ 결산 프로세스 관리 (Closing Control)
*   **엔티티 연결:** `ClosingService.java`, `AnnualClosingService.java`
*   **UI 구성:**
    *   **Status Timeline:** 전표 마감 → 잔액 확정 → 결산 보고서 생성 → 이월 순의 프로그레스 표시.
    *   **Control Panel:** 마감 실행, 마감 취소(승인자 전용) 버튼.
*   **핵심 기능:** 결산 전 오류(대차 불일치 등) 자동 체크 및 알림.

---

## 6. UI/UX 구현 가이드 (Backend 연동 기반)

*   **Real-time Feedback:** 전표 입력 시 대차 차액이 0이 되지 않으면 '저장' 버튼을 비활성화하고 차액을 실시간으로 상단에 표시합니다.
*   **Search Overlay:** 계정 과목이나 거래처 입력 시 텍스트만 치는 것이 아니라, 오버레이 팝업을 띄워 검색 속도를 높입니다.
*   **Rich Table Features:** 대용량 전표 조회를 위해 가상 스크롤(Virtual Scroll)과 엑셀 다운로드 기능을 필수로 구현합니다.

---

## 4. 초보자를 위한 개념 설명 (UI-Entity 연결)

*   **그리드(Grid):** 엑셀 시트처럼 생긴 표입니다. 부기(Bookkeeping) 업무는 수많은 줄(Line)로 이루어지므로, 웹에서도 엑셀처럼 편하게 입력할 수 있는 그리드 라이브러리가 중요합니다.
*   **마스터-디테일(Master-Detail):** 하나의 전표 머리글(날짜, 제목 등) 아래에 여러 개의 분개 상세(계정, 금액 등)가 붙는 구조입니다. 우리 UI에서도 이를 직관적으로 구분해서 보여주어야 합니다.
*   **검증(Validation):** 사용자가 실수로 잘못된 값을 넣지 못하게 막는 방어막입니다. 특히 회계에서는 '돈의 합계'가 맞는지가 가장 중요하므로 UI에서 이를 가장 먼저 체크합니다.

---

**설계자: [프론트]**
*작성일: 2026-04-22*
