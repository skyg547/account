# Cloudflare Tunnel 실행 가이드

## 구성과 전제 조건

`cloudflared`는 서버에서 Cloudflare로 아웃바운드 연결을 만들고, 외부 HTTPS 요청을
컨테이너 내부 HTTP 서비스로 전달합니다. 이 모듈의 Compose에는 `ports`, 호스트
네트워크, Docker 소켓, 인증서 마운트가 없습니다. Java API/core/batch의 책임은 바뀌지 않습니다.

```text
브라우저 HTTPS → Cloudflare → cloudflared → account-frontend-nginx:80 → 웹 / API
                                          (account-network)
```

| 모드 | 파일 | 필요한 입력 | 용도 |
| --- | --- | --- | --- |
| Quick | `cloudflared/docker-compose.yml` | 없음 | 계정 없이 임시 공개 테스트 |
| Named | `cloudflared/docker-compose.named.yml` | 터널 토큰, 도메인, 원격 라우트, Access 정책 | 지속적으로 보호할 개발 서버 |

두 파일은 독립 Compose 프로젝트 정의이며 **함께 `-f`로 병합하지 않습니다**.
같은 프로젝트 `account-cloudflare-tunnel`과 서비스 `cloudflared`를 사용해 전환 시
기존 커넥터를 교체합니다. 명령은 저장소 루트의 Bash에서 실행합니다.
Docker Compose v2 이상은 `docker compose`, Docker CLI 없이 Compose 바이너리만
있으면 `docker-compose`를 사용합니다. Podman에서는 아래의 `docker compose`를
`podman-compose`로, `docker`를 `podman`으로 바꿉니다. 단, Podman Compose 1.6은
`rm` 하위 명령이 없으므로 컨테이너 제거는 맨 아래의 Podman 전용 명령을 사용합니다.

1. 같은 컨테이너 엔진에 외부 네트워크 `account-network`와 실행 중인 Nginx가 있어야 합니다.
   컨테이너 이름 또는 네트워크 별칭은 `account-frontend-nginx`, 내부 포트는 `80`이어야 합니다.
   이 모듈은 네트워크/Nginx/DB를 생성하거나 재기동하지 않습니다.
   루트 `docker-compose.yml`의 기본 네트워크는 `account-dev-network`입니다. 그 스택을
   사용한다면 Quick은 `export ACCOUNT_NETWORK_NAME=account-dev-network`, Named는
   `cloudflared/.env`의 같은 키를 변경하고 Nginx도 그 네트워크에 있는지 확인하세요.
2. DNS와 Cloudflare 방향 아웃바운드 통신이 필요합니다. 터널 전송은 `7844/UDP`(QUIC)
   또는 `7844/TCP`(HTTP/2), Quick URL 발급은 HTTPS를 사용합니다. 인바운드 방화벽 개방은 필요 없습니다.
3. 기존 Nginx Compose의 `80/443` 또는 다른 앱의 호스트 포트 바인딩은 자동으로 제거되지 않습니다.
   Tunnel만으로 접속하려면 해당 소유 Compose에서 기존 공개 포트를 별도로 제거하거나 제한하세요.

## Quick Tunnel: 계정 없이 임시 URL 발급

Quick URL은 인증 장벽이 없는 **공개 주소**입니다. 기존 Nginx는 `/grafana/`, `/kibana/`,
`/pgadmin/` 같은 관리 경로도 라우팅할 수 있으므로, 합성 데이터만 있는 테스트 원본을
사용하세요. URL을 아는 것 자체가 접근 권한 검증은 아닙니다. 실제 개발 데이터에는
아래 Named + Access 절차를 사용합니다.

```bash
# .env를 자동으로 읽지 않으므로 토큰 없이 독립 실행 가능
unset CLOUDFLARE_TUNNEL_TOKEN
docker network inspect "${ACCOUNT_NETWORK_NAME:-account-network}" --format '{{.Name}}'
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml config --quiet
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml up -d
# Quick 모드에서만 확인: *.trycloudflare.com 주소가 표시됨
# URL을 공유 기록/PR에 복사하지 마세요.
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml logs --tail 60 cloudflared
```

