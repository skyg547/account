# Portainer CE 개발 런북

선택적 이미지 동기화와 Kubernetes 전환은
[컨테이너 관리·배포 로드맵](container-management-and-deployment-roadmap.md)을 참고하세요.

Portainer는 기존 컨테이너를 브라우저에서 관리하는 별도 관리자 UI입니다.
Nginx가 `/portainer/` 요청을 내부 `account-portainer:9000`으로 전달하므로
Portainer의 9000/9443/8000 포트를 호스트에 공개할 필요가 없습니다.
15개 MSA 및 플랫폼 스택의 Compose 파일·포트·데이터는 변경하지 않습니다.

## 전제 조건과 권한

- 저장소 루트에서 실행합니다. Docker Engine + Compose와 기존
  `account-network`가 필요합니다. 다른 네트워크 이름은
  `ACCOUNT_NETWORK_NAME`으로 지정하며 Nginx도 같은 네트워크여야 합니다.
- 관리 대상 엔진의 Unix socket이 실제 소켓인지 먼저 확인합니다.
  Docker 기본은 `/var/run/docker.sock`입니다. 누락된 소켓을 디렉터리로
  자동 생성하지 않도록 Compose에 `create_host_path: false`를 설정했습니다.
- Portainer에 소켓을 주면 그 엔진의 관리자 권한을 줍니다. 컨테이너의
  `cap_drop`, 읽기 전용 rootfs, `no-new-privileges`는 소켓 API 권한을 제한하지
  않습니다. `DAC_READ_SEARCH`만 복원하여 rootful Docker의 UID 0도 일반 호스트
  사용자가 소유한 0600 관리자 파일을 읽게 합니다. 이는 읽기 권한 우회이며
  파일 쓰기 권한 우회는 허용하지 않습니다. 회계 앱 로그인·역할과 Portainer
  로그인·역할은 별개입니다.
- HTTP 예제는 로컬 개발 전용입니다. Nginx는 loopback에 바인딩하고 원격에서는
  SSH 포워딩을 사용합니다. 공용 도메인/터널 공개 전에는 HTTPS 및 별도의
  접근 제어를 적용하고 로그인·Origin·WebSocket 검증을 다시 수행합니다.
- `--admin-password-file`로 최초 관리자 계정을 먼저 초기화하여 공개된
  초기 설정 화면의 선점 위험을 줄입니다. 기존 `/data`에서는 이 파일로
  비밀번호가 변경되지 않습니다. 비밀번호 변경은 인증된 UI에서 수행합니다.

## 관리자 파일과 모듈 기동

다음 명령은 저장소 밖 사용자 전용 디렉터리에 임의 비밀번호를 생성하며
값을 터미널에 출력하지 않습니다. 기존 파일은 덮어쓰지 않습니다. 생성한 값은
소유자가 로컬 보안 도구로 확인하여 로그인하고 채팅·PR·로그에 붙이지 않습니다.

```bash
export PORTAINER_ADMIN_PASSWORD_FILE="$HOME/.local/share/account-portainer/admin-password"
python3 - <<'PY'
import os, pathlib, secrets
p = pathlib.Path(os.environ['PORTAINER_ADMIN_PASSWORD_FILE'])
p.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
fd = os.open(p, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as out:
    out.write(secrets.token_urlsafe(36))
PY
test -S /var/run/docker.sock
docker network inspect account-network --format '{{.Name}}'
docker compose -f portainer/docker-compose.yml config --quiet
docker compose -f portainer/docker-compose.yml up -d
```

`account-portainer`는 0.5 CPU, 512 MiB, 128 PID로 제한됩니다. 컨테이너는
`/data` named volume과 `/tmp` tmpfs만 쓰며 로그 회전은 10 MiB × 3입니다.
이미지는 쉘/HTTP 클라이언트를 보장하지 않으므로 가짜 내부 healthcheck 대신
아래 Nginx 경유 API 검사를 사용합니다. Compose의 running은 API 준비 완료와
다릅니다. `depends_on`으로 전체 MSA를 시작하지 않습니다.

## Nginx 연결

`frontend-nginx/nginx.conf`에 다음 흐름이 포함되어 있습니다.

1. `/portainer`는 상대 경로 `/portainer/`로 301 이동하여 외부 포트 8080을 보존합니다.
2. Portainer `--base-url /portainer`는 브라우저 리소스 경로를 설정합니다.
   실제 upstream API는 루트 경로이므로 Nginx가 접두사를 **한 번** 제거합니다.
