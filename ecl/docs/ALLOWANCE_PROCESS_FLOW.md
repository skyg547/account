# 대손충당금(IFRS 9) 산출 흐름

이 문서는 현재 구현 범위인 IFRS 9 대손충당금 산출 흐름만 설명합니다.

## Job

- 표준 Job: `allowanceEclJob`
- 입력: `allowance_exposure_snapshots`
- 출력: `allowance_ecl_results`, `allowance_summary`

## 순서

| 순서 | 단계 | 내용 |
| ---: | --- | --- |
| 1 | Exposure Sync | snapshot을 ECL 입력 계좌/고객으로 동기화 |
| 2 | DQ | 필수값과 산출 가능 상태 검증 |
| 3 | Stage/PD | IFRS 9 Stage와 PD 산출 |
| 4 | EAD/LGD | CCF, 담보 회수 가능성, LGD 반영 |
| 5 | Weighted ECL | 미래전망 시나리오 가중 평균 ECL 산출 |
| 6 | Completion | 완료 상태 확정 |
| 7 | Summary | 회계 대손충당금 summary 재생성 |

