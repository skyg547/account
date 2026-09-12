# 🛡️ Internal Audit (내부 감사 모듈 문서)

이 디렉터리는 내부통제 및 감사(RCM, 설계/운영 평가, 결함 추적)를 전담하는 `internal-audit` 모듈의 상세 기술 문서 모음입니다.

---

## 📚 문서 목록

1. **[process-flow.md](./process-flow.md)**: RCM 통제 활동 등록부터 설계 평가(Design Evaluation), 운영 평가(Operating Evaluation), 결함(Deficiency) 조치까지의 핵심 업무 프로세스 흐름.
2. **[schema.md](./schema.md)**: 소유 테이블 스키마 구조 (Flyway V60–V62) 및 엔티티 매핑 명세.
3. **[issue-665-command-idempotency-plan.md](./issue-665-command-idempotency-plan.md)**: 명령 재시도·legacy key·감사 원자성 계약과 격리 PostgreSQL 검증 절차.

---

## 🚀 빠른 시작

로컬 개발 환경에서의 독립 실행 및 테스트 방법은 상위 [README.md](../README.md)를 참고하세요.