3. 변수 upstream과 기존 runtime DNS resolver로 Portainer 미기동 시에도
   Nginx는 기동하며, 요청에는 502를 반환합니다. 재생성 후 IP 변경은 DNS
   `valid=10s` 이후 반영됩니다. 정적 리소스 요청도 같은 location을 사용합니다.
4. 원래 `Host`와 포트를 보존하여 CSRF Origin 검사를 지원합니다.
   HTTP/1.1 Upgrade/Connection, 3600초 timeout과 buffering off가 콘솔·로그를
   지원합니다. WebSocket URL에 토큰이 들어갈 수 있어 이 경로의 access log와
   request URI를 포함하는 upstream error log는 끕니다. 경로별 오류 상세를 잃는
   대신 HTTP 상태/API 준비 검사로 진단합니다. 외부 프록시에서도 쿼리 토큰을
   기록하지 않아야 합니다.

처음 Nginx를 만드는 경우에만 아래 예제를 사용합니다. 기본 standalone
Compose는 80/443을 공개하므로 로컬 검증에서는 loopback 주소까지 지정합니다.
HTTPS 포트 선언만으로 인증서/TLS가 설정되는 것은 아닙니다.

```bash
NGINX_HTTP_PORT=127.0.0.1:8080 NGINX_HTTPS_PORT=127.0.0.1:8443 \
  docker compose -f frontend-nginx/docker-compose.yml up -d --build
docker exec account-frontend-nginx nginx -t
```

이미 8080을 사용하는 Nginx가 있으면 새 프록시를 기동하지 않습니다.
먼저 해당 컨테이너의 **mount 항목만** 확인하여 실제 설정 소유 위치를 찾습니다.
현재 설정을 백업하고 이 PR의 두 Portainer location을 반영한 다음
`nginx -t` 성공 후 `nginx -s reload`합니다. 템플릿 마운트 방식은 템플릿 수정만으로
이미 생성된 `conf.d/default.conf`가 갱신되지 않습니다. 운영 절차에 따라
entrypoint 재렌더링 또는 검증된 프록시 재생성을 수행합니다. 기존 BFF·Grafana
등의 현장별 설정을 통째로 덮어쓰지 않습니다. 임시 rendered config 수정은
재시작 후 사라지므로 영구 배포와 구분합니다.

## Podman 개발 환경

