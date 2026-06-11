# 🗺️ Discovery Service (유레카 서버) - "사내 전화번호부"

`discovery` 모듈은 MSA 환경에서 서비스 레지스트리(Service Registry) 역할을 하는 인프라 핵심 서버입니다. Netflix Eureka Server 구현체를 기반으로 합니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 마이크로서비스에서 레지스트리가 왜 필요한가요?**
MSA 환경에서는 트래픽에 따라 서버(지점)가 수십 개로 늘어나고, IP 주소와 포트가 수시로 바뀝니다. 만약 서버의 IP 주소를 하드코딩하면, 서버가 죽거나 증설될 때마다 코드를 일일이 수정해야 하는 불상사가 생깁니다.

**Discovery Service (Eureka)**는 모든 마이크로서비스가 켜질 때마다 "저 지금 8082번 포트에서 영업 시작했습니다!" 하고 신고(Register)하는 **'사내 중앙 전화번호부'**입니다.
다른 서버가 `master-data`를 찾고 싶을 때, IP 주소 대신 `master-data`라는 이름만 대면 유레카가 알아서 현재 영업 중인 IP 주소를 알려줍니다.

---

## 2. 🔄 이 폴더의 역할 및 흐름 (Process Flow)

- `discovery`는 그 자체로 비즈니스 로직을 갖지 않는 순수 인프라 요소입니다 (Anti-Skeleton Policy 예외).
- 다른 모듈(`app`, `contracts` 등)은 클라이언트로서 Eureka Client 의존성을 가지고 있으며, 설정에 `defaultZone`을 지정하여 켜질 때 자신의 정보를 동기화합니다.
- **실행 순서:** MSA 시스템 기동 시 **Config Server 다음으로 켜져야 하는** 핵심 인프라입니다.

---

## 3. 🧭 실행 방법 (Docker & Local)

상세 문서는 [docs/README.md](./docs/README.md)에서 `concept`, `local-run` 순서로 확인합니다.

**IntelliJ 로컬 실행:**
1. `Config Server bootRun`을 먼저 실행합니다.
2. `Discovery bootRun`을 실행합니다.
3. 브라우저에서 `http://localhost:8761`을 확인합니다.

**최신 엔터프라이즈 Docker 환경 (권장):**
멀티스테이지 Dockerfile이 적용되어 있으며, 루트 디렉토리의 통합 `docker-compose.yml`을 이용하면 다른 서비스와의 의존성에 맞춰 자동으로 실행됩니다.
```bash
docker-compose up -d discovery
```
실행 후 브라우저에서 `http://localhost:8761`에 접속하면, 현재 우리 시스템에 어떤 마이크로서비스들이 살아있는지 한눈에 볼 수 있는 대시보드가 열립니다.

**PowerShell 로컬 실행:**
```powershell
.\gradlew :discovery:bootRun --console=plain
```

**PowerShell 검증:**
```powershell
.\gradlew :discovery:test --console=plain --max-workers=1 --no-daemon
```
