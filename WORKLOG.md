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
- MSA의 "프랜차이즈 본사" 역할을 하는 **중앙 설정 서버(Config Server)** 인프라 신규 도입 (포트 8888).
- 수십 개로 쪼개질 마이크로서비스들의 `application.yml` 파일을 한 곳에서 중앙 통제할 수 있도록 `config-repo` 디렉토리 신설.
- 프로젝트 룰에 따라 `config-server` 모듈 내에 초보자를 위한 비유와 실행법이 담긴 `README.md` 가이드 작성.

### [백엔드]
- `config-server` 독립 모듈 생성 및 `spring-cloud-config-server` 의존성 주입 완료.
- `discovery`, `master-data`, `journal-ledger`, `gateway` 등 모든 핵심 서비스의 복잡한 로컬 `application.yml` 설정을 `config-repo` 폴더의 단일 텍스트 파일들로 전면 이관.
- 공통 모듈인 `shared-kernel`에 `spring-cloud-starter-config` 의존성 추가.
- 각 서비스의 `application.yml`은 오직 `spring.config.import=optional:configserver:http://localhost:8888/` 속성만을 갖도록 대폭 경량화(초경량 지점 셋팅 완료).

**Next 담당자 ([기획/팀장] -> [QA]):**
- 가장 먼저 `./gradlew :config-server:bootRun`을 실행하여 본사(8888) 서버를 띄운다.
- 브라우저에서 `http://localhost:8888/master-data/default`를 호출하여 중앙 설정이 정상적으로 반환되는지 확인.
- 이후 `discovery`, `gateway` 등 나머지 서비스들을 띄우고, 이들이 켜지면서 본사로부터 설정을 올바르게 다운로드받는지 검증 요망.
