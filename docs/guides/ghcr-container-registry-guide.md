# GHCR 컨테이너 이미지 일괄 게시

`tools/docker/push-ghcr.sh`는 이미 빌드된 로컬 이미지를
`ghcr.io/skyg547/account/<service>:<tag>`로 태깅하고 순차 푸시한다.
입력 목록은 [image-targets.json](../../deploy/image-targets.json)의 `enabled: true`
대상이다. 스크립트가 빌드, 로그인, pull 또는 배포를 실행하지는 않는다.
로컬 빌드 방법은 [컨테이너 이미지 계약](container-images.md)을 따른다.

## 게시 대상과 전제 조건

- Bash, Python 3(표준 라이브러리만 사용), Docker 또는 Podman이 필요하다.
- 실행할 엔진과 동일한 사용자/rootless 저장소에 모든 선택 이미지가 있어야 한다.
- 기본 소스는 `account/<service>:local`이다. Compose가 자동 생성한 이름은
  아래 빌드 명령처럼 표준 이름으로 준비해야 한다. Podman에서 명시적인
  `localhost/account`를 사용했다면 `--source-prefix localhost/account`를 지정한다.
- 기본 목록은 36개다. 15개 업무 모듈은 각각 API와 Batch(30개)를 게시한다.
  나머지는 Auth/Internal Audit API 2개와 플랫폼 3개, Frontend 1개다.

| 분류 | manifest 이름 |
| --- | --- |
| 업무 모듈 (각 `-api`, `-batch`) | account-mart, asset-lease, budget, closing, deposit, ecl, expenditure-resolution, journal-ledger, loan, master-data, payable, receivable, reconciliation, reporting, tax |
| 추가 API | auth-api, internal-audit-api |
| 플랫폼 | config-server, discovery, gateway |
| UI | frontend |

PostgreSQL/Redis/Kafka 같은 외부 배포 이미지와 manifest 밖의 migration-runner는
이 일괄 게시 목록에 포함하지 않는다. 새 실행 모듈은 기존 manifest 계약에 먼저
등록한다. library/core는 독립 컨테이너가 아니며 API/Batch 패키징 경계를 유지한다.
이 명령은 단일 로컬 이미지 게시용이며 다중 아키텍처 manifest 조립은 하지 않는다.

## 1. 빌드와 게시 계획 확인

아래 명령은 저장소 루트의 Bash에서 실행한다. `engine=docker`로 바꿔도 동일하다.
예시는 두 이미지 선택이며 전체 빌드는 위 컨테이너 이미지 계약을 참고한다.
개발 전용 Frontend 이미지 대신 `frontend/Containerfile`로 배포 산출물을 준비한다.

```bash
engine=podman
"$engine" build --file config-server/Dockerfile --tag account/config-server:local .
"$engine" build --file frontend/Containerfile --tag account/frontend:local frontend

# 버전과 commit을 조합하고 게시 이후 동일 태그를 다른 내용으로 재사용하지 않는다.
release_tag="v1.0.0-$(git rev-parse --short=12 HEAD)"
bash tools/docker/push-ghcr.sh --engine "$engine" --tag "$release_tag" --dry-run
```

기대 결과는 `DRY-RUN: 36 images`와 소스 → GHCR 매핑 36줄이다.
dry-run은 엔진 호출/로그인/로컬 이미지 검사를 하지 않으므로 이미지가 없어도
목록을 검토할 수 있다. 실제 실행에서는 선택한 모든 이미지가 필요하다.

## 2. PAT 인증과 권한 분리

로컬/개발 서버 게시에는 **PAT (classic)**의 `write:packages` 권한과 해당
패키지에 대한 쓰기 접근이 필요하다. 다운로드 전용 계정은 `read:packages`를
사용한다. SSO 조직은 토큰의 SSO 승인을 확인한다. `delete:packages`는 필요 없다.
PAT 범위를 선택할 때 불필요한 `repo` 권한이 함께 선택되지 않았는지 확인한다.
[GitHub 공식 인증 안내](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-with-a-personal-access-token-classic)

운영자가 직접 실행하는 다음 Bash 예시는 토큰을 명령줄 인수나 shell history에
붙여넣지 않는다. 토큰을 로그·Issue·PR·채팅에 기록하지 않는다.

```bash
(
  set +x
  set -euo pipefail
  engine=podman # Docker는 docker
  read -r -p 'GitHub username: ' ghcr_user
  read -r -s -p 'GHCR PAT (classic): ' ghcr_pat
  printf '\n'
  trap 'unset ghcr_pat' EXIT
  printf '%s' "$ghcr_pat" | "$engine" login ghcr.io --username "$ghcr_user" --password-stdin
  unset ghcr_pat
)
```

엔진이 관리하는 credential store를 사용하며 스크립트는 인증 파일/환경값을
읽거나 출력하지 않는다. Docker와 Podman의 로그인 저장소는 별개이므로 게시할
엔진으로 로그인한다. 작업 종료 후 전용 게시 세션에서만 `podman logout ghcr.io`
(Docker는 `docker logout ghcr.io`)로 인증을 해제한다.

