# 📝 Module Sequential Review (2026-05-06)

> ⚠️ **공지:** 이 과거 검수 이력은 **[TOTAL_QUALITY_REPORT.md](./TOTAL_QUALITY_REPORT.md)**의 히스토리 세션에 통합되었습니다.

---


## 1) 컴파일 점검 결과

- `:master-data:compileJava` -> **SUCCESS**
- `:journal-ledger:core:compileJava` -> **SUCCESS**
- `:receivable:compileJava` -> **FAIL**
- `:expenditure-resolution:compileJava` -> **FAIL**
- `:tax:compileJava :payable:compileJava :closing:core:compileJava` -> **SUCCESS**

---

## 2) 주요 Findings (심각도 순)

### Critical

1. `receivable` 모듈 전면 컴파일 실패 (BOM 인코딩 문제)
- 증상: `illegal character: '\ufeff'` 다수 발생.
- 범위: `receivable/src/main/java/com/ho/account/receivable/**` 하위 Java 파일 28개.
- 근거 예시: [receivable/src/main/java/com/ho/account/receivable/adapter/in.web/CollectionController.java:1](receivable/src/main/java/com/ho/account/receivable/adapter/in.web/CollectionController.java#L1)
- 영향: 모듈 빌드 불가, 배포/병합 불가.

2. `expenditure-resolution` 모듈 컴파일 실패 (`DepartmentPersistencePort` 시그니처 불일치)
- `DepartmentPersistencePort`에서 `findByCode`가 제거되고 `findActiveByCode`로 변경됐지만 호출부 미반영.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/BudgetControlAdapter.java:36](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/BudgetControlAdapter.java#L36)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:65](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L65)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/port/out/DepartmentPersistencePort.java:16](master-data/src/main/java/com/ho/account/masterdata/core/application/port/out/DepartmentPersistencePort.java#L16)
- 영향: 지출결의 모듈 빌드 불가.

3. 리스 지급 결의 생성 시 차/대 계정이 동일해질 수 있는 재무 흐름 오류
- `MonolithLeasePaymentResolutionAdapter`가 `command.accountCode()`를 지급계정/상세비용계정에 동시에 세팅.
- 결과적으로 승인 시 전표 생성 로직에서 차변/대변이 동일 계정으로 상계될 위험이 큼.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java:27](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java#L27)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java:33](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java#L33)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:223](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L223)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:239](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L239)
- 영향: 리스 지급 회계 분개의 실질 정보가 소실될 수 있음.

### High

4. 승인 전표 생성 시 핵심 마스터 조회를 `orElse(null)` 처리 (실패 은닉)
- 계정과목/거래처/부서 미존재를 예외로 끊지 않고 null 라인 생성 가능.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:220](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L220)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:223](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L223)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:224](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L224)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:239](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L239)
- 영향: 런타임 NPE/불완전 전표 저장 리스크.

5. API 정보 회귀 가능성 (`ExpenditureResolutionDto` 이름 필드 상시 null)
- `departmentName`, `paymentAccountName`, `accountSubjectName`, `businessPartnerName`이 의도적으로 null 반환.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:74](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L74)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:76](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L76)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:174](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L174)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:177](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L177)
- 영향: 화면/리포트 표시 품질 저하, 하위 호환성 저하 가능.

