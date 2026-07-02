# WORKLOG (Source of Truth)

> 이 문서는 프로젝트의 전체 작업 이력과 컨텍스트를 유지하기 위한 통합 워크로그입니다.
> 이전 작업 내역은 사용자 요청에 의해 초기화되었습니다.

### 📅 2026-06-18 (Codex 수정)
### [구조화] Payable/Receivable/Reconciliation/Tax/Expenditure Resolution core/api/batch 실행 모듈 분리
- **작업 배경**:
  - 기존 `payable`, `receivable`, `reconciliation`, `tax`, `expenditure-resolution`은 주로 `compileJava/test` 검증 대상이었고 단독 Spring Boot API/BATCH 실행 진입점이 없었다.
  - 사용자 요청에 따라 업무 로직은 `core`가 소유하고, `api`/`batch` 실행 모듈은 반드시 `core`를 참조하도록 구조를 정리했다.
- **수정 범위**:
  - `settings.gradle`에 `:module:core`, `:module:api`, `:module:batch` 하위 프로젝트를 추가하고 기존 `:module` 경로는 호환 alias로 유지했다.
  - 기존 `src` 소스와 테스트를 각 `core/src`로 이동했다.
  - `api`/`batch` Spring Boot main class와 H2 local `application.yml`을 추가했다.
  - local profile용 외부 포트 어댑터를 core의 infrastructure/local 경계에 추가해 master-data, journal, ledger, tax, asset 외부 서버 없이 H2 컨텍스트를 올릴 수 있게 했다.
  - Gradle 하위 프로젝트의 group/archive 식별자를 고유하게 지정해 `com.ho:core` capability 충돌을 방지했다.
  - `tax:core`의 사용되지 않는 `journal-ledger:core` 직접 의존을 제거했다.
  - `expenditure-resolution`의 master-data 코드 기반 JPA 참조를 DB FK 대신 포트 검증 중심으로 맞추고 H2 기동 가능한 매핑으로 보정했다.
  - 각 모듈 README/local-run 문서와 `docs/local-development.md`를 새 실행 구조 기준으로 갱신했다.
- **검증 명령**:
  - `.\gradlew projects --console=plain`
  - `.\gradlew :payable:core:compileJava :payable:api:compileJava :payable:batch:compileJava :receivable:core:compileJava :receivable:api:compileJava :receivable:batch:compileJava :reconciliation:core:compileJava :reconciliation:api:compileJava :reconciliation:batch:compileJava :tax:core:compileJava :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:compileJava :expenditure-resolution:api:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :payable:core:test :receivable:core:test :reconciliation:core:test :tax:core:test :expenditure-resolution:core:test --console=plain --max-workers=1`
  - `.\gradlew :payable:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1`
  - `.\gradlew :receivable:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1`
  - `.\gradlew :reconciliation:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1`
  - `.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1`
  - `.\gradlew :expenditure-resolution:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1`
  - `.\gradlew :payable:batch:bootRun :receivable:batch:bootRun :reconciliation:batch:bootRun :tax:batch:bootRun :expenditure-resolution:batch:bootRun --console=plain --max-workers=1`
  - `.\gradlew --stop`, `jps -lv`로 프로젝트 Gradle/BootRun 프로세스 정리 확인.
- **검증 결과**:
  - 5개 대상 모듈의 core/api/batch 컴파일 성공.
  - 5개 대상 모듈의 core 테스트 성공.
  - 5개 API 실행 진입점이 H2/local 컨텍스트로 기동 후 종료되는 스모크 검증 성공.
  - 5개 Batch 실행 진입점이 H2/local 컨텍스트로 기동 후 종료되는 스모크 검증 성공.
  - 작업 후 Gradle daemon은 남아 있지 않고, 프로젝트 BootRun Java 프로세스도 남아 있지 않음을 확인했다.
- **남은 리스크**:
  - 이번 검증은 H2/local adapter 기준이며 PostgreSQL profile과 실제 외부 서비스 연동은 별도 검증이 필요하다.
  - 새 Batch 실행 모듈 일부에서 Spring Batch `jobRegistryBeanPostProcessor` 관련 BeanPostProcessorChecker 경고가 남아 있다. deposit/reporting에 적용한 JobRegistry 지연 등록 패턴을 후속으로 이식할 수 있다.
  - `expenditure-resolution`은 아직 master-data 내부 엔티티를 JPA 연관으로 직접 참조하는 구간이 남아 있어, 장기적으로는 코드 값과 `MasterDataQueryPort` 검증 중심으로 더 분리하는 것이 DDD/헥사고날 방향에 맞다.

### 📅 2026-06-18 (Codex 수정)
### [수정] Batch JobRegistry 조기 초기화 경고 제거
- **Git 동기화**:
  - `git fetch origin` 실행.
  - `main...origin/main` 차이 `0 0` 확인 후 작업 시작.
- **수정 범위**:
  - `deposit:batch`, `reporting:batch`에 Batch Job 등록 전용 설정을 추가.
  - Spring Batch 기본 `jobRegistryBeanPostProcessor` BeanDefinition을 제거하고, Spring Batch 5.1.1의 대체 경로인 `JobRegistrySmartInitializingSingleton`으로 Job 등록 시점을 모든 singleton 생성 이후로 이동.
  - `deposit/docs/local-run.md`, `reporting/docs/local-run.md`, `docs/local-development.md`에 Batch JobRegistry 설정 의미를 초보자 기준으로 설명.
- **검증 명령**:
  - `.\gradlew :deposit:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
- **검증 결과**:
  - Deposit Batch, Reporting Batch 로컬 기동 성공.
  - 기존 남은 리스크였던 `jobRegistryBeanPostProcessor` 관련 BeanPostProcessorChecker 경고가 두 Batch 기동 로그에서 재현되지 않음.
  - Batch 앱에는 업무 if/for/math 로직을 추가하지 않았고, JobRegistry 등록 인프라만 조정.
- **남은 리스크**:
  - 운영/일반 profile에서는 기존 logstash 전송이 유지되므로 수집기가 필요하다.
  - H2/Flyway 버전 권고 경고는 로컬 H2 조합에서 남아 있으며, 운영 DB 검증과 별도이다.

### 📅 2026-06-17 (Codex 수정)
### [수정] 로컬 standalone 실행 시 LoadBalancer 자동 구성 비활성화
- **Git 동기화**:
  - `git fetch origin` 실행.
  - `main...origin/main` 차이 `0 0` 확인 후 작업 시작.
- **수정 범위**:
  - 로컬 H2/메모리/로컬 어댑터 실행 명령에서 외부 서비스 디스커버리를 끄는 경우 `--spring.cloud.loadbalancer.enabled=false`를 함께 사용하도록 정리.
  - `.run/Deposit API bootRun.run.xml`, `.run/Deposit Batch Context.run.xml`, `.run/Loan API bootRun.run.xml`, `.run/Closing API bootRun.run.xml`, `.run/Asset Lease API bootRun.run.xml`, `.run/Reporting API bootRun.run.xml`, `.run/Reporting Batch Context.run.xml` 갱신.
  - `deposit`, `loan`, `closing`, `asset-lease`, `reporting`의 README/local-run 문서와 `docs/local-development.md` 실행 예시를 동일한 기준으로 맞춤.
- **검증 명령**:
  - `.\gradlew :deposit:api:bootRun --args="--spring.main.web-application-type=none --spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :deposit:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:api:bootRun --args="--spring.main.web-application-type=none --spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - 수정한 IntelliJ `.run` XML 파싱 확인.
  - 활성 로컬 실행 문서와 `.run` 설정에서 `spring.cloud.discovery.enabled=false` 사용 시 `spring.cloud.loadbalancer.enabled=false` 누락 여부 확인.
- **검증 결과**:
  - Deposit API/BATCH, Reporting API/BATCH 로컬 기동 성공.
  - Deposit API에서 남아 있던 Spring Cloud LoadBalancer BeanPostProcessor 경고는 `spring.cloud.loadbalancer.enabled=false` 적용 후 재현되지 않음.
  - 수정한 `.run` XML 파싱 성공.
  - 활성 로컬 실행 문서와 `.run` 설정 기준 누락 없음.
- **남은 리스크**:
  - Batch 컨텍스트에서는 Spring Batch `jobRegistry` 관련 BeanPostProcessor 경고가 남아 있다. LoadBalancer 경고와 별도이며 현재 기동 실패를 유발하지 않는다.
  - 운영/일반 profile에서는 기존 logstash 전송이 유지되므로 수집기가 필요하다.

### 📅 2026-06-17 (Codex 수정)
### [수정] Spring Cloud LoadBalancer Caffeine 캐시 의존성 반영
- **Git 동기화**:
  - `git fetch origin` 실행.
  - `main...origin/main` 차이 `0 0` 확인 후 작업 시작.
- **수정 범위**:
  - Eureka/Gateway/OpenFeign을 직접 사용하는 실행 모듈에 `com.github.ben-manes.caffeine:caffeine` 의존성을 추가.
  - 대상: `account-mart:mart-api`, `account-mart:mart-batch`, `asset-lease`, `auth`, `deposit:api`, `ecl:ecl-api`, `ecl:ecl-batch`, `gateway`, `governance`, `journal-ledger:api`, `journal-ledger:core`, `loan:api`, `master-data`.
  - `docs/local-development.md`에 LoadBalancer, Eureka, Caffeine의 역할과 로컬 실행 시 의미를 초보자 기준으로 보강.
- **검증 명령**:
  - Spring Cloud 클라이언트 build 파일 검색으로 Caffeine 누락 여부 확인.
  - `.\gradlew :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava :asset-lease:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava --console=plain --max-workers=1 --no-daemon --continue`
  - `.\gradlew :auth:compileJava :deposit:api:compileJava :gateway:compileJava :governance:compileJava :journal-ledger:api:compileJava :journal-ledger:core:compileJava :loan:api:compileJava :master-data:compileJava --console=plain --max-workers=1 --no-daemon --continue`
  - `.\gradlew :deposit:api:compileJava --console=plain --max-workers=1 --no-daemon --stacktrace`
  - `.\gradlew :deposit:api:bootRun --args="--spring.main.web-application-type=none --spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
- **검증 결과**:
  - Spring Cloud 클라이언트를 직접 쓰는 build 파일에는 Caffeine 의존성이 모두 존재함을 확인.
  - Caffeine 의존성을 추가한 전체 대상 모듈의 `compileJava` 성공.
  - 대표 모듈 `deposit:api:compileJava` 성공.
  - 대표 로컬 기동 `deposit:api:bootRun` 성공.
  - 이전 남은 리스크였던 Spring Cloud LoadBalancer 기본 캐시/Caffeine 권고 경고는 대표 기동 로그에서 재현되지 않음.
- **남은 리스크**:
  - Spring Cloud 내부 `BeanPostProcessorChecker` 경고는 Caffeine 캐시 경고와 별개로 대표 기동 로그에 남아 있다. 기능 실패는 아니지만, Spring Cloud 버전 업그레이드 또는 관련 자동 구성 조건 정리 시 별도 검토가 필요하다.
  - 운영/일반 profile에서는 기존 logstash 전송이 유지되므로 수집기가 필요하다.

### 📅 2026-06-15 (Codex 수정)
### [수정] 로컬 bootRun logstash 연결 경고 제거
- **수정 범위**:
  - `account-mart:mart-api`, `ecl:ecl-api`, `gateway`, `discovery`, `master-data`, `journal-ledger`, `journal-ledger:api`, `journal-ledger:batch`, `asset-lease`의 `logback-spring.xml`을 `local` profile 기준으로 분기.
  - `spring.profiles.active=local`일 때는 `LOGSTASH` appender를 만들거나 root logger에 연결하지 않고 콘솔 로그만 사용하도록 정리.
  - 운영/일반 profile(`!local`)에서는 기존 logstash JSON 전송 구조와 custom field를 유지.
  - `.run/Deposit API bootRun.run.xml`, `.run/Deposit Batch Context.run.xml`, `.run/Reporting API bootRun.run.xml`, `.run/Reporting Batch Context.run.xml`에 `--spring.profiles.active=local` 추가.
  - `deposit/README.md`, `deposit/docs/local-run.md`, `reporting/README.md`, `reporting/docs/local-run.md`, `docs/local-development.md`의 로컬 실행 명령을 `local` profile 기준으로 갱신.
- **검증 명령**:
  - `git fetch origin`
  - `git rev-list --left-right --count main...origin/main`
  - `.\gradlew :deposit:api:bootRun --args="--spring.main.web-application-type=none --spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :deposit:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:api:bootRun --args="--spring.main.web-application-type=none --spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - 모든 `logback-spring.xml` XML 파싱 확인.
  - Deposit/Reporting IntelliJ `.run` XML 파싱 확인.
  - `git diff --check`
- **검증 결과**:
  - 원격 동기화 확인: `main...origin/main` 차이 `0 0`.
  - Deposit/Reporting API/BATCH 네 가지 bootRun 컨텍스트 스모크 성공.
  - 이전처럼 `LogstashTcpSocketAppender`의 `localhost:5000` 연결 실패 경고가 발생하지 않음.
  - logback XML, `.run` XML 파싱 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - `local` profile이 아닌 운영/일반 profile에서는 logstash 수집기가 떠 있어야 한다.
  - Spring Cloud LoadBalancer 기본 캐시 경고는 남아 있으며, 운영 profile에서는 Caffeine cache 적용 여부를 별도 판단해야 한다.