처음 생성한 패키지는 기본 private이며, 경로에 `account`를 포함하는 것만으로
저장소 연결이 생기지는 않는다. 패키지 설정에서 `skyg547/account` 연결과 필요한
사용자/워크플로 접근 권한을 확인한다.
[GitHub 패키지 연결 안내](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#pushing-container-images)

향후 Actions에 연결할 때는 승인된 게시 job에 `contents: read`, `packages: write`와
`GITHUB_TOKEN`을 사용하고 패키지의 repository 접근을 설정한다. 이 변경은 수동/자동
호출 가능한 스크립트와 런북을 제공하며 이벤트 기반 CI workflow는 추가하지 않는다.
이미지 게시 권한, 배포 승인, PR Ready/merge/Issue close 권한은 각각 별도다.

## 3. 선택 또는 전체 게시

```bash
# 먼저 빌드한 두 개만 게시 (Docker도 같은 옵션)
bash tools/docker/push-ghcr.sh --engine podman --tag "$release_tag" \
  --target config-server --target frontend

# 모든 활성 이미지가 로컬에 준비된 경우 전체 게시
bash tools/docker/push-ghcr.sh --engine docker --tag "$release_tag"

# 별도 로컬 네임스페이스/소스 태그를 쓰는 경우
bash tools/docker/push-ghcr.sh --engine podman --tag "$release_tag" \
  --source-prefix localhost/account --source-tag build-715 --target gateway
```

| 옵션 | 기본값 / 동작 |
| --- | --- |
| `--tag` | 필수 목적지 태그. 1–128자 ASCII 영숫자/`_`로 시작하고 이후 `.`/`-`도 허용. 자동 `latest` 게시 없음 |
| `--engine` | `docker`; `podman` 선택 가능 |
| `--source-prefix` | `account`; scheme/태그 없는 소문자 repository prefix |
| `--source-tag` | `local`; 목적지와 동일한 태그 문법 |
| `--target` | 생략 시 모든 활성 대상. 이름을 하나씩 반복 지정. 알 수 없거나 disabled/중복 선택은 오류 |
| `--dry-run` | 계획만 출력; 엔진 설치와 인증이 없어도 실행 가능 |

순서는 입력 검증 → 전체 소스의 이미지 ID 검사 → ID를 고정하여 목적지 태깅 →
이미지별 순차 푸시다. 사전 검사 실패 시 태깅/푸시는 0건이다. tag 또는 push가
실패하면 즉시 non-zero로 종료하고 이미 성공한 수를 보고한다. 엔진 상세 출력은
credential helper 정보 노출을 피하려고 숨긴다. 성공한 이미지마다 `PUSHED`를,
모두 성공하면 `SUCCESS: pushed N/N images`를 출력한다.

**여러 패키지의 게시에는 원자성이 없다.** 중간 실패 시 이전 푸시는 유지된다.
릴리스 태그를 사용하는 동시 게시/로컬 재태깅은 피하고, 최종 N/N 확인 전에는
배포하지 않는다. 같은 소스 ID로 재실행하면 완료된 이미지도 다시 게시한다.
재빌드로 내용이 달라졌다면 새 릴리스 태그를 사용한다. 기존 원격 태그의 덮어쓰기를
서버에서 방지하는 기능은 이 스크립트에 없다.

## 4. 게시 후 확인과 배포

패키지 페이지에서 선택 대상 전체와 버전을 확인한 다음 필요한 이미지를 pull한다.

```bash
engine=podman # Docker는 docker
image="ghcr.io/skyg547/account/config-server:$release_tag"
"$engine" pull "$image"
"$engine" image inspect --format '{{json .RepoDigests}}' "$image"
```

출력된 `ghcr.io/skyg547/account/config-server@sha256:...`를 릴리스 기록에 보관한다.
로컬 이미지 ID는 registry manifest digest와 다를 수 있으므로 배포 digest로 복사하지
않는다. 운영은 [운영 Compose 가이드](production-compose.md)의 digest 고정 계약에
맞춰 검증된 `image@sha256:...`를 사용하고, health/DB migration은 별도로 검증한다.

## 실패 복구와 검증

- `local image preflight failed`: 동일 엔진/사용자 저장소의 소스 이름과 태그를 확인하고
  누락 이미지를 빌드한다. 이 단계는 엔진 접근 오류도 포함한다.
- `push failed`: 로그인 만료, PAT scope, 패키지 쓰기 권한/SSO와 네트워크를 확인한다.
  토큰 또는 인증 파일을 출력하지 말고 동일 엔진의 `login --password-stdin`을 재실행한다.
- 중간 실패/중단: 성공한 패키지와 원격 상태를 확인한 뒤 같은 소스 ID로 재시도한다.
  실패한 전송이 서버에서 완료됐을 가능성도 있으므로 `PUSHED` 로그만을 원격 진실로 보지 않는다.
- 릴리스 롤백은 이전에 검증한 digest로 배포한다. 이미지/볼륨 일괄 삭제와 prune은
  필요 없다. 도구 자체는 이 Issue 변경만 reviewed revert PR로 되돌릴 수 있다.

다음 로컬 계약 검사는 실제 registry 쓰기 없이 실행한다.

```bash
bash -n tools/docker/push-ghcr.sh
python3 -m unittest discover -s tools/docker -p 'test_push_ghcr.py' -v
bash tools/docker/push-ghcr.sh --engine podman --tag verify-715 --dry-run
git diff --check
```

테스트는 가짜 Docker/Podman으로 명령 인수, 36개 매핑, 소스 ID 고정, 전체 사전 검사,
입력 거부 및 실패 중단을 검증한다. 이는 실제 GHCR 인증/업로드 성공 증거가 아니다.
실제 게시 검증은 승인된 릴리스 이미지로 위 로그인 → 선택 게시 → pull/digest 확인을
수행하고 결과를 릴리스 기록에 남긴다.
