# Reporting Module Architecture Guide

## 1. Hexagonal Architecture (Ports & Adapters)
본 모듈은 '핵심 비즈니스'를 '외부 기술'로부터 격리하는 헥사고날 아키텍처를 따릅니다.

### 레이어별 책임
*   **Adapter Layer (External)**:
    *   `api/adapter/in/web`: RESTful API 접점 (`ReportingController`)
    *   `batch/adapter/in/batch`: 야간 자동 배치 작업 (`ReportingBatchAdapter`)
    *   `core/infrastructure/persistence`: DB 및 타 모듈 연동 구현체 (`LedgerClientAdapter`)
*   **Application Layer (Internal)**:
    *   `port.in`: 외부에서 진입하는 규격 (`GenerateStatementUseCase`)
    *   `port.out`: 내부에서 외부로 요청하는 규격 (`LoadLedgerPort`, `LoadReportHistoryPort`)
    *   `service`: 유즈케이스 흐름 제어 (`ReportingService`)
*   **Domain Layer (Heart)**:
    *   `model`: 핵심 엔티티 및 비즈니스 규칙 (`FinancialStatement`, `ReportLine`)

## 2. DDD (Domain-Driven Design) 전략
*   **Aggregate Root**: `FinancialStatement`가 애그리거트 루트입니다. 보고서 항목(`ReportLine`)의 추가 및 금액 확정은 반드시 루트를 통해서만 이루어집니다.
*   **Rich Domain Model**: 엔티티가 단순히 데이터만 담는 것이 아니라, `finalizeStatement()`와 같이 비즈니스 정합성을 스스로 검증하는 행위를 가집니다.
*   **Ubiquitous Language**: '당기', '전기', '주석 번호' 등 실제 회계 업무에서 사용하는 용어를 코드에 직접 반영하였습니다.

## 3. 대외 보고서 처리 프로세스
1.  **Request**: 웹 요청 또는 배치 스케줄에 의한 트리거.
2.  **Data Loading**: 인접 모듈(`journal-ledger`) 및 과거 히스토리 DB에서 데이터 로드.
3.  **Core Logic**: 계정별 합산, 비교식 재무제표 산출, 주석 번호 매핑.
4.  **Finalization**: 데이터 정합성 검토 및 보고서 상태 확정.
