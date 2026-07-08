# expenditure-resolution beginner guide

## 한 문장 요약

`expenditure-resolution`은 회사 돈을 쓰기 전에 지출결의서를 만들고, 예산을 확인하고, 승인되면 전표와 AP 지급으로 이어주는 모듈이다.

초보자 관점에서는 다음 순서로 보면 된다.

1. 사용자가 지출결의서를 작성한다.
2. 시스템은 부서, 계정과목, 거래처, 세금계산서가 실제로 사용할 수 있는 코드인지 확인한다.
3. 결의서 상세 금액만큼 예산을 사용 처리한다.
4. 승인 요청과 승인을 거치면 journal-ledger에 전표 생성을 요청한다.
5. AP 지급은 승인된 지출결의와 유효한 매입 세금계산서에 연결된다.

## 현재 구조

`expenditure-resolution`은 `core/api/batch` 구조다.

- `core`: 지출결의 상태 전이, 예산 사용, 세금계산서 연결 검증, 전표 생성 요청 순서를 담당한다.
- `api`: Controller와 HTTP 요청/응답 DTO를 담당한다. 요청 DTO는 core command로 변환된다.
- `batch`: Spring Batch Job/Step 실행 흐름만 담당하고, 승인 대상 처리 업무는 core 유즈케이스에 위임한다.

## API DTO와 core command 분리

API 요청 DTO는 JSON 입력을 검증하기 위한 객체다. 예를 들어 `ExpenditureResolutionRequestDto`는 제목, 결의일, 지급예정일, 상세 라인 금액이 비어 있지 않은지 확인한다.

core는 이 DTO를 직접 받지 않고 `ExpenditureResolutionCommand`, `APPaymentCommand`를 받는다. 이렇게 하면 core 업무 로직은 HTTP, Bean Validation, Controller 변경에 덜 흔들린다.

## 외부 모듈 연결 방식

core는 master-data 내부 Repository나 Entity를 직접 참조하지 않는다. 부서, 계정과목, 거래처는 `MasterDataQueryPort`로 조회한다.

세금계산서는 `TaxInvoiceQueryPort`로 조회하고, 지출결의/AP 지급에는 `PURCHASE` 타입이면서 `ACTIVE` 상태인 세금계산서만 연결한다.

예산은 `Budget`에 `deptCode`, `accountCode` 코드만 저장한다. 기준정보 이름과 상태는 master-data 포트에서 확인한다.