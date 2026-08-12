$issues = @(
    @{ Title="[gateway][security] JWT Secret 하드코딩 및 비대칭키 구조 전환 필요"; BodyFile="issue1.md"; Body=@"
## 목표 (Goal)
JWT Secret 하드코딩 제거 및 비대칭키 구조 전환

## 현재 상태 (As-Is)
`AUTH_JWT_SECRET`의 기본값(`modern-account-system-super-secret-key-1234567890`)이 평문으로 하드코딩되어 소스코드 유출 시 심각한 보안 사고를 초래할 수 있음

## 기대 상태 (To-Be)
하드코딩된 Secret 기본값을 제거하고, Config Server에서 암호화된 상태로 주입받도록 구성. HS256(대칭키) 대신 JWKS를 활용한 RS256/ES256(비대칭키) 검증 구조로 전환

## 대상 파일
gateway/src/main/resources/application.yml

## 카테고리
security

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] JWT Secret 평문 하드코딩 제거, Config Server 연동 또는 환경변수 전환

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[closing][msa] journal-ledger:core 직접 의존에 의한 Bounded Context 경계 위반"; BodyFile="issue2.md"; Body=@"
## 목표 (Goal)
closing 모듈의 journal-ledger:core 직접 의존 제거 및 통신 방식 개선

## 현재 상태 (As-Is)
closing 모듈이 journal-ledger:core에 직접 의존하고 내부 도메인 엔티티(JournalEntry, JournalEntryStatus)와 인바운드 포트(JournalUseCase)를 직접 import

## 기대 상태 (To-Be)
build.gradle에서 project(':journal-ledger:core') 의존성 제거, contracts 모듈의 공통 DTO 사용, FeignClient/REST API 또는 Kafka 이벤트 기반 통신으로 변경

## 대상 파일
closing/api/build.gradle, closing/batch/build.gradle, closing/batch/.../JournalLedgerClosingJournalEntryAdapter.java

## 카테고리
msa

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] journal-ledger:core 직접 의존성 제거, 통신 방식 전환

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[deposit][architecture] 낙관적 잠금(Optimistic Locking) 누락으로 인한 잔액 갱신 손실(Lost Update) 위험"; BodyFile="issue3.md"; Body=@"
## 목표 (Goal)
DepositAccount 엔티티에 동시성 제어 적용

## 현재 상태 (As-Is)
DepositAccount 엔티티에 동시성 제어를 위한 버전 관리 필드가 없어 동시 트랜잭션 시 잔액 정합성 깨짐 위험

## 기대 상태 (To-Be)
@Version private long version 필드 추가하여 JPA 낙관적 잠금 활성화 또는 Pessimistic Lock 적용

## 대상 파일
deposit/core/.../domain/DepositAccount.java

## 카테고리
architecture

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] 동시 입금/출금 시 갱신 손실 방지 검증, 관련 테스트 통과

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[expenditure-resolution][msa] 모듈 간 직접 의존 및 MSA/헥사고날 경계 심각한 위반"; BodyFile="issue4.md"; Body=@"
## 목표 (Goal)
expenditure-resolution 모듈의 직접 의존성 제거 및 MSA/헥사고날 경계 준수

## 현재 상태 (As-Is)
expenditure-resolution:core가 journal-ledger:core 및 asset-lease:core에 컴파일 타임 의존성을 맺고 타 Bounded Context의 인커밍 포트와 도메인 엔티티를 직접 사용

## 기대 상태 (To-Be)
core 간 Gradle 의존성 제거, contracts 모듈 DTO + Outgoing Port 사용, infrastructure에서 FeignClient/REST/Kafka로 통신

## 대상 파일
expenditure-resolution/core/build.gradle, expenditure-resolution/core/.../ExpenditureResolutionService.java

## 카테고리
msa

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] core 모듈 간 직접 의존성 0건, 포트 기반 통신 전환

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[expenditure-resolution][financial] 결의서 수정/반려 시 예산 복원 누락 (이중 차감 버그)"; BodyFile="issue5.md"; Body=@"
## 목표 (Goal)
결의서 수정/반려 시 예산 정합성 보장

## 현재 상태 (As-Is)
updateResolution 호출 시 기존 예산 차감을 복원하지 않고 새 항목에 대해 useBudget 수행하여 이중 차감. rejectResolution도 예산 복원 없음

## 기대 상태 (To-Be)
BudgetService에 restoreBudget 기능 추가, 수정 시 선 복원 후 차감, 반려 시 전액 복원

## 대상 파일
expenditure-resolution/core/.../ExpenditureResolutionService.java

## 카테고리
financial

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] 결의서 수정/반려 시 예산 정합성 보장, 단위 테스트 통과

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[payable, receivable][architecture] 도메인 엔티티에 JPA 의존성 강결합 — 헥사고날 위반"; BodyFile="issue6.md"; Body=@"
## 목표 (Goal)
도메인 모델에서 JPA 의존성 제거 (헥사고날 아키텍처 준수)

## 현재 상태 (As-Is)
Payment, Payable, Receivable 등 핵심 도메인 모델에 @Entity, @Table 등 JPA 어노테이션이 혼재

## 기대 상태 (To-Be)
영속성 모델(JPA Entity)을 adapter.out.persistence 인프라 계층으로 분리, 도메인은 순수 POJO로 유지

