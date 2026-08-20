# 🛡️ Internal Audit 모듈 가이드 문서

## 1. 🏛️ 계층별 소유권 및 책임

| 계층 (Layer) | 책임 및 역할 |
| --- | --- |
| **Domain** | RCM(통제 활동 매트릭스), 통제 설계 평가(Design Evaluation), 운영 평가(Operating Evaluation), 감사 추적(Audit Log) 엔티티 및 핵심 규칙 |
| **Application** | 내부통제 평가 계획 수립, 평가 점수 산정, 재평가 워크플로우 조정, 감사 보고서 집계 서비스 |
| **Outbound Adapter** | JPA 엔티티 매핑, 감사 로그 영속화, RCM 통제 기준 영구 저장소 연동 |
| **Inbound Adapter (API)** | JWT 인증/인가(`X-Auth-User` 검증), RESTful 컨트롤러, Bean Validation, DTO 변환 |

---

## 2. 🐣 초보자를 위한 내부통제 및 감사 용어

1. **RCM (Risk Control Matrix, 리스크 통제 매트릭스)**:
   - 조직의 재무/운영 위험을 식별하고 이를 완화하기 위해 설계된 통제 활동(Control Activities)의 정의서입니다.
2. **설계 평가 (Design Effectiveness Evaluation)**:
   - 통제가 리스크를 예방하거나 적시에 탐지할 수 있도록 올바르게 설계되었는지를 평가합니다.
3. **운영 평가 (Operating Effectiveness Evaluation)**:
   - 설계된 통제가 정의된 주기와 절차에 따라 실제 업무에서 효과적으로 운영되고 있는지를 표본 검사(Sampling)를 통해 평가합니다.
4. **감사 추적 (Audit Trail)**:
   - 모든 평가 변경, 승인자 정보, 평가 점수 수정 이력을 불변(Immutable) 로그로 기록합니다.

---

## 3. 🧭 로컬 실행 및 검증

### 단위 테스트 및 빌드 검증:
```powershell
.\gradlew.bat :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar --console=plain
```

### 독립 단독 실행 (`local` 프로파일):
```powershell
.\gradlew.bat :internal-audit:api:bootRun --args="--spring.profiles.active=local" --console=plain
```

자세한 로컬 실행 가이드는 [../README.md](../README.md)를 참고하십시오.
