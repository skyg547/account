# Asset-Lease Process Flow

## 1. 전체 흐름

```mermaid
flowchart TD
    A[고정자산 등록] --> B[취득 분개 생성]
    B --> C[월별 감가상각]
    C --> D[자산 처분]

    E[리스 계약 등록] --> F{IFRS 16 대상인가?}
    F -- 아니오 --> G[지급성 비용 처리만 수행]
    F -- 예 --> H[사용권자산/리스부채 초기 인식]
    H --> I[리스 지급 스케줄 생성]
    I --> J[월별 리스 회계 처리]
    J --> K[필요 시 재측정]
    K --> I
```

## 2. 고정자산 취득 흐름

```mermaid
sequenceDiagram
    participant API as FixedAssetController
    participant Service as FixedAssetService
    participant Repo as FixedAssetRepository
    participant Journal as JournalService

    API->>Service: registerAsset(request)
    Service->>Service: 현재장부가/상각기준 초기화
    Service->>Repo: FixedAsset 저장
    Service->>Journal: 취득 분개 생성
    Note over Journal: 차변 자산계정\n대변 10100 현금/예금
```

설명:
- 자산코드, 취득가액, 내용연수, 감가상각방법, 자산계정, 상각누계액계정, 감가상각비 계정을 넣어 자산을 등록합니다.
- 등록 직후 취득 분개를 만듭니다.

## 3. 월별 감가상각 흐름

```mermaid
flowchart LR
    A[ACTIVE 자산 조회] --> B[월 상각액 계산]
    B --> C[감가상각 분개 생성]
    C --> D[상각누계액/장부가 업데이트]
    D --> E{잔존가치 이하인가?}
    E -- 예 --> F[FULLY_DEPRECIATED]
    E -- 아니오 --> G[ACTIVE 유지]
```

설명:
- `STRAIGHT_LINE`은 자산에 계산된 기간 상각액을 사용합니다.
- `DECLINING`은 서비스에서 장부가 기준으로 계산합니다.
- 감가상각 분개는 생성 후 승인 요청과 승인까지 바로 수행합니다.

## 4. 자산 처분 흐름

```mermaid
flowchart TD
    A[disposeFixedAsset] --> B[장부가 계산]
    B --> C[매각대금과 비교]
    C --> D[상태 DISPOSED]
    D --> E[처분 분개 생성]
    E --> F[차변 상각누계액 제거]
    E --> G[대변 취득원가 제거]
    E --> H[차변 현금]
    E --> I[이익 또는 손실 계정 반영]
```

설명:
- 장부가와 매각대금을 비교해 처분이익 또는 처분손실을 계산합니다.
- 현재 구현은 분개 생성 후 승인 요청과 승인까지 즉시 수행합니다.

## 5. IFRS 16 초기 인식

```mermaid
sequenceDiagram
    participant API as LeaseAccountingController
    participant Lease as LeaseService
    participant Accounting as LeaseAccountingService
    participant Journal as JournalService

    API->>Lease: createLeaseContract(request)
    Lease->>Lease: 계약 저장
    alt IFRS16 적용 + 단기/소액 예외 아님
        Lease->>Accounting: recognizeInitialLease(contract)
        Accounting->>Accounting: ROU Asset 생성
        Accounting->>Accounting: Lease Liability 생성
        Accounting->>Accounting: 지급 스케줄 생성
        Accounting->>Journal: 초기 인식 분개 생성
    end
```

설명:
- IFRS 16 대상이고 단기리스/소액리스 예외가 아니면 자동으로 초기 인식이 수행됩니다.
- 초기 인식 분개는 시작일 전날 기준으로 작성됩니다.

## 6. IFRS 16 월별 처리

```mermaid
flowchart TD
    A[월 마감일 처리] --> B[ACTIVE IFRS16 계약 조회]
    B --> C[당월 스케줄 선택]
    C --> D[ROU 감가상각 반영]
    C --> E[리스부채 이자/원금 반영]
    D --> F[감가상각 분개]
    E --> G[지급 분개]
    F --> H[스케줄 상태 PAID]
    G --> H
```

분개 구조:
- 감가상각 분개
  - 차변 `51500` 감가상각비
  - 대변 `12399` 감가상각누계액
- 지급 분개
  - 차변 `93100` 이자비용
  - 차변 `25100` 리스부채
  - 대변 `10100` 현금/예금
- `LeasePaymentResolutionPort` 연계 시에도 상환 스케줄의 `interestPortion`과 `principalPortion`을 별도 차변 라인으로 전달한다.

## 7. IFRS 16 재측정

```mermaid
flowchart LR
    A[remeasureLease] --> B[미래 스케줄 삭제]
    B --> C[월지급액/종료일/할인율 갱신]
    C --> D[미래 지급 현재가치 재계산]
    D --> E[리스부채 현재가치 수정]
    E --> F[ROU 자산 장부가 조정]
    F --> G[새 스케줄 생성]
    G --> H[재측정 분개 생성]
```

설명:
- 재측정 시 과거 스케줄은 유지하고, 기준일 이후 미래 스케줄만 삭제 후 다시 만듭니다.
- 조정액이 0이면 분개를 만들지 않습니다.

## 8. 현재 구현상 주의점

- 고정자산과 리스가 한 모듈에 같이 있어 경계가 넓습니다.
- 일부 분개는 `create -> requestApproval -> approve`까지 한 서비스에서 끝납니다.
- IFRS 16 계정코드가 하드코딩되어 있어 계정체계 변경에 취약합니다.
- 리스 비용지출 연계는 계약의 `paymentDay`와 배치 날짜가 일치할 때만 생성됩니다.
