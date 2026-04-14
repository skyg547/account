# Contracts Beginner Guide

## 1. 이 모듈은 왜 필요한가요

MSA나 모듈형 구조에서 가장 흔한 문제는 "다른 모듈 내부 클래스를 직접 참조하다가 의존성이 꼬이는 것"입니다. `contracts`는 그 문제를 막기 위해 있습니다.

쉽게 말하면:
- 구현은 각 모듈에 둠
- 약속은 `contracts`에 둠

## 2. 초보자가 먼저 알아야 할 용어

- 포트(Port): 다른 모듈이 호출할 수 있도록 열어둔 인터페이스
- 커맨드(Command): 요청할 때 보내는 데이터 묶음
- 레퍼런스(Ref): 조회 결과를 간단히 전달하는 참조 DTO
- 구현체(Adapter): 포트를 실제 서비스에 연결하는 클래스

## 3. 왜 직접 서비스 클래스를 부르면 안 되나요

예를 들어 `asset-lease`가 `expenditure-resolution`의 내부 서비스 클래스를 바로 호출하면:
- 구현 변경에 약해지고
- 테스트가 어려워지고
- 순환 의존이 생기기 쉽습니다

그래서 `LeasePaymentResolutionPort` 같은 인터페이스만 보고 호출합니다. 누가 실제로 처리할지는 구현 모듈이 책임집니다.

## 4. 대표 예시로 보면

전표 생성:
- 호출 모듈은 `JournalPostingPort`만 압니다.
- 실제 전표 생성은 `journal-ledger` 쪽 구현체가 합니다.

마스터 조회:
- 호출 모듈은 `MasterDataQueryPort`로 계정/부서/거래처를 묻습니다.
- 실제 데이터는 `master-data`가 제공합니다.

리스 월지급:
- `asset-lease`는 `LeasePaymentResolutionPort`에 요청만 던집니다.
- 실제 지출기안 생성은 `expenditure-resolution`이 처리합니다.

## 5. 초보자가 가장 먼저 이해해야 할 포트

1. `JournalPostingPort`
2. `MasterDataQueryPort`
3. `SourceDocumentProvider`
4. `BudgetControlPort`
5. `LeasePaymentResolutionPort`

이 다섯 개를 이해하면 모듈 간 연결 대부분이 보입니다.

## 6. `SourceDocumentProvider`는 왜 특별한가요

전표를 보다 보면 "이 전표가 어떤 원문서에서 왔는지"를 열어보고 싶습니다.

이때:
- 전표는 `lineageSourceType`
- 전표는 `lineageSourceId`

를 들고 있고,
- 각 모듈은 `SourceDocumentProvider`를 구현해
- 자기 문서를 찾아주는 역할을 합니다

즉, 드릴다운 연결 규약입니다.

## 7. 현재 코드에서 주의할 점

- 이 모듈은 구현이 없으니 여기만 봐서는 실제 검증 로직이 보이지 않습니다.
- `Map<String, Object>` 같은 느슨한 반환도 있어 호출자가 타입을 잘 맞춰야 합니다.
- 계약 필드가 바뀌면 여러 모듈이 동시에 깨질 수 있습니다.

## 8. 문서 추천 순서

1. [README.md](./README.md)
2. [process-flow.md](./process-flow.md)
3. [schema.md](./schema.md)

이 순서로 보면 "왜 있는지 -> 어떻게 연결되는지 -> 어떤 데이터가 오가는지"를 쉽게 이해할 수 있습니다.
