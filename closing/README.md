# 🚪 Closing Service (결산 및 마감 관리)

`closing` 모듈은 회계 기수(월/연말)의 문을 닫아 더 이상 과거 날짜로 전표가 생기지 않게 하고, 외화 평가나 감가상각 같은 결산 자동 분개를 수행하는 결산 센터입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

회계팀의 매달 말일은 전쟁입니다. 이 과정을 도와주는 것이 `closing` 모듈입니다.
1. **마감 통제:** "이번 달 장부 닫습니다!" 하면, 아무도 이번 달 날짜로 뒤늦게 전표를 만들지 못하도록 셔터를 내립니다.
2. **평가 및 자동 분개:** 기말 환율을 적용해 외화 예금의 원화 가치를 재평가하거나, 자산 감가상각비를 계산해 결산 전표(조정 분개)를 자동으로 찍어냅니다.
3. **이월 (Carry-forward):** 연말이 되면 올해의 수익/비용을 싹 비우고, 내년도 장부의 시작 잔액(기초 잔액)으로 넘겨주는 작업을 통제합니다.

> **💡 중요 포인트:** `closing` 모듈이 "문 닫혔어!"라고 판단하는 로직(`validateReadyToClose`)에는 모든 필수 결산 작업(Task)이 끝났는지, 다른 부서의 마감(Gate)이 통과되었는지 확인하는 깐깐한 도메인 규칙이 들어 있습니다.

---

## 2. 🔄 아키텍처 및 연동 흐름 (Process Flow)

### 📌 헥사고날 아키텍처 (DDD)
타 모듈(`journal-ledger` 등)이 전표를 끊기 전, 이 전표의 회계일자가 유효한지 `contracts` 모듈의 `AccountingPeriodStatusPort`를 통해 `closing` 쪽에 물어봅니다.

### 📌 결산 캘린더 (ClosingCalendar) 도메인 주도 검증
과거에는 NPE 에러가 나거나 코드가 꼬이는 일이 있었지만, 최근 리팩토링으로 상태 판정 검증 로직이 도메인 모델 내부로 응집되었습니다. 마감을 닫으려면 `ClosingCalendar`가 자신의 상태를 스스로 검증합니다.

### 📌 FiscalPeriod 참조 분리
결산 조정, 기간 잠금, 재오픈 승인, 평가/충당 배치는 `master-data`의 `FiscalPeriod` 엔티티를 직접 참조하지 않습니다. 저장값은 `fiscalPeriodId`이며, 회계기간 조회/상태 변경은 `contracts`의 `FiscalPeriodControlPort`를 통해 수행합니다.

### 📌 자동 평가/충당 분개 룰
평가 배치와 충당 배치는 더미 계정이나 고정 금액을 사용하지 않습니다. `account.closing.accounting.valuation-rules.*`, `account.closing.accounting.provision-rules.*` 설정으로 유형별 차변 계정, 대변 계정, 금액을 지정해야 하며, 설정이 없으면 자동 분개를 생성하지 않고 실패합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
멀티스테이지 Dockerfile을 통해 빌드되며, 통합 환경에서 Eureka/Config 의존성을 물고 자동으로 구동됩니다.
```bash
docker-compose up -d closing
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :closing:api:bootRun
```
