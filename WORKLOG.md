# WORKLOG (Source of Truth)

## 📅 2026-04-21
### [기획/팀장]
- `master-data` 모듈을 독립 마이크로서비스로 전환하기 위한 설계 및 실행 착수.
- 전표(Journal)와 원장(Ledger)이 의존하는 핵심 기준 정보(마스터 데이터)의 독립성 확보.

### [백엔드]
- `master-data` 모듈을 Spring Boot 애플리케이션으로 승격 (포트: 8082).
- `build.gradle`에 `spring-boot`, `eureka-client` 의존성 추가 및 `MasterDataApplication` 생성.
- 독립 H2 인메모리 DB 및 Eureka 서버 연동 설정 (`application.yml`) 완료.
- `./gradlew :master-data:build` 컴파일 및 빌드 성공 확인.

### [모델러]
- 핵심 마스터 데이터 엔티티(`AccountSubject`, `BusinessPartner`, `Department`)의 SCD2(Slowly Changing Dimension Type 2) 적용 상태 전수 검사.
- 모든 핵심 마스터에 `validFrom`, `validTo`를 이용한 유효기간 기반 이력 관리 로직이 정교하게 구현되어 있음을 확인(DoD 달성).

### [기획/팀장]
- 중앙 집중형 로그 관리를 위한 ELK(Elasticsearch, Logstash, Kibana) 스택 및 시스템 상태 모니터링을 위한 프로메테우스(Prometheus), 그라파나(Grafana) 설계 도입.
- MSA 분산 추적(Distributed Tracing)을 위한 **Zipkin(집킨)** 스택 추가 도입.
- 기존 모놀리식 구성을 해체하고 `elasticsearch`, `logstash`, `kibana`, `prometheus`, `grafana`, `zipkin` 6개의 독립 모듈(폴더)을 프로젝트 최상단(루트)으로 완전 분리 이동.
- 각 폴더 내에 초보 개발자를 위한 친절한 비유를 담은 `README.md` 가이드 문서 작성 (그라파나의 프로메테우스 자동 연결 설정 포함).
- 프로젝트 룰에 따라 '1-Directory 1-README' 및 '초보자용 설명' 원칙 준수.

### [기획/팀장]
- 운영(Production) 레벨의 시스템 안정성을 확보하기 위해 **① 서킷 브레이커(Resilience4j)** 와 **② 중앙 인증 시스템(JWT Auth)** 전격 도입.
- 기존 내부회계 관리를 담당하는 `governance` 모듈의 고유 역할을 보존하고, 오직 인증(로그인 및 JWT 발급)만을 전담하는 **`auth` 마이크로서비스 모듈을 신규 생성**하여 도메인 경계를 명확히 분리.
- 운영 환경을 위한 필수 인프라 점검 수행 및 마지막 퍼즐인 **③ Flyway(DB 형상 관리)** 와 **④ OpenFeign(동기 통신 최적화)** 전격 도입 설계.
- 대규모 협업 및 보안, 확장성을 위한 Advanced MSA 도구 4종(Swagger, Spring Cloud Contract, Redis, Vault) 추가 설계 및 구조화 완비.
- 여태까지 구축한 방대한 MSA 인프라 10종(ELK, Kafka, Zipkin, Config 등)의 역할과 존재 이유를 총망라한 **초보자용 완벽 가이드 (`docs/infrastructure-guide.md`)** 작성 완료.

### [백엔드]
- `shared-kernel/build.gradle`에 `flyway-core`, `spring-cloud-starter-openfeign` 등 공통 라이브러리 추가.
- `master-data`와 `journal-ledger`의 리소스 폴더에 `db/migration/V1__init_baseline.sql` 뼈대 생성.
- `MasterDataApplication`과 `JournalLedgerApplication` 메인 클래스에 `@EnableFeignClients` 어노테이션 추가.
- `shared-kernel`에 통합 명세서 `springdoc-openapi`(Swagger), 소비자 주도 계약 `spring-cloud-starter-contract-verifier`, 초고속 분산 캐시 `spring-boot-starter-data-redis`, 중앙 비밀번호 관리 `spring-cloud-starter-vault-config` 라이브러리 전격 주입.
- `gateway` 모듈의 WebFlux 환경을 위한 전용 Swagger 의존성 추가 및 `config-repo/gateway-service.yml`에 각 서비스의 Swagger 문서를 한 곳으로 끌어모으는(Aggregate) 라우팅 설정 완료.
- 프로젝트 최상단에 `redis`, `vault` 독립 폴더를 생성하고, 각각의 `docker-compose.yml`과 초보자용 비유가 담긴 `README.md` 작성으로 MSA 개별 관리 체계 완성.

**Next 담당자 ([기획/팀장] -> [백엔드]):**
- 뼈대와 인프라 셋팅은 100% 끝났습니다!
- 이제 `journal-ledger`에 숨어있는 **"전표 룰 엔진 (Rule Engine)"** 을 고도화하여, 외부 이벤트(Kafka)가 들어왔을 때 자동으로 완벽한 복식부기 전표를 생성해 내는 **비즈니스 로직(YOLO 코딩)**에 전념할 차례입니다.
