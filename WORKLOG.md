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

## 📅 2026-04-22
### [기획/팀장]
- Phase 1 "회계의 심장" 완성: 전표 자동 생성부터 원장 전기까지의 End-to-End 파이프라인 구축 완료.
- 파편화되어 있던 `ledger` 도메인을 `journalledger` 패키지로 통합 및 헥사고날 아키텍처 구조 정립.

### [백엔드]
- **전표 룰 엔진 고도화:** SpEL(Spring Expression Language)을 도입하여 `${amount} * 0.1`과 같은 복잡한 수식 계산 및 조건 매칭 지원.
- **Kafka 연동:** `KafkaTransactionListener`를 통해 외부 트랜잭션 이벤트를 실시간 수신하여 자동 전표 생성.
- **원장 전기 로직 완성:** `PostingService`를 통해 전표 확정 시 `GlEntry`, `SlEntry` 상세 내역을 생성하도록 구현. (빌드 및 테스트 검증 완료)
- **잔액 이월(Carry-forward) 구현:** `LedgerService`에서 전표 전기 시 직전 최종 잔액을 조회하여 기초 잔액(Beginning Balance)으로 자동 설정하는 로직 추가. (가이드 문서 작성 완료)
- **단위 테스트 및 문서화:** `JournalRuleEngineTest`를 통해 SpEL 로직 검증 완료 및 `ledger-carry-forward.md` 문서 고도화 완료.
- **[New] 배치 및 운영 도구:** `journal-ledger:batch` 모듈에 잔액 재집계 배치 구현. `RUNBOOK.md` 및 `start-*.bat` 스크립트 제작으로 운영 자동화 완성.
- **[New] 인프라 정합성:** 모든 모듈의 Eureka, Kafka, Zipkin 연동 상태 점검 및 최적화 완료.

### [모델러]
- `GlBalance`, `SlBalance` 엔티티 통합 및 `@Entity`, `@Table` 등 JPA 어노테이션 보강.
- 엔티티 내부에 `addDebit`, `addCredit`, `recalculate` 등 비즈니스 로직을 캡슐화한 풍부한 도메인 모델(Rich Domain Model) 구현.

### [프론트]
- **프론트엔드 집중 개발 뼈대 구축 완료.**
- **아키텍처 및 디자인 설계:** `docs/` 내에 Next.js 15 기반 아키텍처 및 글래스모피즘(Glassmorphism) 레이아웃 설계서 작성 완료.
- **프로젝트 초기화:** `frontend/` 디렉토리에 Next.js 15 (TypeScript, App Router) 독립 프로젝트 생성.
- **프리미엄 디자인 시스템:** TailwindCSS 대신 Vanilla CSS(CSS Modules)를 사용하여 우리만의 독창적인 다크 테마 및 유리 효과(Glass Style) 토큰 정의 (`globals.css`).
- **공통 인터페이스 구현:**
    - `Sidebar.tsx`: Lucide 아이콘을 활용한 세련된 좌측 메뉴.
    - `Navbar.tsx`: 통합 검색창 및 사용자 프로필이 포함된 상단 바.
    - `RootLayout`: 사이드바와 컨텐츠 영역이 조화로운 전역 구조 완비.
- **대시보드 메인 구현:** 총 자산, 부채, 당기순이익 위젯 및 실시간 전표 유입 리스트가 포함된 첫 화면 개발 완료.
- **문서화:** 초보 개발자를 위한 로컬 `frontend/README.md` 가이드 및 코드 내 주석 작업 완료.

### [프론트]
- **프론트엔드 초보자 맞춤형 교육 환경 및 인프라 구축 완료.**
- **교육 문서 3종 세트 작성:**
    - `beginner-guide.md`: 비유를 통한 Next.js 기초 및 폴더 구조 설명.
    - `development-guide.md`: 실전 페이지 생성 및 CSS Modules 사용법 가이드.
    - `runbook.md`: 초보자가 겪는 주요 에러(Port 3000 등) 해결 방법 명시.
- **Docker 기반 독립 실행 환경 구축:** `Dockerfile` 및 `docker-compose.yml` 작성을 통해 프런트엔드만 따로 띄워 테스트 가능한 환경 제공.
- **코드 내 상세 가이드 주석:** `layout.tsx`, `page.tsx` 등 핵심 파일에 초보자가 이해하기 쉬운 한글 주석 작업 완료.
- **README 고도화:** 모든 가이드 문서로 연결되는 포털 형태의 `frontend/README.md` 전면 개편.

**Next 담당자 ([프론트] -> [QA]):**
- 작성된 가이드 문서의 가독성 및 도커 컨테이너 빌드 정상 여부 검증이 필요합니다.
- 초보 개발자 입장에서 가이드를 따라 서비스가 정상 실행되는지 최종 확인을 요청합니다.

### [백엔드] master-data DDD/Hexagonal 보강
- 현재까지의 MSA 전환 작업을 `79e1e01` 커밋으로 고정하고 `origin/main`에 push 완료.
- `master-data`의 Controller/DTO/Application Service 파일 위치를 package 구조와 맞게 `masterdata.api` 및 `masterdata.core.application` 아래로 정리.
- `AccountSubject`, `BusinessPartner`, `Department`, `Product` 저장 흐름에 `core.port.out` 출력 포트 인터페이스를 추가하여 application service가 JPA Repository를 직접 알지 않도록 정리.
- `BusinessPartner` API를 JPA Entity 직접 요청/응답 방식에서 `BusinessPartnerRequestDto`/`BusinessPartnerDto`/`BusinessPartnerCommand` 경유 방식으로 전환.
- 초보 개발자가 요청 흐름을 따라갈 수 있도록 `master-data/README.md`, `master-data/docs/README.md`, `beginner-guide.md`, `process-flow.md`에 헥사고날 요청 흐름과 패키지 책임을 상세화.
- `./gradlew :master-data:compileJava` 성공 확인.
