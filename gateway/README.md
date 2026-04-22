# 🚪 API Gateway (스프링 클라우드 게이트웨이)

`gateway` 모듈은 호텔의 "1층 안내데스크" 역할을 하는 외부 요청의 단일 진입점입니다.

## 1. 목적 (초보자를 위한 설명)
- **주소 은닉:** 외부 손님(프론트엔드)이 `master-data`(8082)나 `journal-ledger`(8081)의 포트 번호를 일일이 외우지 않고, 게이트웨이(8080) 하나만 바라보고 통신하게 합니다.
- **자동 라우팅:** Eureka(길찾기 앱)를 통해 올바른 서비스로 트래픽을 자동 분배합니다.
- **공통 보안 및 추적:** 공통 CORS, Zipkin 트레이싱(운송장 번호 발급), JWT 출입증 1차 검문(필터)을 여기서 모두 수행합니다.
- **두꺼비집(서킷 브레이커):** 뒷단 서버가 3초 이상 뻗으면 게이트웨이가 즉시 차단(Fallback)하여 시스템 연쇄 붕괴를 막습니다.

## 2. 실행 방법
Eureka 서버(`discovery`)와 Config 서버(`config-server`)가 켜진 후에 실행되어야 합니다.
```bash
./gradlew :gateway:bootRun
```

## 3. Docker 로 실행하기
```bash
docker build -t account/gateway-service .
docker run -p 8080:8080 account/gateway-service
```
