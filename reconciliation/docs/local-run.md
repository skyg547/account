# reconciliation local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`reconciliation`은 현재 독립 실행 애플리케이션이 아니므로 `bootRun`으로 바로 띄우는 모듈이 아니다. 모듈 단위에서는 테스트와 컴파일로 업무 흐름을 검증한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > reconciliation > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Reconciliation Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.

추가된 실행 구성:

- `.run/Reconciliation Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :reconciliation:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :reconciliation:compileJava --console=plain --max-workers=1 --no-daemon
```

## HTTP API를 로컬에서 호출하려면

현재 reconciliation 모듈에는 `SpringBootApplication`이 없다. 따라서 아래 중 하나가 필요하다.

1. 기존 호스트 애플리케이션이 reconciliation 패키지를 컴포넌트 스캔하도록 구성한다.
2. 테스트 전용 또는 통합 실행용 Boot 앱 모듈을 별도로 만든다.
3. 컨트롤러 테스트를 추가해 `@WebMvcTest` 또는 `@SpringBootTest`로 웹 계약을 검증한다.

## 테스트 데이터 주의사항

대사 실행에는 최소한 다음 데이터가 필요하다.

- `ReconciliationUnit`과 `criteriaJson`
- `ReconciliationRule`
- `DifferenceReasonCode` 중 `GENERIC_MISMATCH`
- `RECON_EXTERNAL_STAGE_RECORD` 원천 집계 데이터
- `JournalQueryPort`가 반환할 대상 원장 집계

조정 전표 자동 생성까지 확인하려면 `JournalPostingPort` 테스트 더블 또는 journal-ledger 통합 구성이 필요하다.
