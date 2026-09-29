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
| 일마감 상태 | `DailyClosingStatus`, `EodState` | 영업일별 EOD/BOD 상태와 처리자·시각을 보존합니다. |

## 일·월·연 결산 모델

| 범위 | 권위 모델 | 설명 |
| --- | --- | --- |
| 일 | `DailyClosingStatus` | 날짜별 EOD/BOD 상태 머신과 감사 필드 |
| 월/기간 | `ClosingCalendar` + Master Data `FiscalPeriod` | 태스크·게이트와 실제 회계기간 잠금 상태 |
| 연 | `ClosingCalendar(fiscalPeriod=YEAR)` + `AnnualClosingService` | 연간 진행표와 손익 대체 전표 생성 |

사용되지 않던 setter 기반 `ClosingPeriod`는 월/연 상태를 이중으로 저장하므로 제거했습니다.

## 초보자가 봐야 할 데이터 흐름

1. `master-data`가 회계기간을 관리합니다.
2. `closing`은 회계기간 ID를 받아 결산 캘린더, 태스크, 게이트, 잠금, 재오픈 승인을 기록합니다.
3. `journal-ledger`는 실제 전표와 GL 잔액을 관리합니다.
4. `ecl`은 IFRS 9 Stage/PD/LGD/EAD 계산 결과를 `allowance_summary`에 확정합니다.
5. `closing:batch`는 `allowance_summary`와 실제 전기된 충당금 잔액을 같은 통화끼리 비교해 차이만 전표로 만듭니다.

## 변경 요청의 입력·처리·결과와 재시도

일반 Closing 변경 요청은 다음 순서로 처리됩니다.

1. **입력:** 호출자는 캘린더, 상태, 사유, 계정 코드 같은 업무 값과 JWT를 Gateway에 보냅니다.
   Gateway는 외부가 보낸 `X-Auth-*`를 제거하고, 검증한 JWT에서 `X-Auth-User`와
   `X-Auth-Roles`를 다시 만듭니다. JSON/query의 `user`, `requestedBy`, `approvedBy`, `runBy`는
   신원이 아니므로 보내도 인증 주체나 감사 주체가 되지 않습니다.
2. **처리:** Closing API는 actor가 있는지와 역할 목록에 `ROLE_ADMIN`,
   `ROLE_ACCOUNTING_ADMIN`, `ROLE_CLOSING_MANAGER` 중 하나가 있는지 먼저 확인합니다. 통과한 뒤에만
   기존 회계기간, 상태 전이, 체크리스트, 금액 등 업무 규칙을 실행합니다.
3. **출력:** 성공하면 변경된 업무 상태와 감사 기록에는 `X-Auth-User`의 주체가 남습니다. 재오픈은
   요청 호출의 헤더 주체와 승인·반려 호출의 헤더 주체가 달라야 합니다.
4. **실패:** actor가 없으면 401, 역할이 없거나 허용되지 않으면 403입니다. 권한 통과 뒤에는 기존
   업무 규칙에 따라 4xx/5xx가 날 수 있으며, 이를 권한 성공이나 업무 성공으로 바꾸어 해석하지 않습니다.
5. **재시도:** 401/403이면 JSON에 사용자 이름을 추가하지 말고 Gateway 인증과 역할 부여를 바로잡은
   뒤 다시 요청합니다. 네트워크 단절처럼 결과가 불명확하면 먼저 조회·감사 기록으로 반영 여부를
   확인합니다. 일반 생성·변경 명령 전체가 멱등이라고 가정해 무조건 재전송하면 중복이나 충돌이 날 수
   있습니다. 월말 전이 복구는 작업 ID 기반의 별도 절차를 따릅니다.

이 권한 경계는 캘린더·태스크·게이트, 잠금·해제, 재오픈, HTTP 평가·충당, 조정, 마감 판정,
연차 결산 변경에 적용됩니다. 기존 EOD 통제는 유지되고, GET 조회나 admission 조회 권한은 이번
변경으로 넓어지지 않습니다. HTTP 평가·충당 권한도 Spring Batch 스케줄러나 Job 실행 권한과는
별개입니다. API는 Gateway 뒤에만 두어야 하며, 로컬에서 임의 헤더로 직접 호출한 결과는 기능 확인이지
Gateway의 헤더 재작성과 네트워크 차단을 증명하는 보안 테스트가 아닙니다.

