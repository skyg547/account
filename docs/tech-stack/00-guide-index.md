# 📚 [Tech 00] 기술 스택 통합 가이드 (Overview)

우리 **'모던 재무 시스템'**에 사용된 핵심 기술 10가지와 오픈소스 오픈소스 생태계를 초보자도 쉽게 이해할 수 있도록 정리한 기술 총서입니다.

## 🧭 인덱스 및 학습 순서

| 순번 | 분류 | 기술명 | 주요 역할 | 가이드 링크 |
| :--- | :--- | :--- | :--- | :--- |
| **01** | **Core** | Next.js 15 | 프론트엔드 전체 아키텍처 및 라우팅 | [보기 (01)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/01-modern-core.md) |
| **02** | **Core** | Java 21 | 최신 문법을 활용한 백엔드 언어 정석 | [보기 (01)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/01-modern-core.md) |
| **03** | **Logic** | Spring Boot 3.4 | 엔터프라이즈 서버 기동 및 핵심 엔진 | [보기 (02)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/02-arch-framework.md) |
| **04** | **Logic** | Hexagonal Architecture | 유연하고 확장성 있는 육각형 설계 구조 | [보기 (02)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/02-arch-framework.md) |
| **05** | **Data** | JPA & Hibernate | 객체와 테이블을 잇는 통역사 (ORM) | [보기 (03)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/03-db-query.md) |
| **06** | **Data** | QueryDSL | 타입 안전하게 쿼리하는 동적 쿼리 도구 | [보기 (03)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/03-db-query.md) |
| **07** | **Sync** | TanStack Query | 똑똑한 데이터 배달 및 서버 상태 관리 | [보기 (04)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/04-frontend-ui.md) |
| **08** | **UI** | Lucide Icons & CSS Modules | 세련된 디자인 아이콘 및 모듈화된 스타일 | [보기 (04)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/04-frontend-ui.md) |
| **09** | **Infra** | Apache Kafka | 거대 우체국 역할을 하는 메시지 브로커 | [보기 (05)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/05-infra-messaging.md) |
| **10** | **Infra** | Eureka & Cloud Config | MSA 전용 전화번호부 및 중앙 통제실 | [보기 (05)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/05-infra-messaging.md) |
| **11** | **Gate** | API Gateway & Resil4j | 호텔 안내데스크 및 장애 차단(두꺼비집) | [보기 (06)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/06-msa-orchestration.md) |
| **12** | **Auth** | Auth & JWT | 놀이공원 자유이용권(무상태 보안 인증) | [보기 (06)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/06-msa-orchestration.md) |
| **13** | **Logs** | ELK Stack | 중앙 관제소(로그 수집/검색/시각화) | [보기 (07)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/07-observability.md) |
| **14** | **Stats** | Prometheus & Grafana | 서버 건강진단 간호사 및 종합 전광판 | [보기 (07)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/07-observability.md) |
| **15** | **Trace** | Zipkin | 요청 흐름 추적(택배 배송 조회 시스템) | [보기 (07)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/07-observability.md) |
| **16** | **DB Ver** | Flyway | DB 설계도 버전 관리 및 안전한 자동화 | [보기 (08)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/08-advanced-infra.md) |
| **17** | **Cache** | Redis | 초고속 화이트보드 및 중복 방지 자물쇠 | [보기 (08)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/08-advanced-infra.md) |
| **18** | **Secret** | HashiCorp Vault | 스위스 은행 비밀 금고(보안 정보 저장) | [보기 (08)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/08-advanced-infra.md) |
| **19** | **Call** | OpenFeign | 스마트 사내 전화기(선언적 서비스 호출) | [보기 (08)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/08-advanced-infra.md) |
| **20** | **Docs** | Swagger / OpenAPI | 사진 포함 통합 메뉴판(API 자동 명세화) | [보기 (08)](file:///c:/Users/skyg547/IdeaProjects/account/docs/tech-stack/08-advanced-infra.md) |

---

## 🎓 학습 가이드 (Tip)
- **순차 학습 권장:** 01번부터 순서대로 읽으시면 시스템이 어떤 철학으로 만들어졌는지 이해하기 쉽습니다.
- **실전 코드 확인:** 이론을 읽은 뒤에는 실제 프로젝트의 `frontend/src` 폴더나 `backend/**/core` 폴더의 코드를 열어보세요. 문서의 예시 코드를 실제로 발견할 수 있습니다.
- **YOLO 정신:** 모르는 기술이 나와도 두려워하지 마세요. 우리 시스템의 문서는 초보자도 전문가로 성장할 수 있도록 끊임없이 최신화됩니다.
