# Receivable Module (매출채권)

## 1. 비즈니스 목적
`receivable` 모듈은 제품/서비스 판매 후 발생하는 매출채권(Accounts Receivable)과 고객으로부터의 수납(Collection) 및 매칭(Clearing) 과정을 관리합니다.

## 2. 아키텍처: Hexagonal (Ports & Adapters)
- **domain**: **Rich Domain Model**. 채권의 잔액 계산 및 수납 매칭 규칙이 엔티티 내부에 캡슐화되어 있습니다.
- **application.service**: 자동/수동 매칭 흐름 및 전표 발행 오케스트레이션.
- **adapter.in.web**: REST API 진입점.

## 3. 핵심 비즈니스 규칙 (초보자 가이드)
- **매출채권(Receivable)**: "나중에 받을 돈"입니다.
- **수납(Collection)**: 실제로 통장에 들어온 돈입니다.
- **매칭(Matching)**: "이 돈이 어떤 채권을 갚은 것인가"를 연결하는 과정입니다. 매칭이 완료되어야 회계상 매출채권이 사라집니다.

## 4. 실행 및 테스트
- 빌드: `./gradlew :receivable:build`
- 테스트: `./gradlew :receivable:test`
