# 모노레포 컨테이너 빌드 현대화 RFC

- 상태: **조건부 제안(Proof of Concept 필요)**
- 추적: GitHub Issue [#642](https://github.com/skyg547/account/issues/642)
- 조사 기준: `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc`, 2026-09-22
- 결정 요약: JVM 실행 이미지에는 Google Jib을 우선 후보로 삼아 `:discovery` 단일 모듈 PoC를 수행한다. 현재 Dockerfile은 PoC와 단계적 전환 동안 유지한다. Next.js는 운영 `Containerfile`과 개발 `Containerfile.dev`를 계속 사용한다.
- 이번 변경은 설계 문서뿐이다. Gradle 플러그인, Dockerfile/Containerfile, Compose, 애플리케이션 코드, CI와 레지스트리 동작은 바꾸지 않는다.

## 1. 문제와 조사 범위

### 1.1 재현 가능한 현재 인벤토리

추적 파일과 [실행 이미지 manifest](../../deploy/image-targets.json)를 기준으로 집계했다. `Dockerfile`이라는 이름만 세어 “40+가 모두 같은 실행 이미지”라고 간주하지 않았다.

| 항목 | 확인 결과 | 의미 |
| --- | ---: | --- |
| Gradle project | 72 | `./gradlew projects --offline` 출력의 project 수. library/core/호환 aggregate도 포함한다. |
| `Dockerfile*` / `Containerfile*` | 71 | 정확한 Dockerfile 64, `Dockerfile.simple` 2, Containerfile 계열 5. |
| Compose YAML | 56 | 파일명에 `compose`가 포함된 추적 `.yml`/`.yaml`. |
| Canonical Java image target | 35 | manifest의 infra 3, API 17, Batch 15. |
| Canonical frontend target | 1 | `frontend/Containerfile`을 사용하는 Next.js 운영 이미지. |
| Canonical 밖의 container build definition | 35 | 공통 builder, legacy/archived, 개발 frontend, 도구와 독립 infra 이미지. Jib 일괄 전환 대상이 아니다. |

71개 정의의 용도별 분해는 다음과 같다.

| 분류 | 수 | 처리 원칙 |
| --- | ---: | --- |
| Canonical JVM | 35 | 이 RFC의 Jib/공통 builder 비교 대상. |
| Canonical frontend 운영 | 1 | Next.js Containerfile 유지. |
| 루트 공통 파라미터 builder | 2 | [Dockerfile](../../Dockerfile), [Containerfile](../../Containerfile). 방안 B의 이미 존재하는 기준선. |
| Legacy module Dockerfile | 16 | manifest 비정본. 사용처 확인 없이 삭제하지 않는다. |
| Archived skeleton | 2 | 마이그레이션 근거 보존. 실행 대상으로 세지 않는다. |
| Frontend alternate/development | 2 | `frontend/Dockerfile`, `frontend/Containerfile.dev`. 개발/호환 목적을 분리한다. |
| Tooling Containerfile | 2 | 제한된 외부개발 builder와 prebuilt-JAR packager. |
| 독립 infra Dockerfile | 11 | Kafka, Redis, Vault, observability 등. JVM Gradle Jib 대상이 아니다. |

집계 재현 명령은 환경 파일이나 credential을 읽지 않는다.

```bash
git ls-files '*Dockerfile*' '*Containerfile*'
git ls-files '*compose*.yml' '*compose*.yaml'
./gradlew projects --offline --console=plain --no-daemon --max-workers=1
```

현재 canonical Java Dockerfile 35개는 합계 903줄이고 파일당 25~37줄이다. 경로와 주석 때문에 파일 hash는 서로 다르지만 다음 계약은 35/35에서 반복된다.

- builder: `gradle:8.7-jdk17-alpine`
- runtime: `eclipse-temurin:17-jre-alpine`
- `USER gradle`로 `bootJar` 생성 후 정확히 한 개의 non-plain JAR 선택
- runtime `USER app:app`
- `-XX:MaxRAMPercentage=75.0`과 entropy JVM option
- `java -jar /app/app.jar`

차이는 작지만 중요하다. Config Server, Discovery, Gateway 세 이미지만 `Asia/Seoul`의 OS timezone data를 설치하고, Config Server와 Discovery 두 이미지만 image-level `HEALTHCHECK`를 가진다. 따라서 단순 문자열 치환으로 35개를 한 파일로 합치면 이 예외를 잃는다.

### 1.2 현재 병목의 정확한 범위

현재 Dockerfile은 저장소 전체를 복사하고 루트 wrapper로 특정 `bootJar`를 실행한다. 예를 들어 [Discovery Dockerfile](../../discovery/Dockerfile)은 저장소 루트 context와 `:discovery:bootJar`를 요구한다. 이 구조는 작은 소스 수정에도 builder context 전송과 Gradle 작업을 다시 평가하고, 최종 fat JAR을 하나의 application layer로 복사한다.

다만 Jib이 **모듈의 독립 checkout 빌드 문제까지 해결하지는 않는다**. `:discovery`도 루트 `settings.gradle`, 공통 [루트 build](../../build.gradle), wrapper와 project dependency가 필요하다. Jib은 Docker build context와 Docker daemon 의존을 없앨 수 있지만 Gradle 모노레포 의존 그래프를 없애는 도구가 아니다. 독립 module build가 필요하면 별도의 composite build, dependency publication 또는 저장소 분리 결정이 필요하다.

또한 “코드 한 줄 변경은 항상 0.5초” 같은 수치를 현 상태의 사실로 채택하지 않는다. Jib은 기본 exploded mode에서 dependency, resource, class 등 변경 빈도가 다른 파일을 분리하고 바뀐 layer만 다시 만든다. 공식 문서도 이 layering과 cache 재사용을 설명하지만, 이 저장소의 4 CPU 호스트, project dependency, registry와 cache 조건의 실제 결과는 PoC로 측정해야 한다. Jib의 layer 수를 고정된 “정확히 4개”로 계약하지 않는다.

## 2. 목표, 비목표와 결정 기준

### 목표

1. 35개 JVM 실행 이미지의 중복 정책을 한 곳에서 관리한다.
2. Java 17, 실행 JAR, non-root, JVM option, timezone, readiness와 immutable digest 계약을 보존한다.
3. Docker daemon이 없는 CI push와 제한된 폐쇄망 tar 전달 경로를 정의한다.
4. `linux/amd64`와 `linux/arm64`를 검증 가능한 방식으로 게시한다.
5. JVM과 Next.js 빌드의 서로 다른 생명주기를 명시적으로 공존시킨다.
6. 한 모듈부터 성능과 동작 동등성을 측정하고 실패 시 즉시 기존 digest로 복귀한다.

### 비목표

- 이 RFC에서 Dockerfile, Containerfile, Compose 또는 source를 삭제하거나 수정하지 않는다.
- JDK 17을 21/25로 올리거나 Spring Boot/Gradle을 업그레이드하지 않는다.
- 11개 독립 infra image와 Next.js를 Jib으로 빌드하지 않는다.
- `core`, library, legacy aggregate를 실행 이미지로 승격하지 않는다.
- registry credential, 사내 URL, 인증서를 저장소에 기록하지 않는다.
- PoC 전에 현재 35개 Dockerfile의 canonical 지위를 변경하지 않는다.
- 문서상의 명령을 현재 구현된 task 또는 성공한 image build로 오인하지 않는다.

### 우선순위

동작·보안 동등성 > rollback 가능성 > 재현성 > warm rebuild/push 효율 > cold build 시간 > 이미지 크기 순서로 판단한다. 빠르더라도 readiness, 사용자, digest 또는 폐쇄망 재현성이 깨지면 채택하지 않는다.

## 3. 방안 A/B 비교

### 방안 A — Google Jib

Gradle 실행 project에만 `com.google.cloud.tools.jib`을 적용하고 공통 convention이 base image, JVM option, user, label과 output을 관리한다. Jib Gradle plugin은 registry push(`jib`), local daemon load(`jibDockerBuild`), tar 생성(`jibBuildTar`)을 제공한다. 공식 Gradle 문서 기준 현재 예시는 3.5.4이며, 실제 도입 시 version catalog/검증 정책과 호환성을 별도 승인한다.

### 방안 B — 공통 파라미터 Containerfile

이미 존재하는 루트 [Containerfile](../../Containerfile)처럼 `GRADLE_PROJECT`와 `JAR_DIRECTORY`를 인자로 받아 한 target을 빌드한다. 제한된 개발 스택은 이미 [one-worker builder](../../tools/Containerfile.minimal-auth-java)를 사용한다. 이 패턴을 canonical 35개로 확장하려면 manifest가 두 인자를 소유하고 build 도구와 Compose가 공통 파일을 가리키게 한다.

| 기준 | A. Jib | B. 공통 Containerfile |
| --- | --- | --- |
| 중복 제거 | 실행 project 목록과 Gradle convention으로 중앙화 | 공통 파일과 manifest argument로 중앙화 |
| application layering | exploded class/resource/dependency layer가 기본이며 source-only 변경의 upload 절감 가능 | 현재처럼 fat JAR 한 layer. Spring Boot layered jar 또는 명시적 추출을 추가해야 세분화 가능 |
| Docker daemon | registry/tar에는 불필요. local daemon target만 daemon 필요 | Docker/Podman/BuildKit daemon 또는 builder 필요 |
| 모노레포 checkout | 여전히 필요 | 전체 repository build context가 필요 |
| 임의 OS 조작 | `RUN` 없음. 별도 base image 또는 `extraDirectories`로 해결 | `RUN`, package 설치, user 생성, shell probe를 직접 표현 가능 |
| 현재 동작 동등성 | timezone/user/healthcheck를 의식적으로 재설계해야 함 | 기존 Dockerfile 행위를 가장 쉽게 보존 |
| 재현성 | 입력과 base digest 고정 시 재현 가능한 metadata/layer 생성에 강점 | BuildKit, package repository와 Dockerfile 명령의 결정성에 좌우됨 |
| multi-architecture | 여러 platform의 registry manifest list push 지원. multi-platform local daemon/tar는 지원하지 않음 | buildx/Podman 기능으로 manifest와 arch별 output 구성 가능하지만 별도 도구/에뮬레이션 검증 필요 |
| 폐쇄망 | cache와 plugin/dependency/base 선반입 후 single-platform `jibBuildTar --offline`; 깨끗한 환경에서 자동 해결되지 않음 | base/builder image, Gradle cache와 context를 선반입해야 함. `--network none` 통제가 직관적 |
| private registry | 직접 push, credential helper와 mirror 설정 지원 | builder 로그인/credential store와 별도 push 단계 필요 |
| 개발자 진입점 | Gradle task로 JVM build와 결합 | Docker/Podman 명령으로 언어 독립적 |
| frontend/infra | 부적합 | 동일 빌더 계열로 유지 가능 |
| rollback | 기존 Dockerfile image와 digest를 병행하면 쉬움 | 기존 구조와 유사하여 가장 단순 |
| 주요 위험 | Gradle 결합, healthcheck/OS customization, Jib cache 운영, multi-arch tar 제한 | daemon 권한, 전체 context, fat JAR cache invalidation, builder image 중복 다운로드 |

### 3.1 권고 결정

**A를 JVM의 목표 상태로 조건부 채택하고 B를 대조군·호환 fallback으로 유지한다.** 즉, 지금 35개 Dockerfile을 삭제하는 “전면 도입”은 승인하지 않고 다음 순서를 따른다.

1. `:discovery`만 Jib 설정을 추가하는 후속 PoC에서 기존 Dockerfile 결과와 비교한다.
2. 필수 동등성 gate와 성능 gate가 통과하면 manifest에 target별 builder 종류를 표현하고 JVM target을 wave 단위로 전환한다.
3. 전환 중 현재 Dockerfile과 공통 Containerfile을 rollback 경로로 유지한다.
4. 모든 canonical target과 CI/Compose/게시 도구가 새 경로를 검증한 뒤에만 중복 Dockerfile 삭제를 별도 Issue로 제안한다.
5. 임의 OS package나 shell이 필요한 예외 target은 조직 공통 base image 또는 방안 B를 사용한다. Jib 설정 안에서 기능별 편법을 늘리지 않는다.

이 선택은 Jib 자체가 무조건 우월해서가 아니라, 35개 JVM target에서 Docker daemon 제거와 application-layer cache라는 추가 이익을 측정할 가치가 있기 때문이다. 동등성만 필요하고 cache 이익이 관측되지 않으면 B가 더 단순한 결론이 될 수 있다.

## 4. 제안 JVM 빌드 계약

### 4.1 적용 대상과 단일 진실 원천

[image-targets.json](../../deploy/image-targets.json)을 계속 실행 대상의 단일 진실 원천으로 둔다. `subprojects { apply plugin: jib }`처럼 72개 전체에 적용하지 않는다. `java-infra`, `java-api`, `java-batch` 중 `enabled=true`인 target만 명시적으로 opt-in한다. `core`, contracts, shared-kernel, compatibility aggregate, migration utility에는 image task를 만들지 않는다.

후속 구현에서는 다음 정보를 manifest 또는 검증된 convention mapping 한 곳이 소유해야 한다.

- Gradle project와 image repository name
- builder 종류(`dockerfile` 또는 `jib`)와 rollout 상태
- Java major version과 base image **digest**
- main class 자동 탐지 결과 또는 명시값
- JVM flag, port, label, non-root identity
- timezone 예외와 health policy
- target platform, tar/output 경로와 registry promotion 규칙

Jib configuration을 root `build.gradle`의 거대한 조건문으로 키우기보다, PoC 후 검증된 convention plugin으로 옮기는 것을 권고한다. 단, PoC 하나를 위해 미리 추상화하지 않는다.

### 4.2 image 내용과 entrypoint

- 기본 `containerizingMode=exploded`를 검증한다. 현재 “non-plain bootJar 정확히 1개” 정책과 결과가 다르므로 classpath/main class 및 Spring Boot resource loading을 회귀 시험한다.
- Java toolchain과 runtime은 17을 유지한다.
- 기존 `MaxRAMPercentage=75.0`과 entropy option을 보존한다. Batch/저자원 prebuilt 경로의 65%는 별도 정책이며 전역 75%로 덮지 않는다.
- image에 profile, DB URL, credential, config-repo 내용을 넣지 않는다. 현재처럼 runtime environment/read-only volume으로 주입한다.
- Jib 기본 재현 시간을 유지하고 `USE_CURRENT_TIMESTAMP`를 사용하지 않는다. 동일 source와 동일 base digest의 연속 build가 같은 image digest인지 확인한다.
- image tag는 사람이 읽는 보조 식별자이며 배포와 rollback은 digest를 사용한다.

### 4.3 non-root와 쓰기 경로

현재 Java Dockerfile은 `app:app`을 만들지만 숫자 UID/GID를 계약하지 않는다. Jib은 `RUN adduser`를 실행하지 않으므로 다음 중 하나를 PoC에서 선택해야 한다.

1. 승인된 공통 base image가 고정 숫자 UID/GID와 필요한 CA/timezone 파일을 제공한다.
2. upstream base에서 작동하는 고정 숫자 UID/GID를 Jib `container.user`에 지정하고 `/app`, `/tmp`, volume 권한을 실기동으로 검증한다.

사용자 이름 문자열만 옮겨 존재하지 않는 계정을 가리키거나 root로 되돌아가는 것은 허용하지 않는다. 운영의 read-only root filesystem, `cap_drop: ALL`, `no-new-privileges`, tmpfs 계약은 [production Compose](../../compose.prod.yml)에서 계속 강제한다. 숫자 UID/GID 표준화는 현재 계약의 단순 보존이 아니라 새 정책이므로 PoC 결과와 함께 승인한다.

### 4.4 healthcheck와 timezone

현재 일반 API의 readiness는 주로 Compose가 담당하고 Config Server/Discovery만 image에도 probe가 있다. Compose probe는 `CMD-SHELL`과 `wget`을 사용한다. Jib plugin configuration에는 이 Dockerfile `HEALTHCHECK`를 그대로 옮길 1:1 계약이 없으므로 다음을 적용한다.

- 1차 Temurin PoC에서는 기존 Compose readiness를 반드시 실행하고 image-level probe가 사라지는 차이를 기록한다.
- image 자체 health metadata를 release 요건으로 유지한다면 Jib 확장 또는 custom base 편법보다, Compose/배포 플랫폼의 외부 HTTP probe를 canonical contract로 승격하는 후속 변경을 우선한다.
- probe가 없다는 이유로 process start만 성공한 상태를 healthy로 간주하지 않는다.
- Config Server → Discovery → client의 `service_healthy` 기동 순서를 그대로 시험한다.

`Asia/Seoul` OS timezone은 플랫폼 세 target의 현행 예외다. 1차 PoC에서 Discovery의 Java default timezone, log timestamp와 `/actuator` 동작을 대조한다. `-Duser.timezone=Asia/Seoul`만으로 충분한지, OS `/etc/localtime`까지 필요한지는 별도 측정 결과로 결정한다. 35개 전체에 timezone package를 무조건 추가하지 않는다.

## 5. Base image 결정

### 5.1 Temurin과 Distroless 비교

| 기준 | `eclipse-temurin:17-jre-alpine` | `gcr.io/distroless/java17-debian13:nonroot` |
| --- | --- | --- |
| 1차 PoC 비교 가능성 | 현재 runtime과 같아 builder 방식만 격리 가능 | libc, OS, user, 도구가 동시에 바뀌어 원인 분리가 어려움 |
| JVM 계열 | Eclipse Temurin | Distroless Java 17도 Temurin OpenJDK를 포함하므로 JVM vendor 교체가 핵심 차이는 아님 |
| OS/runtime | Alpine/musl, shell·BusyBox 기반 도구 사용 가능 | Debian 계열 최소 runtime, package manager와 기본 shell 없음 |
| 현재 readiness | `wget`/`CMD-SHELL` 계약을 유지하기 쉬움 | 현재 Compose probe와 직접 호환되지 않음 |
| 공격 표면/scan noise | 더 많은 OS 도구와 package | 필요한 runtime 중심으로 축소 가능 |
| 장애 조사 | exec와 기존 runbook이 쉬움 | production image 내부 shell 조사 불가. 별도 debug image/ephemeral debug 절차 필요 |
| architecture | upstream manifest 범위를 검증해야 함 | 공식 Java 17 Debian 13은 amd64, arm64 등 여러 architecture 제공 |

### 5.2 권고 순서

1. **Discovery Jib 1차 PoC는 `eclipse-temurin:17-jre-alpine`의 승인 digest를 사용한다.** Jib 효과와 base image 효과를 섞지 않는다.
2. Jib 동등성 통과 뒤 동일 application layer를 Temurin과 Distroless에 각각 올리는 2차 A/B를 수행한다.
3. Distroless 채택 전 현재 `wget` probe를 외부 probe 또는 shell 없는 exec 방식으로 교체하고, 인증서, DNS, locale/timezone, heap dump/JFR, `/tmp`, crash log와 운영 디버깅을 검증한다.
4. production에 `debug`/`debug-nonroot` tag를 사용하지 않는다. 장애 분석용 image는 동일 digest 계보와 제한된 접근 절차를 갖춘 별도 운영 자산으로 둔다.
5. tag가 아니라 signature를 검증한 digest를 내부 registry에 mirror하고 정기 rebuild/취약점 대응 주기를 정한다.

Distroless 공식 문서는 shell/package manager가 없고 Java 17 Debian 13이 multi-architecture index와 `nonroot` tag를 제공한다고 명시한다. 작은 image라는 일반론만으로 이 저장소의 최종 image가 더 작거나 더 안전하다고 단정하지 않는다. 실제 SBOM과 scanner 결과를 비교한다.

## 6. Multi-architecture, 폐쇄망 tar와 registry 정책

### 6.1 Multi-architecture

목표 platform은 우선 `linux/amd64`, `linux/arm64` 두 개다. 현재 [GHCR 가이드](ghcr-container-registry-guide.md)는 multi-arch manifest를 조립하지 않는다고 명시하므로 이는 신규 기능이다.

Jib은 base manifest에서 여러 platform을 선택해 registry에 Docker manifest list로 push할 수 있다. 그러나 공식 FAQ가 명시하듯 multi-platform build는 local Docker daemon output과 `jibBuildTar` output을 지원하지 않고 OCI image index에도 제한이 있다. 따라서 다음 두 경로를 분리한다.

- 연결된 CI: architecture별 image digest를 push한 뒤 Jib이 생성한 manifest-list digest, 각 child digest와 architecture를 기록한다.
- 폐쇄망 전달: architecture별 **단일-platform tar**를 따로 만들고 checksum, platform, base/application digest를 inventory에 기록한다. 승인된 내부 registry에서 필요하면 별도 도구로 manifest를 조립한다.

QEMU를 썼다는 이유만으로 실행 호환성을 주장하지 않는다. amd64/arm64 native runner 각각에서 `java -version`, non-root, readiness, smoke test를 통과해야 한다. base image와 모든 native library가 두 architecture를 제공하지 않으면 해당 target의 multi-arch 승격을 차단한다.

### 6.2 폐쇄망 tar는 사전 공급망이 필요하다

`jibBuildTar`는 image tar를 만들 수 있지만 `--offline`은 인터넷 없는 빈 머신에서 dependency와 base layer를 만들어 주지 않는다. 사전 반입 목록은 다음을 포함한다.

- Gradle distribution, Jib plugin과 모든 build/runtime dependency의 검증된 cache
- architecture별 base image digest/layer 또는 승인된 `tar://` base
- source revision, lock/checksum metadata, CA와 내부 registry trust
- 산출 image tar의 SHA-256, image/config/layer digest, SBOM, scan 결과와 provenance
- load 후 동일 digest와 platform을 확인하는 명령 및 결과

연결 구역에서 cache를 한 번 “따뜻하게 한 개인 작업 디렉터리”를 복사하는 방식은 표준 공급망이 아니다. 읽기 전용 artifact bundle을 생성하고 checksum/signature로 반입 승인하며, 유효 기간과 폐기 절차를 둔다. credential과 registry auth 파일은 bundle에 넣지 않는다.

후속 PoC의 single-platform 예시는 다음과 같다. 현재 branch에는 Jib task가 없으므로 지금 실행 가능한 명령이 아니다.

```bash
./gradlew :discovery:jibBuildTar \
  --offline --console=plain --no-daemon --max-workers=1 \
  -Djib.to.image=account/discovery:poc-jib

docker load --input discovery/build/jib-image.tar
# 또는 승인된 Podman load 경로
```

완전 폐쇄망 시험은 네트워크 namespace/방화벽으로 외부 연결을 차단한 깨끗한 runner에서 수행한다. 기존 공유 cache를 삭제하거나 전역 image prune을 실행하지 않고 작업별 임시 cache와 engine namespace를 사용한다.

### 6.3 Private registry

Harbor, ECR, GHCR 등 제품별 URL을 build logic에 하드코딩하지 않고 배포 환경이 target repository를 주입한다.

- base image는 허용된 내부 mirror 경로와 digest를 사용한다. public registry fallback을 production CI에서 허용하지 않는다.
- registry는 TLS를 사용하고 사설 CA를 runner Java truststore에 배포한다. `allowInsecureRegistries`와 HTTP credential 전송은 금지한다.
- auth는 workload identity 또는 최소 권한 credential helper를 우선한다. 사용자명/비밀번호를 Gradle 파일, command line, 로그, image layer에 넣지 않는다.
- pull, stage push, promotion 권한을 분리한다. CI는 필요한 repository만 push하고 운영 배포자는 digest만 pull한다.
- mutable tag push 뒤 digest를 기록하고 scan/signature/policy gate를 통과한 동일 digest만 release tag 또는 production reference로 승격한다.
- base와 application cache는 runner 간 공유 가능하지만 tenant/repository 권한과 cache poisoning 방어, TTL, 크기 상한을 둔다.
- direct Jib push 실패가 local build 성공으로 위장되지 않도록 registry 응답, digest와 manifest를 다시 읽어 검증한다.

## 7. JVM과 Next.js의 이원화 공존

Jib은 JVM application용 도구다. 프론트엔드를 Jib에 맞추거나 모든 container build가 하나의 도구여야 한다는 목표를 두지 않는다.

| 대상 | 개발 build | 운영/release build | 유지 이유 |
| --- | --- | --- | --- |
| JVM 35개 | Gradle test/bootRun, 필요 시 Jib daemon load | Jib registry push 또는 arch별 tar; rollout 중 Containerfile fallback | JVM dependency/class layering과 daemonless push |
| Next.js | [Containerfile.dev](../../frontend/Containerfile.dev), source bind mount, named `node_modules`/`.next`, `next dev` | canonical [frontend/Containerfile](../../frontend/Containerfile), standalone output, UID/GID 1001 | npm/Next standalone과 개발 hot reload는 JVM lifecycle과 다름 |
| 독립 infra 11개 | 기존 Dockerfile/Compose | upstream/internal mirror 정책 | Gradle project가 아니며 vendor image customization 필요 |

`Containerfile.dev`는 운영 image 정의가 아니다. 운영은 계속 standalone server/static/public만 담고 `GATEWAY_INTERNAL_URL`과 BFF secret을 runtime에 주입한다. frontend image도 JVM과 같은 immutable digest, scan/signature, amd64/arm64 gate를 적용할 수 있지만 build 구현은 분리한다.

공통화할 것은 도구가 아니라 결과 계약이다.

- OCI/Docker image 형식과 image label(source revision, build identity)
- 허용 registry, immutable digest, SBOM/scan/signature와 provenance
- non-root/read-only filesystem, secret 비포함, health/readiness
- architecture inventory, promotion/rollback과 보존 기간

## 8. `:discovery` 단계적 PoC

### 8.1 후보 선정 이유와 제한

Discovery는 단일 Java infra project이고 DB migration이나 금융 상태 변경이 없어 첫 대상에 적합하다. 동시에 timezone, non-root, image/Compose readiness, Config Server 선행 조건을 모두 가져 “쉬운 happy path”만 측정하는 것을 피한다.

PoC는 Discovery가 전체 API/Batch를 대표한다고 가정하지 않는다. project dependency가 많은 API와 장기 실행 Batch는 후속 대표 target에서 별도로 확인한다.

### 8.2 Phase 0 — 기준선 고정

1. 동일 commit, JDK/Gradle, CPU 4개, memory limit, registry 위치와 네트워크 조건을 기록한다.
2. current [Discovery Dockerfile](../../discovery/Dockerfile)의 base digest와 결과 image digest를 기록한다.
3. `:discovery:test`, `:discovery:bootJar`, current Dockerfile build와 readiness smoke를 통과시킨다.
4. cold는 작업별 빈 cache 3회, warm은 각 방식 1회 예열 뒤 source-only 변경과 resource/dependency 변경을 각 5회 측정한다. 중앙값과 범위를 보고한다.
5. 공유 Gradle/Docker/Jib cache를 지우지 않는다. 별도 임시 cache와 별도 tag를 사용하고 전역 prune을 금지한다.

측정값은 wall-clock, CPU time/peak RSS, 결과 image size, 생성/재사용 layer, registry push bytes/time, image digest, readiness 도달 시간이다. 로그에 credential이나 registry 내부 주소를 남기지 않는다.

### 8.3 Phase 1 — Jib 설정과 정적 계약

후속 PoC branch에서만 다음을 구현한다.

- Jib plugin version/checksum 고정, `:discovery`만 opt-in
- Temurin 17 Alpine base digest 고정
- exploded mode, main class, port, JVM option, label과 non-root user 설정
- deterministic output path와 `poc-jib-<commit>` tag
- manifest/정책 테스트에 Jib target을 추가하되 기존 Dockerfile assertion은 유지
- source와 image에 credential/config-repo/environment profile이 포함되지 않는 검사

현재 `container-images.ps1`, `ContainerImagePolicyTest`와 Compose는 모듈 Dockerfile을 명시적으로 요구한다. 이 테스트를 무시하거나 삭제하지 않고 dual-path PoC를 표현하도록 확장한다.

### 8.4 Phase 2 — 동작 동등성

아래는 후속 branch에서 실행할 최소 gate다.

```bash
./gradlew :discovery:test :discovery:bootJar \
  --offline --console=plain --no-daemon --max-workers=1

./gradlew :discovery:jibBuildTar \
  --console=plain --no-daemon --max-workers=1 \
  -Djib.to.image=account/discovery:poc-jib

./gradlew :discovery:jibDockerBuild \
  --console=plain --no-daemon --max-workers=1 \
  -Djib.to.image=account/discovery:poc-jib
```

Docker와 Podman load/run 경로를 각각 지원한다고 결정했다면 둘 다 검사한다. 적어도 다음 항목은 자동 또는 기록 가능한 smoke evidence가 있어야 한다.

- Java 17과 기대 main class/entrypoint
- UID 0이 아닌 고정 user, read-only root filesystem과 `/tmp` tmpfs
- JVM option 75%, timezone/log timestamp 동등성
- Config Server healthy 후 Discovery 시작과 `/actuator/health/readiness` `UP`
- 일회성 synthetic client의 register → registry lookup → renew → cancel 수명주기와 cancel 뒤 stale registration 부재
- stop signal과 grace period, 비정상 종료 코드, restart 동작
- source/config/secret 비포함, SBOM/scan 결과, base/application digest
- 동일 입력 2회 build의 동일 digest

### 8.5 Phase 3 — Registry, architecture와 폐쇄망

1. 격리된 staging repository에 Jib direct push하고 returned digest를 다시 조회한다.
2. amd64/arm64 native runner에서 각각 smoke를 실행하고 manifest-list child digest를 확인한다.
3. architecture별 tar를 생성해 checksum inventory와 함께 폐쇄망 runner로 반입한다.
4. 외부 네트워크가 차단된 깨끗한 runner에서 `--offline` build/load/run/readiness를 재현한다.
5. 기존 Dockerfile image digest로 되돌려 같은 Config Server/Compose 조건에서 readiness와 synthetic client 재등록/registry 재구성을 재확인한다.

### 8.6 Go / No-Go 기준

#### 필수 동등성 gate — 하나라도 실패하면 No-Go

- test, bootJar, start, readiness와 Config Server 의존 순서가 모두 통과한다.
- synthetic client의 register/lookup/renew/cancel과 rollback 뒤 재등록이 성공하며 stale instance가 남지 않는다.
- Java 17, non-root, JVM option, timezone, read-only/tmpfs와 secret 비포함 계약이 보존된다.
- base가 digest로 고정되고 동일 입력 build digest가 재현된다.
- amd64와 arm64 모두 native smoke를 통과한다. 조직이 단일 architecture만 승인하면 multi-arch rollout을 명시적으로 보류한다.
- 폐쇄망을 지원한다고 표기하려면 빈 runner offline tar build/load/run이 재현된다.
- 기존 Dockerfile image digest로 15분 안에 복귀하고 readiness를 회복하는 연습이 성공한다.

#### 효율 gate — JVM 기본값을 Jib으로 바꾸기 위한 조건

- warm source-only 변경 5회 중앙값이 current Dockerfile build/push보다 **20% 이상 짧거나**, registry 전송 application bytes가 **50% 이상 적다**.
- cold build 중앙값이 기준선보다 20% 넘게 느려지지 않는다.
- runtime image size가 10% 넘게 증가하지 않고 high/critical vulnerability가 기준선보다 늘지 않는다. 예외는 보안 담당자가 근거와 만료일을 승인해야 한다.
- task/convention/운영 문서가 target별 Dockerfile보다 유지하기 어렵다는 reviewer 판단이 없다.

첫 조건을 만족하지 못해도 daemonless CI가 필수이고 운영 복잡도가 줄었다는 정량 근거가 있으면 제한 채택을 제안할 수 있다. 그 경우 성능 향상으로 포장하지 않고 별도 승인 결정을 남긴다.

### 8.7 Rollout ladder

각 단계는 별도 PR, immutable digest와 rollback 연습을 갖는다.

1. Discovery 한 개 shadow build; 배포 reference는 기존 image 유지
2. Discovery development canary
3. Config Server/Gateway 등 Java infra — 각 target 고유 health/timezone 검증
4. Auth API 등 대표 stateless API 하나
5. 대표 Batch 하나 — 종료 코드, 재시작 금지, job parameter와 재실행 정합성 확인
6. 도메인별 2~4개 wave로 나머지 Java target 전환
7. 모든 소비자와 CI가 Jib digest를 사용한 뒤 Dockerfile 제거 여부를 별도 RFC/Issue로 결정

한 wave가 실패하면 다음 wave를 중단한다. 이미 성공한 target까지 자동으로 되돌리지 않고 영향과 공통 원인을 평가한다.

### 8.8 Rollback

- 기존 Dockerfile, Compose와 Dockerfile-built digest를 PoC와 rollout 동안 보존한다.
- Jib image는 별도 repository/tag/digest로 병행하고 기존 tag를 덮어쓰지 않는다.
- rollback은 배포 reference를 마지막 검증 Dockerfile digest로 바꾸고 컨테이너를 순차 재생성한 뒤 readiness, client 재등록과 registry 재구성을 확인한다.
- registry image, cache, volume을 자동 삭제하거나 `image prune`, `down -v`를 실행하지 않는다.
- Gradle/Jib 설정 rollback은 reviewed revert로 수행하고 shared history를 보존한다.
- DB schema/data 변화가 없으므로 Discovery PoC의 data rollback은 없다. 후속 Batch/API rollout은 각 업무 모듈의 독립 rollback gate를 추가한다.

## 9. 위험, 미결정 사항과 후속 Issue

| 항목 | 현재 결론 | 후속 결정/증거 |
| --- | --- | --- |
| Jib version과 공급망 | 문서 예시 version을 바로 채택하지 않음 | plugin portal artifact/checksum, Gradle 8.7/Boot 3.2.5 호환성 |
| Jib vs common Containerfile 성능 | 가설 | 4 CPU 동일 조건 benchmark raw result |
| non-root UID/GID | root 금지, 숫자 표준은 미정 | volume/tmpfs/runtime 권한과 조직 base image 여부 |
| healthcheck | Compose readiness 보존 | image metadata 필요 여부와 shell 없는 probe 설계 |
| timezone | Discovery 현행 동등성 우선 | JVM flag만으로 충분한지 OS timezone 필요 여부 |
| Distroless | 2차 후보 | readiness/debug/native library/SBOM/scan A/B |
| Multi-arch local tar | Jib 단일 tar로 묶지 않음 | arch별 tar inventory와 내부 manifest 조립 도구 |
| Private registry | TLS/digest/credential helper 원칙 | Harbor/ECR/GHCR별 identity, CA, retention/promotion runbook |
| Build cache | 공유 가능하나 신뢰 경계 필요 | namespace, eviction, poisoning 방어와 용량 |
| Dockerfile 삭제 | 이번 범위 밖 | 모든 35 target 전환과 rollback 보존 기간 이후 별도 승인 |

후속 Issue는 최소한 `Discovery Jib PoC`, `base image/readiness A/B`, `multi-arch and air-gap bundle`, `manifest/CI/Compose rollout`, `legacy Dockerfile retirement audit`로 나눈다. 한 PR에 plugin 도입, base 교체, 전체 Compose 변경과 파일 삭제를 함께 넣지 않는다.

## 10. 외부 근거와 해석 한계

- [Jib Gradle plugin 문서](https://github.com/GoogleContainerTools/jib/blob/master/jib-gradle-plugin/README.md): registry/daemon/tar task, exploded mode, base image, platform, cache, credential helper와 mirror 설정.
- [Jib FAQ](https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md): application layering, 재현 build, offline cache와 multi-platform output 제한.
- [Distroless README](https://github.com/GoogleContainerTools/distroless/blob/main/README.md): shell/package manager 부재, Java 17 Debian 13의 tag/architecture와 signature 검증.
- [Distroless Java image 설명](https://github.com/GoogleContainerTools/distroless/blob/main/java/README.md): Java 17 image가 Temurin OpenJDK를 포함하고 `java -jar` 계열 entrypoint를 제공함.
- [Eclipse Temurin container 저장소](https://github.com/adoptium/containers/blob/main/README.md): 공식 image 계열과 지원 OS family.

외부 문서는 도구의 능력을 보여 줄 뿐 이 저장소에서의 성능, 보안 통과 또는 운영 적합성을 증명하지 않는다. RFC의 수치와 Go/No-Go는 저장소 기준선과 후속 PoC evidence로 판단한다.

## 11. 문서 변경 검증과 승인 경계

이 RFC PR은 다음 정적 검증만 수행한다.

```bash
git diff --check
rg -n '^(<<<<<<<|=======|>>>>>>>)' \
  docs/guides/container-build-modernization-rfc.md \
  docs/ai-harness/agent-status.md \
  docs/ai-harness/worklog.md \
  docs/ai-harness/handoff.md \
  docs/history/CODEX_WORKLOG.md
# Python 표준 라이브러리로 추적 inventory와 Markdown local link를 검사한다.
```

문서 PR의 성공은 Jib build, registry push, multi-arch, offline tar 또는 runtime 동등성 성공을 의미하지 않는다. Draft PR은 `Refs #642`로 사람 리뷰를 요청하며 Ready, merge, Issue close와 기존 Dockerfile 삭제는 별도 승인 gate다.
