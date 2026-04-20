# Shared-Kernel Beginner Guide

## 1. shared-kernel은 무엇인가요

모든 모듈이 조금씩 공통으로 필요로 하는 아주 작은 공용 부품 상자라고 보면 됩니다.

예:
- 컨텍스트 이름
- 서비스 capability
- 서비스 설명 구조
- 민감정보 마스킹 규칙

## 2. 왜 이런 모듈이 필요한가요

각 모듈이 같은 enum, 같은 어노테이션, 같은 직렬화 규칙을 제각각 만들면:
- 중복이 생기고
- 표현이 달라지고
- 유지보수가 어려워집니다

그래서 정말 여러 곳에서 공통으로 쓰는 최소한의 것만 `shared-kernel`에 둡니다.

## 3. 초보자가 먼저 알아야 할 것

### `BoundedContext`

이 값은 "이 서비스가 어느 업무 그룹에 속하는가"를 나타냅니다.

예:
- `MASTER_DATA`
- `JOURNAL_LEDGER`
- `LOAN`

### `ServiceCapability`

이 값은 "이 서비스가 정확히 무엇을 제공하는가"를 나타냅니다.

예:
- `SOURCE_DOCUMENT_LOOKUP`
- `JOURNAL_POSTING`

### `ServiceDescriptor`

서비스를 소개하는 카드 같은 구조입니다.

예:
- 서비스 이름
- 어느 컨텍스트인지
- 어떤 capability가 있는지
- 설명 한 줄

### `@Masked`

이 필드는 화면이나 API 응답에서 그대로 보여주면 안 된다는 뜻입니다.

예:
- 이메일
- 계좌번호
- 등록번호

## 4. 마스킹은 어떻게 동작하나요

필드에 `@Masked(pattern = "EMAIL")` 같은 표시를 붙이면, JSON으로 바꿀 때 `MaskingSerializer`가 값을 일부 가려 줍니다.

예:
- `abc@example.com`
- `ab****@example.com`

주의:
- 어노테이션만 붙였다고 자동 완성되는 것은 아니고, 직렬화 설정이 연결되어 있어야 합니다.

## 5. 현재 코드에서 기억할 점

- 이 모듈은 작고 단순한 공통 코드만 담고 있습니다.
- 비즈니스 규칙은 여기 있지 않습니다.
- `app`에서는 서비스 디스커버리 레지스트리 구현과 연결되고, `governance` 같은 모듈에서는 마스킹과 연결해서 보면 이해가 쉽습니다.

## 6. 문서 추천 순서

1. [README.md](./README.md)
2. [process-flow.md](./process-flow.md)
3. [schema.md](./schema.md)

짧은 모듈이라 이 순서대로 보면 바로 끝납니다.