### 📅 2026-06-12 (Codex 수정)
### [수정] Gemini standalone API/Batch 검수 결과 반영 및 로컬 실행 정리
- **수정 범위**:
  - `asset-lease`는 기존 `AssetLeaseApplication`이 정식 실행 앱이므로 Gemini가 추가한 미추적 `AssetLeaseApiApplication`, `AssetLeaseBatchApplication`을 제거해 `bootJar` main class 충돌을 해소.
  - `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`는 현재 단일 `java-library` 모듈이므로 잘못 추가된 API/BATCH Application 후보를 제거하고 기존 테스트/컴파일 검증 흐름 유지.
  - `deposit:batch`에 Spring Boot 플러그인, Boot BOM, `bootJar` mainClass, H2 runtime, Batch test 의존성을 추가해 실제 Batch 컨텍스트 앱으로 전환.
  - `reporting:api`, `reporting:batch`에 Spring Boot 플러그인, `bootJar` mainClass, H2 runtime을 추가하고 `scanBasePackages`를 `com.ho.account.reporting`으로 제한.
  - `deposit` 로컬 단독 실행용 `LocalDepositMasterDataAdapter`, `LocalDepositJournalPostingAdapter`를 추가. `account.deposit.local-adapters.enabled=true`일 때만 master-data/journal-ledger 외부 포트를 학습용으로 대체.
  - `reporting` memory 모드용 `InMemoryLedgerBalanceAdapter`, `InMemoryJournalQueryAdapter`를 추가. `account.reporting.persistence.mode=memory`일 때 journal-ledger 없이 보고서 생성/주석 drill-through 컨텍스트를 기동.
  - `deposit:batch`, `reporting:batch`에 `@EntityScan`, `@EnableJpaRepositories`를 명시해 batch main class 패키지와 core 영속성 패키지 간 스캔 범위를 정렬.
  - `.run/Deposit API bootRun.run.xml`, `.run/Deposit Batch Context.run.xml`, `.run/Reporting API bootRun.run.xml`, `.run/Reporting Batch Context.run.xml` 추가.
  - `deposit/README.md`, `deposit/docs/README.md`, `deposit/docs/local-run.md`를 추가하고, `reporting` 및 전사 로컬 실행 문서를 최신 실행 구조로 갱신.
- **검증 명령**:
  - `.\gradlew :asset-lease:bootJar :deposit:core:test :deposit:api:bootJar :deposit:batch:bootJar :reporting:core:test :reporting:api:bootJar :reporting:batch:bootJar :expenditure-resolution:compileJava :payable:compileJava :receivable:compileJava :reconciliation:compileJava :tax:compileJava --console=plain --max-workers=1 --no-daemon --continue`
  - `.\gradlew :deposit:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :deposit:batch:bootRun --args="--spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - `.\gradlew :reporting:batch:bootRun --args="--spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain --max-workers=1 --no-daemon`
  - IntelliJ `.run` XML 파싱 확인.
  - `git diff --check`
- **검증 결과**:
  - 전체 대상 `bootJar`/`compileJava`/`deposit:core:test`/`reporting:core:test` 성공.
  - `deposit:api`, `deposit:batch`, `reporting:api`, `reporting:batch` 개별 bootRun 컨텍스트 스모크 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - 로컬 bootRun 시 logstash 수집기(`localhost:5000`)가 없으면 연결 경고와 종료 지연이 발생한다. 기능 실패는 아니지만 로컬 profile에서 logstash appender 비활성화 설정이 필요하다.
  - `deposit` 로컬 어댑터는 운영 전표를 저장하지 않으므로 운영에서는 master-data/journal-ledger 실제 어댑터로 교체해야 한다.
  - `reporting` memory 모드는 샘플 GL 잔액과 빈 drill-through를 반환하므로 운영형 검증은 실제 `LedgerQueryPort`, DB, Flyway seed가 필요하다.

### 📅 2026-06-11 (Codex 검수)
### [검수] Gemini standalone API/Batch Application 추가 작업 검토
- **검수 범위**:
  - 로컬 미추적 Application 파일: `asset-lease`, `deposit`, `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `reporting`, `tax`의 API/Batch Spring Boot Application 후보.
  - 기존 정상 standalone 구조 비교: `loan`, `closing`, `journal-ledger`, `account-mart`, `ecl`, `master-data`, `governance`, `auth`, `gateway`, `discovery`, `config-server`.
- **Git 동기화**:
  - `git fetch origin` 실행.
  - `main...origin/main` 차이 `0 0` 확인.
- **주요 검수 결과**:
  - `asset-lease`는 기존 `AssetLeaseApplication`이 있는데 Gemini가 `AssetLeaseApiApplication`, `AssetLeaseBatchApplication`을 추가해 main class 후보가 3개가 되었고 `:asset-lease:bootJar`가 실패한다.
  - `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`는 `java-library` 모듈인데 Batch Application이 `@EnableBatchProcessing`을 사용한다. 해당 모듈 build.gradle에는 `spring-boot-starter-batch`가 없어 `compileJava`가 실패한다.
  - `reporting:api`, `reporting:batch`는 Application 클래스 컴파일은 가능하지만 하위 모듈이 `java-library`라 `bootRun`/`bootJar` 태스크가 없다. 단독 실행 앱으로는 아직 불완전하다.
  - `deposit:api`는 기존 `DepositApplication`과 Spring Boot 플러그인이 있어 API 단독 실행 구조가 이미 있다. `deposit:batch`는 `java-library`라 새 Batch Application만으로는 단독 실행 구조가 완성되지 않는다.
- **검증 명령**:
  - `.\gradlew :asset-lease:compileJava :expenditure-resolution:compileJava :payable:compileJava :receivable:compileJava :reconciliation:compileJava :reporting:api:compileJava :reporting:batch:compileJava :tax:compileJava :deposit:batch:compileJava --console=plain --max-workers=1 --no-daemon --continue`
  - `.\gradlew :asset-lease:bootJar :deposit:api:bootJar :reporting:api:tasks :reporting:batch:tasks :payable:tasks :reconciliation:tasks --console=plain --max-workers=1 --no-daemon --continue`
- **검증 결과**:
  - `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`, `deposit:batch` 실패.
  - `asset-lease:bootJar` 실패: main class 후보가 `AssetLeaseApplication`, `AssetLeaseApiApplication`, `AssetLeaseBatchApplication` 3개라 단일 main class를 결정하지 못함.
  - `deposit:api:bootJar` 성공.
  - `reporting:api`, `reporting:batch`, `payable`, `reconciliation` task 목록에는 `bootRun`/`bootJar`가 없음.
- **권고**:
  - API/Batch 단독 실행을 하려면 기존 정상 모듈처럼 `:module:api`, `:module:batch`, `:module:core` 하위 프로젝트로 분리하고 각 실행 모듈에 Spring Boot 플러그인을 적용해야 한다.
  - 단일 `java-library` 모듈에 Application 클래스만 추가하는 방식은 bootRun 태스크를 만들지 못하고, batch 의존성/컴포넌트 스캔 문제를 유발한다.
  - `scanBasePackages = "com.ho.account"`는 과도하게 넓어 다른 모듈 Bean까지 스캔할 수 있으므로 실행 모듈별 소유 패키지로 제한해야 한다.

### 📅 2026-06-11 (Codex 문서)
### [문서] Foundation/Infra 문서 9차 통합 및 IntelliJ 실행 설정 정리
- **수정 범위**:
  - `contracts/docs/local-run.md`, `shared-kernel/docs/local-run.md`를 추가하고 `README.md`/docs 인덱스에서 library 모듈 컴파일 검증 흐름을 연결.
  - `master-data/docs/beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 SCD2 기준정보, 변경 요청 승인/반영, 포트/어댑터 흐름을 현재 코드 기준으로 통합.
  - `governance/docs/beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 감사 로그, 승인, SOD, Auth 역할 반영 흐름을 현재 코드 기준으로 통합.
  - `auth/docs`, `config-server/docs`, `gateway/docs/README.md`, `gateway/docs/local-run.md`, `discovery/docs/README.md`, `discovery/docs/local-run.md`를 추가.
  - 깨진 `discovery/docs/concept.md` 원문을 `discovery/docs/archive/concept_legacy_corrupt_2026-06-11.md`로 이동해 보존하고, 새 `concept.md`를 현재 Eureka 흐름 기준으로 작성.
  - `auth`, `gateway`, `discovery`, `config-server`, `master-data`, `governance` README에 IntelliJ 실행 순서와 PowerShell Gradle 명령을 보강.
  - `config-repo/README.md`, `docs/README.md`, `docs/local-development.md`, `docs/module-documentation-sequence.md`에 foundation/infra 실행 순서와 IntelliJ `.run` 설정을 반영.
  - Foundation/Infra용 IntelliJ 공유 Gradle Run Configuration `.run/Config Server bootRun.run.xml`, `.run/Discovery bootRun.run.xml`, `.run/Auth bootRun.run.xml`, `.run/Master Data bootRun.run.xml`, `.run/Governance bootRun.run.xml`, `.run/Gateway bootRun.run.xml`, `.run/Foundation Library Compile.run.xml`, `.run/Foundation Infra Tests.run.xml` 추가.
  - `InMemoryLoginAttemptAdapter`, `JwtAuthenticationFilter`, `MasterDataChangeRequestService`, `MasterApprovalService`에 운영 개선 필요 지점을 `@todo`로 명시. 총 5건.
- **검증**:
  - `.\gradlew :contracts:compileJava :shared-kernel:compileJava :master-data:test :governance:test :auth:test :gateway:test :discovery:test :config-server:assemble --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - `auth` 로그인 실패/잠금은 운영 다중 인스턴스에서 Redis/DB 기반 공유 어댑터가 필요하다.
  - `gateway`는 JWT 서명 검증 후 roleVersion을 전달하지만, 역할 변경 직후 기존 JWT 차단은 Auth token-version 검증 또는 캐시 정책과 연동해야 한다.
  - `master-data` 변경 요청 반영은 targetType별 실제 도메인 applier와 대량 예약 반영 chunk 처리가 필요하다.
  - `governance` 승인 흐름은 미지원 masterType을 조용히 승인하지 않는 fail-closed 정책이 필요하다.

### 📅 2026-06-11 (Codex 문서)
### [문서] Reconciliation/Reporting 문서 8차 통합 및 로컬 실행 가이드 정리
- **수정 범위**:
  - 기존 `reconciliation/docs/README.md` 인덱스를 `reconciliation/docs/archive/README_legacy_index_2026-06-11.md`로 이동해 보존.
  - `reconciliation/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 대사 단위, 대사 규칙, 실행 이력, 차이 배정/해소, 외부 단계 집계, 조정 전표 흐름을 현재 코드 기준으로 통합.
  - `reporting/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `local-run.md`를 현재 `reporting:core`, `reporting:api`, `reporting:batch` 하위 모듈 구조와 감독보고/주석 마트 흐름 기준으로 보강.
  - `reconciliation/README.md`, `reporting/README.md`, `docs/README.md`, `docs/module-documentation-sequence.md`에서 잘못된 standalone Docker 실행 안내를 현재 library 모듈 기준의 IntelliJ/Gradle 검증 방식으로 교체.
  - Reconciliation/Reporting용 IntelliJ 공유 Gradle Run Configuration `.run/Reconciliation Module Tests.run.xml`, `.run/Reporting Module Tests.run.xml` 추가.
  - `ReconciliationService`, `ReportingBatchAdapter`, `LocalRegulatoryFilingGatewayAdapter`에 운영 개선 필요 지점을 `@todo`로 명시. reconciliation 2건, reporting 2건.
- **검증**:
  - `.\gradlew :reconciliation:test :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - `reconciliation` 규칙 삭제는 현재 물리 삭제라 과거 대사 이력 감사 관점에서 논리 비활성화 전환이 필요하다.
  - `reconciliation` 자동 조정 전표 source document id는 시간 기반이라 장애 재시도 시 멱등 키 보강이 필요하다.
  - `reporting` Batch Adapter는 운영 대량 배치 전환 시 Spring Batch `Job`/`Step`/`JobParameter` 구조가 필요하다.
  - `reporting` 로컬 감독보고 게이트웨이의 랜덤 반려는 테스트 재현성을 위해 설정화가 필요하다.

### 📅 2026-06-11 (Codex 문서)
### [문서] Asset-Lease/Tax 문서 7차 통합 및 로컬 실행 가이드 정리
- **수정 범위**:
  - 기존 `asset-lease/docs/README.md`, `tax/docs/README.md` 인덱스를 각각 `docs/archive/README_legacy_index_2026-06-11.md`로 이동해 보존.
  - `asset-lease/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 고정자산 등록/상각/처분, 감가상각 Batch, IFRS 16 리스 최초 인식/월별 처리/재측정, Kafka 이벤트, 리스 지급결의 포트 흐름을 현재 코드 기준으로 통합.
  - `tax/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 AP 세금계산서 생성/조회/수정/논리 취소, 금액 정합성, 외부 조회 포트 흐름을 현재 코드 기준으로 통합.
  - `asset-lease/docs/api-spec.md`, `requirements.md`에 현재 API 필드, `X-User-ID`, 로컬 실행 전제, Batch/리스 계정 고도화 필요 지점을 보강.
  - `asset-lease/README.md`, `tax/README.md`, `docs/README.md`, `docs/module-documentation-sequence.md`에 새 문서와 IntelliJ/Gradle 실행 방식을 연결.
  - Asset-Lease/Tax용 IntelliJ 공유 Gradle Run Configuration `.run/Asset Lease API bootRun.run.xml`, `.run/Asset Lease Tests.run.xml`, `.run/Tax Module Tests.run.xml` 추가.
  - `LeaseContractRequest`의 사용하지 않는 master-data 엔티티 import를 제거.
  - `AssetDepreciationBatchConfig`, `LeaseEntryService`, `LeaseAccountingController`, `TaxInvoiceQueryAdapter`에 운영 개선 필요 지점을 `@todo`로 명시. asset-lease 4건, tax 1건.
- **검증**:
  - `.\gradlew :asset-lease:test :tax:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - `asset-lease` Batch는 현재 날짜 결정과 도메인 계산 일부가 Batch Config 안에 남아 있어 JobParameter와 `DepreciationPipeline` 중심으로 보강해야 한다.
  - 리스 회계 계정 코드는 현재 서비스 상수이므로 운영 회계 정책별 설정/포트 분리가 필요하다.
  - `tax`는 standalone `bootRun` 앱이 아니므로 실제 HTTP API 호출은 호스트 Spring Boot 애플리케이션 또는 별도 통합 실행 앱에서 검증해야 한다.
  - 취소된 세금계산서를 외부 조회 포트에서 반환할지 정책 확정이 필요하다.

