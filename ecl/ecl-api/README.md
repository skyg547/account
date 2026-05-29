# 대손충당금(IFRS9) API 모듈

> **Role**: [백엔드 개발자]
> **Metaphor**: [고객 응대 창구]

## 💡 초보자를 위한 개념 설명
본 모듈은 재무 결산 시스템의 **'친절한 창구 직원'**입니다. 
- 공장(Batch)에서 열심히 만들어낸 대손충당금(IFRS9) 산출 결과를 프론트엔드 대시보드나 타 시스템이 볼 수 있도록 API로 제공합니다.
- 사용자의 배치 실행, EAD 검증, 모델 파라미터 조회 요청을 시스템 내부에 전달합니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 **Adapter** 레이어에 해당합니다. 
- **Inbound Adapter**: REST Controller를 통해 외부 요청을 수신합니다.
- **Port**: `ecl-core`에서 정의한 Service Interface(Port)를 호출하여 비즈니스 로직을 수행합니다.
- 기술적인 세부사항(Spring MVC, JSON 직렬화 등)이 핵심 비즈니스 로직(Core)에 영향을 주지 않도록 철저히 분리되어 있습니다.

## 🛠️ 주요 기능
- **대손충당금 지표 조회**: 차주별, 계좌별 EAD/PD/LGD/ECL 조회.
- **배치 모니터링 연동**: 현재 진행 중인 대손충당금(IFRS9) 산출 상태 조회.
- **모델 메타데이터 관리**: 등급별 PD, LGD 세그먼트, 모델 파라미터 조회/수정.

## 🚀 실행 및 설정
- **Port**: 8083
- **Main 기술**: Spring Boot, Spring Web, Eureka Client

## 🧪 IntelliJ HTTP Client
- IntelliJ에서 바로 호출할 수 있는 샘플 요청 파일: `allowance-api.http`
