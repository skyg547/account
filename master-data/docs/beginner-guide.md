# master-data beginner guide

## 한 문장 요약

`master-data`는 회계 시스템의 기준 이름표를 관리합니다. 계정과목, 부서, 거래처, 상품, 회계기간처럼 여러 모듈이 공통으로 참조하는 데이터를 한 곳에서 관리합니다.

초보자 관점에서는 회사 전체가 쓰는 공식 주소록과 계정표입니다. 각 업무 모듈이 제각각 거래처 이름이나 계정명을 들고 있으면 나중에 전표, 대사, 보고 숫자가 맞지 않습니다.

## 핵심 개념

| 용어 | 설명 |
| --- | --- |
| 계정과목 | 돈의 성격을 분류하는 기준. 예: 현금, 매출, 지급수수료. |
| 부서 | 비용과 수익을 귀속할 조직 단위. |
| 거래처 | 돈을 주고받는 외부/내부 상대방. |
| 상품 | 대출, 수신, 서비스 상품처럼 금융 거래가 속한 기준. |
| 회계기간 | 전표 입력과 마감 통제를 위한 기간 기준. |
| 기준일 환율 | 조회일보다 미래가 아닌 환율 중 가장 최근 고시분 1건. 결산 평가의 재현성을 위해 날짜가 중요합니다. |
| SCD2 | 값을 덮어쓰지 않고 과거 버전과 현재 버전을 함께 보존하는 이력 관리 방식. |
| requestedVersion | 결재 문서가 만들거나 종료하려는 SCD2 순번. 오래된 결재가 최신 데이터를 덮지 못하게 합니다. |
| lockVersion | 같은 결재 문서를 여러 서버가 동시에 승인/반영하지 못하게 하는 JPA 기술 잠금 값. |
| sourceReference | Governance 승인처럼 외부에서 온 같은 요청을 재시도해도 중복 결재가 생기지 않게 하는 계보 키. |
| appliedAt | 승인된 변경이 실제 기준정보에 반영된 시각. 승인 시각과 시행 시각이 달라 별도로 기록합니다. |

## 왜 SCD2가 중요한가

부서명이 2026년 6월 1일에 바뀌었다고 해서 2025년 전표의 부서명까지 바뀌면 안 됩니다. `master-data`는 실제 컬럼인 `validFrom`, `validTo`로 "그 당시 기준정보"를 재현합니다. `isCurrent` 컬럼을 따로 저장하지 않고, 조회 기준일이 두 날짜 사이인지 계산해 활성 여부를 판단합니다.

새 버전은 `validTo`가 `validFrom`보다 빠른지 먼저 검사한 뒤 기존 버전을 종료합니다. 거래처의
현재 활성 조회는 legacy `useYn`도 확인하지만, 과거 기준일 조회는 유효기간으로 당시 버전을
복원합니다. SCD2 교체 마감인 `closeVersion`은 과거 행의 `useYn`을 유지하지만, 업무 종료
`terminate`는 같은 행을 false로 바꿉니다. 따라서 나중에 업무 종료된 거래처는 종료 전 기준일의
이름과 유형은 복원해도 `active`까지 당시 값으로 되돌리려면 별도 상태 이력이 필요합니다.

계정과목 이름만 고칠 때도 새 계정과목 행을 만듭니다. 비영업수익/비영업비용 분류와 규제 보고
매핑을 기존 행에서 그대로 가져와야 새 이름으로 작성되는 보고서도 같은 분류를 사용합니다.
분류나 매핑을 의도적으로 바꿀 때는 승인 요청에 새 값 또는 명시적 매핑 해제 플래그를 넣습니다.
요청자·승인자·사유와 실제 반영 시각이 남으며, 관리자의 직접 PUT은 이 필드 변경을 받지 않습니다.

## DDD와 헥사고날 관점

- `master-data:api`: HTTP 요청만 받는 독립 실행 인바운드 어댑터입니다.
- `master-data:batch`: 스케줄러 파라미터와 Spring Batch Job/Step 흐름만 소유하는 독립 실행 인바운드 어댑터입니다.
- `master-data:core`: API와 Batch가 함께 사용하는 업무 library입니다.
- `core.application.service`: 기준정보 생성, 수정, 승인 반영 같은 유즈케이스 흐름을 조율합니다.
- `core.domain.model`: 계정과목, 거래처, 부서, 상품, 통화 같은 도메인 모델입니다.
- `core.domain.policy`: 유효기간과 SCD2 기본 정책을 다룹니다.
- `core.application.port.out`: 저장소 세부 기술을 숨기는 출력 포트입니다.
- `core.infrastructure.persistence`: Spring Data JPA 기반 저장 어댑터입니다.

### 거래처를 두 모델로 나눈 이유

`BusinessPartner`는 "어떤 거래처가 유효한가"를 판단하는 업무 모델입니다. 반면
`BusinessPartnerJpaEntity`는 "어떤 테이블과 컬럼에 저장할 것인가"를 설명하는 기술
모델입니다. 하나의 클래스가 두 책임을 모두 가지면 지연 로딩 프록시나 기본 생성자 같은 JPA
요구가 업무 규칙보다 우선하기 쉽습니다.

그래서 애플리케이션 서비스는 `BusinessPartnerPersistencePort`만 호출하고, JPA 어댑터가
다음 순서로 번역합니다.

