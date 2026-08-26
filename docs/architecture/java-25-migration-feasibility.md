# ☕ Java 25 LTS 전환 타당성 및 아키텍처 영향 분석 보고서 (Java 25 Migration Feasibility)

본 문서는 **Account.AI 멀티모듈 금융/재무회계 MSA 시스템**의 런타임 플랫폼을 현재 **Java 17 (LTS)**에서 차세대 **Java 25 (LTS, 목표 2025-09 GA)**로 전환하기 위한 기술적 타당성, 프레임워크 호환성, 컨테이너 빌드 영향도 및 단계별 마이그레이션 로드맵을 정리한 아키텍처 조사 보고서입니다.

---

## 1. 📌 개요 및 현황 인벤토리 (Current State Inventory)

### 1) 저장소 현재 기술 스택
- **Java Toolchain:** JavaLanguageVersion.of(17) (uild.gradle:26)
- **Spring Boot:** 3.2.5 (uild.gradle:2)
- **Spring Cloud:** 2023.0.1 (uild.gradle:10)
- **Gradle Wrapper:** 8.7 (gradle/wrapper/gradle-wrapper.properties:3)
- **QueryDSL:** 5.0.0 (uild.gradle:11)
- **Container Base Image:** eclipse-temurin:17-jre-alpine

### 2) 전환 목표 및 범위
- **목적:** 가상 스레드(Virtual Threads) 2세대 고도화, 불변 금융 도메인 모델 생성자 안전성(JEP 513), 초저지연 Generational ZGC를 활용한 회계 트랜잭션 및 대량 배치 성능 극대화
- **적용 대상:** 16개 전체 마이크로서비스 및 공통 라이브러리(shared-kernel, contracts)

---

## 2. 🚀 Java 25 LTS 핵심 기능 및 금융 시스템 기대 효과

`mermaid
mindmap
  root((Java 25 LTS 도입))
    Virtual Threads
      2세대 스케줄러 최적화
      I/O 블로킹 동시성 극대화
    Flexible Constructor Bodies (JEP 513)
      super 호출 전 BigDecimal 유효성 검증
      불변 금융 도메인 생성 안전성
    Structured Concurrency (JEP 480)
      병렬 대사 작업 수명주기 일괄 관리
      스레드 누수 방지 및 서브태스크 자동 취소
    Generational ZGC
      밀리초 이하 GC 일시정지 (Sub-millisecond Pause)
      대용량 배치 메모리 회수 극대화
`

1. **생성자 본문 유연화 (Flexible Constructor Bodies - JEP 513):**
   - 기존 Java에서는 super(...) 호출 전에 어떠한 로직도 실행할 수 없었으나, JEP 513을 통해 super(...) 호출 전 BigDecimal 금액 검증(equirePositive, equireNonNull) 및 방어적 복사를 선행할 수 있어 불변 금융 엔티티의 무결성이 대폭 향상됩니다.
2. **구조화된 동시성 (Structured Concurrency - JEP 480):**
   - 분산 대사(Reconciliation) 및 대손충당금(ECL) 병렬 연산 시 하위 작업의 취소/실패를 단일 작업 단위로 묶어 처리함으로써 스레드 누수를 원천 차단하고 트랜잭션 롤백 안정성을 확보합니다.
3. **가상 스레드 (Virtual Threads) 및 Generational ZGC:**
   - Spring MVC 기반 I/O 바운드 요청 처리 시 OS 스레드 고갈 없이 수만 개의 동시 전표 요청을 처리하며, 세대별 ZGC(-XX:+UseZGC -XX:+ZGenerational)를 통해 대용량 배치 처리 중에도 GC Pause를 1ms 미만으로 유지합니다.

---

## 3. 🔍 공식 호환성 매트릭스 (Compatibility Matrix)

공식 Spring 및 Gradle 기술 명세에 따른 버전별 Java 25 지원 현황입니다.

