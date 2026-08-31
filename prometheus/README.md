# Prometheus external-dev 최소 모니터링

Prometheus는 Spring Boot의 `/actuator/prometheus`에서 숫자 메트릭을 주기적으로 수집하는
시계열 데이터베이스입니다. 이 개발 서버 구성은 전체 17개 API나 Batch를 시작하지 않고,
이미 실행 중인 external-dev 최소 인증 경로만 관측합니다.

## 무엇을 수집하나요?

`account-network` 내부 DNS를 사용해 다음 6개 target만 수집합니다.

| Job | Target | 용도 |
| --- | --- | --- |
| `prometheus` | `localhost:9090` | Prometheus 자체 상태 |
| `minimal-auth` | `minimal-auth:8084` | 인증 API |
| `minimal-master-data` | `minimal-master-data:8082` | 부서·기준정보 |
| `minimal-discovery` | `minimal-discovery:8761` | Eureka |
| `minimal-config-server` | `minimal-config-server:8888` | 설정 서버 |
| `minimal-gateway` | `minimal-gateway:8000` | API Gateway |

중지된 legacy 이름과 `host.docker.internal`을 함께 등록하지 않습니다. 대상 하나가 잠시
내려가도 Prometheus 자체 readiness는 유지되고 해당 target만 `down`으로 표시됩니다.

## 처음 실행하기

저장소 루트에서 실행합니다. 먼저 #520 external-dev 최소 인증 스택이 healthy이고 외부
network `account-network`가 있어야 합니다. 기존 이름 `prometheus`인 legacy 컨테이너는
삭제하거나 시작하지 않습니다. 이 구성은 고유 이름 `account-prometheus-dev`, loopback
포트 `19090`, 별도 named volume을 사용합니다.

```bash
podman compose -f prometheus/docker-compose.yml config --quiet
podman run --rm \
  -v "$PWD/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  --entrypoint /bin/promtool docker.io/prom/prometheus:v3.14.0 \
  check config /etc/prometheus/prometheus.yml
podman compose -f prometheus/docker-compose.yml up -d --wait
```

Docker 사용자는 위 명령의 `podman`을 `docker`로 바꿉니다. 이미지가 로컬에 없으면 먼저
승인된 이미지 취득 절차가 필요합니다. 소스 메트릭을 수집할 뿐 업무 데이터나 로그인 응답을
읽지 않습니다.

## 값 비노출 검증

준비 상태는 본문을 출력하지 않고 HTTP 성공 여부만 확인합니다. target API 응답도 그대로
출력하지 않고 Python 표준 라이브러리가 합계만 계산합니다.

```bash
curl --fail --silent --show-error --output /dev/null --max-time 5 \
  http://127.0.0.1:19090/-/ready
curl --fail --silent --show-error --max-time 5 \
  http://127.0.0.1:19090/api/v1/targets \
  | python3 -c 'import json,sys; a=json.load(sys.stdin)["data"]["activeTargets"]; total=len(a); up=sum(t["health"] == "up" for t in a); print(f"targets total={total} up={up} down={total-up}"); raise SystemExit(0 if total == 6 and up == 6 else 1)'
podman compose -f prometheus/docker-compose.yml ps
podman stats --no-stream account-prometheus-dev
```

현재 최소 스택이 모두 실행 중이면 `targets total=6 up=6 down=0`이어야 합니다. 이후 어떤
서비스가 의도적으로 중지됐다면 Prometheus는 healthy일 수 있지만 target 검증은 실패하는
것이 정상입니다. 원인을 확인한 뒤 다시 실행하세요. 메트릭 원문, label, target API JSON을
Issue나 채팅에 붙이지 않습니다.

## 자원과 재실행

- host 공개는 `127.0.0.1:19090` 하나뿐입니다.
- CPU 0.5, memory 512 MiB, pids 128 상한을 사용합니다.
- 설정은 read-only, 시계열 데이터만 전용 named volume에 씁니다.
- 설정 변경 후에는 아래 순서로 문법을 다시 검사하고 한 서비스만 강제로 재생성합니다.
  단순 `up -d`는 bind-mounted 파일 내용 변경을 감지하지 못할 수 있습니다.

```bash
podman compose -f prometheus/docker-compose.yml config --quiet
podman run --rm \
  -v "$PWD/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  --entrypoint /bin/promtool docker.io/prom/prometheus:v3.14.0 \
  check config /etc/prometheus/prometheus.yml
podman compose -f prometheus/docker-compose.yml \
  up -d --wait --force-recreate prometheus-dev
```

인증 없는 HTTP reload/quit lifecycle API는 활성화하지 않습니다.

## 안전한 중지와 롤백

다음 명령은 새 프로젝트의 Prometheus 한 개만 중지하고 컨테이너와 named volume을
보존합니다.

```bash
podman compose -f prometheus/docker-compose.yml stop prometheus-dev
```

다시 시작할 때는 `up -d --wait`를 사용합니다. legacy `prometheus`, external-dev 앱,
PostgreSQL, Redis와 다른 관측 서비스는 건드리지 않습니다. `down -v`, `prune`, 전체
컨테이너 stop은 사용하지 않습니다.
