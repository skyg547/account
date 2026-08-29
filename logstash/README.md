# Logstash 개발 서버 실행 가이드

Logstash는 Spring Boot 서비스의 JSON 로그를 TCP 5000에서 받고 Elasticsearch로 전달합니다.
이 폴더의 Compose는 개발 서버용 독립 프로젝트 `account-logstash-dev`입니다. Spring
프로파일은 없으며, #520의 최소 인증 `external-dev` 프로젝트에도 포함되지 않습니다.

## 이 폴더의 파일

- `config/logstash.yml`: HTTP 상태 API 9600과 저자원 pipeline 설정
- `pipeline/logstash.conf`: JSON TCP 5000 입력과 `account-logs-dev-*` 출력
- `docker-compose.yml`: 두 파일의 read-only mount, healthcheck와 자원 제한

healthcheck는 HTTP 200만 보지 않습니다. Elasticsearch 응답의 `timed_out=false`와
`status=yellow|green`을 함께 확인하므로 red/timeout 상태에서는 fail-closed로 실패합니다.

5044 Beats 기본 pipeline과 `stdout/rubydebug` 출력은 사용하지 않습니다. 실제 업무 로그나
synthetic 문서 본문을 터미널에 출력하지 않기 위해서입니다. TCP 5000과 API 9600은
`account-network` 안에서만 열고 호스트 포트는 만들지 않습니다. 호스트 전달은 #566이
담당합니다.

## 1. 사전 확인

저장소 루트에서 실행합니다. Elasticsearch가 `account-network`에서 먼저 실행 중이어야 합니다.

Podman:

```bash
podman network exists account-network
podman ps --filter name=elasticsearch --format '{{.Names}}|{{.Status}}'
```

Docker:

```bash
docker network inspect account-network >/dev/null
docker ps --filter name=elasticsearch --format '{{.Names}}|{{.Status}}'
```

이후 명령은 Docker 사용자가 `podman`을 `docker`로 바꿔 실행할 수 있습니다.

## 2. 시작 전 정적 검사

Compose 렌더링은 설정값이나 문서 내용을 출력하지 않는 quiet 모드로 확인합니다.

```bash
podman compose -f logstash/docker-compose.yml config --quiet
```

pipeline 문법은 별도 임시 컨테이너에서 검사합니다. 네트워크를 끄고 CPU와 메모리를 제한하며,
검사가 끝나면 컨테이너는 자동 삭제됩니다.

```bash
podman run --rm --network none --cpus 0.5 --memory 640m --pids-limit 256 \
  -e LS_JAVA_OPTS="-Xms256m -Xmx256m" \
  -v "$PWD/logstash/config/logstash.yml:/usr/share/logstash/config/logstash.yml:ro" \
  -v "$PWD/logstash/pipeline/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro" \
  docker.elastic.co/logstash/logstash:7.17.10 \
  bin/logstash --config.test_and_exit
```

## 3. 기존 컨테이너에서 안전하게 교체

현재 `logstash` 컨테이너가 실행 중이면 새 서비스와 DNS 별칭이 겹치므로, 위 검사가 통과한
뒤 그 컨테이너 하나만 정지합니다. 새 구성 검증이 끝나기 전에는 기존 컨테이너를 삭제하지
않습니다.

```bash
podman stop logstash
podman compose -f logstash/docker-compose.yml up -d --wait
```

새 컨테이너 이름은 `account-logstash-dev`입니다. 5000/9600은 host에 공개하지 않습니다.

## 4. 값·문서 본문 비노출 검증

먼저 HTTP API와 내부 TCP 5000이 열렸는지만 확인합니다.

```bash
podman exec account-logstash-dev curl --fail --silent --show-error \
  http://127.0.0.1:9600/_node/pipelines/main >/dev/null

podman run --rm --network account-network --cpus 0.05 --memory 32m --pids-limit 16 \
  docker.io/library/alpine:3.20 sh -c 'nc -z -w 2 logstash 5000'
```

synthetic event는 계정·토큰·실제 업무값 없이 만들고 count만 출력합니다.

```bash
PROBE_ID="issue-561-$(date -u +%Y%m%d%H%M%S)"
printf '{"message":"redacted synthetic probe","service":"issue-561","verification_id":"%s"}\n' \
  "$PROBE_ID" \
  | podman run --rm -i --network account-network --cpus 0.05 --memory 32m \
      --pids-limit 16 docker.io/library/alpine:3.20 nc -w 3 logstash 5000

podman exec account-logstash-dev curl --fail --silent --show-error \
  "http://elasticsearch:9200/account-logs-dev-*/_count?q=verification_id.keyword%3A${PROBE_ID}" \
  | python3 -c 'import json,sys; print("synthetic_count=" + str(json.load(sys.stdin)["count"]))'
```

`synthetic_count=1`이면 TCP → Logstash → Elasticsearch 경로가 정상입니다. 쿼리도
`account-network` 안에서 실행하므로 Elasticsearch의 host 9200 전달이 필요하지 않습니다.
count 이외의 Elasticsearch 응답이나 문서 본문은 출력하지 않습니다.

## 5. 상태와 롤백

```bash
podman ps --filter name=account-logstash-dev \
  --format '{{.Names}}|{{.Status}}|{{.Ports}}'
podman stats --no-stream account-logstash-dev
```

새 구성을 되돌릴 때는 이 Compose 서비스만 정지합니다. 보존한 이전 `logstash`가 정상적인
`exited` 상태일 때만 선택적으로 다시 시작합니다. `stopping`에 고착됐거나 이미 제거한
컨테이너에는 `start`를 반복하지 않습니다.

```bash
podman compose -f logstash/docker-compose.yml stop logstash
# 이전 컨테이너가 실제로 남아 있고 exited일 때만:
podman start logstash
```

`down -v`, `prune`, Elasticsearch volume/index 삭제, 전체 컨테이너 일괄 정지는 사용하지
않습니다.
