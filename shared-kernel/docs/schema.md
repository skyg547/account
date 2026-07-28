# Shared-Kernel 코드 스키마와 소유권

## 최소 공통 타입

### `ServiceDescriptor`

- `serviceName`: 공백 불가, trim
- `context`: 필수 `BoundedContext`
- `capabilities`: null 불가, 불변 Set
- `description`: 공백 불가, trim

### `ServiceDiscoveryRegistry`

- descriptor 전체 조회
- 이름/BoundedContext/Capability 조회
- Java 타입별 로컬 Bean 조회

이 계약은 원격 주소, host, port, health를 제공하지 않습니다.

### `@Masked` / `MaskingSerializer`

JSON 문자열 출력 마스킹 계약입니다. `REG_NO`, `ACCOUNT`, `EMAIL`을 지원하고 그 외 값은
fail-closed 처리합니다.

## 현재 있지만 이동 대상인 타입

### ECL/Allowance 업무 타입

- `CrStaging`
- `CalculationStatus`
- `CustomerType`
- `ProductCategory`
- `CurrencyCode`
- `CdmDataReadyEvent`
- `BaseEntity`

`CurrencyCode`/고객·상품 분류는 account-mart와 ECL이 함께 쓰지만 전사 모든 도메인이 같은 enum을
사용하지는 않습니다. ECL 계산/상태 타입과 JPA 감사 상속은 더욱 특정 컨텍스트에 가깝습니다.
전용 allowance 계약과 ECL persistence 지원 패키지로 이동할 TODO가 있습니다.

### API Adapter 타입

- `ApiResponse`
- `GlobalExceptionAdvice`
- `ResourceNotFoundException`

API 응답 정책은 각 API 모듈과 프론트 계약 영향을 받습니다. 공통화가 정말 필요한지 확인하고,
필요하면 web-starter 모듈로 분리해야 합니다.

## 의존성 스키마 부채

현재 shared-kernel `api` 의존성에는 다음 인프라가 포함됩니다.

- JPA/QueryDSL
- Jackson/Logstash
- Actuator/Prometheus/Tracing/Zipkin
- Kafka/Flyway
- Springdoc/Redis/Vault

공유 타입 때문에 이 인프라가 전이되는 구조는 헥사고날 경계를 흐립니다. 각 Adapter 직접 의존과
Gradle convention/platform으로 옮긴 뒤 shared-kernel은 최소 Java/Jackson 계약만 남기는 것이
목표입니다.