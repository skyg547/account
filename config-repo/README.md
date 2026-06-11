# 📜 Config Repo (중앙 설정 저장소)

`config-repo`는 모든 마이크로서비스가 공통으로 사용하는 설정 파일(`application.yml`)들을 중앙에서 관리하는 저장소입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 왜 각 모듈의 application.yml을 쓰지 않고 따로 빼두나요?**
MSA 환경에서는 서비스가 10~20개씩 됩니다. 만약 데이터베이스 비밀번호나 공통 로깅 포맷이 바뀌면 20개의 폴더를 다 뒤져서 수정하고, 20번 다시 빌드/배포를 해야 합니다.
하지만 `config-repo`라는 '레시피 본사'에 파일들을 모아두면, 여기서 설정 파일 한 줄만 바꾸면 즉각적으로 모든 지점(서비스)에 반영할 수 있습니다!

---

## 2. 🔄 어떻게 동작하나요? (Process Flow)

1. 이 폴더 안에 `gateway-service.yml`, `auth-service.yml` 처럼 각 서비스 이름에 맞는 설정 파일을 저장합니다.
2. `config-server` (포트 8888)가 켜지면서 이 폴더를 읽어들입니다.
3. 각 모듈(`gateway`, `auth` 등)이 켜질 때, 자기 폴더에 있는 설정은 무시하고 `config-server`에게 "내 설정 줘!"라고 요청해서 이 폴더의 내용을 받아갑니다.

---

## 3. 🐳 Docker 컨테이너 환경

- 이 모듈은 단독 실행되는 서버가 아닙니다.
- `docker-compose` 환경에서는 `config-server`가 로컬 볼륨이나 깃허브를 통해 이 저장소의 파일들을 읽어가도록 세팅되어 있습니다.

## 4. 로컬 확인

`config-server`를 루트 디렉터리에서 실행한 뒤 아래 URL로 설정이 내려오는지 확인합니다.

```powershell
Invoke-WebRequest http://localhost:8888/master-data/default
Invoke-WebRequest http://localhost:8888/gateway-service/default
```

IntelliJ에서는 `Config Server bootRun` 실행 구성을 사용합니다.
