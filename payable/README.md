# Payable Module (매입채무)

## 1. 비즈니스 목적
`payable` 모듈은 기업의 영업활동에서 발생하는 매입채무(Accounts Payable)의 전 생애주기를 관리합니다. 인보이스 등록부터 채무 인식, 지급 실행, 선급금 상계까지의 과정을 회계 정합성에 맞게 처리합니다.

## 2. 아키텍처: Hexagonal (Ports & Adapters)
본 모듈은 기술 변화에 유연하고 도메인 로직이 격리된 **헥사고날 아키텍처**를 따릅니다.

- **application.port.in**: 비즈니스 유즈케이스 인터페이스 (`PurchaseUseCase`, `PaymentUseCase`)
- **application.port.out**: 영속성 및 외부 시스템 연동 인터페이스
- **application.service**: 도메인 객체를 협업시켜 유즈케이스를 실현하는 서비스 레이어
- **domain**: 핵심 비즈니스 규칙이 담긴 **Rich Domain Model**. `BigDecimal`을 사용한 정밀한 금액 계산과 상태 전이 로직을 포함합니다.
- **adapter.out.persistence**: JPA 기반의 영속성 구현체

## 3. 핵심 비즈니스 규칙 (초보자 가이드)
- **매입채무(Payable)**: "나중에 갚아야 할 돈"입니다. 인보이스가 등록되면 생깁니다.
- **잔액 관리**: 지급이나 상계가 발생하면 `outstandingAmount`(미지급 잔액)가 줄어듭니다. 이 잔액이 0이 되어야 채무가 종결(PAID)됩니다.
- **선급금 상계(Offset)**: 물건을 받기 전 미리 준 돈(선급금)을 나중에 생긴 채무와 까는 작업입니다.

## 4. 실행 및 테스트
- 빌드: `./gradlew :payable:build`
- 테스트: `./gradlew :payable:test`
