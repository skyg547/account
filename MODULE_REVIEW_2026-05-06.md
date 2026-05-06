# Module Sequential Review (2026-05-06)

검수 기준:
- DDD / Hexagonal Architecture 준수 여부
- 주석/설명 정합성
- 재무 업무 흐름(분개/정산/마스터 참조) 타당성
- 모듈 컴파일 가능 여부

검수 순서:
1. `master-data`
2. `journal-ledger:core`
3. `receivable`
4. `expenditure-resolution`
5. `tax`, `payable`, `closing:core` (연관 모듈)

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
