# 대손충당금(IFRS 9) 핵심 개념

## 핵심 변수

| 약어 | 의미 | 사용 위치 |
| --- | --- | --- |
| PD | 부도율 | Stage/등급 기반 산출 |
| LGD | 부도 시 손실률 | 담보 회수 가능성 반영 |
| EAD | 부도 시 노출액 | 잔액과 미사용 한도에 CCF 적용 |
| ECL | 기대신용손실 | `EAD * PD * LGD`를 기간/시나리오별로 반영 |

## IFRS 9 Stage

- Stage 1: 정상 자산, 12개월 ECL.
- Stage 2: 유의적 악화 자산, Lifetime ECL.
- Stage 3: 손상 자산, Lifetime ECL.

## 미래전망 ECL

`ForwardLookingEclService`는 기준, 호황, 침체 시나리오별 ECL을 산출하고 가중평균해 `weighted_ecl`을 생성합니다. 이 값이 회계 대손충당금 summary의 원천입니다.
