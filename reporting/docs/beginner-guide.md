# reporting beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`reporting`은 흩어져 있는 전표와 원장 데이터를 모아 사람이 읽을 수 있는 의미 있는 재무 보고서(재무상태표, 손익계산서 등)로 만들어주는 핵심 모듈입니다.

## 2. 초보자를 위한 개념 설명 (Beginner's Concept Explanation)

이 시스템이 낯선 초보자 분들을 위해 전문 용어를 일상적인 비유로 설명해 드립니다.

- **보고 라인 (Report Line)**: 가계부의 '식비', '교통비', '주거비' 같은 요약 항목입니다. 수많은 자잘한 지출 내역들을 한 줄로 요약해서 보여줍니다.
- **매핑 (Mapping)**: 영수증의 어떤 항목(계정)을 가계부의 어느 줄(보고 라인)에 넣을지 정해주는 규칙표입니다. (예: '스타벅스 커피 결제 내역' -> '식비'로 분류)
- **스냅샷 (Snapshot)**: 마치 사진을 찍듯, 특정 시점(예: 12월 말)의 재무 상태를 딱 멈춰서 저장해 둔 결과물입니다. 나중에 "그때 그 숫자 어떻게 나온 거야?"라고 물어볼 때 증거로 꺼내볼 수 있습니다. (SCD2 방식으로 관리되어 과거 기록도 안전하게 보존됩니다)
- **드릴스루 (Drill-through)**: 가계부의 '식비 50만 원'이라는 숫자를 콕 누르면, 그 50만 원이 어떤 결제 내역(스타벅스, 마트 등)들로 이루어졌는지 세부 영수증(원천 전표)까지 파고들어 보여주는 기능입니다.

## 3. 핵심 기술 스택 및 아키텍처

우리의 `reporting` 모듈은 최신 기술 표준을 따릅니다:

- **헥사고날 아키텍처 (Ports and Adapters)**: 핵심 비즈니스 로직(보고서 산출 로직)이 외부 기술(웹, DB, 외부 시스템)에 의존하지 않도록 격리되어 있습니다.
- **ID 기반 참조**: 다른 모듈의 데이터를 가져올 때 객체를 직접 참조하지 않고, 고유 식별자(ID)만 참조하여 모듈 간의 결합도를 낮춥니다.
- **SCD2 (Slowly Changing Dimensions)**: 보고서 매핑 규칙이나 계정 정보가 변경되더라도 덮어쓰지 않고 이력을 보존합니다. 이를 통해 과거 특정 시점의 보고서를 정확히 재현할 수 있습니다.
- **Multi-stage Docker 환경**: 개발, 테스트, 운영 환경에 맞춰 일관성 있고 가벼운 컨테이너 배포를 지원합니다.

## 4. 실제 시나리오

### 4.1 재무상태표 실시간 조회 (Port/Adapter 흐름)

1. **[웹 어댑터]** 기준일자를 입력받습니다 (`ReportingController`).
2. **[유스케이스/포트]** 해당 일자에 유효한 매핑 정보를 읽어옵니다 (`GenerateStatementUseCase`).
3. **[아웃바운드 어댑터/포트]** `LedgerClientAdapter`가 `LedgerQueryPort`를 통해 기준일의 GL 잔액 요약을 조회합니다 (`LoadLedgerPort`).
4. **[도메인 모델]** 라인별 금액을 계산하여 `FinancialStatement`를 생성합니다.

### 4.2 스냅샷 생성 및 이력 관리

1. 보고 유형(BS, IS)과 기준일자를 정합니다.
2. 현재 매핑 기준으로 금액을 계산합니다.
3. 스냅샷 헤더와 디테일을 생성합니다.
4. 동일 기준일에 새로 만들면 버전이 올라갑니다 (SCD2 방식 적용).

## 5. 처음 읽는 코드 순서 (헥사고날 구조 기준)

1. **Domain Model**: `reporting/core/src/main/java/com/ho/account/reporting/domain/model/FinancialStatement.java` (비즈니스 객체)
2. **Domain Model**: `reporting/core/src/main/java/com/ho/account/reporting/domain/model/ReportLine.java` (비즈니스 객체)
3. **Inbound Port**: `reporting/core/src/main/java/com/ho/account/reporting/application/port/in/GenerateStatementUseCase.java` (입력 인터페이스)
4. **Application Service**: `reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java` (유스케이스 구현체)
5. **Outbound Port**: `reporting/core/src/main/java/com/ho/account/reporting/application/port/out/LoadLedgerPort.java` (출력 인터페이스)
6. **Outbound Adapter**: `reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java` (외부 연동 구현)
7. **Inbound Adapter**: `reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java` (웹/REST API)

## 6. 자주 헷갈리는 지점

### 6.1 실시간 조회와 스냅샷은 다릅니다
- 실시간 조회는 지금 전표 상태를 기반으로 바로 계산해서 보여줍니다.
- 스냅샷은 과거의 계산 결과를 저장해둔 고정된 데이터입니다.

### 6.2 POSTED 전표만 봅니다
- 작성 중이거나 승인만 된 전표는 집계에서 제외됩니다. `journal-ledger`의 잔액 요약을 통해 장부에 반영된 금액만 취급합니다.

### 6.3 객체 직접 참조 금지 (ID 기반 참조)
- JPA 연관관계를 맺을 때 다른 모듈의 엔티티를 직접 `@ManyToOne` 등으로 묶지 않고, `Long ledgerId`처럼 ID 값만 들고 있어야 합니다.
