# 대손충당금(IFRS 9) 입문 가이드

은행은 대출을 보유하는 동안 앞으로 못 받을 수 있는 금액을 미리 비용으로 인식해야 합니다. IFRS 9 대손충당금은 이 예상 손실을 기준일마다 계산하는 절차입니다.

## 한 문장으로 이해하기

`account-mart`가 기준일의 대출 상태를 사진처럼 고정하면, `ecl`이 고객의 부도 가능성과 회수 가능 금액을 계산하고, `closing`이 그 결과를 회계 전표로 반영합니다.

## 계산 재료

| 개념 | 초보자 설명 | 간단한 예 |
| --- | --- | --- |
| PD | 고객이 부도날 확률 | PD 2%는 비슷한 고객 100명 중 약 2명이 부도날 수 있다는 의미입니다. |
| LGD | 부도 후에도 회수하지 못할 비율 | 100만 원 중 담보 처분 후 45만 원을 잃으면 LGD는 45%입니다. |
| EAD | 부도 시점에 노출될 금액 | 현재 잔액과 부도 전 추가 사용 예상 한도를 합칩니다. |
| ECL | 예상 손실 금액 | 기본 구조는 `EAD * PD * LGD`이며 기간·시나리오를 추가 반영합니다. |

## 업무와 데이터 흐름

1. `allowance_exposure_snapshots`가 기준일 입력을 고정합니다.
2. 데이터 품질 검증이 필수값 누락과 산출 불가 대상을 차단합니다.
3. Stage와 PD를 정하고, 상품 CCF와 담보를 반영해 EAD/LGD를 계산합니다.
4. 미래전망 시나리오를 가중평균해 계좌별 `weighted_ecl`을 확정합니다.
5. 완료된 결과를 계정 매핑과 결합해 `allowance_summary`를 만듭니다.
6. `closing`은 목표 충당금과 현재 원장 잔액의 차이만 전표로 반영합니다.

## 개발자가 볼 코드

| 개념 | 코드 |
| --- | --- |
| 입력 snapshot 동기화 | `AllowanceExposureSyncService` |
| Stage 판정 | `StagingService` |
| PD/LGD/EAD 산출 | `PdCalculationService`, `LgdCalculationService`, `EadCrmCalculationService` |
| 미래전망 ECL | `ForwardLookingEclService` |
| 회계 summary | `AllowanceSummaryService` |

## EAD 계산 코드를 읽는 방법

- `EadCalculator`는 순수 계산을 담당합니다.
- 계산 결과는 배열이 아니라 `EadCalculationResult` 값 객체로 반환합니다.
- `securedLgdFloor`, `unsecuredLgdFloor`, `defaultCcfRate`는 `AllowanceModelParams`라는 이름 있는 모델 정책으로 전달됩니다.
- 필수 LGD나 CCF 비율이 누락·범위 초과이면 임의 숫자로 계산하지 않고 실패합니다.
- 상품 마스터에 CCF가 없을 때만 `DEFAULT_CCF_RATE` 정책값을 사용합니다.

## 검증 포인트

- 기준일 입력 snapshot 건수와 `cr_accounts` 동기화 건수가 맞는지 확인합니다.
- Stage별 weighted ECL 합계가 `allowance_summary`의 목표 충당금과 일치해야 합니다.
- 상품/사업부/통화별 계정 매핑이 없으면 summary 생성이 실패해야 합니다.
- 모델 비율은 0~1 사이여야 하며, 필수 LGD/PD 파라미터 누락 시 배치가 실패해야 합니다.
- 같은 `baseDate`, `runId`, `modelVersion`으로 재실행해도 중복 결과가 생기지 않아야 합니다.

상세 업무 단계는 [ALLOWANCE_PROCESS_FLOW.md](ALLOWANCE_PROCESS_FLOW.md), 실행 절차는 [ALLOWANCE_SERVICE_RUNBOOK.md](ALLOWANCE_SERVICE_RUNBOOK.md)를 참고합니다.
