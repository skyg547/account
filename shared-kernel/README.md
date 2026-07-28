# Shared Kernel: 작고 명확한 공용 부품 상자

## 초보자를 위한 DNA 비유

여러 서비스가 날짜, 통화, 마스킹 같은 말을 제각각 정의하면 같은 값을 서로 다르게 해석합니다.
`shared-kernel`은 모든 모듈이 합의한 **작은 공통어 사전과 공용 부품 상자**입니다.

DNA 비유는 “모든 기술 라이브러리를 이곳에 넣는다”는 뜻이 아닙니다. 공유 커널의 항목 하나를
바꾸면 여러 바운디드 컨텍스트가 동시에 영향을 받으므로, 정말 공동 소유하는 타입만 남겨야 합니다.

## 현재 실제 구성

- `BoundedContext`, `ServiceCapability`, `ServiceDescriptor`
- 같은 Spring 프로세스의 Bean을 찾는 `ServiceDiscoveryRegistry`
- JSON 출력 마스킹을 위한 `@Masked`와 `MaskingSerializer`
- 기존 공통 API 응답/예외/JPA 감사 타입
- account-mart와 ECL이 함께 쓰는 신용위험 enum 및 `CdmDataReadyEvent`

`ServiceDiscoveryRegistry`는 Eureka/Consul이 아닙니다. 현재 구현은 같은
`ApplicationContext`에 등록된 `DiscoverableService` Bean을 capability로 찾는 로컬 레지스트리입니다.

## 이번 점검에서 강화한 부분

- Spring `ApplicationContext`를 매번 조회하던 서비스 로케이터를 생성자 주입 불변 목록으로 변경
- 서비스 이름 중복을 애플리케이션 시작 시 fail-fast
- 서비스 설명과 이름의 공백 값 거부
- 사업자번호, 계좌번호, 이메일 마스킹 정책을 형식 보존 방식으로 보강
- 짧은 값, 미지원 패턴은 원문 대신 전체 마스킹
- 구현이 없는 `@DistributedLock` 사용을 ECL 소비자에서 제거하고 실제 락 포트 TODO 명시
- 실행 기능이 없는 Docker/Compose 뼈대를 archive로 이동

## 현재 구조 부채와 TODO

현재 `build.gradle`은 JPA, QueryDSL, Kafka, Redis, Vault, Actuator, tracing, Swagger 등을 `api`로
전파합니다. 이는 “작은 공유 커널” 원칙과 맞지 않지만 여러 모듈이 전이 의존성에 기대고 있어
한 번에 제거하면 빌드가 깨집니다.

다음 순서로 분리합니다.

1. 각 모듈이 실제 사용하는 starter를 직접 선언
2. 공통 버전은 Gradle platform/convention plugin으로 이동
3. ECL 전용 `BaseEntity`/`CrStaging`/`CalculationStatus`를 ECL 또는 allowance 계약으로 이동
4. account-mart/ECL 이벤트를 버전이 있는 통합 계약 모듈로 이동
5. 로컬 capability 레지스트리를 최소 SPI로 분리
6. shared-kernel에서 JPA/Spring Web/인프라 의존성 제거

## 실행과 문서

이 모듈은 `java-library`이며 Spring Boot 서버가 아닙니다. H2/PostgreSQL, 내장 WAS, Docker로
기동하지 않습니다.

```powershell
.gradlew :shared-kernel:test :shared-kernel:compileJava --console=plain --max-workers=1 --no-daemon
```

문서는 [docs/README.md](./docs/README.md)부터 읽습니다. 과거 Docker 뼈대는
[docs/archive/legacy-runtime-skeleton](./docs/archive/legacy-runtime-skeleton/README.md)에 보존했습니다.