# Issue #43: EOD/BOD 결산 상태 관리

## 문제

기존 `EodState`는 잘못된 패키지에 고립되어 실제 유즈케이스·영속성·API 호출자가 없었습니다. `DailyClosingStatus`도 `isClosed` Boolean과 public setter만 가져 EOD 준비, 실행, 완료, 다음 영업일 개시를 검증하거나 감사할 수 없었습니다. 같은 날짜를 `CLOSED → BOD_IN_PROGRESS → OPEN`으로 순환시키면 전일 마감 이력도 사라집니다.

setter 기반 `ClosingPeriod`는 실제 월 마감 기준인 `ClosingCalendar`와 Master Data `FiscalPeriod`를 중복 저장했으며 사용처와 테이블 migration이 없었습니다.

## 구현 계약

- 일마감 권위 모델은 날짜별 `DailyClosingStatus`와 canonical `EodState`입니다.
- 같은 날짜의 상태는 `OPEN → PRE_CLOSING → CLOSING_IN_PROGRESS → CLOSED`이며 준비 단계에서만 `OPEN`으로 취소할 수 있습니다.
- `CLOSED` 행은 terminal입니다. BOD는 잠긴 전일 `CLOSED`를 확인한 뒤 명시적인 다음 영업일을 별도 `BOD_IN_PROGRESS → OPEN` 행으로 생성합니다.
- 모든 변경은 actor와 시각을 기록하고 `@Version` 및 locked lookup으로 동시 변경을 통제합니다.
- 임의 상태 setter/API는 제공하지 않습니다. 유즈케이스와 HTTP는 bootstrap, prepare/cancel/start/complete EOD, start/complete BOD 명령만 제공합니다.
- 변경 명령은 Gateway가 JWT에서 재생성한 `X-Auth-User`와 `X-Auth-Roles`를 사용합니다.
- 월·연 기간 상태는 기존 `ClosingCalendar`와 Master Data `FiscalPeriod`를 유지하고, 연차 손익 대체는 `AnnualClosingService`를 사용합니다. 사용되지 않던 `ClosingPeriod` 병렬 모델은 제거합니다.

## 영속성 및 라우팅

- `DailyClosingStatusPersistencePort`와 JPA 어댑터를 분리했습니다.
- V50은 기존 `date/is_closed/closed_at/closed_by` 테이블을 상태·버전·감사 컬럼으로 확장하고 Boolean 값을 `OPEN`/`CLOSED`로 backfill한 뒤 `is_closed`를 제거합니다.
- Closing API는 `classpath:db/closing-migration`만 실행하고 다른 모듈의 적용 이력과 충돌하지 않도록 `flyway_schema_history_closing`을 사용하며, 기존 스키마는 version 49에서 baseline합니다.
- Gateway는 `/api/closing/**`를 legacy catch-all보다 먼저 `lb://closing-service`로 보내고 전용 CircuitBreaker/fallback을 사용합니다.

## 검증 범위

- 전체 허용/금지 상태 전이와 `CLOSED` 불변성
- actor/날짜 검증과 UTC Clock
- 첫 날짜 bootstrap, 멱등 재시도, explicit next-business-date rollover, 전일 보존
- JPA 잠금 조회, optimistic version 증가, legacy migration backfill
- trusted actor/role API 명령 매핑과 오류 응답
- Gateway 전용 route 순서와 CircuitBreaker 정책
- Closing Core/API/Batch 전체 테스트와 API/Batch bootJar, Gateway 전체 테스트

## 의도적으로 남긴 경계

`EodState.isTransactionAllowed()`는 일마감 도메인 판단이지만 Journal의 현재 `AccountingPeriodStatusPort`는 Master Data 월 회계기간만 확인합니다. 일마감 상태를 Journal 신규 전표 생성 게이트에 연결하는 작업은 서비스 간 계약·장애 시 fail-closed 정책을 함께 정해야 하므로 이 Issue의 상태 머신 구현과 분리합니다. 연결 전에는 EOD 상태만으로 시스템 전체 거래가 자동 차단된다고 간주하면 안 됩니다.

상태만 뒤집는 빈 Spring Batch Job도 만들지 않았습니다. 실제 EOD Job은 게이트, FX/ECL, 실패·재시작 지점을 함께 조정하는 별도 업무 범위여야 합니다.