### 📅 2026-06-10 (Codex 문서)
### [문서] Payable/Receivable 문서 6차 통합 및 library 모듈 실행 가이드 정리
- **수정 범위**:
  - 기존 `payable/docs/README.md`, `receivable/docs/README.md` 인덱스를 각각 `docs/archive/README_legacy_index_2026-06-10.md`로 이동해 보존.
  - `payable/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 매입 인보이스, 매입채무, 지급 런, 지급 실행, 선급금, 상계 흐름을 현재 코드 기준으로 통합.
  - `receivable/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 매출 인보이스, 매출채권, 수납, 자동/수동 매칭, 부분 매칭 흐름을 현재 코드 기준으로 통합.
  - `payable/README.md`, `receivable/README.md`, `docs/README.md`, `docs/module-documentation-sequence.md`에 새 문서 읽기 순서와 IntelliJ/Gradle 검증 방식을 연결.
  - Payable/Receivable용 IntelliJ 공유 Gradle Run Configuration `.run/Payable Module Tests.run.xml`, `.run/Receivable Module Tests.run.xml` 추가.
  - `PurchaseInvoiceId`의 깨진 한글 주석을 초보자용 업무 식별자 설명으로 복구.
  - `PurchaseController`, `PaymentController`에 인바운드 DTO/Bean Validation 분리 필요 지점을 `@todo`로 명시하고, `CollectionController`에 수납/매칭 인바운드 어댑터 역할 주석을 추가.
- **검증**:
  - `.\gradlew :payable:test :receivable:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - `payable`, `receivable`은 현재 standalone `bootRun` 앱이 아니므로 실제 HTTP API 호출은 호스트 Spring Boot 애플리케이션 또는 별도 통합 실행 앱에서 검증해야 한다.
  - Payable 컨트롤러는 일부 요청에서 raw `Map` 또는 JPA 엔티티 직접 바인딩을 사용하므로 DTO/Bean Validation 분리가 필요하다. 관련 `@todo`는 4건이다.

### 📅 2026-06-10 (Codex 문서)
### [문서] Loan 문서 5차 통합 및 EIR/Batch 실행 가이드 정리
- **수정 범위**:
  - 깨진 `loan/docs/README.md` 원문을 `loan/docs/archive/README_legacy_corrupt_2026-06-10.md`로 이동해 보존.
  - `loan/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 대출 생성, 실행, 이연 항목, EIR 스케줄, 재계산, 일일 이자 Batch 흐름을 현재 코드 기준으로 통합.
  - `loan/README.md`에 문서 읽기 순서, IntelliJ/Gradle 실행 예시, 필수 `account.loan.accounting.*` 설정을 보강.
  - Loan용 IntelliJ 공유 Gradle Run Configuration `.run/Loan API bootRun.run.xml`, `.run/Loan Batch Context.run.xml` 추가.
  - `DeferredItemType`, `DeferredItem`, `EIRAmortizationSchedule`, `RecalculationRun`, `LoanEvent`, `LoanDisbursal`, `LoanJdbcAdapter`의 깨진 한글 주석을 초보자용 업무 설명으로 복구.
  - `EIRCalculator`에 이연 수수료/비용 부호 정책 보강 필요 지점을 `@todo`로 명시.
- **검증**:
  - `.\gradlew :loan:core:test :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - EIR 계산은 현재 수수료/비용 부호 정책을 단순화하고 있어 운영 회계 정책 반영 전 보강이 필요하다.
  - 일일 이자 Batch 실제 전표 생성은 ACTIVE 대출, `loan_amortization_schedule_entries`, master-data 계정 seed가 준비된 환경에서 별도 확인해야 한다.

### 📅 2026-06-10 (Codex 문서)
### [문서] Closing 문서 4차 통합 및 결산 실행 가이드 정리
- **수정 범위**:
  - 깨진 `closing/docs/README.md` 원문을 `closing/docs/archive/README_legacy_corrupt_2026-06-10.md`로 이동해 보존.
  - `closing/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 결산 캘린더, 태스크, 게이트, 기간 잠금, 재오픈, FX/ECL 배치 흐름을 현재 코드 기준으로 통합.
  - `closing/README.md`에 문서 읽기 순서, IntelliJ/Gradle 실행 예시, ECL 충당 Job 선행 조건을 보강.
  - Closing용 IntelliJ 공유 Gradle Run Configuration `.run/Closing API bootRun.run.xml`, `.run/Closing Batch Context.run.xml` 추가.
  - `ClosingCalendar`, `ClosingTask`, `ClosingGate`, `ClosingPeriod`, `DailyClosingStatus`의 깨진 한글 주석을 초보자용 업무 설명으로 복구.
  - `FxValuationBatchConfig`, `FxValuationService`에 대량 외화 잔액 처리와 자산/부채 계정 차대변 판정 개선 필요 지점을 `@todo`로 명시.
- **검증**:
  - `.\gradlew :closing:core:test :closing:api:compileJava :closing:batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - FX 평가 전표는 현재 자산 계정 중심의 차대변 처리를 전제로 하며, 부채 계정은 계정 성격 기반 판정 보강이 필요하다.
  - FX 평가 Reader는 현재 집계 잔액 메모리 로딩 구조이며, 운영 대량 계정 환경에서는 Paging/Partition 전환 검토가 필요하다.

### 📅 2026-06-10 (Codex 문서)
### [문서] Journal Ledger 문서 3차 통합 및 계층 가이드 정리
- **수정 범위**:
  - 소스 트리 하위 legacy README 3개를 `journal-ledger/docs/archive`로 이동해 보존.
  - `journal-ledger/docs/layer-guide.md`를 추가해 application/domain/adapter/infrastructure 책임과 출력 포트 경계를 현재 코드 기준으로 통합.
  - `journal-ledger/README.md`, `docs/README.md`, `docs/beginner-guide.md`, `docs/process-flow.md`에 로컬 실행, JDBC bulk 모드, 계층 가이드 링크를 보강.
  - Journal Ledger용 IntelliJ 공유 Gradle Run Configuration `.run/Journal Ledger API bootRun.run.xml`, `.run/Journal Ledger API JDBC Bulk.run.xml` 추가.
  - `docs/module-documentation-sequence.md`에 Journal Ledger 완료와 다음 `closing` 순서를 기록.
- **검증**:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - JDBC bulk 운영 모드는 실제 PostgreSQL/MySQL 환경에서 배치 크기, 인덱스, 락 대기 부하 검증이 필요하다.

### 📅 2026-06-10 (Codex 문서)
### [문서] ECL 문서 2차 통합 및 IntelliJ 실행 설정
- **수정 범위**:
  - `ecl/README.md` 실행 예시를 Windows PowerShell/IntelliJ 기준으로 보강.
  - ECL 문서 인덱스, 입문, 배치 실행 가이드에 `account-mart` snapshot 선행 조건과 demo profile의 한계를 명확히 기록.
  - `ecl-api`, `ecl-batch`, `ecl-core` README에 로컬 실행/검증 명령을 추가.
  - ECL용 IntelliJ 공유 Gradle Run Configuration `.run/ECL API bootRun.run.xml`, `.run/ECL Batch Context.run.xml` 추가.
  - `docs/module-documentation-sequence.md`에 ECL 완료와 다음 `journal-ledger` 순서를 기록.
- **검증**:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-api:compileJava :ecl:ecl-batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - 실제 `allowanceEclJob` 산출 실행은 `allowance_exposure_snapshots`, 모델 마스터, 계정 매핑 seed가 준비된 환경에서 별도 확인해야 한다.

### 📅 2026-06-10 (Codex 문서)
### [문서] 공통 로컬 실행 가이드 및 account-mart 문서 1차 통합
- **수정 범위**:
  - 깨진 레거시 `docs/beginner_guide.md` 원문을 `docs/archive/beginner_guide_legacy_corrupt_2026-06-10.md`로 이동해 보존.
  - 새 `docs/beginner_guide.md`와 `docs/local-development.md`를 작성해 IntelliJ, JDK 17, Gradle JVM, 모듈별 Spring Boot 실행 기준을 정리.
  - 루트 `README.md`, `docs/README.md`에 로컬 실행 문서와 모듈별 문서 통합 진행표를 연결.
  - `account-mart/docs/README.md`를 추가하고 기존 account-mart 문서들의 읽기 순서, Batch Job 목록, 재실행 체크, IntelliJ/Gradle 실행 예시를 통합.
  - account-mart용 IntelliJ 공유 Gradle Run Configuration `.run/Account Mart API bootRun.run.xml`, `.run/Account Mart Batch Demo.run.xml` 추가.
- **검증**:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - 문서 통합은 모듈별 순차 작업으로 진행 중이며, 다음 대상은 `journal-ledger` 문서다.

### 📅 2026-06-10 (Codex 구현)
### [고도화] ECL 계산 정책·Journal 미결/자동분개 경계 및 상세 문서 통합
- **수정 범위**:
  - ECL EAD/CRM 배열 결과를 `EadCalculationResult` 값 객체로 교체하고 모델 비율 검증 및 이름 있는 기본 CCF 정책을 적용.
  - ECL 모델 파라미터 영속성을 기술 독립 출력 포트와 JPA 어댑터로 분리.
  - ECL 등급·상품·LGD·담보배분·거시시나리오·전이행렬 포트의 Spring Data/캐시 기술을 infrastructure 어댑터로 이동.
  - Journal 미결 영속성 출력 포트, 거래처별 DB 조건 조회, 자동분개 규칙 조회 포트, `JournalSide` 규칙 타입 적용.
  - Journal 전기·잔액 서비스의 직접 Repository 의존 제거, 전표/엔트리/잔액 출력 포트 적용, GL/SL 조건 조회 DB 필터 적용.
  - Journal 운영 대용량 저장 경로로 설정 기반 JDBC bulk 엔트리 insert와 GL/SL 잔액 upsert 어댑터 추가.
  - 잔액 재집계 저장 흐름을 일별 집계 후 bulk 포트 저장으로 변경하고 `YearMonth` 기간 저장 표현을 `yyyy-MM`으로 고정.
  - 구현 완료 후 남아 있던 오래된 stub/향후 개선/인메모리 필터 주석을 실제 업무·데이터 흐름 기준으로 최신화.
- **문서 갱신**:
  - ECL 문서 인덱스와 입문·업무 흐름·데이터 모델 상세화.
  - Journal Ledger 입문·프로세스·스키마 문서 신규 작성 및 README 연결.
  - `docs/todo_remediation_plan.md`에 완료 T47-T53 기록.
- **검증**:
  - ECL core/API/batch 통합 테스트·컴파일 성공.
  - Journal Ledger core/API 통합 테스트 성공.
  - Loan core의 journal 전기 조립 통합 테스트 성공.
  - Journal JDBC bulk insert/upsert H2 집중 테스트 성공.
  - 대상 코드 오래된 표현 검색, 문서 링크 검사, 변경 범위 diff check 성공.
- **남은 리스크**:
  - Journal JDBC bulk 모드는 H2 SQL 동작까지 검증했으며, 운영 DB 기준 배치 크기·인덱스·락 대기·동시 재집계 부하 검증이 남아 있다.

### 📅 2026-06-09 (Codex 구현)
### [고도화] 잔여 TODO 0건 및 DDD/헥사고날 경계 통합
- **수정 범위**:
  - Auth 로그인 성공/실패 감사와 설정 기반 임시 잠금 출력 포트/어댑터 추가.
  - Payable 외부 지급 실행 포트, 멱등 키, 실패/재시도 상태, 정확한 payable ID 연결 및 contracts 기반 기준정보 경계 적용.
  - Receivable 참조번호 우선 자동 매칭 정책, 중복 후보 실패 폐쇄, 수금 배분/잔액 이력 추가.
  - Journal/Unsettled API DTO, 실제 actor 헤더, 미결 인바운드 포트, 반제 참조번호 멱등/감사 정보 추가.
  - Loan 출력 포트, Reconciliation 표준 Aggregate, allowance JPA 소유권, Closing 조정 전표 통제, Tax 논리 취소, Asset actor 전달 변경 검증.
- **문서 갱신**:
  - 관련 모듈 README, `docs/todo_remediation_plan.md`, `CODEX_WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md`
- **검증**:
  - 변경 모듈 집중 테스트 및 컴파일 명령 성공.
  - `journal-ledger:api:test` 통합 검증 중 발견한 Flyway 버전 충돌과 구매 actor 누락을 수정한 뒤 재실행 성공.
  - 최종 변경 모듈 통합 테스트/컴파일 68개 task 성공.
  - Java 소스 `rg -ni "@todo" --glob "*.java" .` 결과 0건.
  - 변경 범위 `git diff --check` 성공(CRLF 안내만 출력).
- **남은 리스크**:
  - Auth 임시 잠금과 Payable 로컬 지급 실행은 운영 환경에서 공유/실제 외부 어댑터로 교체해야 한다.
  - 신규 지급/수금 영속 필드는 운영 통합 스키마 마이그레이션 정책 확인이 필요하다.

### 📅 2026-05-27 (Codex 구현)
### [고도화] Closing ECL 충당 배치의 ECL 산출 결과 포트 연동
- **수정 범위**:
  - Closing Core: `EclAllowanceResultPort`, `EclAllowanceSummary`를 추가해 확정된 IFRS 9 ECL 산출 결과를 외부 포트로 조회할 수 있게 함.
  - Closing Batch: `JdbcEclAllowanceResultAdapter`를 추가해 `allowance_summary` 테이블의 기준일별 summary를 읽는 기본 어댑터를 구현.
  - Closing Batch: `EclProvisionService`에서 대출채권 잔액에 1%를 곱하던 고정 산식을 제거하고, ECL summary의 목표 충당금과 기존 GL 대손충당금 잔액 차이만 보충/환입 전표로 처리하도록 변경.
  - Closing Batch: 통화와 계정은 summary/설정 기반으로 resolve하고, 환입 시 별도 `reversalIncomeAccountCode`를 사용하도록 변경.
- **문서 갱신**:
  - `docs/allowance-ecl-refocus-plan.md`, `closing/README.md`, `closing/docs/README.md`
- **검증**:
  - `.\gradlew :closing:batch:test --console=plain` 성공.
  - 변경 범위 `git diff --check -- closing docs\allowance-ecl-refocus-plan.md docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - `allowance_summary` 테이블을 실제로 생성/적재하는 `account-mart`/`ecl` 전용 마이그레이션과 배치 job은 후속 구현이 필요하다.
  - 결산 자동 승인/전기 통제는 아직 기존 흐름을 유지하며, 별도 결산 승인 정책 포트로 분리하는 보강이 남아 있다.

