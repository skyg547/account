# Discovery 업무/데이터 흐름

Discovery에는 회계 거래 데이터가 아니라 서비스 위치와 lease 상태가 흐릅니다. 이 문서는 한 인스턴스가 시작해서 사라질 때까지의 순서를 설명합니다.

## 1. 서버 기동

```text
DiscoveryApplication
  -> application.yml 로드 (8761, register/fetch=false)
  -> optional Config Server 설정 병합
  -> Eureka Server context/registry 초기화
  -> actuator readiness UP
```

Config Server가 없어도 local 기본값으로 시작합니다. 중앙 설정이 있더라도 Discovery 자체는 Eureka client처럼 자신을 등록하거나 외부 registry를 fetch하지 않습니다.

## 2. 업무 서비스 등록

```text
Auth 또는 업무 API 시작
  -> spring.application.name으로 service ID 결정
  -> instance ID, host, port, status/health URL 구성
  -> POST /eureka/apps/{serviceId}
  -> Discovery registry에 lease 생성
  -> Dashboard와 registry fetch 결과에 노출
```

같은 service ID로 여러 instance ID를 등록하면 수평 확장 상태가 됩니다. Gateway LoadBalancer는 fetch한 `UP` 인스턴스 목록을 사용합니다.

## 3. heartbeat와 lease

```text
등록 서비스 -> 주기적 heartbeat(renew)
Discovery   -> lease lastRenewalTimestamp 갱신
```

heartbeat가 끊기면 lease 만료 후보가 됩니다. 하지만 전체 heartbeat가 급격히 줄면 self-preservation이 네트워크 장애로 판단해 대량 eviction을 보류할 수 있습니다.

초보자가 Dashboard에서 `EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP` 경고를 보더라도 무조건 self-preservation을 끄지 않습니다. 먼저 서비스 수, heartbeat, 네트워크 상태를 확인합니다.

## 4. 조회와 라우팅

```text
Gateway Eureka Client
  -> registry fetch
  -> local cache에 service ID별 UP 인스턴스 저장
  -> lb://master-data 요청
  -> LoadBalancer가 한 instance 선택
  -> 실제 HTTP 요청
```

Discovery가 순간적으로 끊겨도 client의 짧은 local registry cache가 남을 수 있습니다. 반대로 오래된 인스턴스가 cache/registry에 남는 시간도 있으므로 shutdown cancel과 health 상태가 중요합니다.

## 5. 정상 종료와 비정상 종료

### 정상 종료

```text
Service graceful shutdown
  -> Eureka cancel
  -> registry에서 instance 제거
```

### 비정상 종료

```text
Process/network failure
  -> heartbeat 중단
  -> lease 만료 판단
  -> self-preservation/eviction 정책 적용
  -> registry에서 제거
```

## 6. Discovery 재시작

Discovery registry는 별도 H2/PostgreSQL에 영속화하지 않습니다.

```text
Discovery restart
  -> 빈 registry로 시작
  -> 각 Eureka Client heartbeat/registration 재수행
  -> registry 복구
```

따라서 Discovery 기동 직후 registry가 비어 있는 것은 정상일 수 있습니다. readiness `UP`과 개별 서비스 재등록을 함께 확인합니다.

## 7. Docker 기동 순서

```text
Config Server service_started
  -> Discovery process
  -> /actuator/health/readiness UP
  -> Docker discovery=healthy
  -> Auth/업무 서비스/Gateway start
  -> register/fetch
```

단순 `service_started`는 Java 프로세스 생성만 의미하므로 뒤쪽 서비스가 너무 빨리 등록을 시도할 수 있습니다. 현재 루트 Compose는 Discovery 의존성을 `service_healthy`로 통일했습니다.

## 8. 보안 흐름

현재 8761에 접근할 수 있는 주체는 Dashboard 조회와 instance 등록/삭제 API를 호출할 수 있습니다. 운영에서는 네트워크 허용 목록과 서비스 인증을 함께 적용해야 합니다. Gateway 외부 라우트에 Eureka API를 추가해서는 안 됩니다.
