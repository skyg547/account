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
| SCD2 | 값을 덮어쓰지 않고 과거 버전과 현재 버전을 함께 보존하는 이력 관리 방식. |
| requestedVersion | 결재 문서가 만들거나 종료하려는 SCD2 순번. 오래된 결재가 최신 데이터를 덮지 못하게 합니다. |
| lockVersion | 같은 결재 문서를 여러 서버가 동시에 승인/반영하지 못하게 하는 JPA 기술 잠금 값. |

## 왜 SCD2가 중요한가

부서명이 2026년 6월 1일에 바뀌었다고 해서 2025년 전표의 부서명까지 바뀌면 안 됩니다. `master-data`는 실제 컬럼인 `validFrom`, `validTo`로 "그 당시 기준정보"를 재현합니다. `isCurrent` 컬럼을 따로 저장하지 않고, 조회 기준일이 두 날짜 사이인지 계산해 활성 여부를 판단합니다.

## DDD와 헥사고날 관점

- `api`: HTTP 요청을 받는 인바운드 어댑터입니다.
- `core.application.service`: 기준정보 생성, 수정, 승인 반영 같은 유즈케이스 흐름을 조율합니다.
- `core.domain.model`: 계정과목, 거래처, 부서, 상품, 통화 같은 도메인 모델입니다.
- `core.domain.policy`: 유효기간과 SCD2 기본 정책을 다룹니다.
- `core.application.port.out`: 저장소 세부 기술을 숨기는 출력 포트입니다.
- `core.infrastructure.persistence`: Spring Data JPA 기반 저장 어댑터입니다.

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