로그의 HTTPS 주소로 브라우저를 열고 예상 웹 화면과 승인된 읽기 전용 API 경로를
확인합니다. URL 발급만으로 원본 연결 성공을 판단하지 마세요. `502`는 보통 원본
DNS/포트/응답 문제입니다. 실행 중인 연결을 확인한 뒤 실제 응답까지 검증해야 합니다.

Quick 모드는 `restart: "no"`여서 재부팅 시 자동 공개하지 않습니다. 다시 실행하면
URL이 바뀔 수 있습니다. 테스트 후 반드시 중지하세요. Quick Tunnel은 SLA가 없고
동시 요청 200개 제한과 SSE 미지원 제약이 있습니다.
[공식 Quick Tunnel 설명](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/).

```bash
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml stop cloudflared
```

## Named Tunnel: 토큰과 Access로 보호

1. Cloudflare에서 도메인을 준비하고 원격 관리 Cloudflared Tunnel을 만듭니다.
   대시보드의 **Networking > Tunnels** 또는 **Zero Trust > Networks > Connectors**에서
   터널의 Docker 커넥터 설치 화면을 열어 **Tunnel token**을 얻습니다.
   API 관리 토큰/global API key가 아닌 커넥터 실행 토큰입니다.
2. 공개 라우트를 추가하기 **전에** 해당 호스트 전체를 보호하는 Cloudflare Access
   self-hosted 애플리케이션을 만들고 허용 사용자/그룹 정책을 설정합니다. 관리 경로를
   포함한 전체 호스트를 보호하고 불필요한 Bypass 정책을 두지 않습니다.
   API 자동화에는 별도로 승인된 Service Auth 정책과 서비스 토큰을 사용하세요.
3. 터널의 Published application route/Public hostname에 선택한 도메인을 연결합니다.
   Service type은 `HTTP`, URL은 **`account-frontend-nginx:80`**, path는 전체 경로입니다.
   이 설정은 Cloudflare에서 관리합니다. 토큰 실행 Compose의 `command`에는 `--url`을
   넣지 않습니다. 원격 라우트가 잘못되면 커넥터가 Healthy여도 앱에 연결되지 않습니다.
4. 로컬에서 예시를 복사하고 편집기로 토큰을 입력합니다. 아래 초기화는 최초 한 번만 실행합니다.

```bash
# 기존 파일이 있으면 덮어쓰지 않음
(umask 077; set -C; cat cloudflared/.env.example > cloudflared/.env)
# 편집기로 cloudflared/.env의 CLOUDFLARE_TUNNEL_TOKEN을 입력
# 이미 존재하는 파일은 소유자만 읽고 쓸 수 있는지 확인
chmod 600 cloudflared/.env
git check-ignore cloudflared/.env
```

`.env`는 Git에서 제외됩니다. 토큰은 `TUNNEL_TOKEN` 환경변수로 전달하므로 명령 인자에
들어가지 않지만, 엔진 접근 권한자는 컨테이너 환경을 읽을 수 있습니다. 실제 토큰으로
`config`(비 quiet), `inspect` 전체 출력, `set -x`, 환경 출력, Named 디버그 로그 공유를
하지 마세요. 빈/누락 토큰은 Compose 구성 단계에서 거부됩니다.

```bash
# 먼저 Quick을 중지하여 인증 없는 경로를 닫음
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml stop cloudflared
docker compose --env-file cloudflared/.env -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.named.yml config --quiet
docker compose --env-file cloudflared/.env -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.named.yml up -d --force-recreate
```

Named는 `restart: unless-stopped`로 재기동을 지원합니다. 토큰 갱신 후에도 위
`up -d --force-recreate`로 컨테이너 환경을 다시 주입해야 합니다. Cloudflare에서
기존 토큰을 폐기/교체하고 재연결을 확인하세요. Named에서 Quick으로 전환할 때도
Named를 먼저 `stop cloudflared`하고 Quick 파일로 `up -d --force-recreate`합니다.

