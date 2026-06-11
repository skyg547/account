# contracts local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`contracts`는 포트와 DTO만 담는 `java-library` 모듈입니다. 서버 포트가 없고 `bootRun` 대상도 아닙니다.

## IntelliJ에서 확인하기

1. 오른쪽 Gradle 창에서 `account > contracts > Tasks > build > compileJava`를 실행합니다.
2. 또는 상단 Run Configuration에서 `Foundation Library Compile`을 선택합니다.
3. 전체 foundation/infra 묶음을 함께 확인하려면 `Foundation Infra Tests`를 사용합니다.

## PowerShell 명령

```powershell
.\gradlew :contracts:compileJava --console=plain --max-workers=1 --no-daemon
```

`contracts`에는 테스트 소스가 없을 수 있으므로 보통 컴파일 성공 여부가 계약 깨짐 여부를 빠르게 보여줍니다.

## 변경할 때 주의할 점

- 기존 필드명을 바꾸면 여러 업무 모듈의 컴파일이 동시에 깨질 수 있습니다.
- 포트는 구현체가 아니라 계약입니다. JPA, RestClient, Kafka 같은 기술 선택은 구현 모듈의 adapter에 둡니다.
- 금액 필드는 가능한 `BigDecimal`을 유지합니다.
- 새 계약을 추가하면 실제 구현 모듈과 호출 모듈의 문서도 함께 갱신합니다.
