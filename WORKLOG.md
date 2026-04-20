# WORKLOG (Source of Truth)

## 📅 2026-04-20
### [기획/팀장]
- 새로운 서비스 디스커버리 모듈 구축 요청이 들어옴.
- Eureka Server 역할을 할 `discovery` 모듈 구현 계획안(`implementation_plan.md`) 작성 및 승인 획득.
- 업무 API 서비스들을 Eureka Client로 단일 통합(app) 등록 진행함.
- **[구조 개편 결단]** `app` 비즈니스 컨테이너를 삭제하고 본격적인 MSA로 탈바꿈하자는 합의 도달! 시범적으로 분산 문서 중앙화 및 `journal-ledger`의 독립 마이크로서비스 전환 시작.

### [백엔드]
- `discovery` 서버(포트:8761) 구축.
- `app` 모듈 Eureka 클라이언트 연동 마무리. 
- (New) 문서 중앙화: `app/docs` 하위의 모든 마크다운 문서들을 최상위 `docs/` 폴더로 이관 이동 스크립트 실행 성공 (`Move-Item`).
- (New) 본격 MSA화 작업 (Target: `journal-ledger`):
  - `journal-ledger/build.gradle`에서 `java-library`를 폐기하고 `spring-boot`, `eureka-client`, `web` 등의 어플리케이션 의존성 추가.
  - `@SpringBootApplication`, `@EnableDiscoveryClient` 어노테이션이 박힌 메인 클래스 `JournalLedgerApplication` 생성.
  - 전용 `application.yml` 추가. 포트 분리(8081) 및 인메모리 독자 DB(H2) 셋업 추가. 독립 Eureka 네트워킹 설정 완료.

### [QA]
- `DiscoveryApplicationTests.java` ContextLoad 통과 확인.
- `./gradlew :app:build` 컴파일 성과 달성.
- (New) `./gradlew :journal-ledger:build` 단독 빌드 및 테스트 완전 통과. 타 모듈(`app`) 간섭 없이 독자적인 API 서비스 구동 능력 증명! (Exit code: 0)

**Next 담당자 ([기획/팀장]):**
- 1차 시범 분리가 성공함에 따라, 향후 나머지 레거시 라이브러리(`loan`, `closing` 등)의 순차적인 MSA 쪼개기 작업에 착수하거나, 다른 설계 작업을 개시할 수 있음.
