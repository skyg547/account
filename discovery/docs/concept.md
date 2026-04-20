# Discovery Module Concept

## 1. 개요
Service Discovery를 위한 MSA 핵심 인프라 스트럭쳐 요소로, Netflix의 Eureka Server 구현체를 기반으로 한 `discovery-service`입니다.

## 2. 초보자를 위한 개념
> **마이크로서비스에서 레지스트리가 왜 필요한가요?**  
> 시스템 규모가 커지면 서비스 서버(예: `loan-service`, `reconciliation-service` 등)들이 여러 대로 늘어납니다. 이 때 서버의 IP 주소를 하드코딩하면 서버가 죽거나 증설되었을 때 코드를 일일이 수정해야 하는 불상사가 생깁니다. Eureka Server는 이런 각각의 서비스들이 "나 여기 살아있어요" 라고 알려주는 곳입니다. 그래서 MSA 환경에서는 필수적인 인프라 구성요소가 됩니다.

## 3. 타 모듈과의 연동 규칙
- `discovery`는 그 자체로 로직을 갖지 않는 인프라 요소입니다 (Anti-Skeleton Policy 예외).
- 다른 모듈(`app`, `contracts` 등)은 클라이언트로서 `spring-cloud-starter-netflix-eureka-client` 의존성을 추가하고 설정에 `defaultZone`을 `http://localhost:8761/eureka/`로 가리키게 하여 정보를 동기화합니다.