6. `receivable` 테스트 패키지 불일치
- 파일 경로는 `receivable/...`인데 패키지는 여전히 `com.ho.account.income`.
- 근거: [receivable/src/test/java/com/ho/account/receivable/domain/ReceivableTest.java:1](receivable/src/test/java/com/ho/account/receivable/domain/ReceivableTest.java#L1)
- 영향: 테스트 컴파일/실행 시 실패 가능.

### Medium

7. Hexagonal 경계 누수: 웹 어댑터가 도메인 엔티티를 직접 입출력
- 컨트롤러가 DTO 대신 도메인 엔티티를 HTTP 경계에 노출.
- 근거:
  - [receivable/src/main/java/com/ho/account/receivable/adapter/in.web/CollectionController.java:23](receivable/src/main/java/com/ho/account/receivable/adapter/in.web/CollectionController.java#L23)
  - [receivable/src/main/java/com/ho/account/receivable/adapter/in.web/SalesController.java:22](receivable/src/main/java/com/ho/account/receivable/adapter/in.web/SalesController.java#L22)
- 영향: 도메인 불변식 우회/직렬화 결합도 증가.

8. 주석 정합성 오류: 구현과 설명 불일치
- `JournalRuleEngine` 주석에 “스켈레톤/빈 DRAFT 반환”이라 쓰여 있으나 실제로 규칙 평가/분개 생성 로직 구현됨.
- 근거:
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:71](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java#L71)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:224](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java#L224)
- 영향: 유지보수자 오판 가능.

9. 주석 정합성 오류: “ID 기반” 주석과 실제 코드(코드 기반 조회) 불일치
- 근거: [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:97](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L97)
- 영향: 설계 의도 혼선.

10. SCD2 선언과 서비스 구현 간 간극
- `Product`, `Department`는 SCD2 취지 주석을 갖지만, 서비스는 기존 행 직접 업데이트 방식을 사용.
- 근거:
  - [master-data/src/main/java/com/ho/account/masterdata/core/domain/model/Product.java:14](master-data/src/main/java/com/ho/account/masterdata/core/domain/model/Product.java#L14)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:62](master-data/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L62)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java:76](master-data/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java#L76)
- 영향: 이력 관리 정책과 실제 동작의 괴리.

---

## 3) DDD / Hexagonal 관점 종합

- `master-data`, `journal-ledger`는 포트/어댑터 방향성은 유지됨.
- `expenditure-resolution`은 코드 기반 참조 전환 중이며, 포트 시그니처 전파 누락으로 계층 정합성이 깨진 상태.
- `receivable`는 패키지 리네임 자체는 진행됐으나 인코딩/테스트 패키지 불일치로 모듈 완결성이 부족.
- 웹 경계에서 도메인 엔티티 직접 노출은 헥사고날 경계 분리에 부적합.

---

## 4) 재무 업무 흐름 타당성 종합

- 매출채권 수납-매칭 2단계 분개 흐름 자체는 의도상 타당.
- 지출결의 승인 시 분개 생성의 큰 흐름(상세 차변 합산 + 지급계정 대변)은 타당.
- 다만 리스 지급 어댑터의 계정 매핑 방식은 실무 회계 흐름을 왜곡할 수 있어 보완이 필요.
- 승인 시 마스터 누락을 null로 허용하는 구현은 재무 전표 품질 측면에서 부적합.

---

## 5) 결론

- 현재 상태는 `master-data`, `journal-ledger`, `tax`, `payable`, `closing` 컴파일은 통과.
- `receivable`, `expenditure-resolution` 컴파일 실패로 전체 안정 배포는 불가.
- 우선순위는 다음이 적절함:
  1. `receivable` 인코딩(BOM) 정리 + 테스트 패키지 정합화
  2. `expenditure-resolution`의 `findByCode` 호출 전량 정리
  3. 리스 지급 계정 매핑 정책 재설계(차/대 계정 분리)
  4. API DTO 이름 필드 보강 전략(조회 어댑터 또는 응답 모델 재정의)

---

## 6) 후속 조치 결과 (2026-05-06, same day)

후속 수정 적용:
- `receivable` Java 파일 BOM 제거 및 테스트 패키지 정합화
- `expenditure-resolution`의 `DepartmentPersistencePort.findByCode` 호출을 `findActiveByCode`로 정리

재검증 결과:
- `:receivable:compileJava` **SUCCESS**
- `:expenditure-resolution:compileJava` **SUCCESS**
- `:receivable:test` **SUCCESS**
- `:master-data:compileJava :journal-ledger:core:compileJava :receivable:compileJava :expenditure-resolution:compileJava :tax:compileJava :payable:compileJava :closing:core:compileJava` **SUCCESS**

현재 잔여 이슈:
- 리스 지급 결의의 차/대 계정 분리 정책 설계 필요(업무 규칙성 리스크)
- `ExpenditureResolutionService` 내 일부 `orElse(null)` 처리로 전표 품질 리스크
- `JournalRuleEngine` 주석과 구현 불일치(문서 정합성)

---

## 7) Gemini/Codex 통합 최종 검수 (2026-05-06)

검수 대상:
- Gemini 최근 변경: `2298c42`, `88e8672`
- Codex 최근 변경: `08d271f`

재검증:
- `:master-data:compileJava :journal-ledger:core:compileJava :receivable:compileJava :expenditure-resolution:compileJava :tax:compileJava :payable:compileJava :closing:core:compileJava` **SUCCESS**
- `:receivable:test` **SUCCESS**
- `frontend npm run build` **SUCCESS** (unused import/variable ESLint warning만 존재)

### Final Findings

1. Critical: 리스 지급 결의가 차/대 동일 계정 분개를 만들 수 있음
- `MonolithLeasePaymentResolutionAdapter`가 `resolution.paymentAccountCode`와 `detail.accountCode`를 모두 `command.accountCode()`로 세팅.
- 이후 `ExpenditureResolutionService.buildJournalEntry()`는 상세를 차변, `paymentAccountCode`를 대변으로 사용.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java:28](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java#L28)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java:33](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java#L33)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:223](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L223)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:239](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L239)
- 판단: 컴파일은 통과하지만 회계 의미가 훼손될 수 있어 가장 우선 보완 대상.

2. High: 원장 대량처리 최적화가 주장만큼 구현되지 않음
- `WORKLOG.md`에는 `updateLedgerBalancesBulk`가 “메모리 기반 집계 후 벌크 업데이트” 및 “N+1 해결”로 기록돼 있음.
- 실제 구현은 날짜별 그룹화 후에도 `updateDailyBalances()`에서 각 `JournalDetail`에 대해 `updateLedgerBalances()`를 그대로 호출.
- `updateLedgerBalances()` 내부는 건별 조회/저장을 수행하므로 대량처리 관점에서 성능 개선 근거가 약함.
- 근거:
  - [WORKLOG.md:198](WORKLOG.md#L198)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/LedgerService.java:255](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/LedgerService.java#L255)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/LedgerService.java:271](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/LedgerService.java#L271)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/LedgerService.java:275](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/LedgerService.java#L275)
- 판단: 기능 회귀보다는 성능/기록 정합성 이슈지만, 저장소 레벨 벌크 전략이 없는 현재 구조는 대용량 요구사항과 맞지 않음.

3. High: 승인 전표 생성 시 마스터 조회 실패를 null로 허용
- 승인 시 부서/계정/거래처를 조회하면서 `orElse(null)`을 사용.
- 마스터 누락이 즉시 실패로 드러나지 않고, 불완전 전표 또는 후속 NPE/영속성 오류로 전이될 수 있음.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:220](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L220)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:223](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L223)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:224](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L224)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:239](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L239)

4. High: 지출결의 DTO 이름 필드가 항상 null
- 도메인 분리 취지는 이해되지만 API DTO에서 `departmentName`, `paymentAccountName`, `accountSubjectName`, `businessPartnerName`을 모두 null로 반환.
- 기존 화면/호출자가 이름 필드를 사용한다면 회귀 가능성이 큼.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:74](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L74)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:76](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L76)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:174](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L174)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java:177](expenditure-resolution/src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionDto.java#L177)

5. Medium: 웹 어댑터가 도메인 엔티티를 직접 HTTP 경계에 노출
- `receivable` 컨트롤러가 요청/응답 DTO 없이 도메인 엔티티를 직접 사용.
- 헥사고날 경계가 약해지고, 도메인 구조 변경이 API 계약에 바로 전파됨.
- 근거:
  - [receivable/src/main/java/com/ho/account/receivable/adapter/in.web/CollectionController.java:23](receivable/src/main/java/com/ho/account/receivable/adapter/in.web/CollectionController.java#L23)
  - [receivable/src/main/java/com/ho/account/receivable/adapter/in.web/SalesController.java:22](receivable/src/main/java/com/ho/account/receivable/adapter/in.web/SalesController.java#L22)

6. Medium: 주석과 구현이 여전히 어긋남
- `JournalRuleEngine` 주석은 아직 “스켈레톤/빈 DRAFT 반환”이라 설명하지만 실제 코드는 규칙 평가와 분개 라인 생성을 수행.
- `ExpenditureResolutionService`는 코드 기반 조회로 바뀌었는데 “ID 기반” 주석이 남아 있음.
- 근거:
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:71](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java#L71)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:244](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java#L244)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:97](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L97)

7. Medium: SCD2 선언과 서비스 동작이 일치하지 않음
- `Product`는 SCD2 엔티티로 설명하지만 `ProductService.updateProduct()`는 기존 행을 직접 수정.
- `DepartmentService.updateDepartment()`도 같은 방식이라 이력형 마스터 정책과 구현 사이 간극이 남아 있음.
- 근거:
  - [master-data/src/main/java/com/ho/account/masterdata/core/domain/model/Product.java:14](master-data/src/main/java/com/ho/account/masterdata/core/domain/model/Product.java#L14)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:62](master-data/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L62)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java:76](master-data/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java#L76)

### 최종 판단

- 최근 Gemini/Codex 작업으로 컴파일 블로커는 해소되었고, 현재 백엔드 주요 모듈과 프론트 빌드는 통과한다.
- 다만 남은 문제는 “빌드 성공” 이후 단계의 문제들이다:
  - 회계 정책 오매핑 가능성
  - 대량처리 성능 주장과 실제 구현의 차이
  - API 응답 회귀 가능성
  - 주석/설계 문서와 현재 구현 간 불일치
- 따라서 현재 상태는 “배포 전 정책/설계 보완이 필요한 통과 상태”로 판단한다.

---

## 8) 전수 검수 1차 - 코어/계약 계층 (2026-05-06)

검수 범위:
- `shared-kernel`
- `contracts`
- `master-data`
- `governance`

확인 문서:
- 각 모듈 `README.md`
- 각 모듈 `docs/README.md`
- 각 모듈 `docs/beginner-guide.md`
- 각 모듈 `docs/process-flow.md`
- 각 모듈 `docs/schema.md`

재검증:
- `:shared-kernel:compileJava :contracts:compileJava :master-data:test :governance:test` **SUCCESS**

### Findings

1. High: `governance` 승인 연계가 `master-data` 변경요청의 유효일자/버전을 보존하지 않음
- `master-data` 변경요청은 `effectiveDate`, `requestedVersion`을 핵심 필드로 받는다.
- 그러나 `governance`의 승인 API/유스케이스는 이 필드를 받지 않고, 어댑터는 승인 시점마다 `LocalDate.now()`와 고정 버전 `1`을 넣어 즉시 반영한다.
- 결과적으로 미래 효력일 예약, 버전 이력 관리, 승인-적용 분리가 무너질 수 있다.
- 근거:
  - [governance/src/main/java/com/ho/account/audit/infrastructure/masterdata/MasterDataChangeRequestAdapter.java:24](governance/src/main/java/com/ho/account/audit/infrastructure/masterdata/MasterDataChangeRequestAdapter.java#L24)
  - [governance/src/main/java/com/ho/account/audit/infrastructure/masterdata/MasterDataChangeRequestAdapter.java:25](governance/src/main/java/com/ho/account/audit/infrastructure/masterdata/MasterDataChangeRequestAdapter.java#L25)
  - [governance/src/main/java/com/ho/account/audit/application/port/in/MasterApprovalUseCase.java:16](governance/src/main/java/com/ho/account/audit/application/port/in/MasterApprovalUseCase.java#L16)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/command/MasterDataChangeRequestCommand.java:14](master-data/src/main/java/com/ho/account/masterdata/core/application/command/MasterDataChangeRequestCommand.java#L14)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/command/MasterDataChangeRequestCommand.java:15](master-data/src/main/java/com/ho/account/masterdata/core/application/command/MasterDataChangeRequestCommand.java#L15)
  - [master-data/src/main/java/com/ho/account/masterdata/api/dto/MasterDataChangeRequestCreateDto.java:13](master-data/src/main/java/com/ho/account/masterdata/api/dto/MasterDataChangeRequestCreateDto.java#L13)
  - [master-data/src/main/java/com/ho/account/masterdata/api/dto/MasterDataChangeRequestCreateDto.java:14](master-data/src/main/java/com/ho/account/masterdata/api/dto/MasterDataChangeRequestCreateDto.java#L14)
  - [master-data/README.md:53](master-data/README.md#L53)

2. High: `master-data` 소스 파일 다수에 문자열 인코딩 깨짐 존재
- 주석만 아니라 예외 메시지 문자열도 깨져 있어 운영 시 API 오류 메시지가 읽기 어려울 가능성이 높다.
- DDD/헥사고날 구조 이전에 유지보수성과 운영 가독성에 직접 손상이 있다.
- 예시 근거:
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java:25](master-data/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java#L25)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java:33](master-data/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java#L33)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java:15](master-data/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java#L15)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/port/out/AccountSubjectPersistencePort.java:8](master-data/src/main/java/com/ho/account/masterdata/core/application/port/out/AccountSubjectPersistencePort.java#L8)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/command/MasterDataChangeRequestCommand.java:8](master-data/src/main/java/com/ho/account/masterdata/core/application/command/MasterDataChangeRequestCommand.java#L8)

3. Medium: `governance`의 `TracingService`가 포트 계층을 우회하고 Repository를 직접 사용
- `governance` 문서는 application service가 `application.port.out`에 의존하는 구조를 설명한다.
- 실제 `TracingService`는 `AuditLogRepository`를 직접 주입받아 사용한다.
- 근거:
  - [governance/docs/README.md:18](governance/docs/README.md#L18)
  - [governance/src/main/java/com/ho/account/audit/service/TracingService.java:4](governance/src/main/java/com/ho/account/audit/service/TracingService.java#L4)
  - [governance/src/main/java/com/ho/account/audit/service/TracingService.java:16](governance/src/main/java/com/ho/account/audit/service/TracingService.java#L16)
  - [governance/src/main/java/com/ho/account/audit/service/TracingService.java:34](governance/src/main/java/com/ho/account/audit/service/TracingService.java#L34)

4. Medium: `governance` API 경계가 DTO 대신 도메인 엔티티와 `Map`에 기대고 있음
- 응답에서 `AuditLog`, `SystemRole`, `Authorization`, `MasterApproval`를 직접 노출한다.
- 요청도 `Map<String, String>` 기반으로 받아 컴파일 타임 계약이 약하다.
- `master-data` 컨트롤러가 DTO/Command 경계를 지키는 것과 대비된다.
- 근거:
  - [governance/src/main/java/com/ho/account/audit/web/AuditController.java:32](governance/src/main/java/com/ho/account/audit/web/AuditController.java#L32)
  - [governance/src/main/java/com/ho/account/audit/web/AuditController.java:57](governance/src/main/java/com/ho/account/audit/web/AuditController.java#L57)
  - [governance/src/main/java/com/ho/account/audit/web/AuditController.java:67](governance/src/main/java/com/ho/account/audit/web/AuditController.java#L67)
  - [governance/src/main/java/com/ho/account/audit/web/AuditController.java:78](governance/src/main/java/com/ho/account/audit/web/AuditController.java#L78)
  - [governance/src/main/java/com/ho/account/audit/web/AuditController.java:85](governance/src/main/java/com/ho/account/audit/web/AuditController.java#L85)
  - [master-data/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java:23](master-data/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java#L23)
  - [master-data/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:28](master-data/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L28)

5. Medium: `master-data`의 SCD2 설명과 서비스 동작 간 간극이 1차 범위에서도 계속 남아 있음
- `Product`, `Department`는 SCD2 의도를 문서/엔티티에 명시하지만 서비스는 기존 행 직접 업데이트를 유지한다.
- 이 이슈는 이전 통합 검수에서 지적된 내용이 아직 그대로 남아 있다.
- 근거:
  - [master-data/src/main/java/com/ho/account/masterdata/core/domain/model/Product.java:14](master-data/src/main/java/com/ho/account/masterdata/core/domain/model/Product.java#L14)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:62](master-data/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L62)
  - [master-data/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java:76](master-data/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java#L76)

### 판단

- `shared-kernel`과 `contracts`는 현재 범위에서 컴파일/구조상 큰 이상은 없었다.
- 1차 범위의 핵심 리스크는 `governance`와 `master-data` 사이 승인-적용 정책 손실, 그리고 `master-data` 문자열 인코딩 품질 문제다.
- 다음 단계(2차 회계 엔진 계층)로 넘어가기 전에, 위 두 항목은 추후 수정 우선순위 상단에 유지하는 것이 적절하다.

---

## 9) 전수 검수 2차 - 회계 엔진 계층 (2026-05-06)

검수 범위:
- `journal-ledger`
- `closing`
- `reconciliation`
- `reporting`

확인 문서:
- `journal-ledger/README.md`
- `journal-ledger/docs/README.md`
- `journal-ledger/docs/beginner-guide.md`
- `journal-ledger/docs/process-flow.md`
- `journal-ledger/docs/schema.md`
- `journal-ledger/docs/ledger-carry-forward.md`
- `journal-ledger/core/src/main/java/com/ho/account/journalledger/adapter/README.md`
- `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/README.md`
- `journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/README.md`
- `closing/docs/README.md`
- `closing/docs/beginner-guide.md`
- `closing/docs/process-flow.md`
- `closing/docs/schema.md`
- `reconciliation/docs/README.md`
- `reconciliation/docs/beginner-guide.md`
- `reconciliation/docs/process-flow.md`
- `reconciliation/docs/schema.md`
- `reporting/README.md`
- `reporting/docs/README.md`
- `reporting/docs/beginner-guide.md`
- `reporting/docs/process-flow.md`
- `reporting/docs/schema.md`
- `reporting/docs/architecture.md`
- `reporting/docs/external_reporting_guide.md`

재검증:
- `:journal-ledger:core:test` **FAIL**
- `:journal-ledger:api:test` **FAIL**
- `:journal-ledger:batch:compileJava` **SUCCESS**
- `:closing:core:test` **FAIL**
- `:closing:api:compileJava` **SUCCESS**
- `:closing:batch:compileJava` **SUCCESS**
- `:reconciliation:compileJava` **SUCCESS**
- `:reporting:core:test` **SUCCESS**
- `:reporting:api:test` **SUCCESS**
- `:reporting:batch:test` **SUCCESS**

### Findings

1. Critical: `journal-ledger`에 동일한 의미의 전기 API가 둘인데 회계 효과가 다름
- `/api/journals/{id}/post`는 `JournalUseCase.postJournalEntry`를 호출하고, 실제 구현은 전표 상태를 `POSTED`로 바꾼 뒤 저장만 한다.
- `/api/ledger/post/{journalEntryId}`만 `PostingService.postJournalEntry`를 통해 GL/SL 엔트리 생성과 잔액 갱신을 수행한다.
- 같은 "post"라는 용어를 쓰지만 하나는 상태 전환만, 다른 하나는 실제 원장 반영까지 수행한다. 호출자가 잘못된 경로를 쓰면 `POSTED` 상태 전표가 원장 상세 없이 남을 수 있어 재무 흐름상 위험하다.
- 문서도 두 경로를 함께 노출하고 있어 운영자/개발자 혼동 가능성이 높다.
- 근거:
  - [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:90](journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L90)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalEntryService.java:233](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalEntryService.java#L233)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalEntryService.java:243](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalEntryService.java#L243)
  - [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/ledger/GlSlController.java:52](journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/ledger/GlSlController.java#L52)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/PostingService.java:115](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/PostingService.java#L115)
  - [journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/PostingService.java:196](journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/PostingService.java#L196)
  - [journal-ledger/docs/process-flow.md:98](journal-ledger/docs/process-flow.md#L98)
  - [journal-ledger/docs/beginner-guide.md:76](journal-ledger/docs/beginner-guide.md#L76)
  - [journal-ledger/docs/beginner-guide.md:82](journal-ledger/docs/beginner-guide.md#L82)

2. High: `journal-ledger:api`가 현재 컴파일되지 않음
- `LedgerController`가 아직 `DepartmentPersistencePort.findByCode(deptCode)`를 호출한다.
- 포트 시그니처가 `findActiveByCode` 기준으로 정리된 이후 남은 구 호출이라, API 모듈이 현재 빌드 불가 상태다.
- 근거:
  - [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/ledger/LedgerController.java:110](journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/ledger/LedgerController.java#L110)

3. High: `closing:core`의 `determineClosingStatus`가 테스트 계약과 구현 모두에서 어긋나 있음
- 테스트는 "필수 태스크 미완료"와 "게이트 미통과"를 구분한 메시지를 기대한다.
- 실제 서비스는 둘 다 단일 일반 메시지로 묶어 던진다.
- 또한 `calendar.getStatus().name()`을 먼저 호출해, 영속 전 기본값(`@PrePersist`)이 적용되지 않은 테스트/메모리 객체에서는 null 상태 NPE가 발생한다.
- 이 문제는 단순 테스트 깨짐이 아니라 서비스 계약이 경계 밖에서 안전하지 않다는 뜻이다.
- 근거:
  - [closing/core/src/test/java/com/ho/account/closing/application/service/ClosingServiceTest.java:130](closing/core/src/test/java/com/ho/account/closing/application/service/ClosingServiceTest.java#L130)
  - [closing/core/src/test/java/com/ho/account/closing/application/service/ClosingServiceTest.java:153](closing/core/src/test/java/com/ho/account/closing/application/service/ClosingServiceTest.java#L153)
  - [closing/core/src/test/java/com/ho/account/closing/application/service/ClosingServiceTest.java:181](closing/core/src/test/java/com/ho/account/closing/application/service/ClosingServiceTest.java#L181)
  - [closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java:382](closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java#L382)
  - [closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java:388](closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java#L388)
  - [closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java:64](closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java#L64)
  - [closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java:220](closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java#L220)

4. High: `reconciliation`은 아직 헥사고날 경계를 지키지 못했고, 결과도 더미 값 기반
- 서비스가 `journal-ledger`의 Repository와 Entity를 직접 import하고 생성자 주입받는다.
- 이는 `contracts` 또는 `port.out`을 통한 연동이 아니라 타 모듈 내부 구현에 직접 묶이는 구조다.
- 동시에 실제 대사 금액 추출 로직 대신 `1000.00` vs `950.00`, 건수 `10` vs `9`를 하드코딩해 결과를 만든다.
- 즉 컴파일은 되지만 업무상 "대사가 수행되었다"는 결과를 신뢰하기 어렵다.
- 근거:
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:17](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L17)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:21](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L21)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:40](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L40)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:51](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L51)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:358](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L358)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:360](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L360)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:361](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L361)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java:105](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java#L105)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java:118](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java#L118)

5. Medium: `closing` 자동 분개는 더미 계정으로 남아 있어 운영형 재무 흐름으로 보기 어려움
- 평가/충당 배치가 생성하는 자동 전표는 차변 `999998`, 대변 `999999`를 그대로 사용한다.
- 문서도 임시 구현임을 인정하지만, 현재 상태로 실제 결산 배치가 실행되면 정책 미정 계정으로 전표가 발생한다.
- 근거:
  - [closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java:418](closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java#L418)
  - [closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java:419](closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java#L419)
  - [closing/docs/beginner-guide.md:106](closing/docs/beginner-guide.md#L106)
  - [closing/docs/process-flow.md:112](closing/docs/process-flow.md#L112)

6. Medium: `journal-ledger:core` 테스트 소스가 구 패키지 import를 사용해 회귀 검증이 막혀 있음
- 메인 코드는 컴파일되지만, `JournalRuleEngineTest`가 더 이상 존재하지 않는 `com.ho.account.journal.repository.*`를 import한다.
- 실제 기능 회귀를 막아주는 테스트가 빌드 파이프라인에서 실행조차 되지 않는 상태다.
- 근거:
  - [journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngineTest.java:3](journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngineTest.java#L3)
  - [journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngineTest.java:4](journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngineTest.java#L4)
  - [journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngineTest.java:5](journal-ledger/core/src/test/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngineTest.java#L5)

7. Medium: `reporting`은 구조는 분리됐지만 실제 원장 연동 대신 목업 어댑터와 단일 예시 라인만 사용
- `LedgerClientAdapter`는 원장 잔액 두 개(`101`, `102`)와 과거 보고서 한 줄(`ASSET_CASH`)을 하드코딩해 반환한다.
- `ReportingService`도 현재는 재무상태표에서 `ASSET_CASH` 한 줄만 계산한다.
- `ReportingController`는 이 도메인 객체를 그대로 반환한다.
- 따라서 테스트가 통과해도 "실제 원장을 읽어 표준 재무제표를 생성한다"는 문서 수준에는 아직 도달하지 못했다.
- 근거:
  - [reporting/README.md:21](reporting/README.md#L21)
  - [reporting/docs/architecture.md:23](reporting/docs/architecture.md#L23)
  - [reporting/docs/process-flow.md:20](reporting/docs/process-flow.md#L20)
  - [reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java:21](reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java#L21)
  - [reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java:32](reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java#L32)
  - [reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java:51](reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java#L51)
  - [reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java:68](reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java#L68)
  - [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:30](reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L30)

8. Medium: `closing`, `reconciliation` 소스의 주석/문자열 인코딩 깨짐이 여전히 심함
- `closing` 도메인과 `reconciliation` 서비스는 한글 주석과 일부 설명 문자열이 깨진 상태다.
- Phase 1에서 확인한 `master-data` 문제와 동일한 유형이며, 이번 2차 범위에서도 유지보수성과 운영 가독성을 떨어뜨린다.
- 근거:
  - [closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java:8](closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java#L8)
  - [closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java:47](closing/core/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java#L47)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:27](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L27)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:380](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L380)
  - [reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:430](reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java#L430)

### 판단

- 2차 범위는 "빌드 일부 성공"보다 "회계 흐름이 하나로 수렴하는가"가 더 중요하다.
- 현재 기준으로 가장 먼저 손봐야 할 곳은 `journal-ledger` 전기 경로 통합, `closing`의 상태 판정 계약 정리, `reconciliation`의 계약 기반 연동 전환이다.
- `reporting`은 테스트는 녹색이지만 실제 운영 연동이 아직 목업 단계이므로, 회계 엔진의 최종 산출물로 신뢰하기엔 이르다.

---

## 10) 전수 검수 3차 - 업무 서브레저/ERP·Banking 계층 (2026-05-08)

검수 범위:
- `tax`
- `payable`
- `receivable`
- `expenditure-resolution`
- `asset-lease`
- `loan:core`
- `loan:api`
- `loan:batch`
- 연관 변경분: `contracts`, `master-data`

확인 문서:
- `tax/README.md`
- `tax/docs/README.md`
- `tax/docs/beginner-guide.md`
- `tax/docs/process-flow.md`
- `tax/docs/schema.md`
- `payable/README.md`
- `payable/docs/README.md`
- `payable/docs/beginner-guide.md`
- `payable/docs/process-flow.md`
- `payable/docs/schema.md`
- `receivable/README.md`
- `receivable/docs/README.md`
- `receivable/docs/beginner-guide.md`
- `receivable/docs/process-flow.md`
- `receivable/docs/schema.md`
- `expenditure-resolution/docs/README.md`
- `expenditure-resolution/docs/beginner-guide.md`
- `expenditure-resolution/docs/process-flow.md`
- `expenditure-resolution/docs/schema.md`
- `asset-lease/docs/README.md`
- `asset-lease/docs/beginner-guide.md`
- `asset-lease/docs/process-flow.md`
- `asset-lease/docs/schema.md`
- `loan/docs/README.md`
- `loan/docs/beginner-guide.md`
- `loan/docs/process-flow.md`
- `loan/docs/schema.md`
- `docs/loan_accounting.md`
- `docs/db/README.md`

재검증:
- `:contracts:compileJava` **SUCCESS**
- `:tax:compileJava` **SUCCESS**
- `:tax:test` **FAIL**
- `:payable:test` **SUCCESS**
- `:receivable:compileJava` **SUCCESS**
- `:receivable:test` **SUCCESS**
- `:expenditure-resolution:test` **FAIL**
- `:asset-lease:compileJava` **SUCCESS**
- `:asset-lease:test` **SUCCESS**
- `:loan:core:compileJava` **SUCCESS**
- `:loan:api:compileJava` **SUCCESS**
- `:loan:batch:compileJava` **SUCCESS**

### Findings

1. High: `tax:test`가 테스트 소스 컴파일 단계에서 실패함
- `TaxInvoice` 도메인은 setter를 제거하고 정적 팩토리(`create`)와 `updateInfo` 중심으로 변경되어 있다.
- 하지만 `TaxInvoiceTest`는 여전히 `setSupplyAmount`, `setTaxAmount`, `setTotalAmount`를 호출한다.
- 결과적으로 메인 코드는 컴파일되지만 도메인 금액 검증 회귀 테스트가 실행되지 못한다.
- 근거:
  - [tax/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java:15](tax/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java#L15)
  - [tax/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java:16](tax/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java#L16)
  - [tax/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java:17](tax/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java#L17)
  - [tax/src/main/java/com/ho/account/tax/domain/TaxInvoice.java:46](tax/src/main/java/com/ho/account/tax/domain/TaxInvoice.java#L46)
  - [tax/src/main/java/com/ho/account/tax/domain/TaxInvoice.java:51](tax/src/main/java/com/ho/account/tax/domain/TaxInvoice.java#L51)

2. High: `expenditure-resolution:test`가 현재 서비스/도메인 계약을 반영하지 못해 컴파일 실패함
- `ExpenditureResolutionService` 생성자에 `MasterDataQueryPort`가 추가됐지만 테스트의 직접 생성 코드는 기존 8개 인자만 전달한다.
- `DepartmentPersistencePort`는 현재 `findActiveByCode` 기준인데 테스트는 `findByCode`를 mock 처리한다.
- `ExpenditureResolution` 생성자는 protected이고 `setId`, `setStatus`도 공개 API가 아닌데 테스트/인메모리 포트가 이를 직접 호출한다.
- 결과적으로 지출결의 생성, AP 지급, 세금계산서 통합 테스트가 모두 컴파일 단계에서 막힌다.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:44](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L44)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:55](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L55)
  - [expenditure-resolution/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionServiceTest.java:56](expenditure-resolution/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionServiceTest.java#L56)
  - [expenditure-resolution/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionServiceTest.java:85](expenditure-resolution/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionServiceTest.java#L85)
  - [expenditure-resolution/src/test/java/com/ho/account/expenditure/application/service/APPaymentServiceTest.java:49](expenditure-resolution/src/test/java/com/ho/account/expenditure/application/service/APPaymentServiceTest.java#L49)
  - [expenditure-resolution/src/test/java/com/ho/account/expenditure/integration/ExpenditureTaxApiIntegrationTest.java:272](expenditure-resolution/src/test/java/com/ho/account/expenditure/integration/ExpenditureTaxApiIntegrationTest.java#L272)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/domain/ExpenditureResolution.java:83](expenditure-resolution/src/main/java/com/ho/account/expenditure/domain/ExpenditureResolution.java#L83)

3. High: 리스 지급 결의의 차/대 계정 분리는 보완됐지만 IFRS16 월 지급 회계로는 아직 불완전함
- `LeasePaymentResolutionCommand`가 `debitAccountCode`, `creditAccountCode`로 분리된 점은 이전 Critical 이슈를 완화한다.
- 그러나 `LeaseEntryService.createLeaseExpenditure`는 차변에 `contract.getExpenseAccountCode()`를 넣고 월 지급액 전체를 넘긴다.
- 이미 스케줄에는 이자와 원금(`interestPortion`, `principalPortion`)이 계산되어 있지만 지급 결의에는 반영되지 않는다.
- IFRS16 대상 월 지급은 보통 리스부채 원금 감소와 이자비용을 분리해야 하므로, 현재 결의 전표는 비용계정 단일 차변 또는 부채/비용 혼용으로 오기표될 수 있다.
- 근거:
  - [asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:185](asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java#L185)
  - [asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:186](asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java#L186)
  - [asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:198](asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java#L198)
  - [asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:206](asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java#L206)
  - [asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:209](asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java#L209)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java:29](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java#L29)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java:35](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java#L35)

4. Medium: `ExpenditureResolutionUseCase`가 웹 응답 DTO 변환 책임을 포함함
- 인바운드 포트가 지출결의 유즈케이스와 DTO 조립 메서드(`toDto`, `toDtoList`)를 동시에 노출한다.
- 서비스는 이 DTO 조립을 위해 `contracts.masterdata.MasterDataQueryPort`를 직접 주입받는다.
- API 응답 명칭 보강 의도는 타당하지만, 유스케이스 포트가 프레젠테이션 응답 변환 책임까지 갖게 되어 포트 경계가 흐려진다.
- 근거:
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/port/in/ExpenditureResolutionUseCase.java:18](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/port/in/ExpenditureResolutionUseCase.java#L18)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/port/in/ExpenditureResolutionUseCase.java:19](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/port/in/ExpenditureResolutionUseCase.java#L19)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:209](expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java#L209)
  - [expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/web/ExpenditureController.java:28](expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/web/ExpenditureController.java#L28)

5. Medium: `payable`/`receivable` 드릴다운 문서와 구현이 일치하지 않음
- `payable` 문서는 `PayableSourceDocumentProvider`가 `P2P_AP`를 지원한다고 설명한다.
- 실제 `payable/src/main/java`에서 `SourceDocumentProvider` 구현체는 확인되지 않았고, `PurchaseService`는 전표 lineage에 `PURCHASE_INVOICE`를 사용한다.
- `receivable` 문서도 `ReceivableSourceDocumentProvider`와 `O2C_AR`/`SALES_INVOICE` 지원을 설명하지만 현재 구현체는 확인되지 않았다.
- 이 상태에서는 journal-ledger의 drill-down이 문서대로 원천 문서로 수렴하기 어렵다.
- 근거:
  - [payable/docs/process-flow.md:118](payable/docs/process-flow.md#L118)
  - [payable/docs/process-flow.md:119](payable/docs/process-flow.md#L119)
  - [payable/docs/process-flow.md:124](payable/docs/process-flow.md#L124)
  - [payable/src/main/java/com/ho/account/expenditure/application/service/PurchaseService.java:123](payable/src/main/java/com/ho/account/expenditure/application/service/PurchaseService.java#L123)
  - [receivable/docs/process-flow.md:122](receivable/docs/process-flow.md#L122)
  - [receivable/docs/process-flow.md:124](receivable/docs/process-flow.md#L124)
  - [receivable/src/main/java/com/ho/account/receivable/application/service/SalesService.java:103](receivable/src/main/java/com/ho/account/receivable/application/service/SalesService.java#L103)

6. Medium: `loan`은 컴파일되지만 DoD 회귀 검증과 전표 처리 경로가 약함
- `loan:core/api/batch`는 컴파일되지만 테스트 소스가 없어 실행→이연→3개월 상각→중도상환 재계산 DoD를 자동 검증하지 못한다.
- `LoanService`는 `JournalEntry`를 직접 만들고 `journalPersistencePort.save`로 저장한다. 이 경로는 승인/전기 유스케이스를 거치지 않아 GL/SL 반영 여부가 불명확하다.
- `InterestAccrualService`도 `JournalUseCase.createJournalEntry`까지만 호출하고 승인/전기까지 수렴하지 않는다.
- 문서에서 인정한 `Loan`/`LoanContract` 병행 모델도 유지되어 소스문서 드릴다운은 `LoanContract`를 반환하지만 주요 API는 `Loan`을 중심으로 동작한다.
- 근거:
  - [loan/docs/README.md:29](loan/docs/README.md#L29)
  - [loan/docs/README.md:31](loan/docs/README.md#L31)
  - [loan/core/src/main/java/com/ho/account/loan/service/LoanService.java:101](loan/core/src/main/java/com/ho/account/loan/service/LoanService.java#L101)
  - [loan/core/src/main/java/com/ho/account/loan/service/LoanService.java:421](loan/core/src/main/java/com/ho/account/loan/service/LoanService.java#L421)
  - [loan/core/src/main/java/com/ho/account/loan/service/LoanService.java:458](loan/core/src/main/java/com/ho/account/loan/service/LoanService.java#L458)
  - [loan/core/src/main/java/com/ho/account/loan/service/InterestAccrualService.java:70](loan/core/src/main/java/com/ho/account/loan/service/InterestAccrualService.java#L70)
  - [loan/core/src/main/java/com/ho/account/loan/service/LoanSourceDocumentProvider.java:35](loan/core/src/main/java/com/ho/account/loan/service/LoanSourceDocumentProvider.java#L35)

### 판단

- 3차 범위는 `payable`, `receivable`, `asset-lease`, `loan`의 컴파일 또는 테스트 일부가 통과해 2차보다 빌드 상태는 나아 보인다.
- 그러나 `tax`와 `expenditure-resolution`의 테스트 소스가 컴파일되지 않아 업무 서브레저 회귀 검증은 아직 신뢰하기 어렵다.
- 리스 지급, 드릴다운, 대출 전표 처리는 빌드 성공과 별개로 회계 흐름이 원장까지 완전히 수렴하는지 추가 보완이 필요하다.
