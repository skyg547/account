# 재무 마트 API 모듈

`mart-api`는 대손충당금 산출 엔진과 운영 화면이 필요한 기준일 데이터를 조회하는 inbound adapter입니다.

## 제공 범위

- 기준일별 CDM 포지션 조회
- 고객/상품 기준정보 조회
- 수익률곡선, 환율 등 ECL 보조 시장데이터 조회

비즈니스 규칙은 `mart-core`의 port를 통해 접근하며, controller에는 조회 요청 검증과 응답 변환만 둡니다.

## 샘플

IntelliJ HTTP Client 샘플은 `mart-api.http`에 있습니다.

## 로컬 실행

```powershell
./gradlew :account-mart:mart-api:bootRun --console=plain
```

IntelliJ에서는 `AllowanceMartApiApplication`을 실행 클래스로 선택합니다. 기본 포트는 `8085`입니다.

Config Server와 Eureka가 떠 있지 않은 로컬 단독 실행에서는 `application.yml`의 config import가 `optional:`이므로 기동 자체는 가능하지만, 서비스 등록/외부 설정이 필요한 API는 인프라 상태에 영향을 받습니다.

## 검증

```powershell
./gradlew :account-mart:mart-api:compileJava --console=plain
```

Controller는 조회 요청 검증과 DTO 변환만 담당하고, 실제 규칙은 `mart-core` port/service를 통해 호출해야 합니다.