### 📅 2026-05-26 (Codex 설계)
### [설계] account-mart / ecl 대손충당금 산출 전용화 계획
- **검토 범위**:
  - 신규 유입된 `account-mart`, `ecl` 모듈의 README/docs, 주요 배치/산출 클래스, 현재 `closing` ECL 충당 배치 구조를 확인.
  - `account-mart`의 ODS→CDM 변환 역할과 `ecl`의 Stage/PD/LGD/EAD/ECL/RWA 혼합 산출 흐름을 대손충당금 관점에서 재분류.
- **설계 결과**:
  - `account-mart`는 ECL 입력 스냅샷/원천-GL 대사/DQ 전용 마트로 축소하는 방향을 제안.
  - `ecl`은 IFRS 9 Stage, Lifetime PD, EAD/LGD, 미래전망 가중평균 ECL 산출 전용 엔진으로 축소하고 RWA/감독보고/집중도 기능은 기본 실행 경로에서 제외하는 방향을 제안.
  - `closing`은 현재 고정 1% 산식 대신 ECL 산출 결과 summary를 포트로 조회해 보충/환입 전표만 생성하도록 역할을 조정하는 설계를 정리.
- **문서 갱신**:
  - `docs/allowance-ecl-refocus-plan.md`
- **검증**:
  - 문서 설계 작업이라 빌드/테스트는 실행하지 않음.
- **남은 리스크**:
  - 신규 모듈은 아직 루트 `settings.gradle`에 포함되지 않았고, 기존 `project(':common')`, `com.ho.account.shared.finance`, `credit-risk-service`, `risk-data-mart-service` 좌표를 현재 저장소 구조에 맞게 이관해야 한다.
  - `closing`의 기존 ECL 배치는 하드코딩 계정/KRW/1% 산식이 남아 있어 후속 구현에서 ECL 결과 포트 기반으로 교체해야 한다.

### 📅 2026-05-21 (Gemini YOLO 모드 - 12차)
### [프론트엔드]
- **Frontend (Next.js) 재무운영(AR, AP) 관리 화면 API 연동**:
  - `frontend/src/services/receivableService.ts` 및 `payableService.ts` 신규 생성. 백엔드의 `/api/receivable/invoices` 및 `/api/payable/invoices`와 통신하도록 설계.
  - `frontend/src/app/finance/receivable/page.tsx` 및 `payable/page.tsx` 리팩토링: 하드코딩된 그리드 데이터를 제거하고 API를 호출해 실제 매출채권 및 매입채무 데이터를 동적으로 렌더링.
  - 총액 잔액 합계(`Total Receivable Balance` 등)를 API 응답 기반으로 실시간 계산하여 출력하도록 보강.
  - 빌드 검증을 모두 통과하고 상태판(`docs/development-status.md`) 갱신 및 Git 연동 완료.

### 📅 2026-05-21 (Gemini YOLO 모드 - 11차)
### [프론트엔드]
- **Frontend (Next.js) 재무운영(고정자산, 리스 회계) 화면 연동 검증**:
  - `frontend/src/app/finance/assets/page.tsx` 및 `frontend/src/app/finance/lease/page.tsx` 코드를 분석하여, 이미 내부에 하드코딩된 Mock 데이터가 없고 `assetService.getAssets()`, `leaseService.getLeases()` 등을 통한 백엔드 API (`/api/fixed-assets`, `/api/ifrs16/leases`) 리얼 데이터 연동이 완벽하게 구현되어 있음을 코드 레벨에서 확인했습니다.
  - 감가상각 실행(`runDepreciation`) 및 리스 월별 회계 처리(`processMonthly`) 로직의 API 바인딩 역시 정상적으로 적용되어 있음을 교차 검증 완료했습니다.
  - `npm run build`를 통한 런타임/타입 무결성 통과를 재확인하고, `docs/development-status.md`에 `✅ [X]`로 상태가 정확히 기록되어 있음을 확정지었습니다.

### 📅 2026-05-21 (Gemini YOLO 모드 - 9차)
### [프론트엔드]
- **Frontend (Next.js) 재무제표(BS/IS) 화면 API 연동**:
  - `frontend/src/services/reportingService.ts`에서 Mock 데이터(`sampleStatement`)를 제거하고 에러 발생 시 UI로 예외를 던지도록 엄격한 API 연동(Strict Integration) 체계 도입.
  - 백엔드의 `/api/v1/reporting/generate` 엔드포인트와 풀스택 연동.

### 📅 2026-05-21 (Gemini YOLO 모드 - 10차)
### [프론트엔드]
- **Frontend (Next.js) 기준정보(마스터) 관리 화면 API 연동**:
  - `frontend/src/services/masterDataService.ts` 생성: 백엔드의 `/api/basic/account-subjects` 및 `/api/basic/business-partners` 연동.
  - `frontend/src/app/master/account/page.tsx` 리팩토링: 하드코딩된 트리 데이터를 제거하고, `masterDataService`를 통해 받아온 평면 배열을 재귀적으로 `buildAccountTree`하여 렌더링하도록 수정.
  - `frontend/src/app/master/partner/page.tsx` 리팩토링: 하드코딩된 거래처 그리드를 제거하고, 실제 DB의 데이터를 받아 렌더링.
  - 빌드(typing/linting) 통과 확인 및 Git 동기화.

### 📅 2026-05-21 (Gemini YOLO 모드 - 7차)
### [프론트엔드]
- **Frontend (Next.js) UI 연동 및 개발 진입**:
  - `frontend/src/services/closingService.ts` 신규 생성. 결산 태스크 조회 및 '외화 평가(FX)', 'IFRS9 기대신용손실(ECL)' 배치 재실행(Retry Batch) API 연동 로직 추가.
  - `frontend/src/app/closing/page.tsx` 결산 관리 화면 수정. 하드코딩된 Mock 데이터를 `closingService`를 통해 API로 연동하도록 변경 (`useEffect` 사용).
  - 결산 관리 화면 내 "RETRY BATCH" 버튼 클릭 시 실제로 `runValuationBatch` 및 `runProvisionBatch` API를 호출하여 배치를 재가동하도록 `handleRetryBatch` 로직 연동 완료.
  - `frontend/docs/development-status.md`에 결산 관리(Closing) 화면 API 연동 상태 업데이트(`✅ [X]`).

### 📅 2026-05-21 (Gemini YOLO 모드 - 6차)
### [백엔드]
- **결산(Closing) 모듈 대손충당금(ECL) 및 결산조정 배치 구현**:
  - `closing/batch` 하위에 `EclProvisionService` 및 `EclProvisionBatchConfig`를 신규 구현.
  - 원장(GL)의 대출채권 잔액을 기반으로 목표 대손충당금(ECL)을 산출하고, 기존 충당금 잔액을 차감하는 '보충법' 방식의 배치 처리 로직 완성.
  - 대손상각비(비용)와 대손충당금(부채성 자산차감) 자동 분개 생성(Journal Entry) 연동 및 테스트 컴파일 검증 성공.
  - `docs/todo.md`의 `12.4 충당/손상(ECL 연계)` 및 `12.5 결산조정/재분류` 완료(o) 마킹 및 Git 동기화 완료.

### 📅 2026-05-21 (Gemini YOLO 모드 - 5차)
### [백엔드 & 모델러]
- **대출 모듈 11.4 & 11.5 기 구현 확인**:
  - `LoanService.recalculateLoan` 및 `EIRCalculator` 내부에 중도상환, 조건변경 시의 Newton-Raphson 기반 EIR 재계산과 상각 전표, 잔액 조정 전표 발행 로직이 이미 완벽히 구현되어 있음을 코드 및 테스트(`LoanServiceTest`)로 확인. `todo.md`에 완료(o) 마킹.
- **결산(Closing) 모듈 외화평가(FX Valuation) 배치 구현**:
  - `closing/batch` 모듈을 신규 구성 (Spring Batch, JPA 설정 포함).
  - `GlAccountBalanceRepository`에 특정 일자 기준 외화(비 KRW) 잔액을 조회하는 `findLatestForeignCurrencyBalances` 네이티브 Query 추가.
  - `FxValuationService` 생성: 기말 환율을 조회하여 장부 원화 금액과 평가 원화 금액의 차액을 계산하고, "외화환산이익/손실" 회계 전표(Journal Entry)를 자동 발행하는 핵심 로직 구현 및 주석(`🐣 초보자를 위한 설명`) 작성.
  - `FxValuationBatchConfig` 생성: 기말 평가 대상을 조회하여 평가 서비스를 호출하는 Spring Batch (Reader/Processor/Writer) 구성 완료.
  - `closing:batch:compileJava` 성공 확인 및 백그라운드 데몬 정리 완료.

### 📅 2026-05-21 (Gemini YOLO 모드 - 4차)
### [백엔드]
- **대출(Loan) 모듈 일일 EIR 상각 배치 구현**:
  - `loan/batch` 하위에 `LoanInterestAccrualBatchConfig.java` 신규 생성.
  - Spring Batch(Reader/Processor/Writer)를 활용해 매일 자정 ACTIVE 대출의 상각 전표(Journal) 생성 로직 구현 및 주석 추가.
  - `InterestAccrualService`의 개별 상각 처리 메서드를 Public 으로 노출.
  - `loan:batch:compileJava` 성공 확인 및 백그라운드 Gradle 데몬 정리 완료.

### 📅 초기화
- 전체 작업 이력 정리 및 초기화 완료.

### 📅 2026-05-21 (Codex 구현)
### [보안 보강] Governance -> Auth 내부 역할 반영 API 토큰 보호
- **수정 범위**:
  - Auth: 내부 역할 할당 API `POST /api/auth/internal/users/{username}/role-assignments`에 `X-Internal-Auth-Token` 검증을 추가.
  - Auth: `auth.internal-api.token` 설정과 `AUTH_INTERNAL_API_TOKEN` 환경변수 기본값을 추가.
  - Governance: Auth RestClient 역할 반영 호출에 `X-Internal-Auth-Token` 헤더를 포함하도록 보강.
  - Governance: `governance.integrations.auth.internal-token` 설정과 `GOVERNANCE_AUTH_INTERNAL_TOKEN` 환경변수 기본값을 추가.
  - 테스트: Auth 컨트롤러 내부 토큰 검증 테스트와 Governance RestClient 헤더 전송/토큰 설정 검증 테스트를 추가.
- **문서 갱신**:
  - `auth/README.md`, `governance/README.md`, `governance/docs/README.md`
- **검증**:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
- **남은 리스크**:
  - 운영/스테이징 배포 시 `AUTH_INTERNAL_API_TOKEN`과 `GOVERNANCE_AUTH_INTERNAL_TOKEN`을 같은 값으로 주입해야 한다.
  - 토큰 회전, mTLS, 네트워크 ACL 같은 운영 수준의 서비스 간 인증 정책은 별도 설계가 필요하다.

### 📅 2026-05-21 (Codex 구현)
### [고도화] Reporting 감독보고 제출본 버전/정정/검증
- **수정 범위**:
  - Reporting Core: `SubmitRegulatoryReportUseCase`와 `RegulatoryReportSubmissionService`를 추가해 확정 재무제표 스냅샷을 감독보고 제출본으로 등록.
  - Reporting Domain: `RegulatoryReportSubmission` 모델을 추가하고 제출 전 필수 라인/금액, 중복 라인, BS 총계, IS 순액 검증을 수행.
  - Reporting Persistence: JPA/인메모리 제출본 저장 어댑터와 `RPT_REGULATORY_SUBMISSION` Flyway 마이그레이션 추가.
  - Reporting API: `POST /api/v1/reporting/submissions/regulatory` 엔드포인트 추가. 2차 제출부터 정정 사유를 필수로 검증.
  - `docs/todo.md`의 14.1, 14.4, 14.5 완료 표시 보강.
- **문서 갱신**:
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`
- **검증**:
  - `.\gradlew :reporting:core:test :reporting:api:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
- **남은 리스크**:
  - 실제 감독기관 제출 전문/파일 전송, 제출 결과 수신, 반려 후 재제출 상태 모델은 아직 별도 구현이 필요하다.
  - 주석 마트(만기/금리/통화/리스크)와 CF 라인 매핑은 후속 작업 범위로 남아 있다.

### 📅 2026-05-21 (Codex 구현)
### [고도화] Reporting 주석 마트 생성/조회
- **수정 범위**:
  - Reporting Core: `DisclosureNoteMartUseCase`와 `DisclosureNoteMartService`를 추가해 확정 재무제표 스냅샷의 주석 라인을 마트로 전개.
  - Reporting Domain: `DisclosureNoteMart`, `DisclosureNoteMartEntry`를 추가하고 주석 번호/라인 코드 기반으로 만기, 금리, 통화, 리스크 범주를 분류.
  - Reporting Persistence: JPA/인메모리 주석 마트 어댑터와 `RPT_DISCLOSURE_NOTE_MART` Flyway 마이그레이션 추가.
  - Reporting API: `POST /api/v1/reporting/disclosure-notes/generate`, `GET /api/v1/reporting/disclosure-notes` 엔드포인트 추가.
  - `docs/todo.md`의 14.2 완료 표시.
- **문서 갱신**:
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`, `reporting/docs/beginner-guide.md`
- **검증**:
  - 첫 `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 실행은 Gradle daemon stop으로 중단.
  - 동일 명령 재실행 성공.
- **남은 리스크**:
  - 현재 분류는 note 번호와 라인 코드/라벨 기반 기본 규칙이다. 운영 수준에서는 별도 SCD2 공시 분류 매핑 테이블로 정책화가 필요하다.
  - 공시 주석의 상세 원천 drill-through는 아직 보고 라인 코드 수준이며 전표/원천 이벤트까지의 상세 링크는 후속 작업이다.

