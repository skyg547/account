# Shared-Kernel 초보자 가이드

## 1. 공용 부품 상자가 커지면 왜 위험한가

공용 부품 상자는 편리하지만 모든 모듈이 의존합니다. 한 팀이 자신의 업무 enum을 쉽게 넣으면
다른 팀도 그 변경 일정에 묶입니다. DDD에서는 각 바운디드 컨텍스트가 자기 언어를 소유하고,
정말 공동 소유하는 작은 부분만 Shared Kernel로 합의합니다.

### 넣어도 되는 후보

- 여러 모듈이 같은 의미로 사용하는 식별/메타 타입
- 기술과 무관한 공통 직렬화 보안 규칙
- 명확한 소유자와 호환 정책이 있는 통합 타입

### 넣으면 안 되는 후보

- 대출 금리, ECL 단계 판단, 전표 계산 같은 업무 로직
- 특정 모듈만 쓰는 JPA Entity/Repository
- Kafka/Redis/Vault starter를 편하게 전파하기 위한 의존성 묶음
- Controller 예외 정책처럼 API Adapter가 소유해야 하는 정책

## 2. 현재 타입을 현실적으로 읽기

`BoundedContext`와 `ServiceCapability`는 서비스의 업무 분류를 표현합니다.
`ServiceDescriptor`는 이름, 컨텍스트, capability, 설명을 묶은 불변 명함입니다.

`SpringServiceDiscoveryRegistry`는 네트워크 검색기가 아닙니다. Spring이 시작될 때 같은
프로세스의 `DiscoverableService` Bean 목록을 주입받아 불변 스냅샷으로 보관합니다. 이름이
중복되면 어느 Bean을 선택할지 모호하므로 시작을 실패시킵니다.

## 3. 마스킹은 언제 동작하는가

`@Masked`는 원본 필드 값을 바꾸지 않습니다. Jackson이 API 응답을 JSON으로 직렬화할 때
`MaskingSerializer`가 개입합니다.

- `REG_NO`: 앞 5개 문자/숫자만 표시
- `ACCOUNT`: 앞 3개와 뒤 4개만 표시
- `EMAIL`: 로컬 부분 앞 2개만 표시하고 도메인은 유지
- 짧은 값/미지원 패턴: 전체 마스킹

로그 문자열 연결이나 DB 조회 결과에는 자동 적용되지 않습니다. 로그에 민감정보를 직접 넣지
않는 규칙은 별도로 지켜야 합니다.

## 4. 구현 없는 애노테이션 주의

과거 `@DistributedLock`은 애노테이션만 있고 이를 처리하는 AOP/Redis 구현이 없었습니다.
애노테이션 이름만으로 실제 락이 생기지 않습니다. 이번 점검에서 ECL 소비자의 사용과 “락 획득”
표현을 제거했습니다. 이벤트 ID는 Spring Batch 멱등 키로 연결했고, 실제 `DistributedLockPort`와 owner token 기반 어댑터 도입 TODO를 남겼습니다.

## 5. 남은 소유권 정리

`BaseEntity`와 신용위험 enum은 주로 ECL/account-mart만 사용합니다. 이들은 전사 공통이라기보다
allowance 데이터 계약에 가깝습니다. 사용처를 한 번에 깨지 않도록 단계적으로 전용 계약/어댑터
모듈로 이동해야 합니다.