# Asset-Lease Module Docs

`asset-lease` 모듈은 두 가지 영역을 함께 다룹니다.

- 고정자산 취득, 감가상각, 처분
- IFRS 16 리스 계약 등록, 초기 인식, 월별 회계처리, 재측정

## 아키텍처 현대화 (2026-04-29)

`asset-lease` 모듈은 타 모듈과의 결합도를 낮추기 위해 다음과 같은 현대화 작업을 수행했습니다.

- **외부 엔티티 참조 제거**: `FixedAsset`, `LeaseContract`, `AssetHistory` 도메인 엔티티에서 `master-data` 모듈의 `AccountSubject`, `Department`, `BusinessPartner` 엔티티 직접 참조를 제거했습니다.
- **코드 기반 참조 도입**: 엔티티 대신 `String` 타입의 코드(ID) 필드를 사용하며, 필요 시 `MasterDataQueryPort`를 통해 데이터를 검증하거나 조회합니다.
- **포트 중심 설계**: `JournalPostingPort`를 통해 `journal-ledger` 모듈과 통신하며, 물리적으로 분리된 마이크로서비스 환경으로의 전환이 용이하도록 설계되었습니다.

### 초보자를 위한 개념 설명 (Mandatory)
- **엔티티 참조 제거란?**: 이전에는 '자산'이 '부서'라는 실제 객체를 직접 들고 있었다면, 이제는 '부서 코드(예: DEPT-001)'라는 이름표만 가지고 있는 것과 같습니다. 이렇게 하면 '부서' 정보가 바뀌거나 부서 관리 시스템이 따로 떨어져 나가더라도 '자산' 시스템은 큰 영향을 받지 않고 독립적으로 움직일 수 있습니다. 이를 전문 용어로 **'느슨한 결합(Loose Coupling)'**이라고 합니다.

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
