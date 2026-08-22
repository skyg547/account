# ☕ Java 25 LTS 전환 타당성 및 아키텍처 영향 분석 보고서 (Java 25 Migration Feasibility)

본 문서는 **Account.AI 멀티모듈 금융/회계 MSA 시스템**의 런타임을 현재 **Java 17 (LTS)**에서 차세대 **Java 25 (LTS)**로 전환하기 위한 기술적 타당성, 프레임워크 호환성, 컨테이너 빌드 영향도 및 단계별 마이그레이션 로드맵을 정리한 아키텍처 분석 보고서입니다.

---

## 1. 📌 개요 및 목적 (Executive Summary)

- **목적:** 회계 및 대용량 배치 처리의 처리량(Throughput) 극대화, 메모리 풋프린트 절감 및 차세대 언어 기능을 도입하여 코드 안전성과 금융 정밀도 향상
- **전환 대상:** 루트 및 16개 마이크로서비스 (shared-kernel, contracts, journal-ledger, closing, loan, deposit, 	ax, ecl, eporting, master-data, uth, gateway, discovery, config-server 등)
- **핵심 결론:** Spring Boot 3.4+ 및 Gradle 8.9+ 기반 환경에서 Java 25 도입 시 대규모 아키텍처 파괴 없이 점진적(In-place Staged) 업그레이드가 가능하며, 가상 스레드(Virtual Threads) 고도화로 I/O 처리량이 대폭 개선될 것으로 분석됨.

---

## 2. 🚀 Java 25 LTS 핵심 기능 및 금융 시스템 기대 효과

`mermaid
mindmap
  root((Java 25 LTS 도입))
    Virtual Threads
      동시성 처리량 극대화
      HikariCP 및 I/O 블로킹 최소화
    Flexible Constructor Bodies
      super 호출 전 BigDecimal 유효성 검증
      불변 금융 도메인 생성 안전성
    Structured Concurrency
      병렬 대사 작업 수명주기 일괄 관리
      스레드 누수 방지
    String Templates & Pattern Matching
      SQL 및 복합 JSON 파싱 안전성
      분개 규칙 엔진 가독성 향상
`

1. **가상 스레드(Virtual Threads) 2세대 최적화:**
   - 회계 전표 및 거래 대사 등 네트워크 I/O 바운드 작업에서 OS 스레드 고갈 없이 수만 개의 동시 요청 처리 가능.
2. **생성자 본문 유연화 (Flexible Constructor Bodies - JEP 482):**
   - super(...) 호출 전에 BigDecimal 금액 검증(equirePositive, equireNonNull)을 선행할 수 있어 금융 불변 도메인 모델의 무결성 강화.
3. **구조화된 동시성 (Structured Concurrency - JEP 480):**
   - 분산 대사(Reconciliation) 및 대손충당금(ECL) 병렬 연산 시 하위 작업의 취소/실패를 단일 단위로 묶어 트랜잭션 롤백 안정성 확보.

---

## 3. 🔍 프레임워크 및 도구 호환성 분석 (Compatibility Matrix)

| 컴포넌트 | 현재 버전 | Java 25 호환 요구 버전 | 호환성 평가 및 주의사항 |
| :--- | :--- | :--- | :--- |
| **Spring Boot** | 3.3.x | **3.4.0+** (Spring Framework 6.2+) | 완전 지원 (Spring 6.2부터 JDK 25 런타임 및 가상 스레드 공식 지원) |
| **Gradle** | 8.7 | **8.10+** | Gradle JVM Toolchain 호환을 위해 8.10+ 업그레이드 필요 |
| **ByteBuddy / Mockito** | 1.14.x / 5.11.x | **ByteBuddy 1.15+** | JDK 25 클래스파일 버전(Classfile 69) 바이트코드 조작 지원 버전 필수 |
| **Lombok** | 1.18.32 | **1.18.36+** | javac 내부 AST 변경 대응 필요 |
| **Hibernate ORM** | 6.5.x | **6.6.x+** | Java 25 가상 스레드 락 최적화 지원 |

---

## 4. 🐳 컨테이너 및 CI/CD 빌드 변경 영향도

### 📌 1) 루트 및 모듈 Dockerfile/Containerfile
- **Base Image 전환:**
  - 현재: eclipse-temurin:17-jre-alpine
  - 변경: eclipse-temurin:25-jre-alpine 또는 ellsoft/liberica-openjdk-alpine:25
- **JVM 튜닝 플래그:**
  `dockerfile
  ENV JAVA_OPTS= -XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0
  `
  - Generational ZGC(세대별 ZGC)가 Java 21부터 기본 도입되어 Java 25에서 초저지연(Sub-millisecond Pause) GC로 대용량 금융 배치 메모리 반납에 최적화됨.

### 📌 2) Gradle Toolchain 설정 (uild.gradle)
`groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
`

---

## 5. 🗺️ 단계별 마이그레이션 로드맵 (Phased Roadmap)

`mermaid
gantt
    title Java 25 마이그레이션 일정 계획
    dateFormat  YYYY-MM
    section 1단계: 기반 프레임워크
    Gradle 8.10 및 Spring Boot 3.4 업그레이드 :2026-09, 1M
    ByteBuddy/Lombok 의존성 최신화           :2026-09, 1M
    section 2단계: CI/CD & 툴체인
    JDK 25 Toolchain CI 매트릭스 검증       :2026-10, 1M
    Containerfile Temurin 25 이미지 교체    :2026-10, 1M
    section 3단계: 도메인 고도화
    가상 스레드 및 언어 신기능 적용        :2026-11, 2M
`

1. **Phase 1 (사전 준비):** Gradle 8.10+ 및 Spring Boot 3.4.x로 빌드 인프라 선행 업그레이드
2. **Phase 2 (듀얼 컴파일 검증):** Java 17 바이트코드 타깃을 유지하며 JDK 25 컴파일러 및 단위 테스트 전수 검증
3. **Phase 3 (전면 전환):** Container Base Image를 eclipse-temurin:25-jre-alpine으로 교체하고 Production Compose 릴리즈

---

## 6. 📝 결론 및 권고 사항

- Java 25는 금융 트랜잭션의 **초저지연(Generational ZGC)** 및 **고처리량 동시성(Virtual Threads)**을 달성하기 위한 최적의 차세대 플랫폼입니다.
- 단기적으로는 Spring Boot 3.4+ 및 Gradle 8.10 업그레이드를 우선 완료한 후, 2026년 하반기 Java 25 GA 릴리즈에 맞춰 일괄 전환할 것을 권고합니다.
