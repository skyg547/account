# 📚 Repository Docs Hub (전사 문서 허브)

이 디렉터리는 시스템 전체의 구조, 6단계 비즈니스 파이프라인, 그리고 개발 가이드를 파악하기 위한 중앙 문서 저장소입니다.

새로 개편된 문서 체계에 따라 **전역 시스템 문서(`docs/`)**와 **개별 모듈 문서(`<module>/docs/`)**의 역할이 명확히 분리되어 있습니다.

---

## 1. 🐣 처음 오신 분들을 위한 입문 가이드 (Getting Started)

처음 합류하신 개발자나 기획자분들은 아래 순서대로 문서를 확인해 주세요.

1. **[master-domain-glossary.md](guides/master-domain-glossary.md) (⭐ 필수):** 초보자를 위한 쉽게 배우는 마스터 도메인 용어집 (전표, 마감, 대사, IFRS 9 ECL 등을 쉬운 비유로 설명).
2. **[beginner_guide.md](guides/beginner_guide.md):** 프로젝트 통합 가이드. 시스템의 큰 그림과 전체 비즈니스 파이프라인을 다룹니다.
3. **[local-development.md](guides/local-development.md):** IntelliJ IDEA, JDK 21, Gradle, `local` H2 프로파일 실행 방법 가이드.
4. **[development-compose.md](guides/development-compose.md):** Docker/Podman Compose 기반 개발 환경 및 36개 실행 target 연동 절차.
5. **[frontend-runtime-guide.md](guides/frontend-runtime-guide.md):** 프론트엔드를 로컬/개발/운영 프로파일별로 실행·배포하는 초보자 가이드 (루트 Compose 사용법 포함).
6. **[infrastructure_runbook.md](guides/infrastructure_runbook.md):** Gateway, Eureka, Kafka, Config-Server 등 인프라 아키텍처 가이드.

---

## 2. 🏛️ 전역 아키텍처 및 설계 원칙 (`docs/architecture/`)

시스템의 설계 원칙과 MSA 구조에 대해 심도 있게 설명합니다.

- **[architecture.md](architecture/architecture.md):** 백엔드(헥사고날/MSA) 및 프론트엔드(Next.js) 통합 아키텍처 명세서.
- **[business_workflow.md](architecture/business_workflow.md):** 지출결의부터 전표 생성, 결산 마감까지 실제 돈이 어떻게 흘러가는지 업무 시나리오.
- **[domain-catalog.md](architecture/domain-catalog.md):** 전사 도메인 카탈로그 및 20+ 마이크로서비스 맵.
- **[principles_and_policies.md](architecture/principles_and_policies.md):** 헥사고날 아키텍처 원칙, SCD2 이력 관리, `BigDecimal` 정밀도 전사 개발 정책.
- **[msa_roadmap.md](architecture/msa_roadmap.md):** MSA 전환 및 도메인 분리 로드맵.

---

## 3. 🛠️ 개발 및 인프라 운영 가이드 (`docs/guides/`)

- **[master-domain-glossary.md](guides/master-domain-glossary.md):** 비유 중심 회계/재무/기술 마스터 용어집.
- **[local-development.md](guides/local-development.md):** 로컬 환경 구축 및 `local` 프로파일 독립 H2 실행 가이드.
- **[development-compose.md](guides/development-compose.md):** 개발용 Compose (`account-dev-network`) 구성.
- **[frontend-runtime-guide.md](guides/frontend-runtime-guide.md):** 프론트엔드 로컬/개발/운영 실행 및 배포 가이드.
- **[development-postgresql.md](guides/development-postgresql.md):** PostgreSQL 개발 DB 스키마 및 계정 권한 정책.
- **[production-compose.md](guides/production-compose.md):** 프로덕션 Compose 및 보안 환경변수 검증.
- **[container-images.md](guides/container-images.md):** OCI 컨테이너 이미지 (Java 21 / Node 20) 빌드 규격.
- **[infrastructure_runbook.md](guides/infrastructure_runbook.md):** 인프라 구동 및 서비스 디스커버리 런북.
- **[runtime-execution-matrix.md](guides/runtime-execution-matrix.md):** 모듈별 실행 검증 매트릭스.

---

## 4. 📦 모듈별 상세 문서 (`<module>/README.md`)

개별 모듈의 헥사고날 아키텍처 포트/어댑터 구조, 데이터 스키마(ERD), 단독 테스트 및 `bootRun` 방법은 해당 모듈 디렉터리의 `README.md`를 열어보세요.

- **전표/원장 엔진**: [journal-ledger/README.md](../journal-ledger/README.md)
- **결산 통제**: [closing/README.md](../closing/README.md)
- **대출 서비스**: [loan/README.md](../loan/README.md)
- **예금 서비스**: [deposit/README.md](../deposit/README.md)
- **자산/리스**: [asset-lease/README.md](../asset-lease/README.md)
- **매입채무**: [payable/README.md](../payable/README.md)
- **매출채권**: [receivable/README.md](../receivable/README.md)
- **세무 서비스**: [tax/README.md](../tax/README.md)
- **예산 서비스**: [budget/README.md](../budget/README.md)
- **지출결의**: [expenditure-resolution/README.md](../expenditure-resolution/README.md)
- **보고서 엔진**: [reporting/README.md](../reporting/README.md)
- **재무 마트**: [account-mart/README.md](../account-mart/README.md)
- **IFRS9 대손**: [ecl/README.md](../ecl/README.md)
- **공통 커널/계약**: [shared-kernel/README.md](../shared-kernel/README.md), [contracts/README.md](../contracts/README.md)

---

## 5. 🤖 AI 에이전트 하네스 (`docs/ai-harness/`)

- **[10-rules.md](ai-harness/10-rules.md):** 공통 에이전트 규칙 및 클린 루트 디렉터리 정책.
- **[worklog.md](ai-harness/worklog.md):** 중앙 공유 통합 작업 기록.
- **[agent-status.md](ai-harness/agent-status.md):** 서브에이전트 진행 상태 표.
- **[history/](history/):** 에이전트 과거 작업 기록 아카이브.