| 컴포넌트 | 현재 버전 | Java 25 지원 공식 요구 버전 | 공식 근거 및 세부 분석 |
| :--- | :--- | :--- | :--- |
| **Spring Boot** | 3.2.5 | **Spring Boot 4.0 / Spring 7.0** | Spring Boot 3.4.x는 Java 24까지 지원(Spring 6.2 기반). Java 25 LTS의 공식 First-Class 지원은 Spring Framework 7.0 / Spring Boot 4.0(기본선 Java 21+)에서 제공 |
| **Gradle** | 8.7 | **Gradle 9.1.0+** | Gradle 8.7은 Java 21까지 지원하며, Java 25 데몬 실행 및 공식 컴파일 툴체인은 Gradle 9.1.0+ 필요 |
| **ByteBuddy / Mockito** | 1.14.x / 5.11.x | **ByteBuddy 1.15+ / Mockito 5.14+** | Java 25 클래스파일 버전(Classfile 69) 바이트코드 조작 지원 필수 |
| **Lombok** | 1.18.32 | **Lombok 1.18.36+** | Java 25 javac 내부 AST 변경 대응 필요 |
| **Hibernate ORM** | 6.5.x | **Hibernate 6.6+** | Java 25 가상 스레드 락 최적화 및 Java 21+ 레코드 매핑 지원 |

> **참고:** String Templates(JEP 430/459)는 JDK 23에서 철회(Withdrawn)되어 재설계 중이므로 Java 25 표준 스펙에 포함되지 않습니다.

---

## 4. 🐳 컨테이너 및 빌드 인프라 변경 계획

### 1) 베이스 이미지 전환 (Containerfile & Dockerfile)
- **현재:** eclipse-temurin:17-jre-alpine
- **목표:** eclipse-temurin:25-jre-alpine 또는 ellsoft/liberica-openjdk-alpine:25
- **JVM 튜닝 플래그:**
  `dockerfile
  ENV JAVA_OPTS= -XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError
  `

### 2) Gradle Toolchain 설정 예시 (uild.gradle)
`groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
`

---

## 5. 🗺️ 4단계 마이그레이션 로드맵 (Phased Roadmap)

`mermaid
gantt
    title Java 25 LTS 마이그레이션 단계별 로드맵
    dateFormat  YYYY-MM
    section Phase 1 (빌드 인프라 준비)
    Gradle Wrapper 8.10+ 및 9.x 업그레이드     :2026-09, 1M
    ByteBuddy / Mockito / Lombok 최신화      :2026-09, 1M
    section Phase 2 (프레임워크 선행 전환)
    Spring Boot 3.4+ 및 Spring 7.0 M1 검증    :2026-10, 2M
    section Phase 3 (Java 25 툴체인 적용)
    JDK 25 Toolchain 빌드 및 CI 매트릭스 검증 :2026-12, 1M
    section Phase 4 (프로덕션 런타임 전환)
    Container Base Image Temurin 25 교체     :2027-01, 1M
    JEP 513 등 언어 신기능 도메인 적용        :2027-01, 2M
`

1. **Phase 1 (빌드 도구 선행 업그레이드):** Gradle wrapper를 9.x로 업그레이드하고 바이트코드 조작 라이브러리(ByteBuddy, Lombok, Mockito)를 최신화.
2. **Phase 2 (프레임워크 베이스라인 정합화):** Spring Boot를 최신 릴리즈 라인(3.4.x -> 4.x)으로 점진적 상향.
3. **Phase 3 (듀얼 컴파일 및 CI 검증):** Java 17 바이트코드 타깃을 유지하면서 JDK 25 툴체인 컴파일 및 전체 257개 테스트 통과 검증.
4. **Phase 4 (프로덕션 런타임 전환):** 컨테이너 베이스 이미지를 eclipse-temurin:25-jre-alpine으로 일괄 교체하고 JEP 513 도메인 생성자 검증 로직 적용.

---

## 6. 📝 결론 및 권고 사항

- Java 25는 금융 트랜잭션의 **초저지연(Generational ZGC)** 및 **고처리량 동시성(Virtual Threads)**을 달성하기 위한 최적의 플랫폼입니다.
- 단, Spring Boot와 Gradle의 공식 지원 버전(Spring Boot 4.0 / Gradle 9.1.0+)에 맞추어 **1단계 빌드 도구 최신화 -> 2단계 프레임워크 업그레이드 -> 3단계 툴체인 전환**의 점진적 접근을 권고합니다.
