# config-server beginner guide

## 한 문장 요약

`config-server`는 여러 서비스가 각자 들고 있던 설정 파일을 한 곳에서 나눠 주는 중앙 설정 서버입니다.

초보자 관점에서는 프랜차이즈 본사 레시피 서버입니다. 각 지점 서비스는 켜질 때 본사에 접속해 자기 레시피 파일을 받아갑니다.

## 왜 필요한가

서비스가 많아지면 포트, DB, Eureka, Gateway 라우트, JWT 키 같은 설정을 각 모듈에서 따로 관리하기 어렵습니다. `config-repo`에 모아두면 같은 규칙으로 확인할 수 있고, 운영에서는 Git 저장소나 Vault 같은 방식으로 확장할 수 있습니다.

## 현재 로컬 구조

- `config-server`: 설정을 제공하는 서버.
- `config-repo`: 실제 설정 파일이 있는 폴더.
- 각 서비스 `application.yml`: `optional:configserver:http://localhost:8888/`로 Config Server를 조회.

`optional`이므로 Config Server가 꺼져 있어도 일부 테스트는 실행될 수 있습니다. 하지만 실제 포트와 라우트까지 맞춰 실행하려면 Config Server를 먼저 켜야 합니다.