### 📅 2026-05-21 (Codex 구현)
### [고도화] Reporting 감독보고 매핑/제출
- **수정 범위**:
  - Reporting Core: `SubmitRegulatoryFilingUseCase`와 `RegulatoryFilingService`를 추가해 READY 제출본과 주석 마트를 감독보고 제출 패키지로 변환.
  - Reporting Domain: `RegulatoryReportMapping`, `RegulatoryFiling`, `RegulatoryFilingLine`, `RegulatoryFilingPackage`, `RegulatoryFilingReceipt` 모델 추가.
  - Reporting Persistence: 감독보고 SCD2 매핑 로더, 제출 이력 JPA/인메모리 어댑터와 `RPT_REGULATORY_REPORT_MAPPING`, `RPT_REGULATORY_FILING` Flyway 마이그레이션 추가.
  - Reporting Infrastructure: `LocalRegulatoryFilingGatewayAdapter`를 추가해 로컬 접수 영수증을 생성.
  - Reporting API: `POST /api/v1/reporting/regulatory-filings/submit`, `GET /api/v1/reporting/regulatory-filings/latest` 엔드포인트 추가.
  - `docs/todo.md`의 14.3 완료 표시.
- **문서 갱신**:
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`, `reporting/docs/beginner-guide.md`
- **검증**:
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
- **남은 리스크**:
  - 현재 제출 게이트웨이는 로컬 접수 영수증 생성 어댑터이며, 실제 감독기관 전송 프로토콜/인증/반려 응답 처리는 후속 구현이 필요하다.
  - 매핑 seed는 기본 BS/IS 일부 필드만 포함하며, 운영 서식 전체 필드와 버전별 검증 규칙은 추가 보강이 필요하다.

### 📅 2026-05-20 (Codex 구현)
### [수정] Governance 승인 기반 Auth 사용자 역할 반영 연결
- **수정 범위**:
  - Auth: `AuthUserRoleAssignmentUseCase`와 내부 API `POST /api/auth/internal/users/{username}/role-assignments`를 추가해 승인된 역할 목록으로 기존 역할 할당을 교체하고 `roleVersion`을 증가시키도록 구현.
  - Auth: `JpaAuthUserRoleAssignmentAdapter`와 인메모리 대응 어댑터를 추가해 JPA/메모리 모드 모두 역할 교체 흐름을 지원.
  - Governance: `AUTH_USER_ROLE` 승인 apply 어댑터를 추가해 승인 payload의 `username`, `role/roleCode/roles`, `dataScope`를 Auth 내부 API 호출로 변환.
  - Governance: `GOVERNANCE_AUTH_BASE_URL` 기반 Auth RestClient 연동 설정을 추가.
  - Frontend: `/admin/users` 역할 변경 승인 요청 payload에 Auth `username`을 포함하고 `masterKey`도 사용자 이메일 기반으로 변경.
- **문서 갱신**:
  - `auth/README.md`, `governance/README.md`, `governance/docs/README.md`, `frontend/docs/screen-inventory.md`
- **검증**:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
  - `npm run build` (workdir: `frontend`) 성공. 기존 closing 화면 unused variable warning은 남음.
  - `git diff --check -- auth governance frontend docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 실제 분산 환경에서 Governance -> Auth 네트워크 경로와 내부 API 인증 정책은 별도 smoke/E2E 검증이 필요.
  - 전체 `git diff --check`는 이번 범위 밖의 기존 미커밋 변경(journal-ledger/master-data/payable/receivable 등 trailing whitespace) 때문에 실패했다.

### 📅 2026-05-20 (Gemini YOLO 모드 - 3차)
### [기획/팀장 & 백엔드]
- **전체 모듈 순차 점검 및 주석 고도화 (DDD, 헥사고날, 초보자 가이드)**:
  - **04. 마스터(Master Data)**: \AccountSubject\(계정과목), \AccountSubjectService\ 에 SCD2 원칙 및 초보자 설명 추가.
  - **05. 전표/룰 엔진(Journal Ledger)**: \JournalRule\(분개 규칙), \JournalRuleEngine\ 에 자동 분개 과정 설명 추가.
  - **06. 원장(GL)**: \GlAccountBalance\ 에 실시간 잔액 계산 목적 설명 추가.
  - **07. P2P/AP (Payable)**: \PurchaseInvoice\, \Payable\ 에 외상매입금 생성 및 결제 흐름 설명 추가.
  - **08. O2C/AR (Receivable)**: \SalesInvoice\, \Receivable\ 에 매출채권 및 연령(Aging) 분석 설명 추가.
  - **09. 고정자산(FA)**: \FixedAsset\, \FixedAssetEntryService\ 에 감가상각 및 처분 흐름 설명 추가.
  - **10. 리스(Lease)**: \LeaseContract\, \LeaseEntryService\ 에 IFRS16 기반 사용권자산/부채 상각 설명 추가.
  - **11. 대출(Loan)**: \Loan\, \LoanService\ 에 상환 스케줄 생성 및 유효이자율(EIR) 역할 설명 추가.
  - **12. 결산(Closing)**: \ClosingPeriod\, \ClosingService\ 에 마감잠금 및 재오픈 승인 프로세스 설명 추가.
  - **13. 대사(Reconciliation)**: \ReconciliationRun\, \ReconciliationService\ 에 이기종 데이터 대조 및 차이 조정 설명 추가.
- **상태 업데이트**: 전 모듈에 대한 업무 주석 및 DDD 아키텍처 코멘트 적용 완료.

### 📅 2026-05-22 (Codex 검수)
### [리뷰] DDD/헥사고날 업무 흐름 및 정합성 @todo 점검
- **검수 범위**:
  - GL/SL/Journal Ledger, P2P/AP, O2C/AR, Reconciliation, Closing, Reporting 제출 게이트웨이 흐름을 우선 점검.
- **코드 기준으로 확인/추가한 주요 @todo**:
  - 전표 상세 조회의 `APPROVED`/`POSTED` 상태 기준 혼재 및 대사 집계의 전기 상태 필터 누락.
  - 구매/지급/매출/수납 서비스의 하드코딩 계정코드, `SYSTEM` 감사자, Mock 지급 실행, open-item 매칭 정합성 위험.
  - 대사 모듈의 모델 이원화, JSON 기반 미검증 정책, source snapshot skeleton, 대량 루프 매칭 성능, 중복 매칭 위험.
  - 결산 FX/ECL 배치의 KRW/계정/ECL rate 하드코딩, 가상 장부환율, timestamp 기반 slipNo, 자동 승인/전기 통제 미흡.
  - Reporting 감독보고 게이트웨이의 로컬 영수증 생성 어댑터를 실제 프로토콜/인증/반려 callback으로 대체해야 하는 위험.
- **검수 결과**:
  - 현재 빌드 완료 여부와 별개로 운영 정합성 기준에서는 후속 구현이 필요한 지점이 남아 있다.
  - 이번 작업은 동작 변경 없이 검수 주석과 워크로그 기록 중심으로 수행했다.
- **검증**:
  - `rg -n "@todo" journal-ledger payable receivable reconciliation closing reporting`로 주석 위치 확인.
  - `git diff --check` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 주석으로 표시한 항목은 실제 구현 전까지 재무제표/대사/결산 자동화의 운영 신뢰성 리스크로 남는다.

### 📅 2026-05-22 (Codex 구현)
### [고도화] 결산 배치 재실행 정합성 및 대사 매칭 정합성 보강
- **수정 범위**:
  - Closing Batch: `ClosingSlipNoFactory`를 추가해 FX/ECL 자동 전표번호를 기준일, 배치 ID, 계정 식별자 기반 20자 고정 형식으로 생성.
  - Closing Batch: 배치 ID 파라미터가 없을 때 현재시각 대신 기준일(`yyyyMMdd`)을 기본값으로 사용하도록 변경.
  - Closing Batch: 테스트 의존성을 `org.springframework.batch:spring-batch-test`로 수정하고 전표번호 결정성/길이 테스트 추가.
  - Reconciliation: 자동 매칭 엔진이 한 번 매칭한 전표 라인을 같은 실행에서 재사용하지 않도록 변경.
  - Reconciliation: 은행 입금/출금과 전표 DEBIT/CREDIT 방향을 부호로 반영해 반대방향 금액 매칭을 방지.
- **문서 갱신**:
  - `closing/README.md`, `reconciliation/docs/README.md`
- **검증**:
  - `.\gradlew :closing:batch:test --console=plain` 성공.
  - `.\gradlew :reconciliation:test --console=plain` 성공.
- **남은 리스크**:
  - 대사 매칭은 여전히 중첩 루프 기반이므로 1억 건 이상 처리에는 인덱싱/DB 집계 기반 후보 추출이 필요하다.
  - FX/ECL의 통화, ECL rate, 자동 승인/전기 정책은 별도 후속 구현이 필요하다.

### 📅 2026-05-22 (Codex 구현)
### [고도화] Reconciliation 자동매칭 후보 인덱싱
- **수정 범위**:
  - `AutomatedMatchingEngine`이 전표 라인을 부호 반영 금액 기준 `TreeMap` 인덱스로 구성하도록 변경.
  - 은행 거래별로 금액 허용오차 범위에 들어오는 전표 후보만 평가해 전체 전표 라인 중첩 스캔을 제거.
  - 후보 내부는 기존 전표 라인 입력 순서를 유지하도록 정렬해 기존 우선순위 계약을 보존.
  - 허용오차 내 입력 순서 보존 테스트를 추가.
- **문서 갱신**:
  - `reconciliation/docs/README.md`
- **검증**:
  - `.\gradlew :reconciliation:test --console=plain` 성공.
  - 변경 범위 `git diff --check` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 전체 `git diff --check`는 이번 범위 밖 프론트엔드 파일의 trailing whitespace로 실패한다.
  - 인메모리 후보 인덱싱은 단일 실행 내 매칭 비용을 줄이지만, 1억 건 이상에서는 DB/배치 파티셔닝 기반 후보 조회와 청크 단위 상태 저장이 추가로 필요하다.

### 📅 2026-05-27 (Codex 구현)
### [고도화] ECL 대손충당금 summary 생성 경로 추가
- **수정 범위**:
  - ECL Core: `AllowanceSummaryService`, `AllowanceSummaryBuildPort`, `AllowanceSummaryBuildResult`를 추가해 기준일 ECL 결과를 회계 summary로 재생성하는 유즈케이스를 분리.
  - ECL Core Adapter: `JdbcAllowanceSummaryPersistenceAdapter`를 추가해 완료된 `cr_risk_results`를 `allowance_account_mappings`와 조인하고 `allowance_summary`를 SQL bulk 집계로 생성.
  - ECL Batch: `AllowanceSummaryTasklet`과 `allowanceSummaryStep`, `standaloneAllowanceSummaryJob`을 추가해 ECL/RWA 완료 후 closing 입력 summary를 생성.
  - ECL API Migration: `V3__add_allowance_summary.sql`로 `allowance_account_mappings`, `allowance_summary` 테이블 추가.
  - ECL Batch Demo: H2 통합 스키마에 summary 테이블과 샘플 계정 매핑을 추가하고 `cr_accounts.biz_unit_cd` 컬럼을 보강.
  - 문서: `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `ecl/docs/CREDIT_DATA_MODEL_SPEC.md`, `docs/allowance-ecl-refocus-plan.md` 갱신.
- **정합성 포인트**:
  - 회계 계정 매핑 누락 시 기존 summary를 삭제하지 않고 실패하도록 설계.
  - 같은 기준일 이전 run summary가 `closing`에 중복 조회되지 않도록 mapping 검증 통과 후 기준일 summary를 교체.
  - 대량 처리는 application/batch 루프가 아니라 JDBC adapter의 bulk `INSERT ... SELECT`로 수행.
- **검증**:
  - `.\gradlew :closing:batch:test --console=plain` 성공.
  - `.\gradlew projects --console=plain` 성공. 현재 루트 프로젝트 목록에 `ecl`, `account-mart`는 포함되지 않음을 확인.
  - 변경 대상 파일 diff check 성공(CRLF 경고만 출력). 전체 범위 diff check는 이번 범위 밖 `closing/batch/.../FxValuationService.java` 기존 trailing whitespace로 실패.
  - 신규 ECL 파일 및 H2 스키마 trailing whitespace 점검 성공.
- **남은 리스크**:
  - `ecl`/`account-mart`는 아직 루트 Gradle에 편입되지 않아 ECL 신규 단위 테스트를 Gradle로 실행하지 못했다.
  - 기존 `com.ho.account.shared.finance`/`project(':common')` 좌표를 현 저장소의 `shared-kernel`/`contracts` 구조로 이관해야 통합 빌드 가능하다.
  - 대손충당금 전용 운영 경로에서는 RWA/집중도 분석을 제외한 별도 `allowanceEclJob` 분리가 필요하다.

### 📅 2026-05-27 (Codex 구현)
### [통합] account-mart/ecl 루트 Gradle 편입 및 batch wiring 복구
- **수정 범위**:
  - 루트 `settings.gradle`에 `risk-common`, `account-mart:mart-*`, `ecl:ecl-*` 모듈을 포함.
  - `risk-common` 호환 모듈을 추가해 기존 수입 모듈의 `com.ho.account.shared.finance` 엔티티/enum/event/API 응답/예외/락 타입 참조를 복구.
  - `ecl`/`account-mart`의 stale Gradle 의존성, Kafka 의존성 누락, 예전 `domain.*.repository/entity` import를 현재 포트/도메인 패키지로 정리.
  - `account-mart` batch/API 컴파일을 위해 KAP 등급, 조기경보, 계좌금리, 수익률곡선, 대사이력 포트 adapter를 보강.
  - `IntegratedPositionProcessor`가 외화 포지션 `marketValue`를 환율 포트로 KRW 환산하도록 보강.
  - ODS/GL 대사는 계좌 잔액을 상품 GL 계정코드로 집계하고 MATCH/MISMATCH 이력을 모두 남기도록 수정.
  - `mart-batch` demo/test에서 Kafka 없이 검증할 수 있도록 `mart.batch.cdm-event.enabled=false` 설정을 추가.
- **검증**:
  - `.\gradlew projects --console=plain` 성공. 루트 프로젝트 목록에 `risk-common`, `account-mart`, `ecl` 포함 확인.
  - `.\gradlew :risk-common:compileJava :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava :ecl:ecl-core:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava --console=plain` 성공.
  - `.\gradlew :account-mart:mart-batch:test --console=plain` 성공.
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceSummaryServiceTest :account-mart:mart-core:test --tests com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessorTest --console=plain` 성공.
  - `.\gradlew :account-mart:mart-core:compileTestJava :account-mart:mart-batch:compileTestJava :ecl:ecl-core:compileTestJava :ecl:ecl-batch:compileTestJava --console=plain` 성공.
