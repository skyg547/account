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

API로 실행해도 금액을 직접 입력하지 않습니다. 평가 API는 `FX_RATE`, 충당 API는 `ECL`만
받고, Batch와 같은 core 계산 규칙 및 `POSTED` 전표 집계 SQL로 근거를 다시 읽습니다. 즉
운영자가 임의의 고정 금액을 넣어 전표를 만드는 우회 경로는 없습니다.

ECL 목표가100달러라면 기존 충당금80달러와 비교해야 합니다. 장부의 원화 금액104,000원을
100달러에서 빼면 안 됩니다. 환율1,300에서 추가 전표는20달러/26,000원입니다.
환율이1,400으로 바뀌었다면 기존80달러의 장부를 먼저112,000원으로 FX 평가·전기하고
추가20달러/28,000원을 기록합니다. 평가 전에는 ECL을 실패시켜 단위가 섞이는 것을 막습니다.
[계산과 재실행 예시](process-flow.md#ecl-거래통화와-기능통화-대사-gh-781)를 참고하세요.

FX 평가는 아직 별도 외화/기준통화 잔액 테이블을 신뢰하지 않습니다. 실제 전기된 `journal_entries`와 `journal_details`에서 거래통화 금액과 기준통화 금액을 함께 집계합니다. 이 방식은 원장과 맞지 않는 유령 잔액을 피하지만, 1억 건 운영 전에는 전기 시 함께 갱신되고 원장과 자동 대사되는 이중통화 read model로 교체해야 합니다.

연차 손익 대체는 요청자가 이익잉여금 계정을 직접 적는 작업이 아닙니다. API에는
`{"year": 2026}`처럼 연도만 넣고, 운영자가 미리 검토·승인한 `account.closing.annual`
설정에서 그 연도의 목적지를 찾습니다. 쉽게 말해, 이체할 때마다 계좌번호를 손으로
입력하는 대신 승인된 배포 명부에서 정확한 연도의 계좌를 고르는 방식입니다.

승인 규칙에는 단일 법인, 연도, 계정 코드, `postable: true`, 승인자, 변경 참조가
필요합니다. 다만 Master Data는 계정의 실제 postability 필드를 제공하지 않으므로,
`postable: true`는 시스템이 계정 Master에서 발견한 사실이 아니라 배포 검토자의 승인
확인입니다. 시스템은 연도 12월 31일에 그 계정이 실제로 존재하고 코드가 정확하며
`EQUITY`/대변 정상잔액인지를 따로 확인합니다. 자산·부채·수익·비용·알 수 없는 분류이거나
차변 정상잔액이면 Journal을 읽거나 쓰기 전에 실패합니다. 이익이든 손실이든 순액 0이든
이 검증을 건너뛰지 않습니다.

Journal 전표에는 법인 차원이 없습니다. 따라서 하나의 Closing 런타임을 여러 법인용으로
공유하면 안 되며, 법인별로 분리해야 합니다. 설정은 실행 중 수정하는 업무 테이블이 아니라
버전 관리된 배포 입력입니다. 변경할 때는 연차 요청을 멈추고(drain) 모든 instance를
재시작합니다. 새 테이블이 없으므로 이 변경을 위한 DB migration도 없습니다.

연차 손익 대체의 source snapshot은 "이 계산에 사용한 원장과 승인 설정의 지문"입니다. 연도 안의
`POSTED` 전표 헤더와 상세(금액·계정·분류 포함)를 정렬해 식별하므로, 매출 1,000으로 만든 초안 뒤
매출이 1,500으로 바뀌면 예전 초안을 그대로 성공 처리하지 않습니다. 기존 초안을 자동 수정하거나
새 초안으로 덮지 않고 충돌을 내므로, 운영자가 원천과 초안의 lineage·라인을 대사해야 합니다.
승인자나 변경 참조를 바꾸는 설정 개정도 지문을 바꾸므로 pending 초안은 stale가 됩니다.
반면 이미 정확한 금액이 `POSTED`되어 남은 대체액이 0이면 승인 metadata만 바뀌었다고
새 금액 전표를 만들지는 않습니다.

이미 1,000을 대체한 전표가 `POSTED`된 뒤 기존 승인 흐름으로 재오픈되어 매출 500이 추가되면,
기존 1,000을 다시 대체하지 않고 500만 새 delta 초안으로 만듭니다. 연차 서비스 자체가 재오픈 승인을
조회하는 것은 아니며, 재오픈 뒤 허용된 원천 활동의 잔여 금액을 통제하는 계산입니다. 기존 전표도
헤더와 모든 라인이 유효한지 먼저 검사합니다. 예전 `lineageSourceId=연도` 전표는 `POSTED`만 호환하고,
그 금액 역시 누계에 포함합니다.

Journal 상세에 지원 `accountCategory`가 있으면 그대로 쓰고, 없으면 계정 코드와 상세 회계일자로
Master Data의 유효 계정을 조회합니다. 계정이나 분류를 찾을 수 없거나 다른 계정이 반환되거나 분류가
비어 있거나 지원되지 않으면 추정하지 않고 실패합니다. `DRAFT`/`POSTED` 이외 연차 상태도 실패합니다.

이 지문은 Journal 공급자가 보장하는 원자적 DB snapshot이 아닙니다. 현재는 summary를 읽은 뒤 전표별
상세를 다시 읽으므로 중간 변경 가능성과 N+1 비용이 있습니다. 로컬 회귀 테스트는 금액·계정·분류 변경과
delta 계산을 확인하지만 실제 PostgreSQL, 분산 장애, 운영 부하는 증명하지 않습니다.

독립 Journal은 쓰기 요청에 신뢰된 `X-Auth-User`/`X-Auth-Roles` service principal을 요구하지만 현재
Closing HTTP 어댑터는 maker 헤더를 보내지만 신뢰된 주체의 인증을 증명하지 못합니다. 따라서 로컬·통제 포트 테스트가 실제 원격 초안 생성을
증명하지 않으며, 별도 승인된 인증 통합 전에는 배포 API 흐름이 검증됐다고 보면 안 됩니다.

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

`auto-post-adjustments`의 기본값은 `false`입니다. `dev` 원격 Journal 연결에서는 이 값을
`true`로 바꾸어도 자동 승인/전기를 하지 않습니다. Closing이 Journal의 maker 승인 요청,
다른 사람의 checker 승인, poster 전기를 안전하게 이어 줄 서비스 주체 계약을 갖추지 못했으므로
첫 원격 전표 쓰기 전에 실패합니다. `false`로 실행해 만든 DRAFT는 권한 있는 담당자가
Journal의 정상 승인 절차에 따라 검토합니다.

API 실행 이력은 전표가 없거나 지원되는 어댑터에서 실제 자동 전기된 경우 `COMPLETED`, DRAFT 전표가 있으면
`PENDING_APPROVAL`, 처리 중 예외가 나면 `FAILED`입니다. 여러 전표가 생성되어도 기존 이력
테이블에는 ID 한 개만 담을 수 있으므로 `generated_journal_entry_id`는 `null`입니다.

FX Batch는 먼저 전표를 쓰지 않는 전체 검사를 하고, 성공한 뒤 partition/cursor/chunk 전기를
시작합니다. 재시작 시에도 검사를 다시 합니다. 다만 검사와 전기 사이를 묶는 분산 snapshot은
없으므로 운영자는 실행 동안 원장, 기준일 환율, 평가 정책을 변경하지 않아야 합니다.

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
- `dev`에서 실제 금융 실행을 검증할 때만 승인된 source 설정과 함께 `closing.sources.enabled=true`를 명시합니다. 기본값 `false`/미설정은 외부 DB나 Journal 대신 가짜 성공을 주지 않고 실패합니다.
- remote Journal의 멱등 slip 처리, API 중복 요청 방지, 여러 전표 ID 조회, production PostgreSQL 부하는 아직 별도 운영·검증 과제입니다.
- 연차 실행 전 런타임이 한 법인만 전담하는지, 요청 연도와 정확히 일치하는 승인 규칙이 있는지, 연말 Master 계정이 `EQUITY`/`CREDIT`인지 확인합니다.
- 연차 설정 변경은 동료 검토·버전 승인을 남기고, 연차 호출을 drain한 뒤 모든 instance를 재시작합니다. pending 초안이 있었다면 재시도로 덮지 말고 stale 충돌을 대사합니다.
- 재오픈 요청자와 승인자가 다른지, 동일 기간에 대기 중인 요청이 없는지 확인합니다.
- EOD 명령은 Gateway가 검증해 만든 actor/role 헤더를 통해서만 호출하고 서비스 포트를 외부에 노출하지 않습니다.
- 일마감 상태의 `transactionAllowed=false`는 도메인 판단입니다. Journal 전표 생성 게이트에 연결되기 전까지는 월 회계기간 잠금과 별도로 운영 통제가 필요합니다.
- 연차 손익 대체가 409로 실패하면 반복 호출로 덮으려 하지 말고, 현재 `POSTED` 원천과 모든 연차 전표의 lineage·상태·상세를 대사합니다. 이 API는 충돌 전표를 삭제·반려·역분개하지 않으며 생성 전표의 `SYSTEM` 처리자도 사람 승인 증거가 아닙니다.
- 로컬 테스트는 실제 PostgreSQL 실행계획·대량 부하·분산 장애를 증명하지 않습니다. 현재 Closing HTTP adapter가 maker 헤더를 보내더라도 신뢰된 service principal의 인증을 증명하지 못하므로 원격 Journal 쓰기는 별도 인증 통합 전까지 HOLD입니다.
