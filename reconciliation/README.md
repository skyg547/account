# ⚖️ Reconciliation Service (대사 및 정산 관리)

`reconciliation` 모듈은 내부 회계 장부(journal-ledger)의 잔액과 외부 시스템(은행, 거래소 등)의 잔액이 정확히 일치하는지 자동으로 맞춰보는 '틀린 그림 찾기' 엔진입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 대사(Recon)가 왜 필요한가요?**
은행 계좌에는 100만 원이 들어왔다고 찍혀 있는데, 우리 회사 회계 장부에는 90만 원만 기록되어 있다면 10만 원이 비는 큰 사고입니다.
사람이 매일 눈으로 엑셀을 열고 맞출 수 없으니, 시스템이 양쪽의 데이터(Source vs Target)를 가져와서 자동으로 비교하고 틀린 금액을 찾아내는(Mismatch) 것이 대사 작업입니다.

**초보자가 알아야 할 흐름:**
1. **Source (기준):** 은행이나 외부 시스템의 원천 데이터
2. **Target (비교 대상):** 우리 회사의 회계 전표(`journal-ledger` 데이터)
3. **매칭 엔진:** 양쪽 데이터를 놓고 허용오차(Tolerance) 범위를 넘는지 비교합니다.
4. **조정 분개 (Adjustment):** 만약 카드사 수수료 같은 정당한 이유로 차이가 난다면, 시스템이 부족한 차이를 메우기 위해 자동으로 조정 분개(전표)를 끊어 장부를 맞춰줍니다.

---

## 2. 🔄 최근 고도화 내용 및 아키텍처

- **헥사고날 원칙 준수:** 과거에는 `journal-ledger`의 내부 DB(Repository)를 몰래 훔쳐보던 구조였지만, 이제는 `contracts` 모듈의 `JournalQueryPort`를 통해 합법적으로 장부 데이터를 받아오도록 수정되었습니다.
- **외부 스냅샷 포트:** 심화 대사의 `SOURCE`/`INTERFACE` 단계는 `matchingRulesJson`의 고정 금액이 아니라 `ExternalReconSnapshotPort`를 통해 외부 원천/인터페이스 스테이징 데이터를 집계합니다. 기본 구현은 `RECON_EXTERNAL_STAGE_RECORD`를 DB에서 합산하는 Mock/스테이징 어댑터입니다.
- **도메인 정책 (Policy) 도입:** 오차가 얼마까지 허용되는지 판단하는 `ReconciliationTolerancePolicy` 도메인 규칙이 새롭게 적용되었습니다.
- **성능 개선:** 메인 대사의 전표 대상 집계는 `JournalQueryPort.getJournalDetailAggregate`로 DB 단 합계를 사용하고, 심화 대사의 외부 스테이지도 스테이징 테이블 집계 쿼리를 사용합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
```bash
docker-compose up -d reconciliation
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :reconciliation:bootRun
```