ECL 목표가100달러라면 기존 충당금80달러와 비교해야 합니다. 장부의 원화 금액104,000원을
100달러에서 빼면 안 됩니다. 환율1,300에서 추가 전표는20달러/26,000원입니다.
환율이1,400으로 바뀌었다면 기존80달러의 장부를 먼저112,000원으로 FX 평가·전기하고
추가20달러/28,000원을 기록합니다. 평가 전에는 ECL을 실패시켜 단위가 섞이는 것을 막습니다.
[계산과 재실행 예시](process-flow.md#ecl-거래통화와-기능통화-대사-gh-781)를 참고하세요.

FX 평가는 아직 별도 외화/기준통화 잔액 테이블을 신뢰하지 않습니다. 실제 전기된 `journal_entries`와 `journal_details`에서 거래통화 금액과 기준통화 금액을 함께 집계합니다. 이 방식은 원장과 맞지 않는 유령 잔액을 피하지만, 1억 건 운영 전에는 전기 시 함께 갱신되고 원장과 자동 대사되는 이중통화 read model로 교체해야 합니다.

이미 기록한 평가손익도 다음 평가의 장부금액에 포함합니다. USD100을 KRW1,000에 취득한 뒤
KRW200을 평가이익으로 전기했다면 장부금액은 KRW1,200이고 달러 원금은 여전히 USD100입니다.
다음 달 환율이 그대로12이면 추가 평가이익은 0입니다. 역분개가 실제 전기되면 그 반대 금액도
같은 원천통화의 장부금액에 반영합니다. 초안 전표는 아직 장부에 반영하지 않습니다.
통화별 구분과 재실행 전제는 [FX 평가 업무 흐름](process-flow.md#이전-평가를-포함한-장부금액-gh-779)을 참고하세요.

외화로 기록된 모든 계정을 다시 평가하지는 않습니다. 예를 들어 달러로 받은 매출은
현금과 수익 양쪽에 기록되지만, 기말 환율로 평가하는 것은 명시적으로 화폐성으로 지정한
현금입니다. 과거에 인식한 수익과 역사적 원가 고정자산은 평가에서 제외합니다.
계정마다 적용 시작일·종료일과 `MONETARY` 또는 `HISTORICAL_COST` 정책을 등록해야 하며,
설정이 빠지면 임의로 계산하지 않고 실패합니다. [정책과 예시](process-flow.md#평가-대상-계정의-유효일자-정책-gh-780)를 참고하세요.

`closing`이 ECL 모델 계산을 다시 하지 않는 이유는 책임 분리 때문입니다. ECL 산출은 `ecl`의 도메인이고, `closing`은 확정된 회계 금액을 결산 전표로 연결하는 도메인입니다.

## 왜 전표가 기본 DRAFT인가

FX 평가와 ECL 충당 배치는 기본적으로 `DRAFT` 전표를 생성합니다. 결산 전표는 금액이 크고 재실행 가능성이 중요하므로, 운영자가 검토하고 승인/전기하는 흐름을 기본값으로 둡니다.

자동 승인/전기는 `account.closing.accounting.auto-post-adjustments=true`일 때만 허용합니다. 로컬이나 검증 환경에서는 이 값을 기본 `false`로 두는 편이 안전합니다.

## 코드 위치

| 관심사 | 위치 |
| --- | --- |
| REST API | `closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java` |
| EOD/BOD REST API | `closing/api/src/main/java/com/ho/account/closing/web/EodLifecycleController.java` |
| 유즈케이스 흐름 | `closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java` |
| EOD/BOD 유즈케이스 | `closing/core/src/main/java/com/ho/account/closing/application/service/EodLifecycleService.java` |
| 연차 손익 대체 | `closing/core/src/main/java/com/ho/account/closing/application/service/AnnualClosingService.java` |
| 출력 포트 | `closing/core/src/main/java/com/ho/account/closing/application/port/out` |
| 도메인 상태 규칙 | `closing/core/src/main/java/com/ho/account/closing/domain` |
| JPA 어댑터 | `closing/core/src/main/java/com/ho/account/closing/infrastructure/persistence` |
| FX 평가 Batch | `closing/batch/src/main/java/com/ho/account/closing/batch/config/FxValuationBatchConfig.java` |
| ECL 충당 Batch | `closing/batch/src/main/java/com/ho/account/closing/batch/config/EclProvisionBatchConfig.java` |

## 운영 전에 확인할 것

- 모든 필수 태스크가 `COMPLETED`인지 확인합니다.
- 모든 게이트가 `PASSED`인지 확인합니다.
- 태스크/게이트 정의가 최소 한 개씩 존재하고 JSON 조건은 실제 evidence evaluator로 검증되는지 확인합니다.
- 결산 조정 전표의 회계일자가 대상 회계기간 안에 있는지 확인합니다.
- ECL 충당 전표 실행 전 동일 기준일·단일 run/model·단일 법인의 `allowance_summary`가 생성되어 있는지 확인합니다. 현재 GL에는 법인 차원이 없어서 여러 법인을 한 실행에 섞으면 실패합니다.
- FX 평가 전 기준일 환율, 전기된 외화 잔액, 기준일 유효 계정 정보와 계정별 명시적 평가 정책을 확인합니다.
- 재오픈 요청과 결정의 `X-Auth-User`가 서로 다른지, 동일 기간에 대기 중인 요청이 없는지 확인합니다.
- 일반 Closing 변경과 EOD 명령은 Gateway가 검증해 만든 actor/role 헤더를 통해서만 호출하고 서비스 포트를 외부에 노출하지 않습니다.
- 일마감 상태의 `transactionAllowed=false`는 도메인 판단입니다. Journal 전표 생성 게이트에 연결되기 전까지는 월 회계기간 잠금과 별도로 운영 통제가 필요합니다.
