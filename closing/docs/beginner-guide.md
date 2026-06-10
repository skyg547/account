# Closing 입문 가이드

결산은 한 달 또는 1년 동안 쌓인 장부를 확인하고, 더 이상 임의로 바뀌지 않도록 확정하는 업무입니다. 이 모듈은 장부를 직접 계산하는 모듈이 아니라, 마감 전 체크리스트와 승인 흐름을 관리하고 필요한 결산 조정 전표를 만들도록 다른 모듈과 협업합니다.

## 핵심 용어

| 용어 | 코드 | 쉬운 설명 |
| --- | --- | --- |
| 결산 캘린더 | `ClosingCalendar` | 특정 회계연도/기간의 마감 진행표입니다. |
| 결산 태스크 | `ClosingTask` | 마감 전 끝내야 하는 체크리스트 한 줄입니다. 예: 은행잔고 대조, ECL 산출 완료 |
| 결산 게이트 | `ClosingGate` | 다음 단계로 넘어가기 전에 반드시 통과해야 하는 문입니다. |
| 기간 잠금 | `PeriodLock` | 해당 회계기간에 일반 전표가 들어오지 못하게 막는 잠금 기록입니다. |
| 재오픈 승인 | `ReopenApproval` | 이미 닫은 기간을 다시 열기 위한 승인 기록입니다. |
| 평가 배치 | `ValuationBatch` | 외화 평가처럼 결산 시점에 평가 조정이 필요한 실행 기록입니다. |
| 충당 배치 | `ProvisionBatch` | ECL 등 충당금 보충/환입을 처리한 실행 기록입니다. |
| 결산 조정 | `ClosingAdjustment` | 마감 중 승인된 조정 전표와 회계기간을 연결하는 기록입니다. |

## 초보자가 봐야 할 데이터 흐름

1. `master-data`가 회계기간을 관리합니다.
2. `closing`은 회계기간 ID를 받아 결산 캘린더, 태스크, 게이트, 잠금, 재오픈 승인을 기록합니다.
3. `journal-ledger`는 실제 전표와 GL 잔액을 관리합니다.
4. `ecl`은 IFRS 9 Stage/PD/LGD/EAD 계산 결과를 `allowance_summary`에 확정합니다.
5. `closing:batch`는 `allowance_summary`와 GL 기존 충당금 잔액의 차이만 전표로 만듭니다.

`closing`이 ECL 모델 계산을 다시 하지 않는 이유는 책임 분리 때문입니다. ECL 산출은 `ecl`의 도메인이고, `closing`은 확정된 회계 금액을 결산 전표로 연결하는 도메인입니다.

## 왜 전표가 기본 DRAFT인가

FX 평가와 ECL 충당 배치는 기본적으로 `DRAFT` 전표를 생성합니다. 결산 전표는 금액이 크고 재실행 가능성이 중요하므로, 운영자가 검토하고 승인/전기하는 흐름을 기본값으로 둡니다.

자동 승인/전기는 `account.closing.accounting.auto-post-adjustments=true`일 때만 허용합니다. 로컬이나 검증 환경에서는 이 값을 기본 `false`로 두는 편이 안전합니다.

## 코드 위치

| 관심사 | 위치 |
| --- | --- |
| REST API | `closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java` |
| 유즈케이스 흐름 | `closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java` |
| 연차 손익 대체 | `closing/core/src/main/java/com/ho/account/closing/application/service/AnnualClosingService.java` |
| 출력 포트 | `closing/core/src/main/java/com/ho/account/closing/application/port/out` |
| 도메인 상태 규칙 | `closing/core/src/main/java/com/ho/account/closing/domain` |
| JPA 어댑터 | `closing/core/src/main/java/com/ho/account/closing/infrastructure/persistence` |
| FX 평가 Batch | `closing/batch/src/main/java/com/ho/account/closing/batch/config/FxValuationBatchConfig.java` |
| ECL 충당 Batch | `closing/batch/src/main/java/com/ho/account/closing/batch/config/EclProvisionBatchConfig.java` |

## 운영 전에 확인할 것

- 모든 필수 태스크가 `COMPLETED`인지 확인합니다.
- 모든 게이트가 `PASSED`인지 확인합니다.
- 결산 조정 전표의 회계일자가 대상 회계기간 안에 있는지 확인합니다.
- ECL 충당 전표 실행 전 `allowance_summary`가 기준일별로 생성되어 있는지 확인합니다.
- FX 평가 전 기준일 환율과 외화 GL 잔액이 준비되어 있는지 확인합니다.
