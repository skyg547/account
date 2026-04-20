# WORKLOG (Source of Truth)

## 📅 2026-04-20
### [기획/팀장]
- 새로운 서비스 디스커버리 모듈 구축 요청이 들어옴.
- Eureka Server 역할을 할 `discovery` 모듈 구현 계획안(`implementation_plan.md`) 작성 및 승인 획득.
- **후속작업:** 업무 API 서비스들을 Eureka Client로 등록 지시 수령. (다만 아키텍처 상 모듈러 모놀리스로 판단되어 `app` 모듈 한 곳에서 통합 등록하도록 계획 수립 및 진행 결정).

### [백엔드]
- `discovery` 서버 구축:
  - `settings.gradle`에 `discovery` 서브모듈 추가.
  - `discovery/build.gradle`에 Spring Boot Web 및 Spring Cloud Eureka Server 의존성 추가 (Spring Cloud 2023.0.1).
  - `DiscoveryApplication.java` 생성 및 `@EnableEurekaServer` 적용.
  - `application.yml`에 포트 8761 지정 및 독립 서버 작동 옵션(register-with-eureka/fetch-registry false) 부여.
  - 모듈 컨벤션에 맞춰 `README.md` 및 `docs/concept.md` 작성(초보자 배려).
- `app` 모듈(통합 비즈니스 API 노드) 연동:
  - `app/build.gradle`에 `spring-cloud-starter-netflix-eureka-client` 추가. Spring Cloud BOM(`io.spring.dependency-management`) 추가.
  - 외부 변경사항으로 인해 빌드가 깨졌던 `contracts` 모듈 빌드 에러 분석(DiscoverableService 누락) 후, `shared-kernel` 의존성을 `contracts/build.gradle`에 추가하여 문제를 자가교정(Self-Healing) 성공!!
  - `AccountApplication.java`에 `@EnableDiscoveryClient` 어노테이션 적용.
  - `app/.../application.yml`에 Eureka 디폴트존(`http://localhost:8761/eureka/`) 지정.

### [QA]
- `DiscoveryApplicationTests.java`의 ContextLoad 통과 확인 (Exit Code 0).
- `./gradlew :app:build` 명령어를 통해 `app`을 포함한 전체 하위 모듈이 컴파일 에러 없이 연동됨을 확인함.
- `app` 컨텍스트 및 `contracts` 테스트 통과 이력 보고.

**Next 담당자 ([기획/팀장]):**
- 이후 추가할 비즈니스 로직(데이터 마트 설계 혹은 특정 엔티티 매핑 방안)을 논의할 준비 완료.
