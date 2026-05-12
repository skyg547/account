# 전사 문서(docs) 통합 및 최신화 트래커

**작업 원칙:**
1. **무분별한 삭제 지양:** 기존 문서를 삭제하기보다는 내용을 통합하고, 중복된 것을 단일 소스(Source of Truth)로 엮어냅니다.
2. **최신 아키텍처 반영:** 최근 완료된 헥사고날 아키텍처 전환, ID 기반 참조(SCD2), Docker 컨테이너라이제이션 내용을 각 문서에 반영합니다.
3. **초보자 친화적 가이드:** 각 모듈의 목적과 흐름을 쉽게 이해할 수 있도록 비유와 개념 설명을 추가합니다.

---

## 📋 단계별 진행 상황 (Status: `[o]` 완료, `[ ]` 대기)

### Phase 1: 코어 및 계약 계층 (Core & Contracts)
- [o] `shared-kernel`
- [o] `contracts`

### Phase 2: 인프라 및 네트워크 계층 (Infra & Network)
- [o] `config-repo`
- [o] `config-server`
- [o] `discovery`
- [o] `gateway`
- [o] `auth`
- [o] 서드파티 인프라 통합 확인 (`elasticsearch`, `grafana`, `kafka`, `kibana`, `logstash`, `prometheus`, `redis`, `vault`, `zipkin`)

### Phase 3: 기준 정보 및 권한 (Master Data & Governance)
- [o] `master-data`
- [o] `governance`

### Phase 4: 핵심 회계 엔진 (Core Accounting)
- [o] `journal-ledger`
- [o] `closing`
- [o] `reconciliation`
- [o] `reporting`

### Phase 5: 업무 서브레저 (Subledgers)
- [o] `expenditure-resolution`
- [o] `payable`
- [o] `receivable`
- [o] `tax`
- [o] `asset-lease`
- [o] `loan`

### Phase 6: 개별 모듈 내부 문서 고도화 (Module Internal Docs Enhancement)
- [o] 개별 모듈 하위의 `docs/` 폴더 파일들(`beginner-guide.md`, `schema.md` 등) 삭제 취소(복구) 및 내용 고도화
- [o] 헥사고날 아키텍처, ID 참조 방식 등 최신 아키텍처 및 도메인 지식을 초보자 눈높이에 맞춰 친절히 서술

### Phase 7: 최상단 허브 문서 (Root `docs/`)
- [o] 루트 `docs/` 하위 마크다운 파일 전수 조사 및 구조화 (인덱싱, 중복 제거, 목차 최신화)

---

## 📝 작업 로그
* 2026-05-12: 문서 최신화 계획 수립 및 트래커 생성 완료.
* 2026-05-12: (수정) 사용자의 요청으로 각 모듈별 `docs/` 내부 파일(beginner-guide 등) 삭제를 취소하고, 이를 보존하며 최신화/고도화하는 방향(Phase 6 추가)으로 계획 변경.
