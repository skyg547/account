# 대손충당금 입력 마트 코어

`mart-core`는 IFRS 9 대손충당금 산출에 투입되는 입력 데이터를 정제하고 표준화하는 core 모듈입니다.

## 패키지

- `domain/ods`: 원천 원장, 담보, 고객, 상품, DQ/대사 도메인.
- `domain/mart`: CDM 포지션 변환.
- `domain/allowance`: allowance exposure snapshot 생성 결과.
- `domain/marketdata`: 환율, 수익률곡선, 보간 서비스.
- `application/port/out`: 영속성/외부 시스템 접근 port.
- `infrastructure/persistence`: JPA/JDBC adapter.

## 원칙

- 계산 금액과 비율은 `BigDecimal`을 유지합니다.
- core는 도메인 규칙과 port만 정의하고, JPA/JDBC 세부사항은 infrastructure에 둡니다.
- 기준일 재실행이 가능하도록 snapshot 생성은 기준일 단위로 멱등 처리합니다.

## 로컬 검증

```powershell
./gradlew :account-mart:mart-core:test --console=plain --max-workers=1 --no-daemon
```

IntelliJ에서 테스트를 실행할 때도 Gradle runner를 사용하면 멀티모듈 classpath와 annotation processing 설정이 일관됩니다.

## 함께 읽을 문서

- 상위 문서 인덱스: [../docs/README.md](../docs/README.md)
- core 입문 가이드: [docs/MART_CORE_GUIDE_FOR_BEGINNERS.md](docs/MART_CORE_GUIDE_FOR_BEGINNERS.md)
