# 🚪 API Gateway (스프링 클라우드 게이트웨이)

`gateway` 모듈은 호텔의 "1층 안내데스크" 역할을 하는 외부 요청의 단일 진입점입니다.

## 1. 목적 (초보자를 위한 설명)
- **주소 은닉:** 외부 손님(프론트엔드 앱)이 `master-data`(8082)나 `journal-ledger`(8081)의 포트 번호를 일일이 외우지 않고, 게이트웨이(8080) 하나만 바라보고 통신하게 합니다.
- **자동 라우팅:** Eureka(길찾기 앱)에 등록된 서비스 ID를 바탕으로 올바른 서비스로 트래픽을 자동 분배합니다.
- **공통 보안 및 추적:** 공통 CORS, Zipkin 트레이싱(운송장 번호 발급)을 여기서 가장 먼저 수행합니다.

## 2. 현재 라우팅 규칙
- `/api/basic/**` ➔ `master-data` 서비스
- `/api/journals/**`, `/api/ledgers/**`, `/api/unsettled/**` 등 ➔ `journal-ledger` 서비스
- `/api/**` (그 외 나머지) ➔ `account` (기존 모놀리식 구버전)

## 3. 실행 방법
전체 MSA 생태계의 대문이므로, Eureka 서버(`discovery`)가 켜진 후에 실행되어야 합니다.

```bash
./gradlew :gateway:bootRun
```
게이트웨이는 기본적으로 **http://localhost:8080** 에서 기동합니다.
