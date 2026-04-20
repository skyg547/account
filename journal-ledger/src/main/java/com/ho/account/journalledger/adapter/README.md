# Adapter Layer (바깥 세상과의 소통자)

## 📌 초보자를 위한 개념 설명
> **Q. `adapter` 패키지에는 무엇이 들어가나요?**  
> 이 시스템이 밖이랑 어떻게 대화하는지(Web Controller, Kafka Listener, DB Repository)를 물리적으로 구현해 둔 연결 단자(어댑터)입니다. `domain`이나 `application`이 "저장 좀 해줘!" 라고 하면, 여기서 진짜 SQL 쿼리(`@Entity`, JPA)를 날려서 저장합니다.

## 🚀 규약 (Strict Rules)
- **방향성 주의:** 어댑터 계층은 철저하게 안쪽(`application`, `domain`)을 바라보며 의존해야 합니다. 안쪽(`domain`)에서 바깥쪽(`adapter`)의 코드를 참조하는 일은 절대 없어야 합니다 (의존성 역전 원칙).
- **예하 폴더 구성 권장:**
  - `in.web`: REST API 전용 (컨트롤러)
  - `out.persistence`: JPA 엔티티 및 리포지토리 전용
  - `out.client`: 외부 MSA 모듈 통신용 (OpenFeign 등)
