# ECL 대손충당금 문서 안내

이 디렉터리는 `ecl` 모듈의 IFRS 9 대손충당금 산출 문서를 한곳에서 찾기 위한 진입점입니다.
루트 [README](../README.md)는 모듈 역할과 실행 예시만 요약하고, 상세 개념과 업무·데이터 흐름은 이곳에서 관리합니다.

## 처음 보는 개발자의 읽기 순서

1. [ALLOWANCE_BEGINNER_GUIDE.md](ALLOWANCE_BEGINNER_GUIDE.md): 회계·신용위험 개념과 코드 위치를 익힙니다.
2. [ALLOWANCE_PROCESS_FLOW.md](ALLOWANCE_PROCESS_FLOW.md): 기준일 snapshot부터 결산 summary까지 업무 흐름을 확인합니다.
3. [ALLOWANCE_ARCHITECTURE.md](ALLOWANCE_ARCHITECTURE.md): DDD/헥사고날 레이어와 의존 방향을 확인합니다.
4. [BATCH_EXECUTION_FLOW.md](BATCH_EXECUTION_FLOW.md): 실제 Job/Step과 클래스 호출 순서를 추적합니다.
5. [ALLOWANCE_DATA_MODEL_SPEC.md](ALLOWANCE_DATA_MODEL_SPEC.md): 입력·모델 파라미터·산출 테이블을 확인합니다.
6. [ALLOWANCE_SERVICE_RUNBOOK.md](ALLOWANCE_SERVICE_RUNBOOK.md): 로컬 실행, 재실행, 장애 확인 절차를 따릅니다.

## 문서별 책임

| 문서 | 책임 | 중복 방지 기준 |
| --- | --- | --- |
| `ALLOWANCE_BEGINNER_GUIDE.md` | 초보자 개념, 코드 탐색 순서, 검증 질문 | 상세 실행 명령은 런북에 둡니다. |
| `ALLOWANCE_CONCEPTS.md` | PD/LGD/EAD/ECL 용어 사전 | 프로세스 설명은 넣지 않습니다. |
| `ALLOWANCE_PROCESS_FLOW.md` | 업무 단계와 단계별 입출력 | 클래스 단위 호출은 `BATCH_EXECUTION_FLOW.md`에 둡니다. |
| `ALLOWANCE_ARCHITECTURE.md` | 레이어, 포트/어댑터, 배포 구조 | 운영 명령은 런북에 둡니다. |
| `BATCH_EXECUTION_FLOW.md` | Job/Step 및 구현 클래스 추적 | 업무 개념 설명은 입문 가이드로 연결합니다. |
| `ALLOWANCE_DATA_MODEL_SPEC.md` | 테이블 소유권과 핵심 컬럼 | SQL 실행 절차는 런북에 둡니다. |
| `ALLOWANCE_SERVICE_RUNBOOK.md` | 실행·재실행·장애 대응 | 설계 설명은 아키텍처 문서로 연결합니다. |

`process-flow.md`, `BATCH_EXECUTION_GUIDE.md`, `SERVICE_ONBOARDING.md`는 기존 링크 호환과 빠른 탐색을 위해 유지합니다.
새 설명을 추가할 때는 위 표의 대표 문서에 먼저 기록하고, 호환 문서에는 링크와 짧은 요약만 둡니다.

## 변경 시 함께 확인할 항목

- 계산식 변경: `BigDecimal` 정밀도, 0~1 비율 검증, `EadCalculationResult` 필드 의미를 확인합니다.
- 모델 정책 변경: `allowance_model_parameters`와 `AllowanceModelParams`를 함께 확인합니다.
- 배치 변경: `baseDate`, `runId`, `modelVersion`, 재실행 멱등성을 확인합니다.
- summary 변경: 계정 매핑 누락 시 기존 `allowance_summary`가 보존되는지 확인합니다.
- 외부 연동 변경: `account-mart -> ecl -> closing` 데이터 계약을 함께 확인합니다.

## 영속성 경계

- 모델 파라미터, 등급, 상품, LGD 세그먼트, 거시시나리오, 전이행렬, 계좌 담보 배분 포트는 기술 독립 인터페이스입니다.
- Spring Data JPA 저장소와 캐시 어노테이션은 `infrastructure/adapter/persistence` 아래 구현체에 둡니다.
- 새 마스터 저장소를 추가할 때도 application port에는 업무 조회·저장 계약만 선언하고, JPA 파생 쿼리와 캐시는 어댑터에 둡니다.
