# config-server local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

## IntelliJ에서 실행하기

상단 Run Configuration에서 `Config Server bootRun`을 실행합니다.

## PowerShell 명령

```powershell
.\gradlew :config-server:bootRun --console=plain
```

컴파일/패키징만 확인:

```powershell
.\gradlew :config-server:assemble --console=plain --max-workers=1 --no-daemon
```

## 빠른 확인

```powershell
Invoke-WebRequest http://localhost:8888/master-data/default
Invoke-WebRequest http://localhost:8888/gateway-service/default
```

## 주의사항

- 기본 설정은 `file://${user.dir}/config-repo`입니다. 루트 디렉터리에서 실행해야 `config-repo`를 찾습니다.
- 운영 비밀번호, JWT secret, 내부 API token은 config-repo에 평문으로 고정하지 않는 것이 원칙입니다.
