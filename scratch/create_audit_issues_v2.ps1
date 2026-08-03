$issues = @(
    "[p0] master-data:api Application 클래스 누락 — api 모듈 부트 불가",
    "[p0] internal-audit:api Application 클래스 및 소스 누락",
    "[p0] 18개 모듈 테스트 파일 0건 — 최소 ApplicationContext 로드 테스트 추가",
    "[p1] account-mart return null 40건+ 제거 — Optional 또는 예외 전환",
    "[p1] reconciliation return null 14건 제거 — AutomatedMatchingEngine",
    "[p1] expenditure-resolution DtoAssembler return null 3건 제거",
    "[p1] master-data return null 4건 제거 — DTO 및 도메인 모델",
    "[p1] loan return null 2건 제거 — DeferredItemType LoanEvent enum",
    "[p1] internal-audit return null 1건 제거 + 테스트 전무 해소",
    "[p1] gateway JjwtAccessTokenVerifier return null 제거 — 보안 토큰 검증",
    "[p1] journal-ledger JournalRuleEngine return null 제거",
    "[p1] reporting InMemoryJournalQueryAdapter UnsupportedOperationException 제거",
    "[p1] ecl-batch journal-ledger:batch account-mart BatchParameterUtils return null 제거",
    "[p1] deposit 헥사고날 구조 미완성 — scanBasePackages 미설정",
    "[p1] payable 패키지가 expenditure 하위에 위치 — Bean 충돌 위험",
    "[p1] auth:batch 단일라인 Application 클래스 — scanBasePackages 보완",
    "[p2] internal-audit 모듈 README.md 누락",
    "[p2] shared-kernel 테스트 보강 — src=67 vs test=3",
    "[p2] ecl:ecl-core 금융 계산 정밀도 테스트 확인 — BigDecimal 검증",
    "[p2] 전 모듈 교육적 Javadoc 주석 일괄 보강",
    "[p2] master-data:batch MasterDataBatchApplication 단일라인 클래스 보완",
    "[p2] InternalAuditBatchApplication 단일라인 클래스 보완",
    "[p2] config-repo 설정 리포 문서화 보강",
    "[p2] frontend 백엔드 API 정합성 전수 확인"
)

$created = 0
foreach ($title in $issues) {
    gh issue create --title $title --body "전수 조사 결과 이슈. 상세 내용은 audit_report.md 참조. 하네스 원칙(10-rules.md, 80-file-ownership.md, 85-github-issue-agent-loop.md) 기반 검사."
    $created++
    Write-Host "Created ($created/$($issues.Count)): $title"
}
Write-Host "Done! Created $created issues."
