# [ecl] 거시 시나리오 확률 가중치 합계 오류를 그대로 충당금에 반영함

- **Issue ID:** RISK_ECL_03
- **Priority:** P1
- **Module:** ecl
- **Area / category:** core / financial
- **Labels:** module:ecl, area:core, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/calculation/ForwardLookingEclService.java:77
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/calculation/ForwardLookingEclService.java:97
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/calculation/ForwardLookingEclService.java:108
- ecl/ecl-api/src/main/resources/db/migration/V2__align_credit_runtime_schema.sql:17
- ecl/ecl-core/src/test/java/com/ho/account/ecl/core/application/service/calculation/ForwardLookingEclServiceTest.java:34

## Problem statement
미래전망 ECL 서비스는 조회된 각 시나리오의 `probabilityWeight`를 곱해 합산하지만, 각 가중치가 0~1 범위인지와 해당 연도 가중치 총합이 1인지 검사하지 않는다. 시나리오 테이블에도 이 값의 범위 제약이 없다. 불완전하거나 잘못 적재된 모델 데이터가 그대로 `weighted_ecl`과 회계 충당금 summary에 반영된다.

## Reproduction / evidence
한 연도에 `BOOM=0.20`, `BASE=0.50`, `RECESSION=0.20`을 적재하면 총합은 0.90이다. 각 시나리오 ECL이 모두 100.0000일 때 `ForwardLookingEclService.java:97-117`은 90.0000을 정상 반환한다. 가중치 1.10이나 음수도 같은 경로에서 거부하지 않는다. `V2__align_credit_runtime_schema.sql:17-25`는 `NUMERIC NOT NULL`만 설정한다. 기존 테스트는 합계가 정확히 1인 0.2/0.5/0.3만 사용한다(`ForwardLookingEclServiceTest.java:34-53`). 시나리오 자체가 없는 경우의 단일 시나리오 폴백(`ForwardLookingEclService.java:85-90`)과는 별개인 문제다.

## Financial / architectural impact
모델 설정 오류만으로 모든 계좌의 기대신용손실을 체계적으로 과소·과대 평가할 수 있다. 결과가 정상 `COMPLETED` 상태로 집계되면 closing은 잘못된 목표 대손충당금으로 보충 또는 환입 분개를 계산한다. 모델 입력 오류를 계산 시점에 차단하지 않아 감사 가능한 실패 경계가 없다.

## Proposed solution
해당 연도에 시나리오가 존재하면 계산 전에 모든 가중치가 0~1 범위이고 `BigDecimal.compareTo` 기준 총합이 정확히 1인지 검증하고, 위반 시 산출을 실패시킨다. 검증은 캐시 조회와 실시간 조회 모두에 적용하고, 정상 3시나리오 및 0건 폴백 정책은 별도 판단 없이 유지한다. **정확한 수정 allowlist (4파일):** `ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/calculation/ForwardLookingEclService.java`, `ecl/ecl-core/src/test/java/com/ho/account/ecl/core/application/service/calculation/ForwardLookingEclServiceTest.java`, `ecl/ecl-batch/src/test/java/com/ho/account/ecl/batch/AllowanceEclBatchIntegrationTest.java`, `ecl/docs/ALLOWANCE_PROCESS_FLOW.md`.

## Acceptance criteria
- [ ] 시나리오 가중치 합계가 1보다 작거나 크면 계좌 ECL 저장과 summary 확정 전에 배치가 실패한다.
- [ ] 음수·1초과·null 가중치는 명확한 모델 입력 오류로 거부된다.
- [ ] 합계가 1인 정상 시나리오의 기존 ECL 값과, 시나리오 0건일 때의 명시된 폴백은 유지된다.

## Test gap and verification limits
기존 `ForwardLookingEclServiceTest`는 유효한 합계 1의 3개 시나리오만 검증한다. 감사 중 위 RISK_ECL_01에 적은 대상 core 테스트 명령은 통과했다. 수정 후 `./gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --offline --no-daemon --console=plain --max-workers=1`에서 가중치 0.90/1.10/음수/null 및 정상 가중치 회귀를 실행한다. 실제 운영 모델 데이터와 PostgreSQL 배치 산출은 이번 감사에서 검사하지 않았다.