## 대상 파일
payable/core/.../expenditure/domain/*.java, receivable/core/.../receivable/domain/*.java

## 카테고리
architecture

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] domain 패키지에 JPA 의존성 0건, Persistence Adapter에서 매핑 수행

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[reconciliation][financial] 대사 자동 매칭 로직이 단순 총액 비교에 불과함 — 건별 N:M 매칭 엔진 부재"; BodyFile="issue7.md"; Body=@"
## 목표 (Goal)
대사 자동 매칭 로직 고도화 및 건별 N:M 매칭 엔진 도입

## 현재 상태 (As-Is)
performReconciliation이 원천/대상의 전체 건수와 총액만 비교하는 단순 집계 검증 수준으로 건별 불일치 추적/N:M 매칭 불가

## 기대 상태 (To-Be)
건별 상세 데이터 조회 후 복합 키 기반 그룹화, 항목 수준(item-level) N:M 매칭 엔진 알고리즘 도입

## 대상 파일
reconciliation/core/.../ReconciliationService.java

## 카테고리
financial

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] 건별 매칭 로직 구현, N:M 매칭 테스트 통과

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[reconciliation][extensibility] 대량 원장 데이터 메모리 적재로 인한 OOM 위험"; BodyFile="issue8.md"; Body=@"
## 목표 (Goal)
대량 원장 데이터 처리 시 메모리 OOM 위험 제거

## 현재 상태 (As-Is)
buildLedgerSnapshot에서 전체 리스트를 for-loop로 순회하며 메모리상 집계. 데이터 대량 시 OOM 유발

## 기대 상태 (To-Be)
DB에서 SUM 등 Aggregate Query 포트 호출 또는 Spring Batch Chunk 처리 도입

## 대상 파일
reconciliation/core/.../ReconManagerService.java

## 카테고리
extensibility

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] 메모리 적재 제거, DB 단 집계 적용

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[tax][extensibility] 대량 세금계산서 데이터 한번에 메모리 로딩 — OOM 위험"; BodyFile="issue9.md"; Body=@"
## 목표 (Goal)
세금계산서 대량 조회 시 Paging/Cursor 처리로 OOM 방지

## 현재 상태 (As-Is)
validatePurchaseInvoices에서 전체 세금계산서를 findByIssueDateBetween으로 List에 한번에 적재

## 기대 상태 (To-Be)
Spring Batch ItemReader 기반 Paging 조회나 Cursor 방식 적용

## 대상 파일
tax/core/.../TaxInvoiceBatchService.java

## 카테고리
extensibility

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] 대량 데이터 처리 시 OOM 발생하지 않음

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[ecl][financial] 금융 통계(ECL/PD) 계산 로직에 부동소수점(double) 자료형 사용 — 금액 정밀도 위험"; BodyFile="issue10.md"; Body=@"
## 목표 (Goal)
ECL/PD 금융 통계 계산 정밀도 향상

## 현재 상태 (As-Is)
생존확률, 한계부도확률, 담보 최적화 등 IFRS9 핵심 통계 계산에서 double 배열/원시 타입 사용

## 기대 상태 (To-Be)
모든 금융 계산을 BigDecimal로 전환, MathContext로 IFRS 규정 정밀도/반올림 정책 적용

## 대상 파일
ecl/ecl-core/.../LifetimePdService.java, CollateralAllocationService.java, CrAccount.java 등

## 카테고리
financial

## 심각도
critical

## 수용 조건 (Acceptance Criteria)
- [ ] double/float 사용 0건, BigDecimal 전환, 관련 테스트 통과

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[config-server][security] 운영 환경용 Git 백엔드 및 암호화 설정 부재"; BodyFile="issue11.md"; Body=@"
## 목표 (Goal)
운영 환경을 위한 Git 기반 백엔드 및 기밀 정보 암호화 적용

## 현재 상태 (As-Is)
파일 시스템 기반(native) 저장소만 설정. 설정 이력 관리(Git)와 기밀 정보 보호를 위한 암호화 부재

## 기대 상태 (To-Be)
prod 프로파일 분리하여 Git 기반 백엔드 적용, encrypt.key 또는 HashiCorp Vault 연동

## 대상 파일
config-server/src/main/resources/application.yml

## 카테고리
security

## 심각도
high

## 수용 조건 (Acceptance Criteria)
- [ ] 운영 설정에 암호화 적용, Git backend 구성

## 롤백 방법
해당 커밋 revert
"@ },
    @{ Title="[gateway][msa] Gateway 라우팅 룰 및 Rate Limiter, CORS 설정 누락"; BodyFile="issue12.md"; Body=@"
## 목표 (Goal)
Gateway 라우팅, Rate Limiter 및 CORS 설정 추가

## 현재 상태 (As-Is)
백엔드 서비스로의 라우팅 설정(spring.cloud.gateway.routes)이 전혀 없고, Discovery 동적 라우팅도 비활성화. Rate Limiting 및 Global CORS 설정도 누락

## 기대 상태 (To-Be)
Eureka 통한 discovery.locator 자동 라우팅 활성화 또는 명시적 RouteLocator 추가, Redis 기반 RequestRateLimiter 및 Global CORS 정책 구성

## 대상 파일
gateway/src/main/resources/application.yml

## 카테고리
msa

## 심각도
high

## 수용 조건 (Acceptance Criteria)
- [ ] 서비스 라우팅 정상 동작, CORS 설정 완료

## 롤백 방법
해당 커밋 revert
"@ }
)

foreach ($issue in $issues) {
    Set-Content -Path $issue.BodyFile -Value $issue.Body -Encoding UTF8
    $output = gh issue create --title $issue.Title --body-file $issue.BodyFile
    Write-Output $output
}

Remove-Item issue*.md
