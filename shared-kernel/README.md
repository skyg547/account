# 🧬 Shared Kernel (공유 커널) - "MSA의 공통 유전자 (DNA 창고)"

## 1. 초보자를 위한 개념 설명
MSA(마이크로서비스 아키텍처)의 가장 큰 규칙은 "남의 폴더(코드)를 몰래 들여다보거나 의존하지 않는다" 입니다. 각 10개의 서비스는 철저히 남남이어야 합니다.

**하지만! 모든 서비스가 공통으로 숨 쉬듯이 써야만 하는 필수 도구들이 있습니다.**
- "우리 모두 에러 로그는 ELK(블랙박스)로 보내자!"
- "우리 모두 건강 상태는 프로메테우스(의사)에게 보고하자!"
- "우리 모두 DB 테이블 만들 때 Flyway(시공사)를 쓰자!"

만약 10개의 서비스 폴더마다 똑같은 라이브러리 설정(`build.gradle`)과 똑같은 '날짜 변환기' 코드를 복사해서 붙여넣기 한다면? 나중에 설정 하나 바꿀 때 10군데를 다 고쳐야 하는 지옥이 열립니다.

**`shared-kernel`**은 모든 서비스가 태어날 때부터 공통으로 물려받는 **'DNA(공통 라이브러리와 유틸리티 창고)'** 입니다! 다른 서비스들은 이 모듈을 통째로 가져다 씁니다.

---

## 2. 이 폴더 안에 무엇이 들어있나요? (What it is)

### 🛠️ 1. 공통 인프라 라이브러리 (Dependencies)
이곳의 `build.gradle`에 적힌 도구들은 10개의 마이크로서비스 전체에 자동으로 이식됩니다.
- `spring-boot-starter-actuator`: (건강 검진) 체온계
- `micrometer-tracing-bridge-brave`: (Zipkin) 운송장 번호 발급기
- `logstash-logback-encoder`: (ELK) 로그를 JSON으로 예쁘게 포장하는 기계
- `spring-kafka`: (전광판) 카프카와 통신하는 무전기
- `flyway-core`: (DB 도면) 테이블 안전 시공사
- `spring-cloud-starter-openfeign`: (전화기) 다른 부서에 바로 전화 거는 스마트폰
- `springdoc-openapi-starter-webmvc-ui`: (Swagger) API 통합 메뉴판 제작기

### 🧰 2. 공통 유틸리티 코드 (Utils)
- `DateUtils.java`: "날짜 포맷은 무조건 yyyy-MM-dd로 통일해!" 같은 공통 도구
- `ApiResponse.java`: "성공이든 실패든 응답 JSON 껍데기 모양은 똑같이 생겨야 해!" 하는 공통 응답 규격
- `GlobalExceptionHandler.java`: "에러가 나면 무조건 이 형식으로 에러 메시지를 뱉어!" 하는 공통 에러 처리기

---

## 3. 🚨 절대 주의사항 (What it is NOT)

**이 폴더 안에는 비즈니스 로직(돈 계산, 도메인 지식)이 단 한 줄이라도 들어가면 절대 안 됩니다!**

- ❌ **안 되는 것:** `JournalEntry(전표)`, `BusinessPartner(거래처)`, `Loan(대출)` 같은 클래스 파일. (이런 건 각자 자기 부서 폴더나 `contracts` 모듈에 있어야 합니다.)
- ⭕ **되는 것:** "모든 부서가 DB를 쓸 때 공통으로 날짜를 기록하는 `BaseTimeEntity.java`" 같은 순수 기술 뼈대.

> **명심하세요:** `shared-kernel`이 무거워지고 뚱뚱해지면 MSA의 장점(가볍고 독립적임)이 통째로 날아갑니다. 정말로 10개 서비스가 **'모두, 예외 없이'** 쓰는 기술적인 뼈대만 이곳에 넣어야 합니다.
