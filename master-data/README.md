# 🏢 Master Data Service (기준 정보 모듈)

`master-data` 모듈은 전사 회계 및 재무 시스템의 근간이 되는 '기준 정보(Reference Data)'를 중앙에서 관리하는 핵심 도메인입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 기준 정보(Master Data)란 무엇인가요?**
회사에서 사용하는 **계정과목, 거래처, 부서, 통화, 환율, 회계 기수, 상품** 등 바뀌지 않는 기본 뼈대 데이터를 의미합니다.
회계 장부(`journal-ledger`)나 대출(`loan`) 부서에서 전표를 끊을 때마다 "이 거래처 이름이 뭐더라?" 할 때 참조하는 '단일 진실의 원천(Source of Truth)'입니다.

**Q. 왜 다른 모듈에서 직접 DB를 보지 않고 이 모듈을 통하나요?**
만약 거래처 이름이 바뀌었을 때, 모든 모듈의 DB에 그 이름이 퍼져 있다면 수정하기가 불가능합니다.
그래서 다른 모듈들은 거래처나 부서의 '코드(ID)'만 가지고 있고, 실제 이름과 상세 정보는 `master-data`에 물어봐서 가져오는 **참조(Reference) 방식**을 사용합니다.

---

## 2. 🔄 핵심 아키텍처 및 처리 흐름 (Process Flow)

### 📌 헥사고날 아키텍처 (DDD)
외부 요청은 철저히 통제된 경로로만 도메인에 접근합니다.
`Controller` -> `UseCase` -> `Application Service` -> `Output Port` -> `JPA Adapter` -> `DB`

### 📌 SCD2 (Slowly Changing Dimension Type 2) 이력 관리
기준 정보는 함부로 `UPDATE`나 `DELETE`를 하면 안 됩니다. (과거 전표의 기록이 틀어지기 때문입니다.)
본 시스템은 정보 변경 시, 기존 데이터의 `validTo` 날짜를 닫고(Terminate) 새로운 데이터를 추가하는 **SCD2 방식**을 원칙으로 하여 완벽한 과거 이력 추적을 지원합니다.

### 📌 승인 및 변경 통제 (Change Request Flow)
계정과목이나 거래처 같은 중요 정보는 담당자가 마음대로 바꿀 수 없습니다.
1. **요청자:** 변경 요청(`MasterDataChangeRequest`) 생성 (상태: `REQUESTED`)
2. **승인자 (다른 사람):** 검토 후 승인 (상태: `APPROVED`)
3. **시스템:** 오늘 또는 미래의 효력 발생일(`effectiveDate`)에 도달하면 실제 데이터 반영 (상태: `APPLIED`)

---

## 3. 💾 관리하는 주요 도메인 (Schema)

- `AccountSubject`: 계정과목 및 보고서 분류표.
- `BusinessPartner`: 고객, 벤더, 은행 등 거래 상대방.
- `Department`: 조직, 비용 부서(Cost Center). (참고: `auth` 모듈은 부서 마스터를 복사하지 않고 부서 코드만 참조합니다.)
- `Currency` & `ExchangeRate`: 통화 및 일자별 환율.
- `FiscalPeriod`: 회계 기수 및 마감 상태.
- `Product`: 금융 상품 마스터.

---

## 4. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
멀티스테이지 Dockerfile을 통해 빌드되며, 통합 환경에서 Eureka/Config 의존성을 물고 자동으로 구동됩니다.
```bash
docker-compose up -d master-data
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :master-data:bootRun
```
