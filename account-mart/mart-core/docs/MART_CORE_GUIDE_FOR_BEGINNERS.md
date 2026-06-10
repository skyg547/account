# mart-core 입문 가이드

`mart-core`는 IFRS 9 대손충당금 입력 데이터를 만드는 도메인 규칙을 담습니다.

## 핵심 객체

- ODS 계좌/고객/상품/담보: 원천 데이터 모델.
- CDM 포지션: 산출 엔진이 읽을 수 있도록 정제된 기준일 데이터.
- Allowance exposure snapshot: ECL 산출 입력으로 고정된 데이터.

## 개발 원칙

- 원천 테이블 접근은 repository port 뒤에 숨깁니다.
- DQ와 대사 규칙은 core에 두고 batch는 호출만 합니다.
- 금액 계산은 `BigDecimal`로 처리합니다.

## 코드 읽는 순서

1. `domain/ods`: 원천 계좌, 고객, 상품, 담보 모델을 확인합니다.
2. `domain/mart`: `AllowanceInputPosition`과 CDM 변환 규칙을 확인합니다.
3. `domain/allowance`: ECL 입력 snapshot 생성 결과를 확인합니다.
4. `application/port/out`: core가 외부 저장소를 어떤 계약으로 보는지 확인합니다.
5. `infrastructure/persistence`: JPA/JDBC 세부 구현을 확인합니다.

## 테스트

```powershell
.\gradlew :account-mart:mart-core:test --console=plain
```

업무 규칙을 바꾸면 batch 테스트만 보지 말고 core 테스트를 먼저 보강합니다. batch는 오케스트레이션이고, 규칙의 책임은 core에 있습니다.
