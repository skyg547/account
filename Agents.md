# Repo Agent Guide

## Scope
이 문서는 Codex 작업에만 적용되는 작업 가이드다.

## Codex Only Rule
- 본 문서(`Agents.md`)의 규칙은 Codex에만 적용한다.
- Gemini 및 기타 모델은 각 전용 설정 파일을 따른다.
- Codex는 구현보다 검수/리뷰 요청을 받았을 때 `WORKLOG.md`에 검수 결과를 남기고, Codex 자체 작업 이력은 `CODEX_WORKLOG.md`에 분리 기록한다.

## Agent Config Separation
- Codex는 루트 `Agents.md`를 기본 설정 파일로 사용한다.
- Gemini는 루트 `GEMINI.md`를 기본 설정 파일로 사용한다.
- 공통 원칙은 두 문서에 맞춰 동기화하되, 모델별 역할 지시는 각 전용 파일에만 기록한다.

## Working Architecture Rules
- **Layer Separation**: Hexagonal Architecture(Port/Adapter) 원칙을 엄격히 준수한다.
- **Batch Module (Orchestrator)**: 
    - 순수 고수준 오케스트레이터 역할에 집중한다.
    - **비즈니스 연산 로직(if/for/math) 구현을 금지**한다.
    - 1억 건 이상의 대용량 처리를 위한 `Trigger`, `Job/Step Flow Control`, `TaskExecutor(병렬성)`, `Chunk Size` 설정만 담당한다.
- **Core Module (Business Core)**:
    - `application.pipeline`: **Batch 전용 대용량 변환기**. Chunk 단위로 유입되는 대량 데이터를 도메인 규칙에 따라 일괄 변환/가공하는 로직을 담당한다.
    - `application.service`: **유즈케이스 흐름 제어**. 트랜잭션 경계 설정 및 여러 도메인 서비스/컴포넌트 간의 협업을 조정한다.
    - `application.port.out`: **기술 독립적 외부 인터페이스**. 영속성 계층이나 외부 시스템의 구체적 기술(JPA, JDBC, OpenFeign)을 도메인/애플리케이션 레이어로부터 은닉한다.
    - `domain`: **핵심 비즈니스 규칙**. `BigDecimal` 중심의 정밀한 물리 계산 수식 및 엔티티(Entity)의 유효성/상태 변경 행위를 포함한다. (Rich Domain Model 지향)
    - `infrastructure`: **기술적 구현체 (Adapter)**. `port.out` 인터페이스를 상속받아 JPA(QueryDSL), 고성능 JDBC Bulk SQL, 외부 API 연동 등을 실제 구현한다.

- **Anti-Skeleton Policy**: 
    - 인터페이스만 있고 구현이 없는 '깡통 코딩'을 엄격히 금지한다.
    - 단순 위임(Service -> Repository)만 하는 '단순 전달형' 코드를 지양하고 서비스 레벨의 정합성 검증이나 유의미한 도메인 협업을 포함한다.
- **High Performance**: 1억 건 이상의 데이터 처리를 위해 어댑터 레벨에서 `JDBC Bulk Insert/Update` 및 `Partitioning` 적용을 기본으로 한다.

## Working Rules
- 작업 전 변경 대상 모듈의 `README.md`와 `docs/*.md`를 먼저 확인한다.
- 요청 범위를 벗어난 리팩터링은 하지 않는다.
- 계산 및 금액 로직은 가능하면 `BigDecimal` 중심으로 유지한다.
- API 변경 시 컨트롤러, DTO, 서비스, 테스트 영향 범위를 함께 확인한다.
- 배치 변경 시 파라미터, 재실행 가능성, 정합성 검증 포인트를 함께 점검한다.
- 프론트 변경 시 모바일 레이아웃과 기존 데이터 흐름 유지 여부를 확인한다.

## Codex Worklog Review Role
- Codex는 작업 시작 전에 루트 `WORKLOG.md` 최신 항목을 확인한다.
- Codex는 작업 종료 시 실제 변경 내용과 워크로그 기록(범위, 테스트, 리스크)의 일치 여부를 검수/리뷰한다.
- 불일치나 누락이 있으면 해당 항목을 명확히 지적하고 보완 제안을 남긴다.

## Verification
- 가능하면 변경 범위에 맞는 테스트를 우선 실행한다.
- 테스트를 실행하지 못했으면 이유를 명확히 남긴다.
- 설명에는 변경 결과, 검증 여부, 남은 리스크를 포함한다.

## Safety
- 사용자가 명시하지 않은 `git push`, 파괴적 삭제, 대규모 포맷 변경은 하지 않는다.
- 기존 문서를 대체하기보다 보강하는 방향을 우선한다.
- 다른 모듈의 미해결 변경사항은 임의로 되돌리지 않는다.

## Documentation
- 의미 있는 구조 변경이 있으면 관련 `README.md` 또는 `docs/*.md`를 갱신한다.
- 초보자가 따라갈 수 있게 실행 순서와 전제 조건을 짧게 적는다.
