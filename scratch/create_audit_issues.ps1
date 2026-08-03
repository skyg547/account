$issues = @(
    @{
        title = "[보안] application.yml 내 평문 비밀번호 제거 및 환경변수 전환"
        labels = "type:bug,priority:p0"
        body = @"
## 목표 (Goal)
- application.yml에 하드코딩된 평문 비밀번호를 환경변수 참조로 전환

## 현재 상태 (As-Is)
- account-mart/mart-batch/src/main/resources/application.yml:68,88 — password: allowance_password
- ecl/ecl-batch/src/main/resources/application.yml:69,91 — password: allowance_password
- postgres/docker-compose.yml:16 — POSTGRES_PASSWORD: postgres
- postgres/docker-compose.yml:29 — PGADMIN_DEFAULT_PASSWORD: admin
- auth/api/src/main/resources/application.yml:34 — 기본값이 약한 비밀번호(1234)

## 기대 상태 (To-Be)
- 모든 비밀번호가 환경변수 참조 또는 Vault 참조
- docker-compose는 .env 파일 참조로 전환

## 수용 조건 (Acceptance Criteria)
- [ ] rg -i 'password:.*[a-z]' 결과 0건
- [ ] 기존 테스트 통과

## 검증 명령
``````powershell
rg -in "password:" --include "*.yml" .
``````

## 롤백 방법
해당 커밋 revert

## 보안 메모
평문 비밀번호가 Git 히스토리에 남아있으므로 추후 credential rotation 필요
"@
    },
    @{
        title = "[master-data:api] Application 클래스 누락 — api 모듈 부트 불가"
        labels = "type:bug,priority:p0"
        body = @"
## 목표
master-data:api 모듈에 MasterDataApiApplication.java 생성 및 scanBasePackages 설정

## 현재 상태
master-data/api/src/main/java/ — Application 클래스 없음, src 파일 0개, bootJar 생성 불가

## 수용 조건
- [ ] .\gradlew :master-data:api:bootJar 성공
- [ ] ApplicationContext 로드 테스트 통과

## 검증 명령
``````powershell
.\gradlew :master-data:api:compileJava :master-data:api:test --console=plain
``````
"@
    },
    @{
        title = "[internal-audit:api] Application 클래스 및 소스 누락"
        labels = "type:bug,priority:p0"
        body = @"
## 목표
internal-audit:api 모듈에 Application 클래스와 기본 Controller 스캐폴딩 생성

## 현재 상태
internal-audit/api/src/main/java/ — Application 클래스 없음, src 파일 0개
internal-audit:batch도 단일 라인 Application만 존재 (scanBasePackages 미설정)

## 수용 조건
- [ ] .\gradlew :internal-audit:api:bootJar 성공
- [ ] ApplicationContext 로드 테스트 통과

## 검증 명령
``````powershell
.\gradlew :internal-audit:api:compileJava :internal-audit:api:test --console=plain
``````
"@
    },
    @{
        title = "[전 모듈] 18개 모듈 테스트 파일 0건 — 최소 ApplicationContext 로드 테스트 추가"
        labels = "type:test,priority:p0"
        body = @"
## 목표
테스트 파일이 0건인 18개 모듈에 최소한의 ApplicationContext 로드 테스트 추가

## 대상 모듈
master-data:api/batch, internal-audit(전체), loan:batch, deposit:api,
reconciliation:api/batch, payable:api/batch, receivable:batch,
asset-lease:api/batch, tax:api/batch, expenditure-resolution:batch,
account-mart:mart-api, auth:batch

## 수용 조건
- [ ] 모든 대상 모듈에 test 클래스 1개 이상
- [ ] @SpringBootTest(properties=spring.cloud.vault/config/eureka disabled) 패턴 사용
- [ ] 모든 대상 모듈의 .\gradlew :module:test 성공

## 검증 명령
``````powershell
.\gradlew test --console=plain -Porg.gradle.java.installations.paths=C:\Java\jdk17
``````
"@
    },
    @{
        title = "[account-mart] return null 40건+ 제거 — Optional 또는 예외 전환"
        labels = "type:refactor,priority:p1"
        body = @"
## 목표
account-mart 모듈 내 40건 이상의 return null을 Optional.empty() 또는 적절한 예외로 전환

## 현재 상태
- mart-core/infrastructure/persistence/ 하위 14개 Adapter 파일에 return null 산재
- mart-api/controller/MarketRateController.java:131,136 — Controller에서 직접 null 반환
- mart-core/domain/ 프로세서 클래스에서도 null 반환

## 수용 조건
- [ ] rg 'return null' account-mart/ --include '*.java' 결과 0건
- [ ] 기존 테스트 전부 통과

## 검증 명령
``````powershell
.\gradlew :account-mart:mart-core:test :account-mart:mart-api:test :account-mart:mart-batch:test --console=plain
``````
"@
    },
    @{
        title = "[reconciliation] return null 14건 제거 — AutomatedMatchingEngine 및 ReconciliationService"
        labels = "type:refactor,priority:p1"
        body = @"
## 목표
reconciliation:core의 return null 14건 제거

## 현재 상태
- AutomatedMatchingEngine.java:200,205,225,230,253,260,281 — 7건
- ReconciliationService.java:577,581 — 2건
- Command/Request/Policy 클래스 — 5건

## 수용 조건
- [ ] rg 'return null' reconciliation/ --include '*.java' 결과 0건
- [ ] .\gradlew :reconciliation:core:test 통과

## 검증 명령
``````powershell
.\gradlew :reconciliation:core:test --console=plain
``````
"@
    },
    @{
        title = "[expenditure-resolution] DtoAssembler return null 3건 제거"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
ExpenditureResolutionDtoAssembler.java:63,72,81 — 3건

## 검증 명령
``````powershell
.\gradlew :expenditure-resolution:api:test --console=plain
``````
"@
    },
    @{
        title = "[master-data] return null 4건 제거 — DTO 및 도메인 모델"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
- AccountSubjectDto.java:35, DepartmentDto.java:26
- MasterDataChangeRequest.java:192,200

## 검증 명령
``````powershell
.\gradlew :master-data:core:test --console=plain
``````
"@
    },
    @{
        title = "[loan] return null 2건 제거 — DeferredItemType, LoanEvent enum 내 null 반환"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
- DeferredItemType.java:225 — enum lookup에서 null 반환
- LoanEvent.java:186 — enum lookup에서 null 반환

## 기대 상태
알 수 없는 enum 값에 대해 IllegalArgumentException throw 또는 Optional 반환

## 검증 명령
``````powershell
.\gradlew :loan:core:test --console=plain
``````
"@
    },
    @{
        title = "[internal-audit] return null 1건 제거 + 테스트 전무 해소"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
- AuthUserRoleApprovalApplyAdapter.java:101 — return null
- internal-audit:core src=60개이지만 test=0개

## 검증 명령
``````powershell
.\gradlew :internal-audit:core:test --console=plain
``````
"@
    },
    @{
        title = "[gateway] JjwtAccessTokenVerifier return null 제거 — 보안 토큰 검증 경로"
        labels = "type:bug,priority:p1"
        body = @"
## 현재 상태
JjwtAccessTokenVerifier.java:115 — 토큰 검증 실패 시 null 반환 (보안 위험)

## 기대 상태
검증 실패 시 명시적 예외 또는 AuthenticationException throw

## 검증 명령
``````powershell
.\gradlew :gateway:test --console=plain
``````
"@
    },
    @{
        title = "[journal-ledger] JournalRuleEngine return null 제거 — 분개 규칙 엔진 핵심 로직"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
JournalRuleEngine.java:263 — 분개 규칙 매칭 실패 시 null 반환

## 검증 명령
``````powershell
.\gradlew :journal-ledger:core:test --console=plain
``````
"@
    },
    @{
        title = "[reporting] InMemoryJournalQueryAdapter UnsupportedOperationException 제거"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
InMemoryJournalQueryAdapter.java:62 — throw new UnsupportedOperationException

## 기대 상태
인메모리 구현체도 단건 조회를 지원하거나, 명시적 커스텀 예외로 전환

## 검증 명령
``````powershell
.\gradlew :reporting:core:test --console=plain
``````
"@
    },
    @{
        title = "[ecl-batch, journal-ledger:batch, account-mart] BatchParameterUtils return null 제거"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
- ecl/ecl-batch/.../BatchParameterUtils.java:38 — return null
- journal-ledger/batch/.../BatchDateRangeParameterUtils.java:55 — return null
- account-mart/mart-core/.../BatchParameterUtils.java:34 — return null

## 기대 상태
파라미터 파싱 실패 시 IllegalArgumentException throw

## 검증 명령
``````powershell
.\gradlew :ecl:ecl-batch:test :journal-ledger:batch:test :account-mart:mart-batch:test --console=plain
``````
"@
    },
    @{
        title = "[deposit] 헥사고날 구조 미완성 — api/batch 소스 빈약 및 scanBasePackages 미설정"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
- deposit:api — src=2개, test=0개
- deposit:batch — src=3개, test=1개
- DepositApplication에 scanBasePackages 미설정

## 기대 상태
- DepositApplication에 scanBasePackages 추가
- 각 모듈에 최소 ApplicationContext 테스트

## 검증 명령
``````powershell
.\gradlew :deposit:api:test :deposit:core:test :deposit:batch:test --console=plain
``````
"@
    },
    @{
        title = "[payable] 패키지가 expenditure 하위에 위치 — Bean 충돌 위험"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
PayableApiApplication: scanBasePackages = com.ho.account.expenditure
expenditure-resolution과 동일 패키지 스캔 — Bean 충돌 위험

## 기대 상태
payable 전용 패키지 com.ho.account.payable로 분리하거나 스캔 범위 명확화

## 검증 명령
``````powershell
.\gradlew :payable:api:compileJava :payable:core:test --console=plain
``````
"@
    },
    @{
        title = "[auth:batch] 단일라인 Application 클래스 — scanBasePackages 및 구조 보완"
        labels = "type:refactor,priority:p1"
        body = @"
## 현재 상태
AuthBatchApplication.java — 한 줄짜리 클래스, scanBasePackages 없음, src=1개, test=0개

## 검증 명령
``````powershell
.\gradlew :auth:batch:test --console=plain
``````
"@
    },
    @{
        title = "[internal-audit] 모듈 README.md 누락"
        labels = "type:docs,priority:p2"
        body = @"
## 현재 상태
internal-audit/ 디렉터리에 README.md 없음. 다른 모든 비즈니스 모듈은 README.md + docs/ 보유

## 기대 상태
internal-audit/README.md 생성 (모듈 목적, 구조, 실행 방법)
internal-audit/docs/README.md 생성
"@
    },
    @{
        title = "[shared-kernel] 테스트 보강 — src=67 vs test=3 (4.5% 비율)"
        labels = "type:test,priority:p2"
        body = @"
## 현재 상태
shared-kernel은 모든 모듈이 의존하는 핵심 라이브러리. src 67개 대비 test 3개

## 수용 조건
- [ ] 주요 유틸리티/도메인 클래스에 단위 테스트 추가
- [ ] 최소 20% 이상 테스트 파일 비율

## 검증 명령
``````powershell
.\gradlew :shared-kernel:test --console=plain
``````
"@
    },
    @{
        title = "[ecl:ecl-core] 금융 계산 정밀도 테스트 확인 — BigDecimal 검증"
        labels = "type:test,priority:p2"
        body = @"
## 현재 상태
ecl-core는 대손충당금(IFRS9) 핵심 계산 모듈. src 99개 대비 test 19개

## 수용 조건
- [ ] Stage 분류, PD/LGD/EAD 계산, ECL 산출 로직 단위 테스트 확인
- [ ] BigDecimal 정밀도 검증 테스트 포함

## 검증 명령
``````powershell
.\gradlew :ecl:ecl-core:test --console=plain
``````
"@
    },
    @{
        title = "[전 모듈] 교육적 Javadoc 주석 일괄 보강"
        labels = "type:docs,priority:p2"
        body = @"
## 목표
모든 public 클래스와 핵심 메서드에 설계 이유를 포함한 Javadoc 추가

## 대상 모듈 (우선순위)
1. shared-kernel (전 모듈 공용)
2. journal-ledger:core (분개 규칙 엔진)
3. closing:core (결산 상태 머신)
4. ecl:ecl-core (IFRS9 계산)
5. loan:core (EIR/상각)
"@
    },
    @{
        title = "[master-data:batch] MasterDataBatchApplication 단일라인 클래스 보완"
        labels = "type:refactor,priority:p2"
        body = @"
## 현재 상태
MasterDataBatchApplication.java — 한 줄짜리 파일, scanBasePackages 미설정

## 기대 상태
적절한 scanBasePackages, @EntityScan 설정 및 최소 컨텍스트 테스트
"@
    },
    @{
        title = "[InternalAuditBatchApplication] 단일라인 클래스 보완"
        labels = "type:refactor,priority:p2"
        body = @"
## 현재 상태
InternalAuditBatchApplication.java — 한 줄짜리 파일, scanBasePackages 미설정
"@
    },
    @{
        title = "[config-repo] 설정 리포 문서화 보강 — 모듈별 yml 설명 추가"
        labels = "type:docs,priority:p2"
        body = @"
## 현재 상태
config-repo에 application-dev.yml, application-prod.yml, journal-ledger.yml, master-data.yml 등 존재
각 설정 파일의 목적과 키 설명 문서 없음
"@
    },
    @{
        title = "[frontend] 백엔드 API 정합성 전수 확인"
        labels = "type:test,priority:p2"
        body = @"
## 목표
프론트엔드 src/services/ 내 API 호출과 백엔드 Controller 매핑이 일치하는지 확인

## 수용 조건
- [ ] 모든 API 서비스 파일의 엔드포인트가 백엔드 Controller에 존재
- [ ] Request/Response DTO 구조 일치
"@
    }
)

$created = 0
foreach ($issue in $issues) {
    Write-Host "Creating: $($issue.title)"
    gh issue create --title $issue.title --label $issue.labels --body $issue.body
    $created++
    Write-Host "Created ($created/$($issues.Count))"
}
Write-Host "Done! Created $created issues."
