# Asset-Lease Module Docs

`asset-lease` 모듈은 두 가지 영역을 함께 다룹니다.

- 고정자산 취득, 감가상각, 처분
- IFRS 16 리스 계약 등록, 초기 인식, 월별 회계처리, 재측정

## 문서 목록

- [process-flow.md](./process-flow.md): 고정자산과 리스의 처리 흐름을 단계별로 설명합니다.
- [schema.md](./schema.md): 주요 엔티티, 관계, 핵심 필드를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 자산회계를 이해할 수 있도록 쉽게 설명합니다.

## 핵심 진입점

- `FixedAssetController`
- `FixedAssetService`
- `LeaseAccountingController`
- `LeaseService`
- `LeaseAccountingService`
- `AssetSourceDocumentProvider`

## 현재 구현 기준에서 먼저 알아둘 점

- 한 모듈 안에 `FIXED_ASSET`과 `IFRS16_LEASE` 두 업무가 같이 있습니다.
- 고정자산은 취득 분개 생성 후 일부 경로에서 즉시 승인까지 진행합니다.
- IFRS 16 리스는 예외 조건이 아니면 계약 등록 직후 사용권자산과 리스부채를 자동 인식합니다.
- 계정과목이 일부 하드코딩되어 있습니다.
  - 고정자산 취득/처분 현금: `10100`
  - 처분이익/손실: `91100`, `92100`
  - IFRS 16: `12300`, `12399`, `25100`, `51500`, `93100`, `10100`
- 리스 월 지급 해소는 `LeasePaymentResolutionPort`를 통해 `expenditure-resolution`과 연계됩니다.