[공식 Podman 문서](https://docs.portainer.io/start/install-ce/server/podman/linux)는
rootful Podman을 지원하며 rootless는 공식 지원 대상이 아닙니다.
이 환경의 rootless Podman 4.9.3은 별도 실험 검증 대상으로 취급합니다.
Docker Compose 검증 결과를 Docker 실기동 검증으로 해석하지 않습니다.

```bash
systemctl --user start podman.socket
export PORTAINER_SOCKET_PATH="/run/user/$(id -u)/podman/podman.sock"
test -S "$PORTAINER_SOCKET_PATH"
# 위에서 만든 PORTAINER_ADMIN_PASSWORD_FILE도 유지합니다.
podman-compose -f portainer/docker-compose.yml up -d
```

Podman에서도 소켓의 호스트 권한을 완화하거나 무조건 `--privileged`를
추가하지 않습니다. SELinux 또는 Docker 호환 API 차이로 실패하면 실제 오류를
분리하고 공식 지원 환경에서 재검증합니다. rootless 계정의 엔진 밖에 있는
컨테이너는 이 인스턴스가 관리하지 못합니다.

## 검증과 기대 결과

```bash
python3 -m unittest discover -s portainer/tests -v
curl --fail --silent --output /dev/null http://localhost:8080/portainer/
curl --fail --silent --output /dev/null http://localhost:8080/portainer/api/status
curl --silent --head http://localhost:8080/portainer
docker port account-portainer
docker inspect account-portainer --format '{{.State.Status}} {{.RestartCount}} {{.State.OOMKilled}}'
```

기대 결과는 테스트 통과, HTML/API HTTP 200, Location `/portainer/`,
`docker port` 출력 없음, `running 0 false`입니다. Podman은 `docker`를 `podman`으로
대체합니다. 인증이 필요한 API는 로그인 없이 401/403을 반환해야 합니다.

자동화된 브라우저·로그·재기동·WebSocket 검증은 [opt-in live smoke](../../portainer/tests/README.md)의 Node/Playwright 전제와 전용 probe 명령을 사용합니다. Portainer 2.39.7의 Unix-socket HTTP 프록시는 작은 `follow=true` 로그를 즉시 flush하지 않아 드문 출력이 지연될 수 있습니다. 테스트는 충분한 합성 로그를 보내며 조용한 컨테이너의 지연 상한을 보장하지 않습니다.

브라우저에서 `http://localhost:8080/portainer/`를 열고 `admin`과 생성한 비밀번호로
로그인합니다. 실제 JS/CSS 로딩과 로그인 성공을 확인합니다. 검증용 빈 Alpine
컨테이너 하나만 생성하여 로그 스트리밍, Console에서 `printf 'portainer-ok\n'`,
재기동 후 정상 실행을 확인합니다. DevTools에서 콘솔 WebSocket은 101 응답이어야
합니다. 기존 업무 컨테이너의 로그/환경변수/DB 값은 검증 대상으로 열지 않습니다.

Portainer 컨테이너만 재생성한 후 동일 계정과 로컬 환경이 유지되는지 확인합니다.
Nginx의 다른 경로도 변경 전후 비교합니다. 테스트 산출물에는 토큰·비밀번호·
일반 컨테이너 inspect 전체 응답을 저장하지 않습니다.

## 중지·재시작·롤백

```bash
docker compose -f portainer/docker-compose.yml stop portainer
docker compose -f portainer/docker-compose.yml up -d portainer
# 동일 버전에서 컨테이너를 재생성해도 account-portainer-data는 유지됩니다.
docker compose -f portainer/docker-compose.yml up -d --force-recreate portainer
```

롤백은 Portainer만 중지하고 Nginx의 두 location만 백업으로 되돌린 다음
문법 검사·reload합니다. `down -v`, `volume rm`, 전역 prune은 사용하지 않습니다.
`account-portainer-data`와 비밀번호 파일을 보존하면 같은 버전으로 복구할 수 있습니다.
업그레이드 전에는 Portainer를 중지하고 볼륨을 별도로 백업합니다. 새 버전이 DB를
마이그레이션한 후에는 이미지 태그만 낮추지 말고 대응하는 백업을 복원해야 합니다.

공식 근거: [CLI base-url/초기 관리자 파일](https://docs.portainer.io/advanced/cli),
[Nginx 프록시](https://docs.portainer.io/advanced/reverse-proxy/nginx),
[2.39.7 릴리스](https://github.com/portainer/portainer/releases/tag/2.39.7).

## Issue #713 검증 환경과 배포 상태

2026-09-12에 Portainer CE 2.39.7 / Nginx 1.27-alpine / rootless Podman 4.9.3 /
Chromium 143으로 검증했습니다. Compose quiet render, 7개 정적 테스트(34개 잘못된
구성 변형), 실제 브라우저 로그인과 JS/CSS 8개 응답, probe 로그·재기동,
WebSocket 101 및 셸 출력, 영속 볼륨 재사용, backend 중지 시 Nginx 기동·502와
합성 토큰의 로그 미노출, backend 복구를 확인했습니다. Docker 자체의 실기동과
공용 HTTPS/터널은 미검증이며, Java/Gradle은 업무·빌드 코드 변경이 없어 실행하지
않았습니다. 다른 UID 소유 0600 합성 파일은 capability 없는 컨테이너에서 읽기
실패, `DAC_READ_SEARCH`만 복원한 컨테이너에서 읽기 성공을 별도로 확인했습니다.

기존 `account-frontend-nginx`는 현장별 별도 템플릿을 마운트하고 있었고 8080
매핑 메타데이터와 달리 실제 host listener가 없었습니다. 따라서 그 컨테이너나
템플릿을 변경하지 않고, 비어 있던 loopback 8080에 검증 전용 `account-713-nginx`를
띄워 이 PR의 전체 템플릿을 검사했습니다. 이 검증은 기존 Nginx에 대한 영구 배포가
아닙니다. 종료 시 검증 프록시·probe는 정리하고 Portainer는 데이터 볼륨을 유지한
채 중지합니다. 배포 담당자는 기존 Nginx의 실제 설정에 검토된 변경을 반영하고
host listener 문제를 별도로 해결해야 합니다.

이 작업의 보존된 테스트 볼륨을 재사용한다면 현재 bootstrap 파일은
`/tmp/account-713-private/admin-password`(0600, 상위 디렉터리 0700)입니다.
값은 기록하지 않았습니다. `/tmp` 정리 전 소유자가 안전한 영구 저장소로 옮기고
해당 파일 경로로 다시 기동한 뒤 UI에서 비밀번호를 변경해야 합니다.
이미 초기화된 볼륨에 새 bootstrap 파일을 지정해도 계정 비밀번호는 바뀌지 않습니다.
