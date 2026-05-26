# ⚙️ 리스크 데이터 마트 코어 (mart-core)

> **Role**: [리스크 모델러]
> **Metaphor**: [수식 계산기 및 핵심 장부 관리]

## 💡 초보자를 위한 개념 설명
본 모듈은 전사 리스크 데이터의 **'품질 관리실'이자 '표준 장부'**입니다.
- **표준 장부 (CDM)**: 제각각인 원천 데이터를 하나의 표준화된 리스크 장부(Common Data Model)로 통합하는 규칙을 관리합니다.
- **품질 관리 (DQ)**: 들어온 데이터가 믿을만한지, 소수점 자릿수는 맞는지(`BigDecimal` 정밀도 체크) 등 데이터의 신뢰성을 검증합니다.
- 리스크 엔진이 안심하고 요리할 수 있도록 가장 깨끗한 식재료 정보를 제공하는 것이 목표입니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 시스템의 가장 심장부인 **Domain & Application Layer**입니다.
- **Pure Domain**: 특정 DB 기술(JPA, MyBatis 등)에 종속되지 않는 순수한 데이터 모델과 비즈니스 규칙을 지향합니다.
- **Port 정의**: 데이터 마트에 데이터를 넣거나 빼기 위한 모든 통로(Port Interface)를 여기서 정의합니다.
- 외부 시스템(ODS)의 테이블 명칭이 바뀌어도 이 안의 표준 리스크 모델은 견고하게 보호됩니다.

---

## 📂 패키지 구조
- `domain/ods`: 원천 데이터를 담는 ODS 엔티티 및 리포지토리.
- `domain/mart`: 통합 포지션 가공용 프로세서(`IntegratedPositionProcessor`) 및 마트 리포지토리.
- `domain/marketdata`: 시장 데이터 엔티티와 보간(Interpolation) 서비스.
- `domain/governance`: 감사(Audit) 및 운영 제어 데이터.
- `domain/external`: 외부 기관(KAP 등) 연동 데이터.

## 기술 스택
- Java 21 / Spring Boot 3.4
- Spring Batch (대용량 데이터 가공용)
- Querydsl (복잡한 데이터 조회용)
