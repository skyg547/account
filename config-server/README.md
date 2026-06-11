# 📜 Config Server (중앙 설정 서버) - "MSA 레시피 본사"

`config-server`는 수십 개의 마이크로서비스에게 각자의 환경 설정(`application.yml`)을 배급해 주는 본사 서버입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

수십 개의 마이크로서비스가 각각 자기 폴더 안에 `application.yml`을 가지고 있다면, 데이터베이스 비밀번호가 하나 바뀔 때마다 모든 폴더를 뒤져서 파일을 고치고 서버를 껐다 켜야 합니다.

**Config Server**는 프랜차이즈 **본사**입니다. 각 서비스(지점)들은 켜질 때 이 본사(포트 8888)에 접속하여 "나 master-data인데, 내 설정 파일(레시피) 좀 줘!" 라고 요청해서 받아갑니다. 

---

## 2. 🔄 이 폴더의 역할 및 흐름 (Process Flow)

- `ConfigServerApplication`: 8888 포트로 켜져서 설정 파일(yml)을 서빙해 주는 웹 서버입니다.
- 실제 각 서비스들의 설정 파일 내용물은 **프로젝트 최상단의 `config-repo` 폴더**에 모아둡니다.
- **실행 순서:** 전체 MSA 시스템 중 **가장 먼저 켜져야 하는 서버**입니다. 이 서버가 죽어 있으면 다른 서비스들은 자신의 설정을 알 수 없어 켜지지 않습니다.

---

## 3. 🧭 실행 방법 (Docker & Local)

상세 문서는 [docs/README.md](./docs/README.md)에서 `beginner-guide`, `process-flow`, `local-run` 순서로 확인합니다.

**IntelliJ 로컬 실행:**
1. 루트 프로젝트를 Gradle 프로젝트로 엽니다.
2. `Config Server bootRun`을 실행합니다.
3. `http://localhost:8888/master-data/default`로 설정 조회를 확인합니다.

**최신 엔터프라이즈 Docker 환경 (권장):**
이 모듈은 멀티스테이지 Dockerfile을 통해 소스코드 빌드부터 실행까지 통합되어 있습니다.
루트 디렉토리의 통합 `docker-compose.yml`을 이용해 실행하세요.
```bash
docker-compose up -d config-server
```

**PowerShell 로컬 실행:**
```powershell
.\gradlew :config-server:bootRun --console=plain
```

**PowerShell 검증:**
```powershell
.\gradlew :config-server:assemble --console=plain --max-workers=1 --no-daemon
```