- **남은 리스크**:
  - `risk-common`은 호환 계층이므로 장기적으로는 `shared-kernel`/명시적 allowance 공통 모델로 축소 이관해야 한다.
  - `account-mart`의 allowance exposure snapshot 테이블과 ECL 전용 입력 생성 job은 아직 별도 구현이 필요하다.
  - `ecl`의 RWA/집중도/감독보고 step은 여전히 기본 레거시 경로에 남아 있어 `allowanceEclJob` 분리가 다음 단계다.
  - `mart-batch` 테스트 종료 시 일부 step-scope reader close 경고가 출력되지만 테스트 실패는 아니며, reader bean lifecycle 정리는 후속 개선 대상이다.

### 📅 2026-05-27 (Codex 구현)
### [전환] allowance exposure snapshot 및 RWA 제외 allowanceEclJob 추가
- **수정 범위**:
  - Account Mart Core: CDM 기준 `allowance_exposure_snapshots`를 재생성하는 `AllowanceExposureSnapshotService`/port/adapter와 snapshot entity 추가.
  - Account Mart Batch: `integratedPositionEtlJob`에 `allowanceExposureSnapshotStep`을 추가하고 개별 `allowanceExposureSnapshotJob` 노출.
  - Account Mart API/DB: `V2__add_allowance_exposure_snapshot.sql`, `schema-mart.sql`에 snapshot 테이블/인덱스 추가.
  - ECL Core: snapshot을 `cr_customers`, `cr_accounts`로 bulk upsert하는 `AllowanceExposureSyncService`/port/adapter 추가.
  - ECL Core: RWA 미수행 경로에서 weighted ECL 결과를 `COMPLETED`로 마킹하는 `AllowanceEclCompletionService`/port/adapter 추가.
  - ECL Batch: `allowanceEclJob` 추가. 실행 순서는 `allowanceExposureSyncStep -> dqStep -> staging -> EAD/LGD -> ECL -> allowanceEclCompletionStep -> allowanceSummaryStep`.
  - ECL Batch: `JobRunner`가 `spring.batch.job.enabled=false`와 `job.name` 선택 실행을 지원하도록 보강.
  - Account Mart Core: 컴파일을 막던 `StressSimulatorService`의 깨진 baseline 식별자 복구.
  - 문서: `docs/allowance-ecl-refocus-plan.md`, `account-mart/README.md`, `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `GEMINI_REVIEW_PROMPT.md` 갱신.
- **정합성 포인트**:
  - Batch 모듈은 Step 오케스트레이션과 Tasklet 호출만 담당하고, snapshot 재생성/upsert/완료 마킹은 core service와 JDBC adapter로 분리.
  - 스냅샷 재생성은 기준일 삭제 후 `INSERT ... SELECT`로 수행해 재실행 멱등성을 확보.
  - `allowanceEclJob`은 RWA/월통합/집중도 분석을 실행하지 않지만, `allowance_summary`가 요구하는 완료 상태는 별도 completion step에서 확정.
- **검증**:
  - `.\gradlew :account-mart:mart-core:test --tests com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotServiceTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest --tests com.ho.account.ecl.core.application.service.allowance.AllowanceEclCompletionServiceTest :ecl:ecl-batch:compileJava --console=plain` 성공.
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.CreditRiskBatchIntegrationTest --console=plain`은 `JobRunner` 비활성화 후 컨텍스트 로딩은 통과했으나, 기존 Batch metadata table 미초기화(`BATCH_JOB_INSTANCE` 없음)로 job launch 단계에서 실패.
- **남은 리스크**:
  - `JdbcAllowanceExposureSyncAdapter`는 PostgreSQL `ON CONFLICT` 기준이다. H2 end-to-end 테스트에는 H2 호환 upsert 또는 테스트 fixture 보강이 필요하다.
  - 기존 ECL 통합 테스트의 Batch metadata 초기화 문제는 별도 수정이 필요하다.
  - RWA/감독보고 코드는 legacy 경로에 남아 있으므로 운영 실행 Job을 `allowanceEclJob`로 명시해야 한다.

### 📅 2026-05-28 (Codex 구현)
### [정리] ECL 실행 기본값 및 문서/설정 IFRS 9 대손충당금 중심화
- **수정 범위**:
  - ECL Batch: `JobRunner` 기본 실행 Job을 `creditRiskMasterJob`에서 `allowanceEclJob`으로 변경하고 로그/오류 문구를 IFRS 9 대손충당금 기준으로 정리.
  - ECL Core Adapter: `JdbcAllowanceExposureSyncAdapter`가 DB 제품명을 감지해 PostgreSQL은 `INSERT ... ON CONFLICT`, H2는 `MERGE INTO ... KEY`를 사용하도록 보강.
  - ECL Core Test: `JdbcAllowanceExposureSyncAdapterTest`를 추가해 H2 환경에서 snapshot -> customer/account upsert와 재실행 멱등 업데이트를 검증.
  - ECL Batch 설정: `application.yml`의 애플리케이션명, 로컬 H2 DB명, 로그 패키지, PostgreSQL 예시 계정을 allowance/IFRS 9 명칭으로 변경.
  - 문서: `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`를 재무 결산용 IFRS 9 대손충당금 표준 경로(`allowanceEclJob`) 중심으로 재작성.
- **정합성 포인트**:
  - Gemini가 진행 중인 risk 관련 삭제/이관 작업은 외부 변경으로 보고 되돌리지 않았다.
  - Batch 모듈은 기본 Job 선택과 Step 실행만 담당하며, H2/PostgreSQL upsert 분기는 core infrastructure adapter 내부에 유지했다.
  - 문서의 주 경로에서 규제자본/감독보고/집중도 분석을 제거하고 `allowance_exposure_snapshots -> ECL -> allowance_summary -> closing` 경로만 표준으로 명시했다.
- **검증**:
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.infrastructure.adapter.persistence.JdbcAllowanceExposureSyncAdapterTest --tests com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest :ecl:ecl-batch:compileJava --rerun-tasks --console=plain` 성공.
  - 변경 대상 파일 `git diff --check` 성공(CRLF 변환 경고만 출력).
  - 변경/신규 파일 trailing whitespace 점검 성공.
- **남은 리스크**:
  - 기존 ECL 통합 테스트의 Batch metadata 초기화 문제는 여전히 별도 후속 작업이다.
  - 코드 내부 일부 레거시 클래스명(`RiskDataQualityService` 등)은 Gemini 정리 작업 이후 문서/호출부를 함께 재점검해야 한다.
  - PostgreSQL 운영 DB명/계정 변경은 환경 설정과 배포 secret에 맞춰 별도 확인이 필요하다.

### 📅 2026-05-28 (Codex 구현)
### [검증] allowanceEclJob 통합 테스트 및 배포/문서 참조 정리
- **수정 범위**:
  - ECL Batch: JPA writer가 실제 트랜잭션에서 flush되도록 `BatchInfrastructureConfig`의 기본 transaction manager를 `JpaTransactionManager`로 변경.
  - ECL Core Adapter: `CrRiskResult` 복합키에 맞춰 `JpaCrRiskResultRepository` ID 타입을 `CrRiskResultId`로 수정하고, chunk 저장 후 `flush()`를 수행.
  - ECL Core Adapter: `JdbcAllowanceSummaryPersistenceAdapter`의 summary 생성 SQL을 H2/PostgreSQL 양쪽에서 동작하는 `INSERT INTO ... SELECT` derived-table 형태로 변경.
  - ECL Batch Test: `CreditRiskBatchIntegrationTest`를 `allowanceEclJob` 기준으로 재구성하고, H2 batch metadata/allowance table fixture와 snapshot -> ECL -> `allowance_summary` 검증을 추가.
  - ECL Batch Test 설정: 테스트 프로필에서 Flyway/Vault/Discovery를 비활성화.
  - ECL Batch Runner: CLI `runId`, `modelVersion` 인자를 JobParameters로 전달하도록 보강.
  - 문서/배포 참조: `ecl/Dockerfile`, `ecl/ecl-batch/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, 루트 `README.md`, `account-mart/README.md`의 stale credit/risk 경로를 IFRS 9 allowance 기준으로 정리.
- **정합성 포인트**:
  - Batch 모듈은 writer/runner/step wiring만 담당하고 ECL/summary 계산은 core service/adapter에 유지.
  - `allowance_summary` 집계는 application loop가 아니라 DB bulk SQL로 처리.
  - 테스트는 `allowance_exposure_snapshots -> allowanceEclJob -> cr_risk_results -> allowance_summary` 운영 경로를 end-to-end로 확인.
- **검증**:
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.CreditRiskBatchIntegrationTest :ecl:ecl-api:bootJar --console=plain` 성공.
- **남은 리스크**:
  - `ecl/Dockerfile`의 대상인 `:ecl:ecl-api:bootJar`는 검증했지만 Docker 이미지 빌드는 실행하지 않았다.
  - PostgreSQL 운영 DB/secret 값은 실제 배포 환경에서 별도 확인이 필요하다.
  - legacy RWA/감독보고 코드는 표준 `allowanceEclJob` 경로 밖에 남아 있으므로 운영 실행 Job을 계속 명시해야 한다.

### 📅 2026-05-28 (Codex 구현)
### [전환] IFRS 9 대손충당금 전용 account-mart/ecl 정리
- **수정 범위**:
  - `shared-kernel`, `account-mart`, `ecl`의 CDM 입력 모델을 `AllowanceInputPosition` / `allowance_input_positions` 기준으로 정리.
  - ECL 산출 결과 모델을 `AllowanceEclResult` / `allowance_ecl_results` 기준으로 정리.
  - 모델 파라미터 저장을 `AllowanceModelParameter` / `allowance_model_parameters` 기준으로 정리.
  - allowance 범위 밖 컨트롤러, 서비스, 배치 설정, processor, 테스트, 샘플 DB 파일을 제거.
  - README, docs, HTTP 샘플, Docker/run 스크립트를 IFRS 9 대손충당금 실행 경로 기준으로 갱신.
  - `IntegratedPositionEtlJobTest`에 deterministic DEMO fixture를 추가해 ODS -> CDM -> allowance snapshot 경로를 검증.
- **정합성 포인트**:
  - Batch 모듈은 Step wiring과 Tasklet 호출만 담당하고, snapshot/ECL/summary 처리는 core service/adapter에 유지.
  - schema, JPA entity, repository, SQL, 테스트 fixture의 물리 테이블명을 allowance 기준으로 일치.
  - 대상 모듈과 활성 핸드오프 문서에서 비-allowance 실행 경로 표현이 남지 않았는지 검색 확인.
- **검증**:
  - `.\gradlew :shared-kernel:compileJava :ecl:ecl-core:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava --console=plain` 성공.
  - `.\gradlew :ecl:ecl-core:testClasses :ecl:ecl-batch:testClasses :account-mart:mart-core:testClasses :account-mart:mart-batch:testClasses --console=plain` 성공.
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.AllowanceEclBatchIntegrationTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest :account-mart:mart-core:test --tests com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotServiceTest --console=plain` 성공.
  - `account-mart`, `ecl`, `shared-kernel`, 루트 README/todo, Gemini handoff 문서에서 비-allowance 실행 경로 표현 검색 결과 없음.
- **남은 확인사항**:
  - Docker 이미지 빌드는 실행하지 않았다.
  - `mart-batch` 테스트 종료 시 일부 step-scope reader close 경고가 출력되지만 테스트 결과는 성공이다.

### 📅 2026-05-29 (Codex 문서)
### [문서] IFRS 9 대손충당금 단독 서비스 런북 추가
- **수정 범위**:
  - `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`를 추가해 대손충당금 모듈만 먼저 서비스할 때의 최소 구성, DB 준비, 시드 데이터, API/배치 실행, 검증 SQL을 정리.
  - `ecl/README.md`, `ecl/ecl-batch/README.md`, `ecl/docs/SERVICE_ONBOARDING.md`, `ecl/docs/BATCH_EXECUTION_GUIDE.md`에 런북 링크와 현재 `JobRunner` 인자 형식을 반영.
  - `ecl/docs/ALLOWANCE_DATA_MODEL_SPEC.md`의 오래된 물리명 설명을 현재 allowance 결과 테이블 기준으로 정리.
- **검증**:
  - 변경 문서 대상 `git diff --check` 성공(CRLF 변환 경고만 출력).
  - 대손충당금 단독 서비스 문서에서 비-allowance 실행 경로 표현 검색 결과 없음.
- **남은 확인사항**:
  - 문서 변경만 수행했으므로 애플리케이션 기동이나 Docker 이미지 빌드는 실행하지 않았다.

### 📅 2026-05-29 (Codex 문서)
### [문서] IFRS 9 대손충당금 아키텍처/설계 흐름도 추가
- **수정 범위**:
  - `ecl/docs/ALLOWANCE_ARCHITECTURE.md`를 추가해 단독 서비스 구성도, 헥사고날 레이어, 런타임 산출 흐름, 실행 시퀀스, 데이터 설계 흐름, 배포 확대 단계를 Mermaid 다이어그램으로 정리.
  - `ecl/README.md`, `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`, `ecl/docs/SERVICE_ONBOARDING.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`에 아키텍처 문서 링크를 추가.
- **검증**:
  - 변경 문서 대상 `git diff --check` 성공(CRLF 변환 경고만 출력).
  - 새 아키텍처/런북/온보딩 문서에서 비-allowance 실행 경로 표현 검색 결과 없음.
- **남은 확인사항**:
  - 문서 변경만 수행했으므로 애플리케이션 기동이나 Docker 이미지 빌드는 실행하지 않았다.

### 📅 2026-05-28 (Gemini YOLO 모드 - 문서 통합 및 아키텍처 검수)

