# 💰 Receivable Service (매출 채권 관리)

`receivable` 모듈은 회사가 외부(고객)로부터 '받아야 할 돈'을 관리하는 매출채권(Accounts Receivable, AR) 전용 서브레저입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

회사가 물건을 팔았는데 아직 돈을 받지 못했다면 '받을 돈(매출채권)'이 생깁니다.
- **발생:** 영업팀에서 솔루션을 팔고 세금계산서를 발행하면, `receivable` 모듈에 "B고객에게 500만 원 받아야 함"이라고 기록됩니다.
- **수납:** 나중에 회사 통장으로 B고객이 500만 원을 입금하면, 그 입금 내역을 받아 매출채권을 지웁니다(반제, Settlement).

> **💡 중요:** 돈을 받았을 때 "이 돈이 정확히 어떤 건에 대한 입금인가?"를 짝맞추기(매칭) 하는 것이 매출채권 관리의 핵심입니다.

---

## 2. 🔄 아키텍처 및 헥사고날 위반 주의

- 최근 리뷰(Handoff Tasks)에 따르면, `CollectionController`와 `SalesController` 같은 웹 어댑터 계층에서 도메인 엔티티를 직접 반환하여 **헥사고날 아키텍처 원칙을 위반**하고 있는 상태입니다. 
- 추후 외부로 응답을 내보낼 때는 반드시 전용 DTO로 변환하여 반환하도록 고도화해야 합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
```bash
docker-compose up -d receivable
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :receivable:api:bootRun
```
