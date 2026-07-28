# 🏦 Loan Service (대출 회계 관리)

`loan`은 대출 계약, 실행, 이연 수수료/비용, 유효이자율(EIR), 조건 변경, 이자 발생 전표 계보를 관리하는 대출 서브레저입니다. 금액은 `BigDecimal`, 이율은 `0.0450 = 4.50%`인 소수 단위로 다룹니다.

## 문서 읽기 순서

1. [문서 인덱스](docs/README.md)
2. [입문 가이드](docs/beginner-guide.md)
3. [업무 흐름](docs/process-flow.md)
4. [데이터 모델](docs/schema.md)
5. [IntelliJ/Gradle 로컬 실행](docs/local-run.md)

## 실제 업무 흐름

1. 계약 생성: 거래처·통화를 실행일 기준으로 검증하고 `PENDING_DISBURSEMENT`로 저장합니다.
2. 대출 실행: 잠근 계약에 원금 전액을 한 번만 실행하고 `대출채권(차변) / 현금(대변)` 전표를 전기한 뒤 `ACTIVE`로 바꿉니다.
3. 이연 항목: 유형별 계정 코드를 유효일 기준으로 검증하고 초기 전표를 연결합니다.
4. EIR 스케줄: 현재 미상환 원금과 이연 잔액으로 월별 `eir_amortization_schedules`를 생성합니다.
5. 조건 변경: 원래 약정 원금은 보존하고 현재 미상환 잔액·만기·EIR을 변경하며 `RecalculationRun`과 `LoanEvent`를 남깁니다.
6. 이자 발생 Batch: 필수 `accrualDate`에 해당하는 EIR 스케줄을 읽고 성공 건은 건너뛰며 실패 건은 재시도합니다.

`DEFAULT`와 `RECOVERY`는 재계산 사유로 억지 변환하지 않고 상태 이벤트로 처리합니다. 중도상환·조건 변경·리스케줄만 EIR 재계산 흐름을 탑니다.

## 헥사고날 경계

- HTTP는 API 모듈의 DTO와 `LoanUseCase`를 통해 core로 들어옵니다.
- `LoanService`는 Loan 소유 포트에만 의존합니다. JPA, Master Data 엔티티, Journal 엔티티를 직접 사용하지 않습니다.
- `Loan`은 거래처 ID와 통화 코드만 소유하고, `DeferredItemType`은 계정 코드만 소유합니다.
- 현재 단일 저장소 실행을 위한 `LoanReferenceDataAdapter`와 `LoanJournalAdapter`만 provider 내부 포트를 번역합니다.
- 전표는 Loan Aggregate가 소유하지 않으며 ID와 전표번호를 값으로 연결합니다.

> 운영 MSA 분리 시에는 현재 로컬 어댑터를 원격 API/메시지 어댑터로 교체해야 합니다. 완료 조건은 core 의존성에서 provider 모듈 제거, 계약 테스트, 타임아웃·재시도·circuit breaker, 기준정보 스냅샷 정책을 포함하는 것입니다.

## 필수 계정 설정

```yaml
account.loan.accounting:
  cash-account-code: "101000"
  loan-receivable-account-code: "131000"
  deferred-asset-account-code: "118000"
  recognized-income-account-code: "410000"
  accrued-interest-receivable-account-code: "115010"
  interest-income-account-code: "410100"
```

계정은 사용 업무일 기준 Master Data에서 유효해야 합니다. 이연 항목 유형에 별도 계정 코드가 있으면 기본 설정 대신 그 코드를 사용합니다.

## 검증과 실행

```powershell
.\gradlew :loan:core:test :loan:api:bootJar :loan:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

상세 로컬 실행 명령은 [local-run.md](docs/local-run.md)를 참고합니다. 로컬 API 예시는 8087을 쓰며, 전체 루트 Compose에서는 Governance와의 충돌을 피하려고 Loan을 8088로 노출합니다. `loan/Dockerfile`은 프로젝트 기준 Java 17과 `:loan:api:bootJar`를 사용합니다.

## 남은 운영 리스크

- Loan DB와 Journal 전기는 서로 다른 트랜잭션입니다. outbox/inbox, lineage 멱등 키, 재처리·보상 이력, 장애 주입 테스트가 있어야 분산 원자성이 완성됩니다.
- HTTP의 actor는 현재 요청 값입니다. 인증 principal에서 서버가 actor를 결정하도록 바꾸고 위변조 테스트를 통과해야 합니다.
- 현재 생성기는 월별 EIR 스케줄입니다. 매일 이자를 인식해야 하는 상품은 day-count convention, 휴일 달력, 일별 스케줄 정책과 회계 검증값이 추가되어야 합니다.
- V33 고유 인덱스 적용 전 운영 데이터의 중복 사전 점검과 정리 계획이 필요합니다.
