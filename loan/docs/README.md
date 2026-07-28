# Loan 문서 인덱스

`loan`은 대출 계약 생성·실행, 이연 항목, EIR 상각 스케줄, 상태 이벤트, 조건 변경 재계산, 이자 발생 전표화를 담당합니다.

1. [beginner-guide.md](beginner-guide.md): 용어와 상태 전이
2. [process-flow.md](process-flow.md): API·core·Batch·전표 호출 순서
3. [schema.md](schema.md): 테이블, 값 참조, 마이그레이션
4. [local-run.md](local-run.md): 테스트, API, Batch, Docker 실행

## 코드 지도

| 관심사 | 위치 |
| --- | --- |
| REST/DTO/예외 변환 | `loan:api` |
| 인바운드 포트 | `core/application/port/in/LoanUseCase` |
| 유즈케이스 | `core/service/LoanService`, `InterestAccrualService` |
| Batch 대량 변환 | `core/application/pipeline/LoanInterestAccrualPipeline` |
| 업무 규칙 | `core/domain` |
| 출력 포트 | `core/application/port/out` |
| JPA/Master/Journal 어댑터 | `core/infrastructure` |
| Job/Step/Reader/Writer 구성 | `loan:batch` |

API DTO는 API 경계에만 있고 core는 웹·DTO 타입을 알지 않습니다. Batch는 paging/chunk 흐름만 구성하며 대출별 반복과 실패 집계는 core 파이프라인이 수행합니다.

## 빠른 검증

```powershell
.\gradlew :loan:core:test :loan:api:bootJar :loan:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

기존 문서의 인코딩 손상 원문은 [archive/README_legacy_corrupt_2026-06-10.md](archive/README_legacy_corrupt_2026-06-10.md)에 보존되어 있습니다.
