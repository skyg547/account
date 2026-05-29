# 대손충당금(IFRS 9) 입문 가이드

은행은 대출을 보유하는 동안 앞으로 못 받을 수 있는 금액을 미리 비용으로 인식해야 합니다. IFRS 9 대손충당금은 이 예상 손실을 기준일마다 계산하는 절차입니다.

## 계산 재료

- PD: 고객이 부도날 확률.
- LGD: 부도 시 회수하지 못할 비율.
- EAD: 부도 시점의 노출액.
- ECL: 예상 손실 금액.

## 개발자가 볼 코드

| 개념 | 코드 |
| --- | --- |
| 입력 snapshot 동기화 | `AllowanceExposureSyncService` |
| Stage 판정 | `StagingService` |
| PD/LGD/EAD 산출 | `PdCalculationService`, `LgdCalculationService`, `EadCrmCalculationService` |
| 미래전망 ECL | `ForwardLookingEclService` |
| 회계 summary | `AllowanceSummaryService` |

## 검증 포인트

- 기준일 입력 snapshot 건수와 `cr_accounts` 동기화 건수가 맞는지 확인합니다.
- Stage별 weighted ECL 합계가 `allowance_summary`의 목표 충당금과 일치해야 합니다.
- 상품/사업부/통화별 계정 매핑이 없으면 summary 생성이 실패해야 합니다.
