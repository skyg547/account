# config-server docs

`config-server`는 `config-repo`를 기술 독립적인 `EnvironmentRepository` 뒤에서 읽고 각 서비스에 중앙 설정을 제공하는 Spring Boot 인프라 애플리케이션입니다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md): 중앙 설정이 필요한 이유와 레시피 본사 비유.
2. [process-flow.md](./process-flow.md): application/profile 조회, 병합, readiness, 실패 흐름.
3. [local-run.md](./local-run.md): IntelliJ, Gradle, Docker, 종료 방법.
4. [../README.md](../README.md): 모듈 책임과 빠른 시작.
5. [../../config-repo/README.md](../../config-repo/README.md): 실제 native 설정 원본 관리 규칙.

## 현재 실행 계약

- Spring Boot 내장 WAS 포트: `8888`
- backend profile: `native`
- 기본 저장소: 저장소 루트의 `config-repo`
- Docker 저장소: `/config-repo` 읽기 전용 volume
- readiness probe: `master-data/default`에 property source가 하나 이상 있어야 `UP`
- 관측 endpoint: `health`, `info`, `prometheus`
- 데이터베이스: 사용하지 않음

## 검증 범위

- 실제 HTTP `Environment` 조회
- 정상/빈 결과/조회 예외 repository health
- application/native/readiness 설정 정책
- JDK 17 단일 `bootJar` Dockerfile
- 루트/모듈 Compose 저장소 경로와 15개 `service_healthy` 의존 조건
- IntelliJ 실행 인자

설정 값 자체는 테스트·문서·로그에 복사하지 않고 키 구조와 상태만 확인합니다.