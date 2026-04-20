# Shared-Kernel Module Docs

`shared-kernel` 모듈은 여러 모듈이 함께 쓰는 공통 타입과 공통 직렬화 규칙을 제공합니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 공통 타입과 마스킹이 다른 모듈에서 어떻게 쓰이는지 설명합니다.
- [schema.md](./schema.md): 포함된 공통 타입과 필드를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 shared-kernel의 역할을 쉽게 이해할 수 있도록 설명합니다.

## 이 모듈이 하는 일

1. 서비스가 어느 bounded context에 속하는지 표현합니다.
2. 서비스 디스커버리용 공통 설명자/레지스트리 타입을 제공합니다.
3. `@Masked` 어노테이션으로 민감정보 마스킹 의도를 표시합니다.
4. Jackson 직렬화 시 마스킹 규칙을 적용하는 `MaskingSerializer`를 제공합니다.

## 핵심 타입

- `BoundedContext`
- `ServiceCapability`
- `ServiceDescriptor`
- `DiscoverableService`
- `ServiceDiscoveryRegistry`
- `Masked`
- `MaskingSerializer`

## 현재 구현 기준에서 먼저 알아둘 점

- 공통 모듈이라 저장소나 도메인 로직은 없습니다.
- 서비스 메타데이터는 이후 모듈 분리 시에도 그대로 재사용할 수 있는 안정 식별자를 목표로 합니다.
- 마스킹은 어노테이션과 직렬화기만 제공하고, 실제 적용 위치는 각 모듈 엔티티/DTO에 달려 있습니다.
