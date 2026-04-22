# 🗂️ Master Data Service (기준 정보 관리)

## 1. 초보자를 위한 개념 설명
모든 회계/재무 처리의 근간이 되는 '변하지 않아야 할 기준 데이터'를 관리하는 핵심 도메인입니다.
예를 들어 "계정과목코드 101은 현금이다", "거래처코드 A01은 (주)카카오다" 같은 정보를 저장합니다.

**가장 중요한 특징 (SCD2):** 거래처의 이름이나 상태가 바뀌었을 때, 기존 데이터를 덮어쓰지 않습니다. 언제부터 언제까지 유효했는지(`validFrom`, `validTo`) 이력을 100% 남겨두어, 과거 장부를 조회할 때 당시의 거래처 이름이 정확히 나오도록 하는 '시간 여행' 기능을 지원합니다.

## 2. 관리 대상 (Domain)
- `AccountSubject`: 계정과목 (자산, 부채, 자본 등)
- `BusinessPartner`: 거래처 (고객, 벤더, 은행 등)
- `Department`: 부서 (비용 센터, 이익 센터 등)

## 3. DDD / Hexagonal 구조

이 모듈은 "외부 요청이 들어오는 곳"과 "업무 규칙이 사는 곳"과 "DB에 저장하는 곳"을 분리합니다.

- `masterdata.api.web`: REST Controller. HTTP 요청을 받고 DTO로 응답합니다.
- `masterdata.api.dto`: 화면/API 전용 요청/응답 모델입니다. JPA Entity를 외부에 직접 노출하지 않기 위한 완충층입니다.
- `masterdata.core.application.usecase`: 입력 포트입니다. Controller는 구현체가 아니라 이 인터페이스를 호출합니다.
- `masterdata.core.application.command`: 유스케이스로 들어가는 명령 객체입니다.
- `masterdata.core.application.service`: 트랜잭션과 유스케이스 흐름을 담당합니다.
- `masterdata.core.domain.policy`: 유효기간, 활성 여부 같은 순수 도메인 규칙을 둡니다.
- `masterdata.core.port.out`: 출력 포트입니다. core 계층이 DB 기술을 직접 알지 않게 하는 인터페이스입니다.
- `masterdata.core.infrastructure.persistence`: JPA Repository를 호출하는 출력 어댑터입니다.
- `basic.domain`, `basic.repository`: 현재 JPA Entity와 Spring Data Repository가 남아 있는 영속성 모델 영역입니다. 이후 도메인 모델을 더 분리할 때 이 경계를 기준으로 옮깁니다.

초보자 기준으로는 이렇게 보면 됩니다.

`Controller -> UseCase -> Application Service -> Output Port -> JPA Adapter -> Repository -> DB`

## 4. 실행 방법
```bash
./gradlew :master-data:bootRun
```

## 5. Docker 로 실행하기
```bash
docker build -t account/master-data .
docker run -p 8082:8082 account/master-data
```
