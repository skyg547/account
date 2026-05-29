# 대손충당금(IFRS 9) 코어 모듈

`ecl-core`는 IFRS 9 기대신용손실(ECL)과 회계 대손충당금 summary를 산출하는 core 모듈입니다.

## 업무 흐름

| 단계 | 내용 | 주요 컴포넌트 |
| ---: | --- | --- |
| 1 | account-mart snapshot을 ECL 입력 계좌/고객으로 동기화 | `AllowanceExposureSyncService` |
| 2 | 입력 데이터 품질 검증 | `AllowanceDataQualityService` |
| 3 | IFRS 9 Stage 및 PD 산출 | `StagingService`, `PdCalculationService` |
| 4 | CCF, EAD, LGD 산출 | `CcfCalculationService`, `EadCrmCalculationService`, `LgdCalculationService` |
| 5 | 미래전망 weighted ECL 산출 | `ForwardLookingEclService` |
| 6 | 완료 상태 확정 및 회계 summary 재생성 | `AllowanceEclCompletionService`, `AllowanceSummaryService` |

## 원칙

- 금액과 비율 계산은 `BigDecimal` 중심으로 처리합니다.
- application service는 유즈케이스 흐름과 트랜잭션 경계를 담당합니다.
- domain calculator는 산식과 값 검증만 담당합니다.
- infrastructure adapter는 JPA/JDBC 구현 세부사항을 숨깁니다.

## 테스트 위치

- `application/service/allowance`: 회계 대손충당금 유즈케이스 테스트.
- `application/service/calculation`: PD, LGD, EAD, 미래전망 ECL 단위 테스트.
- `domain/calculator`: 순수 산식 테스트.
