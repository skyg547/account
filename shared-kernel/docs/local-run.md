# Shared-Kernel 로컬 검증

## 전제 조건

- JDK 17
- IntelliJ Gradle JVM 17
- 저장소 루트를 Gradle 프로젝트로 열기

`shared-kernel`은 `java-library`라서 `bootRun`, 내장 WAS, H2, PostgreSQL, Docker로 실행하지
않습니다.

## IntelliJ

1. `Foundation Library Compile` Run Configuration을 실행합니다.
2. 또는 Gradle 창에서 `shared-kernel > verification > test`를 실행합니다.
3. 공개 enum/타입을 바꿨다면 account-mart와 ECL 테스트까지 확인합니다.
4. 마스킹 변경 시 governance/master-data JSON 응답 테스트도 확인합니다.

## Gradle

집중 검증:

```powershell
.gradlew :shared-kernel:test :contracts:test --console=plain --max-workers=1 --no-daemon
```

영향 모듈 포함:

```powershell
.gradlew :shared-kernel:test :contracts:test :master-data:core:test :master-data:api:test :master-data:batch:test :closing:core:test :journal-ledger:core:test :account-mart:mart-core:test :ecl:ecl-core:test --console=plain --max-workers=1 --no-daemon
```

## 테스트가 확인하는 내용

- 마스킹 형식과 미지원 패턴 fail-closed
- 로컬 서비스 목록의 불변성/결정적 순서
- capability/context/type 필터
- 중복/공백 서비스 이름 fail-fast
- contracts 전표 명령 불변성 및 필수값
- Master Data 기준일 SCD2 조회 소비 흐름

## 메모리 부족 시

Gradle이 테스트 보고서를 만들기 전에 멈추면 성공으로 기록하지 않습니다.

```powershell
jps -l
.gradlew --stop
```

현재 작업 환경에서는 Windows 페이지 파일 부족이 반복되고 있으므로 최신 테스트 보고서 시각과
소스 시각을 비교하고, 자원이 회복된 뒤 같은 명령을 재실행해야 합니다.