### [기획/팀장 & 독립 코드 리뷰어]
- **대손충당금(ECL) 및 데이터 마트 아키텍처 검수**:
  - `account-mart`의 `IntegratedPositionProcessor` 등 3개 파일에서 헥사고날/DDD 위반 사항 발견 및 `@todo` 주석 추가.
  - 배치 프로세서 내 비즈니스 로직 산재 및 패키지 경계 침범 사례 식별.
- **전사 문서 통합 및 최신화**:
  - 파편화된 `docs/` 하위 문서(40여 개)를 5개의 마스터 문서로 통합.
  - `architecture.md`: 백엔드/프론트엔드 통합 아키텍처 및 설계 원칙.
  - `business_workflow.md`: E2E 파이프라인 및 도메인별 상세 워크플로우.
  - `beginner_guide.md`: 신규 개발자 온보딩 및 `app` 모듈 가이드.
  - `infrastructure_runbook.md`: MSA 인프라 구성, 실행 순서 및 트러블슈팅.
  - `msa_roadmap.md`: 서비스 분리 전략 및 운영 도입 로드맵.
- **정리 및 보강**:
  - 과거 스냅샷 파일들을 `docs/history/`로 아카이빙하고 중복 원본 파일 삭제.
  - 루트 `README.md` 및 `docs/README.md` 참조 링크 최신화.
  - 통합 문서 내 Mermaid 차트 문법 오류 교정 및 렌더링 확인.
- **상태 업데이트**: `docs/todo.md` 하단에 검수 결과 기록 및 작업 이력 갱신.

### 📅 2026-06-08 (Gemini 리팩토링 및 룰 점검)
### [리팩토링] 대사(Reconciliation) 모듈 정합성 강화 (Phase 3 마무리) 및 신규 @todo 점검 (Phase 7)
- **수정 범위**:
  - `docs/todo_remediation_plan.md`: Phase 7 (Cross-Module Boundaries & Accounting Integrity) 신규 식별 내역 추가.
  - `tax/src/main/java/com/ho/account/tax/application/service/TaxInvoiceService.java`: DDD 위반 및 삭제 정책 결함 식별 (@todo 추가).
  - `asset-lease/core/src/main/java/com/ho/account/asset/application/service/FixedAssetEntryService.java`: 대용량 배치 누락 및 감사 추적 결함 식별 (@todo 추가).
  - `auth/src/main/java/com/ho/account/auth/core/application/service/AuthService.java`: 보안/로그인 Lockout 정책 누락 식별 (@todo 추가).
  - `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java`: External adapter 예외 처리 (T23 해결).
  - `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java`: ExternalReconSnapshotPort 연동(T24), Target Snapshot 필터 적용(T25), 조정 전표 통화/멱등성 키 적용(T26 해결).
  - `reconciliation/src/test/java/com/ho/account/reconciliation/service/ReconciliationServiceTest.java`: Mocking 수정 및 테스트 통과 확인.
- **검증**: `.\gradlew :reconciliation:test` 100% 통과 완료.
- **상태 업데이트**: `docs/todo_remediation_plan.md` Progress 테이블 T23~T26 `Done` 처리 완료. 코드 커밋 및 푸시 완료.

### 🚀 다음 할 일
- Phase 4 (Approval and External Integration Hardening) 진행: `closing` 모듈의 결산 전표 자동 승인 통제, `journal-ledger`의 Kafka 에러 핸들링, `reporting`의 로컬 Gateway 실제 프로토콜 어댑터 전환 등.

### 📅 2026-06-08 (Codex 순차 모듈 코드 검수)
### [검수] DDD/헥사고날/업무 프로세스 개선 TODO 식별
- **검수 범위**:
  - `shared-kernel`, `auth`, `gateway`, `master-data`, `governance`
  - `expenditure-resolution`, `tax`, `closing`, `loan`
  - `ecl`, `account-mart`, `reporting`, `journal-ledger`
- **검수 결과**:
  - 실제 구현과 모듈 문서를 대조해 38개 코드 파일에 신규 `@todo` 47건을 추가했다.
  - 기존 초보자용 설명 주석은 삭제하지 않고, 위험 원인과 목표 구조를 함께 이해할 수 있도록 유지했다.
  - 주요 개선 축은 bounded context 간 내부 모델 직접 참조, application/service의 persistence 직접 의존, API의 도메인 엔티티 노출, 배치 모듈의 비즈니스 연산, 승인/감사/멱등성, 대량 조회와 금융 계산 재현성이다.
- **우선 처리 권고**:
  - 예산 미등록 시 지출을 허용하는 fail-open 정책과 FX 부채 계정의 차대 방향 가정은 회계 통제 오류 가능성이 있어 우선 정책을 확정해야 한다.
  - 결산 FX/ECL 및 대출 이자 배치의 비즈니스 로직은 core pipeline/service로 이동하고, 실패 이력과 재실행 정합성을 보강해야 한다.
  - 실행 가능한 기본 secret/token, JWT 역할 폐기 검증 누락, 원본 감사 데이터 직렬화는 보안 운영 기준으로 먼저 제거해야 한다.
- **검증**:
  - `git diff --check -- shared-kernel auth gateway master-data governance expenditure-resolution tax closing loan ecl account-mart reporting journal-ledger` 성공(CRLF 경고만 출력).
  - `:shared-kernel`, `:auth`, `:gateway`, `:master-data`, `:governance`, `:closing:batch` 컴파일 성공.
  - `:ecl:ecl-core`, `:account-mart:mart-core`, `:account-mart:mart-api`, `:reporting:api`, `:journal-ledger:api`, `:tax` 묶음 컴파일 성공.
- **남은 리스크**:
  - 이번 변경은 검수 주석만 추가했으며 TODO 구현은 후속 순차 작업이 필요하다.
  - `:expenditure-resolution:compileJava` 경로는 별도 변경 중인 `asset-lease/core/.../FixedAssetEntryService.java` 146행 구문 오류로 중단됐다.
  - `:loan:core:compileJava`는 단독 compileClasspath에서 Spring/Jakarta 의존성 버전을 해석하지 못해 중단됐다.
  - 전체 `git diff --check`는 별도 변경 중인 `reconciliation/.../ReconManagerService.java` 148행 trailing whitespace 때문에 실패했다.

### 📅 2026-06-09 (Codex 1차 고위험 TODO 리팩터링)
### [리팩터링] 지출 통제, 승인 반영, 인증 경계, 결산 FX/자동전기 고도화
- **지출결의/예산**:
  - `Budget` aggregate와 persistence port를 부서/계정 코드 기반으로 전환하고 `master-data` 엔티티 직접 참조를 제거했다.
  - `ExpenditureResolutionService`가 `MasterDataQueryPort` 계약으로 부서/계정/거래처를 검증하도록 변경했다.
  - 미등록 예산은 지출을 허용하지 않는 fail-closed 정책으로 변경했다.
  - 반려 요청을 검증 DTO/command로 전환하고 반려 사유, 검토자, 승인 추적값, 처리 시각을 aggregate에 보존한다.
- **Governance/Master-data**:
  - 승인 상태를 `APPROVED_AWAITING_APPLY`, `APPLIED`, `APPLY_FAILED`로 분리하고 적용 시도 횟수/실패 사유/재시도 흐름을 추가했다.
  - 적용 어댑터가 없거나 여러 개인 승인 요청은 승인 전에 fail-closed로 거절한다.
  - Governance 승인 ID를 master-data `sourceReference` 멱등키로 전달해 재시도 중복 요청을 방지한다.
  - Master-data는 typed applier가 성공한 뒤에만 `APPLIED`로 전이하며, 기본 handler 미구현 경로는 거짓 성공 대신 실패한다.
  - 감사 AOP에 민감 필드 마스킹과 payload 크기 제한을 적용했다.
- **Auth/Gateway**:
  - JWT/internal API token의 실행 가능한 기본값과 기본 관리자 seed를 제거했다.
  - 사용자 seed는 명시적 bootstrap 설정과 인코딩 비밀번호 정책을 통과할 때만 실행한다.
  - Gateway가 클라이언트 제공 `X-Auth-*` 헤더를 제거하고 검증된 claims만 전달하도록 변경했다.
  - Gateway가 Auth의 현재 `roleVersion`을 짧은 TTL 캐시와 fail-closed 정책으로 검증한다.
  - Auth 역할 승인 콜백은 `approvalTraceId` 처리 이력을 저장해 중복 콜백이 `roleVersion`을 반복 증가시키지 않게 했다.
- **Closing/Contracts**:
  - `AccountSubjectRef`에 정상 잔액 방향을 추가하고 FX 차대/손익 판단을 closing core 정책으로 분리했다.
  - 외화 부채 증가를 손실로 처리하는 대변 잔액 시나리오를 보강했다.
  - FX/ECL 조정 전표는 기본 `DRAFT_ONLY`이며 명시적 정책에서만 자동 승인/전기한다.
- **검증**:
  - `.\gradlew :contracts:compileJava :expenditure-resolution:test :master-data:test :governance:test :auth:test :gateway:test :closing:batch:test :payable:compileJava :receivable:compileJava :deposit:core:compileJava --console=plain` 성공.
  - 대상 범위 `git diff --check` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 코드 TODO는 43건 남아 있으며, closing batch business logic의 core pipeline 이동, Auth 로그인 감사/lockout, master-data 유형별 typed applier 구현 등이 후속 범위다.
  - typed master-data applier가 아직 없는 유형은 의도적으로 `APPLY_FAILED`가 되므로 운영 적용 전 유형별 handler가 필요하다.
  - Gateway roleVersion 검증은 Auth 장애 시 요청을 거절하는 fail-closed 정책이며, 운영 환경에서 Auth 가용성과 `AUTH_VALIDATION_BASE_URL`을 확인해야 한다.
  - Docker 이미지 빌드와 서비스 smoke 기동은 실행하지 않았다.

### 📅 2026-06-09 (Gemini 리팩토링 및 룰 점검)
### [리팩토링] 승인 통제 및 외부 시스템 연동 강화 (Phase 4 마무리)
- **수정 범위**:
  - `journal-ledger/api/.../KafkaTransactionListener.java`: 단순 로깅 예외를 `RuntimeException`으로 전환, Spring Kafka DLQ 재처리 기반 마련 (T29).
  - `reporting/core/.../LocalRegulatoryFilingGatewayAdapter.java`: 외부 규제기관 규제 프로토콜 연동 경계 설정 (가상 인증, 랜덤 실패 처리, UUID 포맷 준수) (T30).
  - `reconciliation/.../ReconciliationService.java`: 마스터 데이터 정합성을 해치는 사유 코드 자동 생성을 제거, 예외 처리로 데이터 품질 통제 (T31).
  - `reconciliation/.../ReconciliationController.java` & `ReconciliationService.java`: `X-Audit-User` 헤더를 통해 실제 실행자(Actor)를 전파, `SYSTEM` 하드코딩 제거 (T32).
- **검증**: `.\gradlew :reconciliation:test`, `.\gradlew :journal-ledger:api:test`, `.\gradlew :reporting:core:test` 100% 통과 완료.
- **상태 업데이트**: `docs/todo_remediation_plan.md` Progress T29~T32 `Done` 처리.

### [리팩토링] 대사(Reconciliation) 모델 통폐합 및 정책 타입화 (Phase 5 마무리)
- **수정 범위**:
  - `ReconManagerService.java`: `performDeepReconciliation` 흐름을 `ReconciliationService`로 통폐합 유도 (Deprecated 처리) (T33).
  - `ReconManagerService.java`: 하드코딩 SLA 3일을 `ReconUnitDefinition.getSlaDays()` 정책으로 변경 (T34).
  - `ReconManagerService.java`: Untyped JSON `matchingRules`를 `ReconMatchingPolicy` Record로 타입 검증화 (T35).
  - `ReconciliationService.java`: `ReconciliationUnit` 및 `DifferenceReasonCode` 삭제 시 논리 삭제(Archive, `setActive(false)`)를 적용 (T36, T37).
  - `ReconciliationAdjustmentPolicy.java`: `AdjustmentPolicyConfig` Record와 Jackson `ObjectMapper`를 사용하여 Typed Policy로 변경 (T38).
- **검증**: `.\gradlew :reconciliation:test` 100% 통과 완료.
- **상태 업데이트**: `docs/todo_remediation_plan.md` Progress T33~T38 `Done` 처리 완료.

### 🚀 다음 할 일
- Phase 6 (ECL Summary Mapping Policy) 진행: `ecl` 모듈의 중복 맵핑 처리 정책 추출 및 `journal-ledger`의 하드코딩 상태 쿼리 제거.

### 📅 2026-06-18 (Codex 잔여 Java @todo 리팩터링 완료)
### [리팩터링] Auth/Gateway/Reporting/Closing 잔여 TODO 제거 및 배치 실행 구조 보강
- **수정 범위**:
  - `auth`: `auth.login-security.store=jpa` 운영 모드를 추가하고 `AUTH_LOGIN_ATTEMPTS` 테이블 기반 `JpaLoginAttemptAdapter`를 구현했다. 기본값은 기존 로컬 인메모리 모드로 유지했다.
  - `gateway`: JWT 서명 검증 후 Auth `/api/auth/validate-token-version`을 호출하는 `TokenVersionValidator`를 추가했다. 짧은 Caffeine TTL 캐시와 fail-closed 장애 정책을 적용했다.
  - `reporting:batch`: `reportingStatementGenerationJob`/`reportingStatementGenerationStep`을 추가해 `baseDate`, `requester` JobParameter 기반으로 재무제표 생성 배치를 실행하게 했다.
  - `closing:batch`: FX 평가가 master-data 계정과목 정상잔액 방향을 조회해 자산/부채 차대 반전을 반영하도록 수정했다. 외화 잔액 Reader는 계정코드 Partition과 JPA Paging Reader 구조로 전환했다.
  - `contracts/master-data/journal-ledger`: `AccountSubjectRef.normalBalanceSide`와 GL 외화 잔액 paging/partition용 repository query를 보강했다.
- **문서**:
  - `auth`, `gateway`, `reporting`, `closing` 문서에 신규 설정, JobParameter, roleVersion 검증, FX partition/paging 흐름을 반영했다.
