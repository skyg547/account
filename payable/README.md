# 💳 Payable Service (매입 채무 관리)

`payable` 모듈은 회사가 외부(공급업체, 거래처)에 '갚아야 할 빚'을 관리하는 매입채무(Accounts Payable, AP) 전용 서브레저입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

회사가 물건을 사고 아직 돈을 주지 않았다면 '외상값(매입채무)'이 생깁니다.
- **발생:** 구매팀에서 노트북 100대를 사고 세금계산서를 끊어오면, `payable` 모듈에 "A업체에 1,000만 원 줘야 함"이라고 외상값이 쌓입니다.
- **지급:** 나중에 재무팀이 통장에서 A업체로 1,000만 원을 송금하면 외상값이 지워집니다(반제, Settlement).

이러한 과정을 통해 "우리가 누구한테, 언제까지, 얼마를 줘야 하는지"를 꼼꼼하게 관리하는 곳입니다.

---

## 2. 🔄 아키텍처 및 연동

- **전표 생성:** 외상값이 생기거나 갚을 때마다 `contracts` 모듈의 `JournalPostingPort`를 호출해 `journal-ledger`에 전표를 넘깁니다.
- **마스터 참조:** 거래처(A업체)가 유효한지 `master-data`의 `MasterDataQueryPort`로 확인합니다.
- **드릴다운 지원:** 회계 전표에서 "이 외상값 어디서 났어?" 하고 물어보면 원천 구매 내역을 보여주는 `SourceDocumentProvider` 역할을 수행해야 합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
```bash
docker-compose up -d payable
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :payable:api:bootRun
```
