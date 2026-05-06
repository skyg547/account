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
