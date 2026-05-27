# ⚙️ 리스크 데이터 마트 코어 (mart-core)

> **Role**: [재무 모델러]
> **Metaphor**: [수식 계산기 및 핵심 장부 관리]

## 💡 초보자를 위한 개념 설명
본 모듈은 전사 리스크 데이터의 **'품질 관리실'이자 '표준 장부'**입니다.
- **표준 장부 (CDM)**: 제각각인 원천 데이터를 하나의 표준화된 리스크 장부(Common Data Model)로 통합하는 규칙을 관리합니다.
- **품질 관리 (DQ)**: 들어온 데이터가 믿을만한지, 소수점 자릿수는 맞는지(`BigDecimal` 정밀도 체크) 등 데이터의 신뢰성을 검증합니다.
- 결산 대손 엔진이 안심하고 요리할 수 있도록 가장 깨끗한 식재료 정보를 제공하는 것이 목표입니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 시스템의 가장 심장부인 **Domain & Application Layer**입니다.
- **Pure Domain**: 특정 DB 기술(JPA, MyBatis 등)에 종속되지 않는 순수한 데이터 모델과 비즈니스 규칙을 지향합니다.
- **Port 정의**: 데이터 마트에 데이터를 넣거나 빼기 위한 모든 통로(Port Interface)를 여기서 정의합니다.
- 외부 시스템(ODS)의 테이블 명칭이 바뀌어도 이 안의 표준 리스크 모델은 견고하게 보호됩니다.

---

## 📂 패키지 구조
- `domain/ods`: 원천 데이터를 표현하는 순수 도메인 모델과 DQ/대사 규칙.
- `application/port/out`: 영속성/외부 시스템 접근을 숨기는 outbound port.
- `infrastructure/persistence`: JPA/JDBC 기반 adapter. ODS/CDM 대량 조회는 projection 또는 bulk SQL을 우선 사용.
- `domain/mart`: 통합 포지션 가공용 프로세서(`IntegratedPositionProcessor`).
- `domain/marketdata`: 시장 데이터 엔티티와 보간(Interpolation) 서비스.
- `domain/governance`: 감사(Audit) 및 운영 제어 데이터.
- `domain/external`: 외부 기관(KAP 등) 연동 데이터.

## 2026-05-27 통합 상태
- `IntegratedPositionProcessor`는 고객/조기경보/계좌금리/환율 포트를 통해 CDM 포지션을 보강합니다.
- 외화 포지션은 기준일 환율로 `marketValue`를 KRW 환산합니다.
- ODS 잔액 대사는 계좌 잔액을 상품의 GL 계정코드로 집계해 `ods_reconcile_hist`에 MATCH/MISMATCH를 모두 남깁니다.
- 조기경보, 계좌금리, 수익률곡선, 대사이력, KAP 등급 마스터 포트는 실제 adapter가 연결되어 batch context가 기동됩니다.

## 기술 스택
- Java 21 / Spring Boot 3.4
- Spring Batch (대용량 데이터 가공용)
- Querydsl (복잡한 데이터 조회용)
