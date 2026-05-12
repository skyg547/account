# 📊 Reporting Service (회계 보고서 관리)

`reporting` 모듈은 회계 시스템의 최종 결과물인 재무상태표(B/S), 손익계산서(I/S) 등 경영진과 외부에 보여줄 각종 보고서를 생성하는 모듈입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

회계의 목적은 결국 "우리 회사 얼마 벌었고, 얼마나 가지고 있어?"를 보여주기 위함입니다.
`journal-ledger`가 매일매일 발생한 거래를 꼼꼼하게 기록하는 일기장이라면, `reporting`은 그 일기장을 모아 요약본(보고서)을 예쁘게 뽑아주는 역할을 합니다.

- **재무상태표 (B/S):** 특정 시점(예: 12월 31일)에 우리 회사의 자산, 부채, 자본이 얼마인지 사진 찍듯 보여주는 보고서.
- **손익계산서 (I/S):** 특정 기간(예: 1월 1일 ~ 12월 31일) 동안 얼마나 벌고 얼마나 썼는지 보여주는 성적표.

---

## 2. 🔄 아키텍처 및 연동 흐름 (Process Flow)

### 📌 데이터 수집 및 헥사고날 구조
`reporting` 모듈은 스스로 DB에 장부를 가지고 있지 않습니다.
보고서를 뽑아달라는 요청이 오면, `contracts` 모듈의 `LedgerQueryPort` 등을 통해 `journal-ledger`에 물어봐서 잔액 데이터를 끌어모은 뒤 양식에 맞게 포맷팅합니다.

> ⚠️ **현재 상태 주의 (Mockup):**
> 최신 리뷰(Handoff Tasks)에 따르면, 현재 이 모듈의 연동 어댑터(`LedgerClientAdapter`)에는 하드코딩된 잔액(`101`, `102`)과 보고서명(`ASSET_CASH`)이 들어가 있는 목업(Mock) 상태입니다. 추후 실제 `journal-ledger` 데이터와 연동하는 고도화 작업이 필요합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
```bash
docker-compose up -d reporting
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :reporting:api:bootRun
```
