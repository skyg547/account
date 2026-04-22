# master-data docs

`master-data` 모듈은 계정과목, 거래처, 부서, 통화, 환율, 회계기간, 상품 같은 기준 정보를 관리한다.

현재 구조는 `api / core / batch` 경계와 헥사고날 포트/어댑터 경계를 함께 사용한다.

- `api`: REST controller와 request/response DTO
- `core.application.usecase`: inbound adapter가 호출하는 입력 포트
- `core.application.service`: 트랜잭션과 유스케이스 흐름을 제어하는 application service
- `core.application.command`: DTO에서 변환된 유스케이스 입력값
- `core.domain.policy`: SCD2 유효기간, 활성 판정 같은 순수 도메인 정책
- `core.port.out`: application service가 의존하는 출력 포트
- `core.infrastructure.persistence`: Spring Data JPA Repository를 감싸는 출력 어댑터
- `batch`: 기준정보 유효성 점검 같은 배치 오케스트레이션

다른 모듈은 이 데이터를 직접 참조하거나 `MasterDataQueryPort`를 통해 조회한다.

핵심 원칙:

1. Controller는 JPA Entity를 요청/응답 모델로 직접 쓰지 않는다.
2. Service는 Repository를 직접 호출하지 않고 `port.out` 인터페이스만 호출한다.
3. JPA, H2, Flyway 같은 기술 선택은 infrastructure adapter 안쪽에 둔다.
4. 유효기간 기본값과 활성 판정은 `MasterDataValidityPolicy`에서 한 번만 정의한다.

## 마스터 변경관리

2차 작업부터 `MasterDataChangeRequest`가 기준정보 변경의 통제 허브 역할을 한다.

- `REQUESTED`: 운영자가 변경을 요청한 상태
- `APPROVED`: 승인자가 승인한 상태
- `REJECTED`: 승인자가 반려한 상태
- `APPLIED`: 승인된 요청이 실제 마스터에 반영된 상태

3차부터 승인된 payload를 실제 마스터 row에 반영하는 `DefaultMasterDataChangeApplier`가 추가됐다.

- 변경요청은 상태 전이를 담당한다.
- 적용기는 payload JSON을 대상별 command로 변환한다.
- 실제 생성/수정/비활성화 규칙은 기존 `AccountSubjectUseCase`, `BusinessPartnerUseCase`, `DepartmentUseCase`, `ProductUseCase`가 처리한다.
- 적용 성공 후 변경요청은 `APPLIED`가 된다.

문서 순서:

1. [beginner-guide.md](/C:/Users/skyg547/IdeaProjects/account/master-data/docs/beginner-guide.md)
2. [process-flow.md](/C:/Users/skyg547/IdeaProjects/account/master-data/docs/process-flow.md)
3. [schema.md](/C:/Users/skyg547/IdeaProjects/account/master-data/docs/schema.md)
