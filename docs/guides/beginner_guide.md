# 모던 재무 시스템 입문 가이드

이 문서는 처음 합류한 개발자와 업무 담당자가 저장소의 큰 그림, 모듈 읽는 순서, 로컬 실행 방법을 빠르게 잡도록 돕는 출발점입니다.

## 1. 먼저 이해할 큰 흐름

이 저장소는 회계/재무 업무를 여러 도메인 모듈로 나눈 멀티모듈 Spring Boot 프로젝트입니다.

1. `master-data`, `contracts`, `shared-kernel`이 기준정보와 공통 계약을 제공합니다.
2. `payable`, `receivable`, `loan`, `asset-lease`, `tax` 같은 업무 모듈이 경제적 사건을 만듭니다.
3. `journal-ledger`가 사건을 전표와 GL/SL 원장으로 바꿉니다.
4. `closing`, `reconciliation`, `reporting`이 결산, 대사, 재무보고를 처리합니다.
5. `account-mart`가 ECL 입력 snapshot을 만들고, `ecl`이 IFRS 9 대손충당금을 산출합니다.

## 2. 설계 원칙

- 헥사고날 아키텍처: application layer는 port만 알고, JPA/JDBC/HTTP 같은 기술은 adapter가 담당합니다.
- DDD: 금액 계산, 상태 전이, 검증 규칙은 가능한 한 domain에 둡니다.
- Batch 경계: batch 모듈은 Job/Step/Reader/Writer와 병렬성 설정을 담당하고, 업무 계산은 core에 둡니다.
- 금액 정밀도: 회계 금액과 비율 계산은 `BigDecimal` 중심으로 처리합니다.
- 문서 동기화: 코드가 바뀌면 해당 모듈 `README.md`와 `docs/*.md`를 같이 갱신합니다.

## 3. 모듈 문서 읽는 순서

새 모듈을 볼 때는 아래 순서로 읽으면 됩니다.

1. 모듈 `README.md`: 모듈의 목적과 빠른 실행 방법
2. 모듈 `docs/README.md`: 상세 문서 목록과 추천 읽기 순서
3. `docs/beginner-guide.md` 또는 유사 입문 문서: 업무 용어와 코드 탐색
4. `docs/process-flow.md`: 유즈케이스와 데이터 흐름
5. `docs/schema.md`: 소유 테이블과 외부 참조 방식
6. 코드: `domain` -> `application/port` -> `application/service` -> `adapter` 또는 `infrastructure`

## 4. IntelliJ에서 로컬로 열기

자세한 실행 절차는 [local-development.md](./local-development.md)를 기준으로 합니다.

간단한 순서는 다음과 같습니다.

1. IntelliJ IDEA에서 `build.gradle` 또는 루트 디렉터리를 엽니다.
2. Project SDK를 JDK 17로 맞춥니다.
3. Gradle JVM도 JDK 17로 맞춥니다.
4. Gradle Tool Window에서 `Tasks > verification > test` 또는 특정 모듈 task를 실행합니다.
5. Spring Boot 실행 클래스가 있는 모듈은 Run Configuration을 만들어 실행합니다.

통합 `app` Gradle 프로젝트는 제거되었습니다. 로컬에 `app/build`만 보이면 과거 빌드의 무시된 산출물이며 실행 모듈이 아닙니다. `auth`, `master-data`, `journal-ledger:api`, `account-mart:mart-api`, `account-mart:mart-batch`, `ecl:ecl-api`처럼 실제 `@SpringBootApplication`이 있는 모듈을 개별 실행합니다.

## 5. 자주 쓰는 Gradle 명령

Windows PowerShell 기준입니다.

```powershell
.\gradlew projects --console=plain
.\gradlew :account-mart:mart-core:test --console=plain
.\gradlew :account-mart:mart-api:bootRun --console=plain
.\gradlew :account-mart:mart-batch:test --console=plain
```

대규모 통합 검증은 오래 걸릴 수 있으므로 변경 모듈의 test/compileJava를 먼저 실행하고, 마지막에 연관 모듈을 넓혀 검증합니다.

## 6. 변경 전후 체크리스트

변경 전:
- 관련 모듈 README와 docs를 읽었는가?
- 이 변경이 domain, application, adapter 중 어디에 속하는지 정했는가?
- API/배치/DB 스키마 영향을 같이 확인했는가?

변경 후:
- 관련 테스트를 실행했는가?
- README/docs/WORKLOG가 실제 변경과 일치하는가?
- 남은 리스크를 문서에 남겼는가?
- Java 소스의 임시 `@todo`가 실제 후속 작업인지 확인했는가?

## 7. 아카이브 안내

이전 `docs/beginner_guide.md`에는 유효한 입문 내용 뒤에 깨진 레거시 App Schema 조각이 섞여 있었습니다. 원문은 삭제하지 않고 [archive/beginner_guide_legacy_corrupt_2026-06-10.md](./archive/beginner_guide_legacy_corrupt_2026-06-10.md)에 보존했습니다. 실행에 필요한 app/profile 정보는 이 문서와 [local-development.md](./local-development.md)에 복원했습니다.
