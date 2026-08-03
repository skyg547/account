$issues = @(
    @{
        title = "[master-data:api] Application 클래스 누락 — api 모듈 부트 불가"
        body = @"
## 목표 (Goal)
master-data:api 모듈에 MasterDataApiApplication.java 생성 및 scanBasePackages 설정

## 현재 상태 (As-Is)
master-data/api/src/main/java/ — Application 클래스 없음, src 파일 0개, bootJar 생성 불가

## 기대 상태 (To-Be)
- MasterDataApiApplication.java 생성, @SpringBootApplication(scanBasePackages = {"com.ho.account.masterdata"}) 설정
- ApplicationContext 로드 테스트 통과

## 수용 조건 (Acceptance Criteria)
- [ ] .\gradlew :master-data:api:bootJar 성공
- [ ] ApplicationContext 로드 테스트 통과

## 검증 명령
```powershell
.\gradlew :master-data:api:compileJava :master-data:api:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[internal-audit:api] Application 클래스 및 소스 누락 — api 모듈 빈 껍데기"
        body = @"
## 목표 (Goal)
internal-audit:api 모듈에 Application 클래스와 기본 Controller 스캐폴딩 생성

## 현재 상태 (As-Is)
- internal-audit/api/src/main/java/ — Application 클래스 없음, src 파일 0개
- internal-audit:batch도 단일 라인 Application만 존재 (scanBasePackages 미설정)

## 기대 상태 (To-Be)
- InternalAuditApiApplication.java 생성, 적절한 scanBasePackages 설정
- InternalAuditBatchApplication에도 scanBasePackages 추가

## 수용 조건 (Acceptance Criteria)
- [ ] .\gradlew :internal-audit:api:bootJar 성공
- [ ] ApplicationContext 로드 테스트 통과

## 검증 명령
```powershell
.\gradlew :internal-audit:api:compileJava :internal-audit:api:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[전 모듈] 18개 모듈 테스트 파일 0건 — 최소 ApplicationContext 로드 테스트 추가"
        body = @"
## 목표 (Goal)
테스트 파일이 0건인 18개 모듈에 최소한의 ApplicationContext 로드 테스트 추가

## 대상 모듈
master-data:api/batch, internal-audit(전체), loan:batch, deposit:api,
reconciliation:api/batch, payable:api/batch, receivable:batch,
asset-lease:api/batch, tax:api/batch, expenditure-resolution:batch,
account-mart:mart-api, auth:batch

## 현재 상태 (As-Is)
위 18개 모듈의 src/test/java 디렉터리에 테스트 클래스가 하나도 없음

## 기대 상태 (To-Be)
각 모듈에 @SpringBootTest(properties = {"spring.cloud.vault.enabled=false", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"}) ApplicationContext 로드 테스트가 최소 1개 존재

## 수용 조건 (Acceptance Criteria)
- [ ] 모든 대상 모듈에 test 클래스 1개 이상
- [ ] 모든 대상 모듈의 .\gradlew :module:test 성공

## 검증 명령
```powershell
.\gradlew test --console=plain -Porg.gradle.java.installations.paths=C:\Java\jdk17
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[account-mart] return null 40건+ 제거 — Optional 또는 예외 전환"
        body = @"
## 목표 (Goal)
account-mart 모듈 내 40건 이상의 return null을 Optional.empty() 또는 적절한 예외로 전환

## 현재 상태 (As-Is)
- mart-core/infrastructure/persistence/ 하위 14개 Adapter 파일에 return null 산재
- mart-api/controller/MarketRateController.java:131,136 — Controller에서 직접 null 반환
- mart-core/domain/ 프로세서 클래스에서도 null 반환

## 기대 상태 (To-Be)
- Adapter: Optional.ofNullable() 또는 매핑 헬퍼 사용
- Controller: 적절한 HTTP 에러 응답 (404/400)
- Domain processor: 명시적 빈 결과 객체 반환

## 수용 조건 (Acceptance Criteria)
- [ ] rg "return null" account-mart/ --include "*.java" 결과 0건
- [ ] 기존 테스트 전부 통과

## 검증 명령
```powershell
.\gradlew :account-mart:mart-core:test :account-mart:mart-api:test :account-mart:mart-batch:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[reconciliation] return null 14건 제거 — AutomatedMatchingEngine 및 ReconciliationService"
        body = @"
## 목표 (Goal)
reconciliation:core의 return null 14건 제거

## 현재 상태 (As-Is)
- AutomatedMatchingEngine.java:200,205,225,230,253,260,281 — 7건
- ReconciliationService.java:577,581 — 2건
- Command/Request/Policy 클래스 — 5건

## 수용 조건 (Acceptance Criteria)
- [ ] rg "return null" reconciliation/ --include "*.java" 결과 0건
- [ ] .\gradlew :reconciliation:core:test 통과

## 검증 명령
```powershell
.\gradlew :reconciliation:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[expenditure-resolution] DtoAssembler return null 3건 제거"
        body = @"
## 목표 (Goal)
ExpenditureResolutionDtoAssembler.java의 return null 3건 제거 및 DTO 변환 안전화

## 현재 상태 (As-Is)
ExpenditureResolutionDtoAssembler.java:63,72,81 — return null 3건

## 기대 상태 (To-Be)
Optional 변환 또는 예외 처리로 null 반환 방지

## 수용 조건 (Acceptance Criteria)
- [ ] rg "return null" expenditure-resolution/ --include "*.java" 결과 0건

## 검증 명령
```powershell
.\gradlew :expenditure-resolution:api:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[master-data] return null 4건 제거 — DTO 및 도메인 모델"
        body = @"
## 목표 (Goal)
master-data:core 내 return null 4건 제거

## 현재 상태 (As-Is)
- AccountSubjectDto.java:35, DepartmentDto.java:26
- MasterDataChangeRequest.java:192,200

## 수용 조건 (Acceptance Criteria)
- [ ] return null 제거 및 Optional / Exception 전환
- [ ] master-data:core 테스트 통과

## 검증 명령
```powershell
.\gradlew :master-data:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[loan] return null 2건 제거 — DeferredItemType, LoanEvent enum 내 null 반환"
        body = @"
## 목표 (Goal)
DeferredItemType 및 LoanEvent enum 내 return null 2건 제거

## 현재 상태 (As-Is)
- DeferredItemType.java:225 — enum lookup에서 null 반환
- LoanEvent.java:186 — enum lookup에서 null 반환

## 기대 상태 (To-Be)
알 수 없는 enum 값에 대해 IllegalArgumentException throw 또는 Optional 반환

## 수용 조건 (Acceptance Criteria)
- [ ] enum 파싱 시 예외 처리 또는 Optional 적용
- [ ] loan:core 테스트 통과

## 검증 명령
```powershell
.\gradlew :loan:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[internal-audit] return null 1건 제거 + 테스트 전무 해소"
        body = @"
## 목표 (Goal)
internal-audit:core 내 return null 제거 및 단위 테스트 구축

## 현재 상태 (As-Is)
- AuthUserRoleApprovalApplyAdapter.java:101 — return null
- internal-audit:core src=60개이지만 test=0개

## 수용 조건 (Acceptance Criteria)
- [ ] return null 제거
- [ ] internal-audit:core에 핵심 로직 단위 테스트 추가

## 검증 명령
```powershell
.\gradlew :internal-audit:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[gateway] JjwtAccessTokenVerifier return null 제거 — 보안 토큰 검증 경로"
        body = @"
## 목표 (Goal)
보안 관문인 Gateway의 JWT 토큰 검증 로직에서 null 반환 제거

## 현재 상태 (As-Is)
JjwtAccessTokenVerifier.java:115 — 토큰 검증 실패 시 null 반환 (보안 위험)

## 기대 상태 (To-Be)
검증 실패 시 명시적 예외 또는 AuthenticationException throw

## 수용 조건 (Acceptance Criteria)
- [ ] 토큰 검증 실패 시 예외 발생 확인
- [ ] gateway 테스트 통과

## 검증 명령
```powershell
.\gradlew :gateway:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[journal-ledger] JournalRuleEngine return null 제거 — 분개 규칙 엔진 핵심 로직"
        body = @"
## 목표 (Goal)
JournalRuleEngine.java 내 return null 제거 및 룰 매칭 예외 처리 강화

## 현재 상태 (As-Is)
JournalRuleEngine.java:263 — 분개 규칙 매칭 실패 시 null 반환

## 기대 상태 (To-Be)
분개 규칙 미매칭 시 명시적 BusinessException throw

## 수용 조건 (Acceptance Criteria)
- [ ] 분개 규칙 미매칭 시 예외 던짐
- [ ] journal-ledger:core 테스트 통과

## 검증 명령
```powershell
.\gradlew :journal-ledger:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[reporting] InMemoryJournalQueryAdapter UnsupportedOperationException 제거"
        body = @"
## 목표 (Goal)
InMemoryJournalQueryAdapter.java의 UnsupportedOperationException 제거

## 현재 상태 (As-Is)
InMemoryJournalQueryAdapter.java:62 — throw new UnsupportedOperationException("Memory mode does not support single journal lookup")

## 기대 상태 (To-Be)
인메모리 구현체도 단건 조회를 지원하거나, 명시적 커스텀 예외로 전환

## 수용 조건 (Acceptance Criteria)
- [ ] 단건 조회 지원 또는 커스텀 예외 전환
- [ ] reporting:core 테스트 통과

## 검증 명령
```powershell
.\gradlew :reporting:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[ecl-batch, journal-ledger:batch, account-mart] BatchParameterUtils return null 제거"
        body = @"
## 목표 (Goal)
배치 유틸리티 클래스 3종의 return null 제거

## 현재 상태 (As-Is)
- ecl/ecl-batch/.../BatchParameterUtils.java:38 — return null
- journal-ledger/batch/.../BatchDateRangeParameterUtils.java:55 — return null
- account-mart/mart-core/.../BatchParameterUtils.java:34 — return null

## 기대 상태 (To-Be)
파라미터 파싱 실패 시 IllegalArgumentException throw

## 수용 조건 (Acceptance Criteria)
- [ ] 파라미터 유효성 검증 실패 시 IllegalArgumentException
- [ ] 관련 배치 모듈 테스트 통과

## 검증 명령
```powershell
.\gradlew :ecl:ecl-batch:test :journal-ledger:batch:test :account-mart:mart-batch:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[deposit] 헥사고날 구조 미완성 — api/batch 소스 빈약 및 scanBasePackages 미설정"
        body = @"
## 목표 (Goal)
deposit 모듈 헥사고날 스캐폴딩 보완 및 scanBasePackages 설정

## 현재 상태 (As-Is)
- deposit:api — src=2개, test=0개
- deposit:batch — src=3개, test=1개
- DepositApplication에 scanBasePackages 미설정 (@SpringBootApplication만 존재)

## 기대 상태 (To-Be)
- DepositApplication에 scanBasePackages 추가
- api에 최소 Controller + DTO 스캐폴딩
- 각 모듈에 최소 ApplicationContext 테스트

## 수용 조건 (Acceptance Criteria)
- [ ] deposit 모듈 scanBasePackages 추가
- [ ] deposit 모듈 테스트 통과

## 검증 명령
```powershell
.\gradlew :deposit:api:test :deposit:core:test :deposit:batch:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[payable] 패키지가 expenditure 하위에 위치 — Bean 충돌 위험"
        body = @"
## 목표 (Goal)
payable 모듈 패키지 구조 및 scanBasePackages 명확화

## 현재 상태 (As-Is)
PayableApiApplication: scanBasePackages = "com.ho.account.expenditure" — expenditure-resolution과 동일 패키지 스캔하여 Bean 충돌 위험 존재

## 기대 상태 (To-Be)
payable 전용 패키지 com.ho.account.payable로 분리하거나 스캔 범위 명확화

## 수용 조건 (Acceptance Criteria)
- [ ] payable 모듈 독립적 스캔 범위 확보
- [ ] payable:api 및 core 테스트 통과

## 검증 명령
```powershell
.\gradlew :payable:api:compileJava :payable:core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[auth:batch] 단일라인 Application 클래스 — scanBasePackages 및 구조 보완"
        body = @"
## 목표 (Goal)
AuthBatchApplication.java 코드 포맷팅, scanBasePackages 추가 및 컨텍스트 테스트 작성

## 현재 상태 (As-Is)
AuthBatchApplication.java — 한 줄짜리 클래스, scanBasePackages 없음, src=1개, test=0개

## 기대 상태 (To-Be)
scanBasePackages 설정, 가독성 높은 클래스 구조화, ApplicationContext 로드 테스트 작성

## 수용 조건 (Acceptance Criteria)
- [ ] scanBasePackages 추가
- [ ] auth:batch 테스트 통과

## 검증 명령
```powershell
.\gradlew :auth:batch:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[internal-audit] 모듈 README.md 누락"
        body = @"
## 목표 (Goal)
internal-audit 모듈 설명 문서(README.md) 작성

## 현재 상태 (As-Is)
internal-audit/ 디렉터리에 README.md 없음 (다른 모든 비즈니스 모듈은 보유)

## 기대 상태 (To-Be)
- internal-audit/README.md 생성 (모듈 목적, 헥사고날 구조, 실행 방법)
- internal-audit/docs/README.md 생성

## 수용 조건 (Acceptance Criteria)
- [ ] internal-audit/README.md 및 docs/README.md 파일 작성

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[shared-kernel] 테스트 보강 — src=67 vs test=3 (4.5% 비율)"
        body = @"
## 목표 (Goal)
전사 공통 모듈인 shared-kernel 단위 테스트 보강

## 현재 상태 (As-Is)
shared-kernel은 모든 모듈이 의존하는 핵심 라이브러리이나 src 67개 대비 test 3개로 유닛 테스트 매우 부족

## 수용 조건 (Acceptance Criteria)
- [ ] 주요 유틸리티/도메인/BaseEntity 클래스 단위 테스트 추가
- [ ] shared-kernel:test 성공

## 검증 명령
```powershell
.\gradlew :shared-kernel:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[ecl:ecl-core] 금융 계산 정밀도 테스트 확인 — BigDecimal 검증"
        body = @"
## 목표 (Goal)
대손충당금(IFRS9) 산출 코어 엔진의 BigDecimal 반올림/오차 검증 단위 테스트 작성

## 현재 상태 (As-Is)
ecl-core는 IFRS9 핵심 계산 모듈로 src 99개 대비 test 19개 존재. 금융 계산 정밀도 명시적 검증 보강 필요

## 수용 조건 (Acceptance Criteria)
- [ ] Stage 분류, PD/LGD/EAD 계산, ECL 산출 로직 단위 테스트 확인
- [ ] BigDecimal 정밀도 검증 테스트 포함

## 검증 명령
```powershell
.\gradlew :ecl:ecl-core:test --console=plain
```

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[전 모듈] 교육적 Javadoc 주석 일괄 보강"
        body = @"
## 목표 (Goal)
모든 public 클래스와 핵심 메서드에 설계 이유를 포함한 교육용 Javadoc(Pedagogical comment) 보강

## 대상 모듈 (우선순위)
1. shared-kernel (전 모듈 공용)
2. journal-ledger:core (분개 규칙 엔진)
3. closing:core (결산 상태 머신)
4. ecl:ecl-core (IFRS9 계산)
5. loan:core (EIR/상각)

## 수용 조건 (Acceptance Criteria)
- [ ] 대상 모듈 핵심 클래스 및 메서드에 설계 이유 Javadoc 주석 작성

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[master-data:batch] MasterDataBatchApplication 단일라인 클래스 보완"
        body = @"
## 목표 (Goal)
MasterDataBatchApplication.java 클래스 구조화 및 scanBasePackages, @EntityScan 설정

## 현재 상태 (As-Is)
MasterDataBatchApplication.java — 한 줄짜리 파일, scanBasePackages 미설정

## 기대 상태 (To-Be)
적절한 scanBasePackages, @EntityScan, @EnableJpaRepositories 설정 및 배치 Job 테스트 작성

## 수용 조건 (Acceptance Criteria)
- [ ] MasterDataBatchApplication 구조화 및 테스트 추가

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[InternalAuditBatchApplication] 단일라인 클래스 보완"
        body = @"
## 목표 (Goal)
InternalAuditBatchApplication.java 클래스 포맷팅 및 scanBasePackages 설정

## 현재 상태 (As-Is)
InternalAuditBatchApplication.java — 한 줄짜리 파일, scanBasePackages 미설정

## 기대 상태 (To-Be)
가독성 높은 포맷팅 및 scanBasePackages 설정

## 수용 조건 (Acceptance Criteria)
- [ ] InternalAuditBatchApplication 설정 보완

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[config-repo] 설정 리포 문서화 보강 — 모듈별 yml 설명 추가"
        body = @"
## 목표 (Goal)
config-repo 내 모듈별 Spring Configuration 프로파일 문서화

## 현재 상태 (As-Is)
config-repo에 application-dev.yml, application-prod.yml, journal-ledger.yml, master-data.yml 등 존재하나 각 설정의 역할 설명 문서 없음

## 기대 상태 (To-Be)
config-repo/README.md 생성하여 프로파일별/모듈별 yml 역할 명시

## 수용 조건 (Acceptance Criteria)
- [ ] config-repo/README.md 작성

## 롤백 방법
해당 커밋 revert
"@
    },
    @{
        title = "[frontend] 백엔드 API 정합성 전수 확인"
        body = @"
## 목표 (Goal)
프론트엔드 src/services/ 내 API 호출과 백엔드 Controller 매핑 및 DTO 일치 확인

## 현재 상태 (As-Is)
프론트엔드 API 서비스 코드와 백엔드 @RestController 엔드포인트 URL/Method/DTO 구조 검증 필요

## 수용 조건 (Acceptance Criteria)
- [ ] 모든 API 서비스 파일의 엔드포인트가 백엔드 Controller에 존재함을 검증
- [ ] Request/Response DTO 구조 일치 확인

## 롤백 방법
해당 커밋 revert
"@
    }
)

$createdCount = 0
foreach ($item in $issues) {
    Write-Host "Creating Issue: $($item.title)"
    gh issue create --title $item.title --body $item.body
    $createdCount++
    Start-Sleep -Seconds 1
}
Write-Host "Batch Issue Creation Complete! Total created: $createdCount"
