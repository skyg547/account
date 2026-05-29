# 재무 마트 API 모듈

`mart-api`는 대손충당금 산출 엔진과 운영 화면이 필요한 기준일 데이터를 조회하는 inbound adapter입니다.

## 제공 범위

- 기준일별 CDM 포지션 조회
- 고객/상품 기준정보 조회
- 수익률곡선, 환율 등 ECL 보조 시장데이터 조회

비즈니스 규칙은 `mart-core`의 port를 통해 접근하며, controller에는 조회 요청 검증과 응답 변환만 둡니다.

## 샘플

IntelliJ HTTP Client 샘플은 `mart-api.http`에 있습니다.
