# Config Server local run

## 전제 조건

- JDK 17
- IntelliJ Gradle JVM 17
- 저장소 루트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기
- `config-repo/master-data.yml` 존재
- 8888 포트가 비어 있음
- 승인된 secret provider 또는 실행 프로세스가 생성한 nonblank `ENCRYPT_KEY`

이 모듈은 DB를 사용하지 않으므로 H2/PostgreSQL을 실행하지 않습니다.

## IntelliJ 실행

1. Gradle 동기화를 완료합니다.
2. Run Configuration에서 `Config Server bootRun`을 선택합니다.
3. 실행합니다.
4. 로그에서 8888 포트 시작을 확인합니다.
5. readiness와 설정 조회 HTTP 상태만 확인합니다.

실행 설정에는 다음 정책이 들어 있습니다.

```text
profile=native
port=8888
search-locations=file:./config-repo
--no-daemon
```

## Gradle 테스트와 패키징

```powershell
.\gradlew :config-server:test :config-server:bootJar --console=plain --max-workers=1 --no-daemon
```

테스트는 native HTTP 조회, strict repository readiness, YAML/Compose/Docker/IntelliJ 정책을 검증합니다.

## Gradle로 서버 실행

반드시 저장소 루트에서 실행합니다.
키는 저장소·명령행·Run Configuration에 기록하지 않고 환경변수로만 전달합니다.

```powershell
$keyBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)
$env:ENCRYPT_KEY = [Convert]::ToBase64String($keyBytes)
try {
    .\gradlew :config-server:bootRun --args="--spring.profiles.active=native --server.port=8888 --spring.cloud.config.server.native.search-locations=file:./config-repo" --console=plain --no-daemon
} finally {
    Remove-Item Env:ENCRYPT_KEY -ErrorAction SilentlyContinue
}
```

다른 경로를 사용해야 하면 승인된 절대 경로를 환경변수로 전달합니다.

```powershell
$env:CONFIG_REPO_LOCATION="file:C:/approved/config-repo"
.\gradlew :config-server:bootRun --console=plain --no-daemon
```

## 안전한 smoke 확인

```powershell
(Invoke-WebRequest http://localhost:8888/actuator/health/readiness).StatusCode
(Invoke-WebRequest http://localhost:8888/master-data/default).StatusCode
(Invoke-WebRequest http://localhost:8888/actuator/prometheus).StatusCode
```

예상 결과는 모두 HTTP 200이고 readiness 본문의 `status`가 `UP`인 것입니다. Config 응답 본문에는 내부 설정이 포함될 수 있으므로 터미널 공유, 이슈, 메신저에 붙이지 않습니다.

## Docker Compose

Docker CLI와 공용 네트워크가 준비된 환경에서 모듈만 실행합니다.

```powershell
docker network create account-network
docker compose -f config-server/docker-compose.yml up --build
```

모듈 Compose는 저장소 루트를 build context로 사용하고 `../config-repo`를 `/config-repo:ro`로 마운트합니다. 루트 Compose에서는 `./config-repo:/config-repo:ro`를 사용합니다.
Compose는 host의 `ENCRYPT_KEY`가 누락되거나 비어 있으면 interpolation 단계에서 실패합니다.
`.env.dev.example`, `.env.external-dev.example`, `.env.prod.example`에는 변수 이름만 있고 값은 비어 있습니다. 복사한 ignored 환경 파일이나 승인된 secret provider에서 실행 직전에 값을 채웁니다.
개발/운영 환경 validator는 실제 env 파일의 빈 `ENCRYPT_KEY`를 거부하고, 운영 template 검사에서만 의도적으로 빈 예시 값을 허용합니다.

## 전체 MSA 순서

```text
Config Server readiness UP
-> Discovery readiness UP
-> Auth/Master Data/Governance/업무 서비스
-> Gateway 8000
```

## 종료와 메모리 정리

실행 터미널에서 `Ctrl+C`를 누른 뒤 다음을 확인합니다.

```powershell
.\gradlew --stop
jps -l
```

`ConfigServerApplication`이나 Gradle daemon이 남아 있으면 종료 원인을 확인합니다. IntelliJ와 SonarLint 프로세스는 IDE 기능이므로 서버 프로세스와 구분합니다.

## 자주 발생하는 문제

| 증상 | 확인 항목 |
| --- | --- |
| 8888 포트 충돌 | 기존 Config Server/Java 프로세스 |
| readiness DOWN | `config-repo` 경로와 `master-data.yml` |
| 응답 property source가 비어 있음 | application/profile 이름 |
| Docker에서 설정 없음 | volume 경로와 `CONFIG_REPO_LOCATION` |
| 기동 즉시 키 입력 오류 | `ENCRYPT_KEY` 누락, 빈 값, 공백 또는 앞뒤 공백 |
| 클라이언트 값이 안 바뀜 | 클라이언트 재시작 또는 승인된 refresh 전략 |
| Config Server 없이 클라이언트가 뜸 | `optional:configserver:` fallback 여부 |
