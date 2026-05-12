# 📝 Journal Ledger Service (전표 및 원장 관리)

`journal-ledger`는 회사에서 돈이 나가고 들어오는 모든 거래가 "차변/대변이 일치하는 복식부기 전표" 형태로 기록되는 회계 시스템의 가장 핵심적인 장부 기록 센터입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 이 모듈은 정확히 무슨 일을 하나요?**
거래를 회계 전표로 만들고, 승인받고, 최종적으로 원장(Ledger)에 반영한 뒤, 나중에 감사관이 "이 돈 어디서 왔어?" 하고 물어볼 때 끝까지 추적할 수 있게 기록을 남기는 곳입니다.

**초보자가 알아야 할 핵심 5가지 개념:**
1. **전표 (`JournalEntry`):** 회계 처리 한 건의 헤더 ("2026-04-13 대출 실행 전표")
2. **전표 라인 (`JournalDetail`):** 실제 차변/대변 상세 줄.
3. **상태 변화:** 작성 중(`DRAFT`) -> 승인 요청(`REQUESTED`) -> 승인됨(`APPROVED`) -> 원장 반영 완료(**`POSTED`**)
4. **GL / SL:** 
   - `GL (총계정원장)`: 계정과목 중심의 큰 장부
   - `SL (보조원장)`: 거래처, 부서 등 상세 차원을 포함한 보조 장부
5. **Lineage (추적 키):** 이 전표가 시스템의 어떤 원천 문서(지출결의서 등)에서 왔는지 알려주는 꼬리표(`lineageSourceType`, `lineageSourceId`). 드릴다운(Drill-down)의 핵심입니다.

---

## 2. 🔄 처리 흐름 및 주요 설계 (Process Flow)

### 📌 수기 전표 vs 자동 전표
- **수기:** 사용자가 직접 차/대를 입력 -> `DRAFT` 저장 -> 결재 후 `APPROVED` -> 원장 전기 `POSTED`.
- **자동:** 외부 도메인(`loan`, `asset-lease` 등)에서 이벤트 발생 -> `JournalRuleEngine`이 룰 확인 후 자동 전표 생성 -> 이후 흐름 동일.

### 📌 승인(APPROVED)과 전기(POSTED)는 다릅니다!
초보자가 가장 많이 헷갈리는 부분입니다. **결재가 났다고 해서 장부 잔액이 바뀌지 않습니다.**
반드시 **전기(Posting)** 과정을 거쳐 상태가 `POSTED`가 되어야만 GL/SL 원장 잔액에 돈이 더해지고 빼집니다.
최근 리팩토링으로 전기 처리는 `PostingService.postJournalEntry`라는 단일 경로로 수렴되어 원장 정합성을 완벽히 보장합니다.

### 📌 모듈 연동 주의점
전표를 끊을 때 계정 코드가 유효한지는 `master-data`에 물어보고, 현재 마감이 닫혔는지는 `closing`에 물어봅니다. 직접 DB를 보지 않고 모두 `contracts` 모듈의 포트를 사용합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
멀티스테이지 Dockerfile을 통해 빌드되며, 통합 환경에서 Eureka/Config 의존성을 물고 자동으로 구동됩니다.
```bash
docker-compose up -d journal-ledger
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :journal-ledger:api:bootRun
```
