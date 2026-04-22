# 📖 원장 잔액 이월 (Ledger Carry-forward) 가이드

이 문서는 재무 시스템에서 장부의 연속성을 보장하기 위해 적용된 **'자동 잔액 이월(Automatic Carry-forward)'** 로직에 대해 설명합니다.

---

## 1. 초보자를 위한 개념 설명
"어제 내 통장에 10,000원이 있었는데, 오늘 아침에 0원으로 시작하면 안 되겠죠?"

회계 장부도 마찬가지입니다. 
- **기초 잔액(Beginning Balance):** 오늘 일을 시작하기 전, 어제로부터 넘어온 금액
- **기말 잔액(Ending Balance):** 오늘 발생한 모든 거래를 더하고 뺀 최종 금액

시스템은 매일 전표가 처음 기록될 때, **직전 최종 잔액을 자동으로 찾아 오늘 장부의 기초 잔액으로 설정**합니다. 이를 통해 수동 작업 없이도 장부가 끊기지 않고 이어지게 됩니다.

---

## 2. 핵심 비즈니스 로직 Flow

1. **전표 전기(Posting) 발생:** 사용자가 전표를 확정하거나 시스템이 자동 생성된 전표를 승인함.
2. **잔액 레코드 확인:** 해당 날짜(`balanceDate`)와 계정 조합의 잔액 데이터가 이미 있는지 DB에서 확인.
3. **기초 잔액 설정 (Carry-forward):**
   - 레코드가 **없을 경우:** DB에서 해당 날짜 이전(`balanceDate < today`) 중 **가장 최근의 `endingBalance`**를 조회.
   - 조회된 금액을 새로운 레코드의 `beginningBalance`로 설정.
   - 만약 한 번도 거래가 없었다면 0원부터 시작.
4. **증분 업데이트:** 오늘 발생한 전표의 차변(Debit) 또는 대변(Credit) 금액을 합산.
5. **최종 잔액 재계산:** `Ending Balance = Beginning + Debit - Credit` (자산 기준).

---

## 3. 관련 핵심 클래스 및 메서드

- **`LedgerService.updateGlBalance()`**: 총계정원장 잔액 이월 로직의 본거지.
- **`GlBalanceRepository.findFirstBy...BeforeOrderByBalanceDateDesc()`**: 직전 잔액을 찾는 핵심 쿼리.
- **`GlBalance.recalculate()`**: 기초 잔액과 증분을 합산하여 기말 잔액을 산출하는 도메인 로직.

---

## 4. 주의사항 (Developer Tips)
- **SCD2 연동:** 계정과목이나 부서가 변경되어도 원장의 이력은 보존되어야 하므로, 잔액 데이터 생성 시점의 마스터 데이터 정보를 정확히 참조합니다.
- **역날짜 전표(Back-dated):** 과거 날짜의 전표를 입력할 경우, 그 이후 모든 날짜의 잔액을 다시 계산(Re-aggregation)해야 합니다. 현재는 `reaggregateLedgerBalancesForPeriod` 기능을 통해 이를 지원합니다.