1. API 명령을 `BusinessPartner.create(...)` 또는 `createNextVersion(...)`으로 검증합니다.
2. 수정이면 신규 버전이 먼저 유효한지 확인한 뒤 기존 버전에 `closeVersion(...)`을 적용합니다.
3. 어댑터가 순수 도메인과 `BusinessPartnerJpaEntity`를 양방향으로 변환합니다.
4. Controller는 도메인을 API DTO로 변환하며 사업자등록번호를 마스킹합니다.

`closeVersion`과 `terminate`도 의도가 다릅니다. 전자는 새 SCD2 버전을 만들기 위해 과거
버전의 기간만 닫고, 후자는 거래처 자체를 사용 중지합니다. 이 차이가 있어 미래 시행일 수정이
예약되어도 기존 거래처가 그 전에 비활성화되지 않습니다. 계좌는 업무 값을 다음 버전에
복사하되 자식 ID는 새로 발급합니다. 그래야 과거 버전의 계좌 FK를 현재 버전으로 옮기지 않고
두 시점의 정산 계좌를 모두 재현할 수 있습니다.

활성 계정과목/상품 목록과 거래처 이름 검색은 모든 이력을 애플리케이션 메모리로 가져오지
않고 DB에 기준일 조건을 전달합니다. 거래처 검색은 과거 종료 버전을 섞지 않으며 빈 검색어로
전체 목록을 우회 조회할 수도 없습니다.

환율은 `effectiveDate <= 조회일` 중 가장 최신 한 건을 선택합니다. 회계기간 마감/재오픈은
행 잠금을 얻은 뒤 도메인 메서드가 수행하며, `PERMANENTLY_CLOSED`는 다시 열 수 없습니다.

## 처음 볼 파일

1. `api/.../MasterDataApplication`: HTTP Spring Boot 실행 진입점.
2. `api/.../AccountSubjectController`, `DepartmentController`, `BusinessPartnerController`, `ProductController`: 기준정보 API. 거래처 Controller는 항상 `BusinessPartnerDto.fromDomain`으로 응답을 조립합니다.
3. `api/.../MasterDataChangeRequestController`: 변경 요청, 승인, 반려, 반영 API.
4. `batch/.../MasterDataBatchApplication`: 웹 서버 없이 기동하는 Batch 실행 진입점.
5. `batch/.../MasterDataValidityJobConfiguration`: `asOfDate`를 검증하고 core pipeline을 호출하는 Job/Step.
6. `core/.../MasterDataChangeRequestService`: 변경 요청, 승인, 반려, 예약 반영 흐름.
7. `core/.../MasterDataChangeApplierRegistry`: 한 기준정보 유형에 반영 전략이 없거나 두 개 이상 연결되는 설정 오류를 차단.
8. `core/.../AccountSubjectMasterDataChangeApplier` 등 typed applier: 승인 요청을 실제 SCD2 생성/수정/비활성화로 연결.
9. `core/.../MasterDataChangeVersionPolicy`: CREATE/UPDATE/DEACTIVATE별 목표 버전과 저장된 SCD2 이력 수를 비교.
10. `core/.../MasterDataValidityReportPipeline`: 기준일을 고정하고 DB 통계 포트의 네 집계 결과를 하나의 core 보고 모델로 조립.
11. `core/.../MasterDataValidityPolicy`: 유효기간과 종료일 정합성 공통 정책.
12. `core/.../MonolithFiscalPeriodControlAdapter`: Closing 계약을 회계기간 포트, 행 잠금, 도메인 상태 전이에 연결.
13. `core/.../JpaBusinessPartnerPersistenceAdapter`: 순수 거래처 도메인과 두 JPA 엔티티를 명시적으로 변환.

## 연속 미래 변경의 유효기간 (Issue #667)

계정과목·부서·상품·거래처의 수정은 현재 버전의 기간을 나누는 SCD2 작업입니다.
새 시작일은 원본 시작일보다 늦어야 하고, 새 종료일은 원본 종료일 이하여야 합니다.
시작일과 종료일은 모두 포함하므로 새 시작일이 원본 종료일과 같은 하루짜리 버전도 가능합니다.

예를 들어 현재 V1이 무기한일 때 오늘+21일부터 V2를 예약하면 V1의 종료일은 오늘+20일이 됩니다.
같은 날 V1을 다시 수정하면서 오늘+10일부터 무기한 V3를 요청하면 V2와 겹치므로
`IllegalArgumentException`으로 거부됩니다. 오늘+21일 또는 그 이후부터 V1을 나누려는 요청도 거부됩니다.
거부 시 원본·예약 이력과 저장 횟수는 바뀌지 않습니다.
오늘+10일부터 **오늘+20일까지**로 명시하면 V1 안의 분할이므로 허용되고 V2는 유지됩니다.

종료일을 생략하면 기존과 같이 `9999-12-31`을 뜻합니다. 원본 종료일이 유한하면 생략한
종료일도 기간 확장으로 거부되므로, 원본 구간 안의 종료일을 명시해야 합니다.
정책은 core에서 직접 수정과 승인된 변경 반영에 공통 적용되며, 거래처 과거 버전의 `useYn`은
단순 SCD2 분할로 비활성화되지 않습니다. 이미 중복된 데이터의 복구와 동시 writer 방지는 별도 작업입니다.

JDK 17과 Gradle 의존성 캐시가 있는 저장소 루트에서 다음 명령으로 검증합니다.

```bash
bash gradlew :master-data:core:test :master-data:api:test :master-data:batch:test --offline --no-daemon --console=plain --max-workers=1
```

기대 결과는 네 유형의 연속 미래 수정·기간 경계 회귀 및 기존 모듈 테스트 통과입니다.
이 검증은 실제 PostgreSQL 및 동시 요청 검증을 대신하지 않습니다.
