# master-data beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`master-data`는 다른 모든 모듈이 공통으로 믿고 쓰는 기준 정보 저장소다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 계정과목

- 회계 처리의 가장 기본 축이다.
- 전표 라인마다 어떤 계정과목에 금액이 들어갈지 결정된다.

### 2.2 거래처

- 고객, 공급처, 은행 같은 상대방이다.
- AP, AR, 대출, 자금 모듈에서 자주 참조된다.

### 2.3 부서

- 조직, 비용센터, 수익센터 역할을 한다.
- 예산 통제와 보고 축에서 중요하다.

### 2.4 통화와 환율

- 거래 통화와 기준 통화 계산에 필요하다.

### 2.5 유효기간

- 이 프로젝트는 일부 마스터를 단순 삭제하지 않고 유효기간으로 관리한다.
- 즉, "지금 유효한 값인가?"를 보는 구조다.

## 3. 이 모듈이 왜 중요한가

- `journal-ledger`는 계정과목, 거래처, 부서가 맞지 않으면 전표 검증에서 실패한다.
- `closing`은 회계기간을 기준으로 마감 여부를 판단한다.
- `reporting`은 계정과목과 조직 축을 믿고 집계한다.
- 따라서 여기서 잘못된 마스터가 들어가면 downstream 모듈이 줄줄이 흔들린다.

## 4. 주요 API

### 4.1 계정과목

- `POST /api/basic/account-subjects`
- `GET /api/basic/account-subjects`
- `GET /api/basic/account-subjects/{code}`
- `PUT /api/basic/account-subjects/{code}`
- `DELETE /api/basic/account-subjects/{code}`

### 4.2 거래처

- `POST /api/basic/businesspartners`
- `GET /api/basic/businesspartners`
- `GET /api/basic/businesspartners/active`
- `GET /api/basic/businesspartners/{businessPartnerCode}`
- `GET /api/basic/businesspartners/search?name=...`
- `PUT /api/basic/businesspartners/{id}`
- `DELETE /api/basic/businesspartners/{id}`

### 4.3 부서

- `POST /api/basic/departments`
- `GET /api/basic/departments`
- `GET /api/basic/departments/active`
- `GET /api/basic/departments/{deptCode}`
- `PUT /api/basic/departments/{deptCode}`
- `DELETE /api/basic/departments/{deptCode}`

## 5. 처음 코드를 읽는 순서

1. `master-data/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java`
2. `master-data/src/main/java/com/ho/account/masterdata/api/dto/AccountSubjectRequestDto.java`
3. `master-data/src/main/java/com/ho/account/masterdata/core/application/usecase/AccountSubjectUseCase.java`
4. `master-data/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java`
5. `master-data/src/main/java/com/ho/account/masterdata/core/port/out/AccountSubjectPersistencePort.java`
6. `master-data/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaAccountSubjectPersistenceAdapter.java`
7. `master-data/src/main/java/com/ho/account/basic/repository/AccountSubjectRepository.java`
8. `master-data/src/main/java/com/ho/account/basic/domain/AccountSubject.java`
9. `master-data/src/main/java/com/ho/account/common/adapter/MonolithMasterDataQueryAdapter.java`

## 6. 헥사고날 흐름을 쉽게 읽는 법

예를 들어 거래처를 등록하면 코드 흐름은 아래처럼 이동한다.

1. `BusinessPartnerController`: HTTP 요청을 받는다.
2. `BusinessPartnerRequestDto`: JSON 요청을 API 전용 객체로 받는다.
3. `BusinessPartnerCommand`: API DTO를 유스케이스 입력값으로 바꾼다.
4. `BusinessPartnerUseCase`: Controller가 바라보는 입력 포트다.
5. `BusinessPartnerService`: 중복 코드, 유효기간 기본값 같은 업무 규칙을 처리한다.
6. `BusinessPartnerPersistencePort`: Service가 바라보는 출력 포트다.
7. `JpaBusinessPartnerPersistenceAdapter`: 출력 포트를 구현하고 Spring Data JPA Repository를 호출한다.
8. `BusinessPartnerRepository`: 실제 DB 접근을 수행한다.

이 구조의 장점은 DB가 JPA에서 JDBC Bulk나 외부 Master API로 바뀌어도 Service 코드를 덜 흔든다는 점이다.

## 7. 자주 헷갈리는 지점

### 6.1 활성 여부 판정 방식이 완전히 같지 않다

- 계정과목, 부서는 `validFrom`, `validTo`를 본다.
- 거래처는 현재 `useYn`도 같이 쓴다.

### 6.2 삭제가 항상 물리 삭제는 아니다

- 계정과목과 부서는 실제로는 종료일을 당겨 비활성화한다.
- 거래처도 현재는 `useYn = false`로 종료한다.

### 6.3 이 모듈은 MSA 분리의 기준점이다

- `MonolithMasterDataQueryAdapter`는 현재 모놀리스 내부 어댑터지만,
- 구조적으로는 다른 모듈이 직접 엔티티를 보지 않고 조회 계약으로 가기 위한 다리다.

## 8. 체크리스트

- 코드가 유일한가
- 활성 기간이 맞는가
- 상위 계층 연결이 맞는가
- 거래처 사용 가능 상태인가
- 다른 모듈이 참조할 최소 필드가 빠지지 않았는가

## 9. 마스터 변경요청은 왜 필요한가

기준정보는 단순 설정값이 아니다. 예를 들어 계정과목 이름이나 거래처 상태가 바뀌면 전표 검증, 보고, 감사 추적이 함께 영향을 받는다.

그래서 운영 변경은 아래처럼 남긴다.

1. `POST /api/master-data/change-requests`로 변경요청을 만든다.
2. `GET /api/master-data/change-requests/pending`으로 승인 대기 목록을 본다.
3. `POST /api/master-data/change-requests/{id}/approve`로 승인한다.
4. `POST /api/master-data/change-requests/{id}/reject`로 반려한다.
5. `POST /api/master-data/change-requests/{id}/apply`로 적용 완료 상태를 남긴다.

중요한 통제:

- 요청자와 승인자는 같을 수 없다.
- 승인 전 요청은 적용 완료 처리할 수 없다.
- 적용일(`effectiveDate`)과 요청 버전(`requestedVersion`)을 반드시 남긴다.
