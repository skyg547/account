# 🔒 Closing Service (결산 마감 관리)

`closing` 모듈은 회계 기간의 종료를 통제하고, 장부를 봉인하여 과거 데이터의 임의 수정을 방지하는 시스템의 '안전 자물쇠' 역할을 합니다. 상태 변경과 결산 전표 생성은 입력이나 기준정보가 빠지면 추정하지 않고 실패시키는 것을 원칙으로 합니다.

일반 전표의 Closing 측 허용 판정은 Master 회계기간 `OPEN`, Closing 캘린더 `OPEN`, 활성 잠금 없음이
모두 확인될 때만 허용합니다. `GET /api/closing/admission?accountingDate=2026-01-15`로 같은 판정을
조회할 수 있습니다. 이는 조회 시점의 결과이며 전기 커밋을 예약하는 권한은 아닙니다.
현재 독립 배포 Journal은 이 조회를 아직 소비하지 않습니다. Journal 소비자 연결, 승인된 조정 전표 경로와
GL07/#762 커밋 동시성 통합 전에는 #772 전체 해결로 간주할 수 없습니다.
정책과 검증 범위는 [업무 흐름](docs/process-flow.md#일반-전표-허용-판정-gh-772)을 참고하세요.

월말 승인·반려와 체크리스트 변경은 캘린더 단위로 직렬화합니다. 원격 마감·재오픈 반영이
불명확하면 영속 전이 기록을 유지하고 자동 재전송하지 않습니다.
[월말 동시 결정과 복구](docs/process-flow.md#월말-동시-결정과-복구-gh-774)를 참고하세요.

최종 마감은 체크리스트뿐 아니라 공급자가 제출한 불변 typed evidence를 필수로 요구합니다.
월 마감에는 AP·AR·리스·대출 보조부, Journal 조정, ECL 대사의 여섯 통제가 필요하고,
`fiscalPeriod=YEAR`에는 연차 손익 대체 통제가 추가됩니다. 최신 스냅샷이 실패·누락·오래됨·
기간 불일치이면 Master를 변경하기 전에 차단합니다. 계약과 신뢰 경계는
[최종 마감 증빙](docs/process-flow.md#최종-마감-증빙-gh-778)을 참고하세요.

---

## 0. 📚 문서 읽기 순서

세부 문서는 `closing/docs`에 모았습니다.

1. [문서 인덱스](docs/README.md)
2. [입문 가이드](docs/beginner-guide.md)
3. [업무 흐름](docs/process-flow.md)
4. [데이터 모델](docs/schema.md)
5. [IntelliJ/Gradle 로컬 실행](docs/local-run.md)

IntelliJ에서는 `.run`의 `Closing API bootRun`, `Closing Batch Context` 실행 설정을 사용할 수 있습니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**결산 마감은 '가계부 한 달 치를 확정하고 테이프로 봉인하는 것'과 같습니다.**
1. **결산(Closing):** 한 달 동안의 모든 거래를 모아 "이달의 이익은 얼마!"라고 최종 확정 짓는 작업입니다.
2. **태스크(Task):** 마감 전 반드시 체크해야 할 리스트입니다. (예: "통장 잔액 확인", "법인카드 정산")
3. **잠금(Lock):** 마감이 끝난 달에 누군가 몰래 과거 날짜로 전표를 넣지 못하도록 해당 기간을 잠그는 것입니다.
4. **재오픈(Reopen):** 정말 중요한 실수로 인해 잠긴 기간을 수정해야 할 때, 높은 책임자의 승인을 받아 임시로 자물쇠를 푸는 절차입니다.
5. **EOD/BOD:** 영업일 한 건을 `OPEN → PRE_CLOSING → CLOSING_IN_PROGRESS → CLOSED`로 닫고, 닫힌 행을 보존한 채 다음 영업일을 `BOD_IN_PROGRESS → OPEN`으로 여는 일마감 흐름입니다.
6. **최종 마감 증빙:** 각 원천 시스템의 실행 ID와 계정·통화별 원천/전기 합계를 함께 보존한 불변 스냅샷입니다. 값이 0이어도 행을 생략하지 않고 `0/0`으로 명시합니다.

---

## 2. 🔄 프로세스 흐름 (Process Flow)

### 🧭 Core/Batch 책임 경계

FX 평가와 ECL 충당의 금액 산출, 차대변 판단, 전표 라인 구성은 `closing:core`의 application service가 담당합니다. `closing:batch`는 Spring Batch Job/Step, Reader/Writer/Tasklet, 외부 Repository/JournalUseCase 어댑터만 담당합니다.

초보자 설명: Batch는 "공장 컨베이어 벨트"이고 core는 "회계 규칙을 판단하는 업무 담당자"입니다. 컨베이어가 어떤 순서로 데이터를 흘릴지는 batch가 정하지만, 어떤 금액을 차변/대변에 놓을지는 core가 결정해야 테스트와 API/Batch 흐름이 같은 규칙을 공유할 수 있습니다.

### 📌 결산 캘린더 및 마감 파이프라인
마감 시작부터 태스크 검증, 최종 기간 잠금까지의 헥사고날 아키텍처 흐름입니다.

```mermaid
sequenceDiagram
    participant User as 사용자/배치
    participant Service as ClosingService
    participant Task as ClosingTask
    participant Journal as Journal-Ledger
    participant DB as Persistence Adapter

    User->>Service: 마감 프로세스 시작
    Service->>Task: 필수 체크리스트 검증
    Task-->>Service: 검증 완료
    
    rect rgb(240, 240, 240)
        Note over Service, Journal: 기간 잠금 및 연동
        Service->>DB: 활성 Period Lock과 감사 로그 저장
        Service->>Journal: 해당 기간 전표 생성 차단 활성화
    end
    
    Service-->>User: 마감 확정 완료
```

### 📌 EOD/BOD 날짜 이력

`DailyClosingStatus`는 영업일별 한 행을 보존합니다. 어제의 `CLOSED` 행을 다시 `OPEN`으로 되돌리지 않으며, BOD 시작 명령이 잠긴 전일 행을 확인하고 운영자가 명시한 다음 영업일 행을 원자적으로 생성합니다. 주말·휴일을 코드가 임의로 `plusDays(1)` 처리하지 않습니다.

상태 변경 API는 임의의 목표 상태를 받지 않고 `prepare`, `start`, `complete`, `cancel-preparation`, `start/complete BOD` 명령으로만 노출됩니다. Gateway가 JWT를 검증해 만든 `X-Auth-User`와 `X-Auth-Roles`를 사용하므로 Closing API 포트를 외부에 직접 공개하지 않습니다.

### 📌 최종 마감 증빙

내부 공급자는 `POST /api/closing/calendars/{calendarId}/final-close-evidence`로 증빙을 추가합니다.
본문에는 `submittedBy`가 없으며, Gateway가 클라이언트의 `X-Auth-*`를 제거하고 검증된
`X-Auth-User`와 `X-Auth-Roles=ROLE_CLOSING_EVIDENCE_PROVIDER`를 다시 만들어야 합니다.
역할과 `account.closing.final-close-evidence.trusted-submitters`의 대소문자 일치 actor가 모두
필요합니다. allowlist 기본값은 비어 있어 전부 거부합니다. 수동 우회나 fail-open 경로는 없습니다.
`account.closing.final-close-evidence.max-age`의 기본값은 `PT24H`이며, 관측 시각은 현재 마감 시작
뒤이면서 미래가 아니고 최대 유효시간 안이어야 합니다.

### 📌 타 모듈과의 연동 (Status Port)
전표 모듈은 전표를 끊기 전, 이 포트를 통해 해당 날짜가 마감되었는지 확인합니다.

```mermaid
flowchart LR
    A[Journal Service] -- "Is date closed?" --> B{AccountingPeriodStatusPort}
    B -- "Status Check" --> C[Closing Service]
    C -- "OPEN / CLOSED" --> A
```

---

## 3. 📊 데이터 모델 (Schema)

회계 기간의 상태와 잠금 이력을 완벽히 추적합니다.

```mermaid
erDiagram
    FISCAL_PERIODS ||--o{ PERIOD_LOCKS : "locked_by"
    CLOSING_CALENDARS ||--o{ CLOSING_TASKS : "manages"
    CLOSING_CALENDARS ||--o{ FINAL_CLOSE_EVIDENCE_SETS : "snapshots"
    FINAL_CLOSE_EVIDENCE_SETS ||--o{ FINAL_CLOSE_EVIDENCE_CONTROLS : "contains"
    FINAL_CLOSE_EVIDENCE_CONTROLS ||--o{ FINAL_CLOSE_EVIDENCE_TOTALS : "reconciles"
    DAILY_CLOSING_STATUS {
        date business_date PK
        string state
        bigint version
        timestamp updated_at
        string updated_by
    }

    FISCAL_PERIODS {
        uuid id PK
        String fiscal_year "연도"
        String period_month "월"
        String status "OPEN, CLOSED"
        Boolean is_current "SCD2"
    }
    
    PERIOD_LOCKS {
        uuid id PK
        uuid fiscal_period_id FK "ID 기반 참조"
        String lock_type "GENERAL, PARTIAL"
        LocalDateTime locked_at "잠금 일시"
    }
```

---

## 4. 🧪 로컬 실행 및 설정 방법

먼저 테스트와 컴파일로 모듈 상태를 확인합니다.

```powershell
.\gradlew :closing:core:test :closing:api:test :closing:batch:test --console=plain --max-workers=1 --no-daemon
```

API 컨텍스트 실행:

```powershell
.\gradlew :closing:api:bootRun --console=plain
```

Batch 컨텍스트만 실행:

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.main.web-application-type=none" --console=plain
```

`bootRun`과 profile을 생략한 실행 JAR은 `local`을 기본값으로 사용합니다. API와 Batch는 서로
분리된 in-memory H2를 사용하고, 외부 control plane과 Batch Job 자동 실행을 끕니다.

ECL 충당 Job 예시:

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=eclProvisionJob closingDate=2026-04-30 provisionBatchId=20260430 --spring.batch.jdbc.initialize-schema=always" --console=plain
```

**설정 주의사항:**
- 자동 평가/충당 분개 계정은 `account.closing.accounting.*` 설정을 통해 동적으로 할당됩니다.
- ECL 충당 배치는 `allowance_summary`에 확정된 IFRS 9 산출 결과가 있을 때만 목표 충당금을 읽어 보충/환입 전표를 생성합니다. 더 이상 `대출채권 잔액 * 1%` 고정률로 목표 충당금을 계산하지 않습니다.
- FX/ECL 배치 전표번호는 기준일, 양수 배치 ID, 계정·통화 식별자를 조합해 20자 이내로 결정적으로 생성됩니다. 날짜와 배치 ID는 모두 필수이며 시스템 날짜나 임의 ID로 대체하지 않습니다.
- FX 평가는 실제 전기 원장인 `journal_entries`/`journal_details`의 `POSTED` 전표를 고정 개수 계정 범위로 스트리밍합니다. 별도 이중통화 잔액 read model이 구축되기 전까지는 이 집계를 원장 기준으로 대사해야 합니다.
- FX 평가 전 `account.closing.accounting.fx-valuation-policies`에 계정별 유효기간과 화폐성/역사적 원가 정책을 명시해야 합니다. 수익·비용·역사적 원가 비화폐성 자산은 제외하고, 정책 누락·중복·계정 분류 불일치는 실패시킵니다. [평가 적격성](docs/process-flow.md#평가-대상-계정의-유효일자-정책-gh-780)과 [설정 예시](docs/local-run.md#fx-평가-적격성-설정-gh-780)를 참고하세요.
- FX 장부금액은 이전에 전기한 평가 조정과 역분개를 원천통화별로 포함합니다. 평가액은 외화 원금을 바꾸지 않으며, 전월 평가가 전기되고 환율이 같으면 다음 평가 차액은 0입니다. [귀속과 대사 기준](docs/process-flow.md#이전-평가를-포함한-장부금액-gh-779)을 참고하세요.
- ECL은 하나의 확정 run/model, 하나의 법인, 동일 기준일의 summary만 허용하며 계정·통화별 목표와 실제 전기된 거래통화·기능통화 잔액을 각각 대사합니다. 외화는 기준일 환율이 필요하며, 기존 장부액이 그 환율과 다르면 FX 평가 전기를 먼저 요구합니다. summary가 비어 있으면 성공으로 처리하지 않습니다. [통화별 계산과 재시도](docs/process-flow.md#ecl-거래통화와-기능통화-대사-gh-781)를 참고하세요.
- FX/ECL 결산 조정 전표는 기본적으로 `DRAFT`로 남아 검토와 승인을 기다립니다. 통제된 환경에서만 `account.closing.accounting.auto-post-adjustments=true`로 자동 승인/전기를 허용합니다.
- 전표 모듈 연동을 위해 `AccountingPeriodStatusPort` 구현체가 정상적으로 노출되어야 합니다.
- 최종 마감 증빙 공급자와 Gateway가 본문·canonical digest·역할 및 actor allowlist 계약을 함께 구현해야 합니다. 헤더/allowlist만으로 암호학적 서비스 identity를 증명하지 않으므로 production은 사설 네트워크와 Gateway 헤더 재구성을 검증하고, 향후 mTLS나 서명된 서비스 토큰 같은 강한 인증을 적용해야 합니다. Closing은 원천 서비스의 실행 완료 자체를 호출해 확인하지 않으며, 잘못되거나 없는 증빙을 허용으로 바꾸지 않습니다.
- 현재 `AccountingPeriodStatusPort`는 월 회계기간 잠금만 확인합니다. `EodState.isTransactionAllowed()`를 Journal 신규 전표 게이트에 연결하는 작업은 별도 변경이며, 연결 전에는 일마감 상태만으로 전표가 자동 차단된다고 간주하면 안 됩니다.
- Closing 전용 Flyway 위치는 `classpath:db/closing-migration`, 독립 이력 테이블은 `flyway_schema_history_closing`입니다. clean DB는 V49의 10개 Closing 소유 테이블 baseline을 적용하고, V50으로 EOD/BOD 상태를 승격한 뒤 V51로 운영 조회 인덱스, V52로 월말 전이 기록, V53으로 불변 최종 마감 증빙과 전이 바인딩을 추가합니다. 기존 legacy DB는 runner가 전체 V49 구조를 확인한 경우에만 baseline을 기록하고 후속 버전을 forward 적용합니다. 새 API/Batch 기동 전 V53까지 migrate/validate해야 합니다.
- Docker 실행이 필요하면 `closing/docker-compose.yml`을 사용할 수 있지만, 신규 개발자는 먼저 위 Gradle 명령으로 컨텍스트와 테스트를 확인하는 편이 문제 범위를 좁히기 쉽습니다.
