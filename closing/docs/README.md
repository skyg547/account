# Closing 문서 인덱스

`closing` 모듈은 날짜별 EOD/BOD, 회계기간 체크리스트, 게이트, 기간 잠금, 재오픈 승인, 결산 조정 전표, FX/ECL 결산 배치를 관리합니다.

초보자는 아래 순서로 읽으면 업무 흐름과 코드 위치를 함께 이해하기 쉽습니다.

1. [beginner-guide.md](beginner-guide.md): 결산 용어와 전체 역할
2. [process-flow.md](process-flow.md): API, Batch, 타 모듈 연동 흐름
3. [schema.md](schema.md): 주요 테이블과 외부 데이터 의존성
4. [local-run.md](local-run.md): IntelliJ/Gradle 로컬 실행 방법

## 현재 기준

- API 엔트리포인트: `closing:api`, `com.ho.account.closing.ClosingApplication`
- Batch 엔트리포인트: `closing:batch`, `com.ho.account.closing.batch.ClosingBatchApplication`
- Runtime 경계: API/Batch는 `com.ho.account` 전체를 스캔하지 않고 Closing과 필요한 Journal/Master Data 로컬 어댑터·저장소만 명시적으로 조립합니다. 두 진입점은 같은 core 금융 정책과 posted-journal 집계 SQL을 사용합니다.
- Core 책임: `closing:core`의 `application.service`, `application.port`, `domain`, `infrastructure`. FX 평가/ECL 충당의 금액 산출, 차대변 판단, 전표 command 구성도 core 책임입니다.
- 일/월/연 권위 모델: 일마감은 `DailyClosingStatus`, 월·연 기간 상태는 `ClosingCalendar`와 Master Data `FiscalPeriod`, 연차 손익 대체는 `AnnualClosingService`가 담당합니다. 별도 `ClosingPeriod` 병렬 모델은 사용하지 않습니다.
- 외부 입력:
  - `master-data`: 회계기간 ID, 회계연도, 회계기간, 시작일/종료일, 마감 상태
  - `master-data`: 기준일 최신 환율과 기준일 유효 계정과목
  - `journal-ledger`: 전표 생성, 전표 조회, 실제 `POSTED` 원장과 GL 잔액 조회
  - `ecl`: `allowance_summary` 기준일별 확정 ECL 목표 충당금

## Fail-closed 기준

- 회계기간이 없으면 해당 날짜를 열린 기간으로 추정하지 않습니다.
- 필수 태스크 또는 게이트 정의가 하나도 없거나 완료되지 않았으면 마감할 수 없습니다.
- JSON 완료/게이트 조건이 설정되어 있지만 typed evidence evaluator가 없으면 통과시키지 않습니다.
- 최종 마감에는 별도의 불변 증빙으로 AP·AR·리스·대출·Journal 조정·ECL 대사가 필요하고 연차에는 손익 대체도 필요합니다. PREPARED에서 전송 직전 만료되면 다시 검증해 차단합니다.
- 재오픈은 `CLOSED` 기간에 대해서만 요청할 수 있고 요청자와 승인자는 달라야 합니다.
- 닫힌 영업일은 같은 행에서 BOD/OPEN으로 되돌리지 않습니다. 다음 영업일은 전일 `CLOSED`를 잠근 뒤 별도 `BOD_IN_PROGRESS` 행으로 생성합니다.
- FX/ECL 날짜와 실행 ID, 환율, 계정, 확정 summary가 빠지면 배치를 실패시킵니다.
- API는 평가 유형 `FX_RATE`, 충당 유형 `ECL`만 허용하며 수동 고정 금액 경로를 제공하지 않습니다.
- `dev`에서 `closing.sources.enabled`가 `false`이거나 없으면 외부 source와 Journal 호출은 대체 성공하지 않고 실패합니다.
- FX Batch validation과 posting은 서로 다른 source read입니다. 분산 snapshot 보장이 없으므로 실행 동안 원장·환율·정책 변경을 운영 절차로 동결합니다.

## 금융 실행의 현재 한계

- remote Journal 호출과 로컬 Batch metadata는 하나의 트랜잭션이 아닙니다. 재시도는 결정적 slip과 Journal 멱등성에 의존합니다.
- API 요청 자체의 멱등 key와 다중 전표 ID 조회 모델은 아직 없습니다. 단일 history ID 컬럼은 0건 또는 여러 건일 때 `null`입니다.
- production PostgreSQL 실행계획과 대용량 부하는 별도 검증이 필요합니다.
- 재오픈 회차 통제에는 V54 migration이 필요합니다. 기존 금융 실행 경로에 관한 이전 설명과 별도로, 최신 스키마·배포 순서는 [schema.md](schema.md#재오픈-회차와-체크리스트-이력-gh-885)를 따릅니다.

## 보존한 이전 문서

기존 `closing/docs/README.md`는 앞부분에 새 인덱스가 있었지만 뒤쪽에 인코딩이 깨진 레거시 문서가 붙어 있었습니다. 원문은 삭제하지 않고 [archive/README_legacy_corrupt_2026-06-10.md](archive/README_legacy_corrupt_2026-06-10.md)에 보존했습니다.

## 빠른 검증

```powershell
.\gradlew :closing:core:test :closing:api:test :closing:batch:test --console=plain --max-workers=1 --no-daemon
```

IntelliJ에서는 `.run` 아래의 `Closing API bootRun`, `Closing Batch Context` 설정을 사용할 수 있습니다.
