# 🏦 Loan Service (대출 회계 관리)

`loan` 모듈은 은행/금융업의 핵심인 고객 대출의 실행, 이자 수익 계산, 상환 처리 등을 관리하는 대출 특화 서브레저입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

대출 업무는 회계 시스템에서 가장 복잡한 영역 중 하나입니다.
1. **대출 실행:** 고객에게 1억 원을 쏴주면, 장부에는 "대출채권 1억 원(받을 돈) 늘어남 / 현금 1억 원 나감" 이라고 전표를 끊습니다.
2. **이자 발생 (Accrual):** 매일매일 이자가 쌓입니다. 고객이 아직 돈을 안 냈더라도 "오늘치 이자 수익이 이만큼 발생했어"라고 장부에 미리 기록(미수이자)해 둡니다.
3. **상환:** 고객이 이자와 원금을 갚으면, 미수이자를 지우고 현금을 늘리는 처리를 합니다.

이 모든 스케줄과 원리금 계산을 도맡아 하는 곳이 바로 `loan` 모듈입니다.

---

## 2. 🔄 아키텍처 및 개선 과제 (Handoff Tasks)

- **도메인 모델 파편화:** 현재 코드 내부에 `Loan` 객체와 `LoanContract` 객체가 병행해서 쓰이고 있어 모델이 쪼개져 있는 상태입니다. 이를 하나의 일관된 대출 도메인으로 통합해야 합니다.
- **계정코드 설정화:** 대출 실행, 이자 발생, 이연 항목 전표의 계정코드는 `account.loan.accounting.*` 설정(`LoanAccountingProperties`)으로 분리되어 환경별로 조정할 수 있습니다.
- **전표 수렴 보강:** 대출 자동 전표는 생성 후 `JournalUseCase.approveJournalEntry`와 `postJournalEntry`를 호출해 `POSTED` 수렴 경로를 탑니다. 현재는 단위 회귀 테스트로 호출 흐름을 검증하며, 실제 `journal-ledger`를 포함한 통합 E2E 테스트는 남은 과제입니다.

대표 설정 키:
```yaml
account:
  loan:
    accounting:
      cash-account-code: "101000"
      loan-receivable-account-code: "131000"
      deferred-asset-account-code: "171000"
      recognized-income-account-code: "401000"
      accrued-interest-receivable-account-code: "11501"
      interest-income-account-code: "41101"
```

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
```bash
docker-compose up -d loan
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :loan:api:bootRun
```
