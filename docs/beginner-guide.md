# App Beginner Guide

## 1. `app` 모듈은 무엇인가요

이 모듈은 회계 시스템의 "실행 껍데기"입니다.

쉽게 말하면:
- 각 업무 모듈은 부품
- `app`은 그 부품을 조립해서 실제로 켜는 본체

## 2. 왜 별도 `app` 모듈이 필요한가요

각 모듈은 저마다 기능을 갖고 있지만, 시스템을 실제로 실행하려면:
- 메인 클래스가 필요하고
- 공통 설정이 필요하고
- 어떤 모듈을 함께 띄울지 정해야 합니다

이 역할을 `app`이 맡습니다.

## 3. 초보자가 먼저 봐야 할 파일

1. `app/src/main/java/com/ho/account/AccountApplication.java`
2. `app/src/main/resources/application.yml`
3. `app/src/main/resources/application-dev.yml`
4. `app/src/main/resources/application-prod.yml`

## 4. 프로파일은 어떻게 이해하면 되나요

Spring Boot는 환경에 따라 다른 설정을 쓸 수 있습니다.

현재 구조:
- 기본: `application.yml`
- 개발: `application-dev.yml`
- 운영 예시: `application-prod.yml`

중요:
- 기본 활성 프로파일이 `local`인데, `application-local.yml`은 없습니다.
- 그래서 초보자는 이 부분이 현재 설정상 어색하다는 점을 알고 있어야 합니다.

## 5. 개발용 실행은 어떤 특징이 있나요

`dev` 프로파일에서는:
- H2 메모리 DB 사용
- H2 콘솔 사용 가능
- SQL 로그 자세히 출력
- Actuator 엔드포인트 전부 노출

즉, 디버깅과 개발 편의를 우선한 설정입니다.

## 6. 운영용 설정은 어떤 특징이 있나요

`prod` 프로파일에서는:
- MySQL 사용
- 계정정보는 환경변수 사용
- 스키마는 자동 변경하지 않고 검증만 함
- 로그를 과도하게 찍지 않음

즉, 안전성과 통제를 우선한 설정입니다.

## 7. 초보자가 자주 헷갈리는 점

### `app`에 비즈니스 로직이 거의 없는데 왜 중요한가요?

시스템이 아예 안 뜨거나, 잘못된 DB에 연결되거나, 프로파일이 안 맞는 문제는 대부분 `app` 설정에서 시작합니다. 그래서 코드가 적어도 중요합니다.

### `application.yml`과 `application.yaml`이 둘 다 왜 있나요?

현재 상태에서는 중복 베이스 설정처럼 보입니다. 유지보수 관점에서는 하나로 정리하는 편이 더 명확합니다.

## 8. 문서 추천 순서

1. [README.md](./README.md)
2. [process-flow.md](./process-flow.md)
3. [schema.md](./schema.md)

이 순서로 보면 `app`이 "무엇인지 -> 어떻게 켜지는지 -> 어떤 설정을 들고 있는지"를 빠르게 이해할 수 있습니다.
