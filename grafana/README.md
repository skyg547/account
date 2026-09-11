# Grafana 개발 관측성 및 프록시 복구

Grafana는 Prometheus가 수집한 지표를 대시보드로 보여줍니다. 이 Compose는
기존 개발 Grafana를 재부팅 후 복구하는 용도이며, 기존 `grafana-storage` 볼륨의
사용자·대시보드를 유지합니다. 신규 빈 볼륨 초기화용 설정은 아닙니다.

## 구성과 사전 조건

- `docker-compose.yml`: `grafana` 컨테이너, CPU 0.50 / RAM 768MB 상한,
  `127.0.0.1:13001` 기본 포트. `GRAFANA_PORT`로 변경할 수 있습니다.
- `provisioning/datasources/datasource.yml`: 공유 네트워크의
  `http://account-prometheus-dev:9090`을 기본 Prometheus 데이터소스로 설정합니다.
- 외부 네트워크 `account-network`와 기존 외부 볼륨 **`grafana-storage`**가 필요합니다.
  `ACCOUNT_NETWORK_NAME`으로 네트워크 이름을 변경할 수 있습니다.
- 기존 관리자 인증은 볼륨에 보존됩니다. 관리자 비밀번호 기본값이나 환경변수
  덮어쓰기를 사용하지 않습니다. 새 설치가 필요하면 별도 초기화 명세에서 승인된
  secret 파일을 마운트하고 `GF_SECURITY_ADMIN_PASSWORD__FILE`을 지정하세요.
  이 복구 절차로 빈 볼륨을 생성하지 마세요.
- `GRAFANA_IMAGE`에는 기존 실행 이미지의 digest 또는 로컬 이미지 ID를 지정합니다.
  기본 이미지는 `grafana/grafana:latest`이므로 복구 시 반드시 기존 이미지를 지정하고
  pull 없이 기동하여 의도치 않은 버전 변경을 피합니다.

## 복구 실행

다음은 저장소 루트에서 실행하는 예입니다. Docker에서는 `podman compose`를
`docker compose`로 바꿀 수 있습니다. 실제 검증 결과는 Issue #696의 PR에 기록합니다.

```bash
# 컨테이너를 제거하기 전에 기존 이미지 식별자만 보관합니다.
export GRAFANA_IMAGE="$(podman inspect --format '{{.Image}}' grafana)"
# netavark netns 유실로 정지된 대상 컨테이너만 제거합니다. 볼륨은 보존합니다.
podman rm -f grafana
podman compose -f grafana/docker-compose.yml up -d --pull never

# 기존 정지 프록시가 같은 이름을 점유하면 해당 컨테이너만 제거한 뒤 실행합니다.
podman compose -f tools/compose.governance-proxy-dev.yml up -d --pull never
```

전용 프록시는 `account-frontend-nginx`로 기동하며 CPU 0.50 / RAM 768MB를
제한합니다. 기본 진입점은 `127.0.0.1:8080`이고 `GOVERNANCE_PROXY_PORT`로 변경합니다.
변경 시 `GRAFANA_ROOT_URL`도 실제 브라우저 진입 주소의 `/grafana/` URL로 맞춥니다
(기본 `http://localhost:8080/grafana/`). 직접 Grafana 포트는 점검용입니다.

프록시는 `/`와 `/api` 요청을 `minimal-frontend:3000`의 API 경계로 전달합니다.
현재 배포된 이전 이미지는 Gateway rewrite를 사용하며, 최신 소스의 BFF 배포도 지원하는 경로입니다.
Host 헤더의 포트를 보존하여 동일 출처 검사와 일치시키며 WebSocket도 전달합니다.
`/grafana/`는 경로를 유지한 채 `grafana:3000`으로 전달합니다. 공식 Nginx entrypoint가
현재 컨테이너 DNS를 읽어 template에 주입하고, 변수 upstream을 재조회하므로
백엔드가 재생성되어 IP가 바뀌어도 다시 연결합니다. 공유 네트워크에서
`minimal-frontend`, `grafana`, `account-prometheus-dev` 이름이 해석되어야 합니다.

## 검증

```bash
# 로컬 프로세스 상태: HTTP 200, Grafana JSON의 database가 ok
curl --fail http://127.0.0.1:13001/grafana/api/health
curl --fail http://127.0.0.1:8080/healthz
# 실제 upstream을 통과하는 별도 검사
curl --fail http://127.0.0.1:8080/grafana/api/health
curl --fail http://127.0.0.1:8080/next.svg
```

`/healthz`는 Nginx 자체의 생존 확인이며 upstream 성공을 보장하지 않습니다.
브라우저 `http://localhost:8080/grafana/`에서 기존 계정으로 로그인한 뒤
Prometheus 데이터소스의 연결 검사(Save & test)와 쿼리를 확인합니다.
GH-696 자동 검증은 저장된 기본 datasource의 URL/type만 읽기 전용 조회하고,
Grafana 컨테이너에서 Prometheus 쿼리가 성공하는지 확인합니다. 사용자·인증정보는 조회하지 않습니다.
인증된 datasource health API나 화면 Save & test는 별도 선택 검증이며 이번 자동 검증의 수행 항목이 아닙니다.

## 롤백

대상 Compose의 `stop`으로 신규 컨테이너만 정지한 뒤, 이전 설정과 보관한 이미지로
대상만 재생성합니다. `grafana-storage` 볼륨과 `account-network`는 삭제하지 않습니다.
볼륨 삭제 옵션(`down -v`, `rm -v`)을 사용하지 않습니다. provisioning은 재시작 시
Prometheus URL을 다시 적용하므로 이전 URL 복원이 필요하면 설정도 함께 되돌립니다.