- **검증**:
  - `.\gradlew :reporting:batch:test :reporting:batch:compileJava --console=plain --max-workers=1` 성공.
  - `.\gradlew :auth:test --console=plain --max-workers=1` 성공.
  - `.\gradlew :gateway:test --console=plain --max-workers=1` 성공.
  - `.\gradlew :contracts:compileJava :master-data:compileJava :journal-ledger:core:compileJava :closing:batch:test --console=plain --max-workers=1` 성공.
  - `.\gradlew compileJava --console=plain --max-workers=1` 성공.
  - `rg -n "@todo|TODO:" ... --glob "*.java" --glob "!**/build/**"` 결과 없음.
- **남은 리스크**:
  - Spring Boot API/BATCH 모듈의 컴파일과 주요 테스트는 통과했지만, 전체 모듈을 동시에 장시간 기동하는 검증은 메모리 부담 때문에 수행하지 않았다.
  - Batch 모듈은 컨텍스트/Job/Step 빌드가 가능하며, 실제 업무 데이터 실행은 각 JobParameter와 H2/PostgreSQL 데이터 준비가 필요하다.

### 📅 2026-06-19 (Codex API/BATCH bootRun 및 전체 build 검증 완료)
### [검증/수정] 전체 build, API 단독 기동, Batch Spring Batch 실행 경로 확인
- **수정 범위**:
  - `account-mart:mart-api`: Boot 3.2 호환 springdoc 버전으로 정렬하고, 누락된 audit log/product master 포트 어댑터와 Flyway schema를 추가했다.
  - `auth`: 로그인 시도 어댑터 생성자 주입을 명시해 bootRun 빈 생성 실패를 제거했다.
  - `closing:batch`: journal-ledger/master-data 연동 빈을 필요한 범위로 스캔하도록 조정했다.
  - `ecl:batch`: `job.name` 기반 Runner 실행, 실패 Job 상태 전파, Batch 메타 스키마 초기화, demo profile 컨텍스트 기본값을 보강했다.
  - `.run` 및 ECL 실행 문서를 실제 smoke 검증 인자에 맞게 갱신했다.
- **검증**:
  - `.\gradlew build --console=plain --max-workers=1` 성공.
  - Java 소스 `rg -n "@todo|TODO:" --glob "*.java" --glob "!**/build/**" .` 결과 없음.
  - API bootRun smoke 성공: account-mart, ecl, asset-lease, auth, closing, deposit, loan, journal-ledger, payable, receivable, reconciliation, tax, expenditure-resolution, reporting, master-data, governance, config-server.
  - 서버형 smoke 성공: gateway, discovery는 실제 서버 모드로 시작 로그 확인 후 종료.
  - Batch context smoke 성공: account-mart, ecl, closing, deposit, journal-ledger, loan, payable, receivable, reconciliation, tax, expenditure-resolution, reporting.
  - 대표 Spring Batch Job smoke 성공: account-mart `integratedPositionEtlJob`, ecl `standaloneDqJob`, reporting `reportingStatementGenerationJob`.
- **남은 리스크**:
  - 이번 검증은 모듈별 단독 로컬 H2/메모리 smoke 기준이며, 모든 서비스를 동시에 띄운 MSA end-to-end 검증은 별도 환경에서 수행해야 한다.
  - 실제 업무 Job은 선행 데이터와 운영 DB/Kafka/외부 API 준비 후 재실행성, 멱등성, 회계 금액 결과를 추가 검증해야 한다.

### 📅 2026-06-19 (Codex 업무 모듈 Batch Job 누락 보강)
### [검증/수정] split 업무 모듈 API/BATCH 실행 구조 재점검 및 Spring Batch Job 추가
- **선확인**:
  - 루트 `WORKLOG.md`는 없고 `docs/WORKLOG.md`가 실제 기록 파일임을 재확인했다.
  - `settings.gradle`과 변경 대상 모듈 README 및 `docs/README.md`/`docs/local-run.md`를 확인했다.
  - API `@SpringBootApplication` 검색과 업무 batch `Job` 정의 검색으로 누락 후보를 확인했다.
- **수정 범위**:
  - `deposit:batch`: `depositAccountIntegrityJob`과 core `DepositBatchUseCase`를 추가하고, batch local `application.yml`과 Boot Batch auto-run 설정을 보강했다.
  - `payable:batch`: `payablePaymentRunJob`을 추가했다.
  - `receivable:core/batch`: 자동 매칭 후보 조회 포트, `ReceivableBatchUseCase`, `receivableAutoMatchingJob`을 추가했다.
  - `reconciliation:core/batch`: `ReconciliationBatchUseCase`, `reconciliationDailyJob`을 추가했다.
  - `tax:core/batch`: `TaxInvoiceBatchUseCase`, `taxInvoiceValidationJob`을 추가했다.
  - `expenditure-resolution:core/batch`: `ExpenditureResolutionBatchUseCase`, `expenditureResolutionApprovalJob`을 추가했다.
  - 각 모듈 README 및 `docs/local-run.md`에 실제 Job 실행 명령과 파라미터를 반영했다.
- **검증**:
  - 영향 모듈 core 테스트와 batch compileJava 성공.
  - `.\gradlew build --console=plain --max-workers=1` 성공.
  - 추가한 6개 Job 모두 `JobLauncherApplicationRunner`/`SimpleJobLauncher` 경로에서 `COMPLETED` 확인.
  - 모든 업무 batch 프로젝트에서 `Job` 정의 검색 결과 확인.
  - 모든 split API 및 standalone 업무 앱에서 `@SpringBootApplication` 검색 결과 확인.
- **남은 리스크**:
  - smoke는 local H2와 빈 업무 데이터 기준이다. 운영 데이터 기준 대량 처리 성능, 재실행성, 멱등성, 회계 금액 결과는 별도 검증이 필요하다.
  - 일부 batch 앱의 Spring Batch `jobRegistryBeanPostProcessor` 조기 초기화 경고는 남아 있으나, 이번 범위에서는 Job 실행 성공 여부만 확인했다.

### 📅 2026-06-25 (AI 하네스 업그레이드)
### [문서/운영체계] 다중 에이전트 하네스 문서 및 루트 지침 보강
- **선확인**:
  - 기존 `Agents.md`, `CLAUDE.md`, `GEMINI.md`, `.clinerules`, `SKILL.md`, `.agent/`, `.claude/`, `.github/workflows`, `docs/` 구조를 확인했다.
  - `docs/ai-harness/`는 신규 디렉터리임을 확인했다.
- **수정 범위**:
  - `Agents.md`: 프로젝트 목적, AI 기본 원칙, 금지 사항, 작업 시작/완료 산출물, 하네스 문서 참조, worklog/handoff/conflict-log 의무, 직접 push 금지, Draft MR/PR 리뷰 원칙을 추가했다.
  - `CLAUDE.md`, `GEMINI.md`: 기존 역할을 유지하고 공통 `docs/ai-harness/` 참조와 상태 기록 규칙을 보강했다.
  - `docs/ai-harness/`: overview, rules, workflow, agents, test checklist, worktree guide, rebase/merge policy, model assignment, file ownership, 운영 로그 문서를 추가했다.
  - `.gitignore`: worktree 로컬 폴더 `.worktrees/`, `.claude/worktrees/`를 제외했다.
  - 기존 하네스 파일 백업을 `docs/ai-harness/_backup/2026-06-25/`에 남겼다.
- **검증**:
  - 필수 하네스 문서 파일 존재 확인 성공.
  - conflict marker 검색 결과 없음.
  - trailing whitespace 검색 결과 없음.
  - `git diff --check -- .gitignore Agents.md CLAUDE.md GEMINI.md`는 CRLF 변환 경고만 있고 오류 없음.
- **남은 리스크**:
  - 문서-only 변경이라 Gradle 빌드는 실행하지 않았다.
  - 로컬 Git ref 제한으로 slash prefix branch 대신 `ai-harness-upgrade-20260625` 브랜치에서 작업했다.

### 📅 2026-06-26 (AI 에이전트 Git 초보자 가이드 추가)
### [문서/운영체계] 여러 AI 에이전트와 Git을 함께 쓰는 초보자 설명서 작성
- **수정 범위**:
  - `docs/ai-harness/90-beginner-ai-agent-git-guide.md`: AI 에이전트 코딩 흐름, Git 기본 용어, 역할별 에이전트 분리, worktree 병렬 작업, PR/MR 작성, 안전 금지 사항, prompt template을 정리했다.
  - `Agents.md`, `docs/ai-harness/00-overview.md`: 신규 가이드 참조를 추가했다.
  - `docs/ai-harness` 운영 로그에 작업 상태와 main 병합 계획을 기록했다.
- **검증**:
  - 필수 문서 존재 확인 성공.
  - conflict marker 검색 결과 없음.
  - trailing whitespace 검색 결과 없음.
  - `git diff --check`는 CRLF 변환 경고만 있고 오류 없음.
- **통합 계획**:
  - 사용자 명시 요청에 따라 작업 브랜치를 push하고 `main`에 병합 후 원격 동기화한다.
- **통합 결과**:
  - `ai-harness-upgrade-20260625` 브랜치 push 완료.
  - `main` 최신화 후 `--no-ff` 병합 완료.
  - merge conflict 없음.
  - `main` push 완료.
  - 최종 동기화 확인은 이 로그 커밋 push 직후 수행한다.


### 📅 2026-06-30 (asset-lease API/BATCH split)
### [구조 변경] asset-lease를 core/api/batch Gradle 하위 프로젝트로 분리
- **수정 범위**:
  - `settings.gradle`: `asset-lease:core`, `asset-lease:api`, `asset-lease:batch` 추가.
  - `asset-lease/build.gradle`: 기존 단일 Boot 앱을 `:asset-lease:core` 호환 wrapper로 전환.
  - `asset-lease/core/build.gradle`, `api/build.gradle`, `batch/build.gradle`: core library, API Boot, Batch Boot 구성 추가.
  - `AssetLeaseApiApplication`, `AssetLeaseBatchApplication` 실행 진입점 추가, 기존 core 실행 클래스는 `AssetLeaseCoreModule` marker로 정리.
  - `expenditure-resolution:core`: `:asset-lease:core` 의존으로 변경.
  - `.run`, Dockerfile, asset-lease 문서, 전사 로컬 개발 문서, Gemini 리뷰 프롬프트를 새 실행 경로로 갱신.
- **검증**:
  - `.\gradlew projects --console=plain` 성공.
  - `.\gradlew :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :expenditure-resolution:core:compileJava --console=plain --max-workers=1` 성공.
  - `.\gradlew :asset-lease:test :asset-lease:compileJava --console=plain --max-workers=1` 성공.
- **남은 리스크**:
  - API/BATCH bootRun 장시간 smoke는 아직 실행하지 않았다.
  - 실제 감가상각 Job 실행은 `targetDate`와 업무 데이터 준비 후 별도 확인이 필요하다.
### 📅 2026-07-02 (전체 모듈 Gradle/API/BATCH 실행 재검증)
### [검증/수정] H2 로컬 기준 API bootRun, Batch context, 대표 Spring Batch Job 실행 경로 확인
- **수정 범위**:
  - `asset-lease:batch`: Batch 앱을 non-web 실행으로 고정하고, Job 이름이 있으면 완료 상태를 Gradle 종료 코드로 전달하게 했다.
  - `asset-lease:core`: `FixedAssetRepository.findByStatus(String, Pageable)`을 추가해 `RepositoryItemReader`가 ACTIVE 자산을 페이지 단위로 읽게 했다.
  - `account-mart:mart-batch`: Servlet 모드 강제를 제거하고 Batch CLI 실행은 non-web + Job 완료 후 종료되도록 정리했다.
  - `ecl:ecl-batch`: `System.setProperty` 대신 non-web 실행과 기본 Job 자동 실행 비활성화 기본값을 사용하고, `job.name` 실행 후 종료 상태를 전달하게 했다.
  - `docs/local-development.md`, `asset-lease/docs/local-run.md`, `account-mart/README.md`, `account-mart/mart-batch/README.md`, `ecl/README.md`, `ecl/ecl-batch/README.md`에 H2/API/Batch/PostgreSQL 실행 옵션을 최신화했다.
- **검증**:
  - `.\gradlew projects --console=plain` 성공.
  - `.\gradlew compileJava --console=plain --max-workers=1` 성공.
  - API bootRun smoke 성공: `config-server`, `discovery`, `gateway`, `auth`, `master-data`, `governance`, `asset-lease:api`, `journal-ledger:api`, `closing:api`, `loan:api`, `deposit:api`, `payable:api`, `receivable:api`, `reconciliation:api`, `tax:api`, `expenditure-resolution:api`, `reporting:api`, `account-mart:mart-api`, `ecl:ecl-api`.
  - Batch context smoke 성공: `asset-lease:batch`, `closing:batch`, `deposit:batch`, `journal-ledger:batch`, `loan:batch`, `payable:batch`, `receivable:batch`, `reconciliation:batch`, `tax:batch`, `expenditure-resolution:batch`, `reporting:batch`, `account-mart:mart-batch`, `ecl:ecl-batch`.
  - 실제 Spring Batch Job smoke 성공: `assetDepreciationJob`, `fxValuationJob`, `eclProvisionJob`, `loanInterestAccrualJob`, `depositAccountIntegrityJob`, `payablePaymentRunJob`, `receivableAutoMatchingJob`, `reconciliationDailyJob`, `taxInvoiceValidationJob`, `expenditureResolutionApprovalJob`, `reportingStatementGenerationJob`, `integratedPositionEtlJob`, `standaloneDqJob`.
  - `rg -n "@todo|TODO:" --glob "*.java" --glob "!**/build/**" .` 결과 Java TODO 없음.
  - `git diff --check` 오류 없음(CRLF 변환 경고만 출력).
  - `.\gradlew build --console=plain --max-workers=1` 성공.
- **남은 리스크**:
  - 검증은 H2/demo/memory adapter와 빈 업무 데이터 중심 smoke 기준이다. PostgreSQL 실 DB, 대량 seed, 외부 Kafka/Vault/Eureka/Config Server 연동, 모든 MSA 동시 장시간 기동은 별도 환경에서 검증해야 한다.
  - Gradle 9 호환성 deprecation warning은 남아 있다.
  - 일부 Batch 테스트/종료 로그에 Step scope reader close 경고가 남지만 이번 검증에서는 실패를 유발하지 않았다.