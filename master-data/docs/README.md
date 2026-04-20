# master-data docs

`master-data` 모듈은 계정과목, 거래처, 부서, 통화, 환율, 회계기간, 상품 같은 기준 정보를 관리한다.

현재 구조는 점진적으로 `api / core / batch` 경계로 정리 중이다.

- `api`: REST controller와 request/response DTO
- `core`: 유스케이스, 도메인 정책, 포트, JPA 어댑터
- `batch`: 기준정보 유효성 점검 같은 배치 오케스트레이션

다른 모듈은 이 데이터를 직접 참조하거나 `MasterDataQueryPort`를 통해 조회한다.

문서 순서:

1. [beginner-guide.md](/C:/Users/skyg547/IdeaProjects/account/master-data/docs/beginner-guide.md)
2. [process-flow.md](/C:/Users/skyg547/IdeaProjects/account/master-data/docs/process-flow.md)
3. [schema.md](/C:/Users/skyg547/IdeaProjects/account/master-data/docs/schema.md)
