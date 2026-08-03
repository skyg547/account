# tax local run

## 실행 프로파일 계약

- 기본 `local` 프로파일은 H2 PostgreSQL mode, 전용 Flyway V1, Hibernate `validate`를 사용한다.
- `dev`/`prod`는 PostgreSQL 15+ 전용이며 런타임 Flyway/DDL/SQL init/Batch metadata init을 금지한다. 배포 전에 서버 버전이 15 이상인지 확인하고 `migration-runner --context=tax`를 실행한다.
- `prod`는 `sslmode=verify-full`을 강제하고 DB 비밀번호는 환경 변수로만 주입한다.
- 실제 PostgreSQL 검증은 승인 환경에서 별도로 필요하다. Master Data 원격 어댑터는 Issue #266에서 구현하므로 그 전까지 완전한 dev/prod 업무 플로우는 준비되지 않았다.

개발 환경은 `DEV_DB_HOST`, `DEV_DB_PORT`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD`, 운영은 대응하는 `PROD_DB_*` 변수를 secret injection으로 제공하고 `--spring.profiles.active=dev|prod`로 시작한다. 사설 호스트나 자격증명을 저장소에 기록하지 않는다.

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 저장소 루트를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`tax`는 이제 `core/api/batch` 하위 Gradle 모듈로 분리되어 있다.

- `tax:core`: 세금계산서 도메인, `TaxInvoiceCommand`/UseCase, persistence adapter, local master-data adapter. HTTP DTO와 Controller는 두지 않는다.
- `tax:api`: Spring Boot HTTP 실행 진입점. 요청 DTO와 Controller를 갖고, 금액 검증과 취소 정책은 core를 참조한다.
- `tax:batch`: Spring Boot Batch 실행 진입점. 신고/대량 검증 Job/Step 제어만 담당하고 세무 규칙은 core를 참조한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > tax > core > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Tax Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.
5. HTTP API를 직접 띄울 때는 Gradle task `:tax:api:bootRun`을 실행한다.
6. Batch 컨텍스트만 확인할 때는 Gradle task `:tax:batch:bootRun`을 실행한다.

추가된 실행 구성:

- `.run/Tax Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :tax:core:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :tax:core:compileJava :tax:api:compileJava :tax:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

API 서버 실행:

```powershell
.\gradlew :tax:api:bootRun --console=plain --max-workers=1
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :tax:batch:bootRun --console=plain --max-workers=1
```

실행 JAR 생성 및 직접 실행:

```powershell
.\gradlew :tax:api:bootJar :tax:batch:bootJar --console=plain --max-workers=1
java -jar tax\api\build\libs\account-tax-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -jar tax\batch\build\libs\account-tax-batch-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```


Batch 컨텍스트는 `TaxBatchJobRegistryConfiguration`으로 Spring Batch Job 등록 시점을 늦춘다. 이 설정은 `jobRegistryBeanPostProcessor` 조기 초기화 경고를 막기 위한 인프라 설정이며, 세금계산서 검증 업무 로직은 포함하지 않는다.

실제 Batch Job 실행:

```powershell
.\gradlew :tax:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=taxInvoiceValidationJob startDate=2026-06-19 endDate=2026-06-19" --console=plain --max-workers=1
```

`taxInvoiceValidationJob`은 지정 기간의 PURCHASE 세금계산서 금액 정합성과 거래처 참조를 core 서비스에서 검증한다.

API 스모크만 확인하고 서버를 계속 띄우지 않을 때:

```powershell
.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1
```

## 예시 요청

```http
POST /api/ap/invoices
Content-Type: application/json
```

```json
{
  "issueId": "TAX-2026-0001",
  "type": "PURCHASE",
  "issueDate": "2026-06-01",
  "businessPartnerCode": "BP001",
  "supplyAmount": 10000,
  "taxAmount": 1000,
  "totalAmount": 11000
}
```

취소 요청:

```http
DELETE /api/ap/invoices/1?reason=wrong-issue
X-User-ID: local-user
```

## 자주 보는 실패

| 증상 | 확인할 지점 |
| --- | --- |
| `AP Invoice는 PURCHASE 타입만 생성할 수 있습니다.` | 요청 `type`이 `PURCHASE`인지 확인 |
| `거래처를 찾을 수 없습니다` | master-data 테스트 더블 또는 호스트 앱 데이터에 거래처 코드가 있는지 확인 |
| 공급가액과 세액의 합이 맞지 않음 | `supplyAmount + taxAmount`가 `totalAmount`와 같은지 확인 |
| 취소 실행자와 사유 필수 오류 | `X-User-ID` 헤더와 `reason` 파라미터가 비어 있지 않은지 확인 |
