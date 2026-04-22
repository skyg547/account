# 🗂️ Master Data Service (기준 정보 관리)

## 1. 초보자를 위한 개념 설명
모든 회계/재무 처리의 근간이 되는 '변하지 않아야 할 기준 데이터'를 관리하는 핵심 도메인입니다.
예를 들어 "계정과목코드 101은 현금이다", "거래처코드 A01은 (주)카카오다" 같은 정보를 저장합니다.

**가장 중요한 특징 (SCD2):** 거래처의 이름이나 상태가 바뀌었을 때, 기존 데이터를 덮어쓰지 않습니다. 언제부터 언제까지 유효했는지(`validFrom`, `validTo`) 이력을 100% 남겨두어, 과거 장부를 조회할 때 당시의 거래처 이름이 정확히 나오도록 하는 '시간 여행' 기능을 지원합니다.

## 2. 관리 대상 (Domain)
- `AccountSubject`: 계정과목 (자산, 부채, 자본 등)
- `BusinessPartner`: 거래처 (고객, 벤더, 은행 등)
- `Department`: 부서 (비용 센터, 이익 센터 등)

## 3. 실행 방법
```bash
./gradlew :master-data:bootRun
```

## 4. Docker 로 실행하기
```bash
docker build -t account/master-data .
docker run -p 8082:8082 account/master-data
```
