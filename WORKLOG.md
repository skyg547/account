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
- 클라우드 배포(K8s) 준비를 위해 **모든 마이크로서비스(`discovery`, `config-server`, `gateway`, `auth`, `master-data`, `journal-ledger`)에 `Dockerfile` 작성 완료.**
- **[New]** 초보자의 눈높이에 맞춰 `contracts`와 `shared-kernel` 같은 핵심 공통 모듈의 존재 이유와 주의사항을 아주 상세하게 풀어쓴 `README.md` 고도화 작업 완료.
- **[New]** 향후 어떤 비즈니스 모듈을 순차적으로 개발할지, 그 우선순위와 명확한 이유를 담은 **'비즈니스 모듈 개발 로드맵 (`docs/next-business-modules-plan.md`)'** 수립 완료.

**Next 담당자 ([기획/팀장] -> [백엔드]):**
- 로드맵 Phase 1에 따라, 이제 인프라를 벗어나 `journal-ledger`의 **"전표 룰 엔진 (Rule Engine)"** 고도화 작업에 돌입합니다.
- 카프카에서 이벤트를 받아 자동으로 복식부기 전표를 찍어내는 코어 비즈니스 로직(YOLO 코딩)을 시작할 준비가 되었습니다.
