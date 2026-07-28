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
복원합니다. 다만 과거 행의 `useYn`은 버전 종료 시 함께 바뀌는 legacy 값이므로, 현재는 당시
거래처명과 유형을 복원하는 데 사용하고 `active`의 과거 의미는 후속 상태 모델 분리가 필요합니다.

## DDD와 헥사고날 관점

- `api`: HTTP 요청을 받는 인바운드 어댑터입니다.
- `core.application.service`: 기준정보 생성, 수정, 승인 반영 같은 유즈케이스 흐름을 조율합니다.
- `core.domain.model`: 계정과목, 거래처, 부서, 상품, 통화 같은 도메인 모델입니다.
- `core.domain.policy`: 유효기간과 SCD2 기본 정책을 다룹니다.
- `core.application.port.out`: 저장소 세부 기술을 숨기는 출력 포트입니다.
- `core.infrastructure.persistence`: Spring Data JPA 기반 저장 어댑터입니다.

활성 계정과목/상품 목록과 거래처 이름 검색은 모든 이력을 애플리케이션 메모리로 가져오지
않고 DB에 기준일 조건을 전달합니다. 거래처 검색은 과거 종료 버전을 섞지 않으며 빈 검색어로
전체 목록을 우회 조회할 수도 없습니다.

환율은 `effectiveDate <= 조회일` 중 가장 최신 한 건을 선택합니다. 회계기간 마감/재오픈은
행 잠금을 얻은 뒤 도메인 메서드가 수행하며, `PERMANENTLY_CLOSED`는 다시 열 수 없습니다.

## 처음 볼 파일

1. `MasterDataApplication`: Spring Boot 실행 진입점.
2. `AccountSubjectController`, `DepartmentController`, `BusinessPartnerController`, `ProductController`: 기준정보 API.
3. `MasterDataChangeRequestController`: 변경 요청, 승인, 반려, 반영 API.
4. `MasterDataChangeRequestService`: 변경 요청, 승인, 반려, 예약 반영 흐름.
5. `MasterDataChangeApplierRegistry`: 한 기준정보 유형에 반영 전략이 없거나 두 개 이상 연결되는 설정 오류를 차단.
6. `AccountSubjectMasterDataChangeApplier` 등 typed applier: 승인 요청을 실제 SCD2 생성/수정/비활성화로 연결.
7. `MasterDataChangeVersionPolicy`: CREATE/UPDATE/DEACTIVATE별 목표 버전과 저장된 SCD2 이력 수를 비교.
8. `MasterDataValidityReportPipeline`: 기준일을 고정하고 DB 통계 포트의 네 집계 결과를 하나의 core 보고 모델로 조립.
9. `MasterDataValidityPolicy`: 유효기간과 종료일 정합성 공통 정책.
10. `MonolithFiscalPeriodControlAdapter`: Closing 계약을 회계기간 포트, 행 잠금, 도메인 상태 전이에 연결.