Cloudflare Tunnel 자체는 사용자 로그인을 강제하지 않습니다. Access 정책 적용 후
비로그인 브라우저에서 인증 요구/차단, 허용 사용자 로그인 후 웹·API 정상 응답,
비허용 사용자 차단을 각각 확인합니다. 세션 쿠키, 리다이렉트, CSRF와 WebSocket도
실제 도메인에서 확인하세요. 기존 Nginx가 `X-Forwarded-Proto`를 내부 HTTP로 덮어쓰는
구성에서는 HTTPS 인지/쿠키가 실패할 수 있으므로 앱 성공을 커넥터 Healthy로 대신하지
마세요. 헤더 신뢰 경계 변경은 해당 프록시 소유 설정에서 검토해야 합니다.
[공식 터널 설정](https://developers.cloudflare.com/tunnel/get-started/),
[Access 애플리케이션 설정](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/).

## 검증·문제 해결·롤백

- **구성 검증**: 두 모드의 `config --quiet`가 성공하고 토큰 누락/빈 값의 Named는 실패해야 합니다.
  Quick은 `.env`와 토큰 없이 성공해야 합니다. 테스트용 가짜 토큰은 실제 연결 성공을 증명하지 않습니다.
- **네트워크**: 기본은 `account-network`입니다. 격리 테스트에는
  `ACCOUNT_NETWORK_NAME=account-tunnel-test`처럼 네트워크 이름을 변경할 수 있습니다.
  해당 네트워크에도 `account-frontend-nginx:80` 원본이 필요합니다.
- **연결 불가**: DNS 및 아웃바운드 7844를 확인합니다. UDP 제한이면
  `CLOUDFLARE_TUNNEL_PROTOCOL=http2`로 설정하고 같은 모드의 `up -d --force-recreate`를 실행합니다.
  `ACCOUNT_NETWORK_NAME`과 protocol의 Quick 재정의는 셸 `export`로 전달합니다.
- **포트와 상태**: `docker compose ... ps`로 포트 공개가 없는지 확인합니다.
  cloudflared 이미지는 셸/curl 기반 healthcheck를 가정하지 않습니다. 실행 상태,
  연결 등록, 실제 HTTP 응답을 함께 검사합니다. Quick URL 발급 직후 DNS 조회가
  실패하면 잠시 뒤 다시 확인하며, DNS 실패를 HTTP 성공으로 기록하지 않습니다. 자원 상한은 CPU 0.5/메모리 256 MiB/PID 128이며
  부하 성능을 보장하는 수치는 아닙니다.
- **버전**: AC에 따라 `docker.io/cloudflare/cloudflared:latest`를 사용합니다.
  `--no-autoupdate`는 컨테이너 내부 자동 갱신을 막습니다. `pull`은 latest가 바뀔 수 있으므로
  운영자는 기존 이미지 ID/digest를 기록하고 새 버전 검증 후 계획적으로 재생성하세요.
- **Named 실환경 게이트**: 실제 토큰·도메인·Access 정책을 준비한 운영자가 위 인증 및
  웹/API 검증을 완료해야 합니다. 저장소 변경 검증에는 자격 증명이나 실제 도메인이 필요하지 않습니다.

중지는 터널만 대상으로 합니다. 외부 네트워크/Nginx/DB/볼륨을 삭제하지 않습니다.
Named의 `.env`가 없어도 토큰을 요구하지 않는 Quick 정의로 같은 서비스에 `stop`할 수 있습니다.

```bash
# 두 모드 모두 같은 프로젝트/서비스이므로 비밀값 없이 중지 가능
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml stop cloudflared
# 필요할 때 중지된 커넥터 컨테이너만 제거
docker compose --env-file /dev/null -p account-cloudflare-tunnel \
  -f cloudflared/docker-compose.yml rm -f cloudflared
```

Podman Compose 1.6에서는 위 `stop`을 실행한 뒤, 고정 프로젝트에서 생성한 정확한
컨테이너를 확인하고 Podman으로 제거합니다. 출력의 `running=false`를 확인하세요.
다른 프로젝트명으로 실행했다면 해당 프로젝트의 컨테이너 이름을 사용합니다.

```bash
podman inspect account-cloudflare-tunnel_cloudflared_1 \
  --format 'running={{.State.Running}}'
podman rm account-cloudflare-tunnel_cloudflared_1
```

소스 롤백은 이 모듈/가이드 변경을 검토 후 되돌립니다. Named 라우트/Access/DNS는
Cloudflare 쪽 별도 상태이므로 운영자가 확인합니다. 전체 스택 `down`, volume 삭제,
네트워크 삭제 또는 prune은 이 모듈 롤백에 필요하지 않습니다.
