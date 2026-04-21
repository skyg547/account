# 🚚 Logstash (로그스태시) - "로그 수집 및 배달부"

## 1. 초보자를 위한 개념 설명
Logstash는 각 서비스(Spring Boot 서버들)가 만들어낸 로그를 **받아서(Input)**, 예쁘게 **포장하고(Filter)**, 목적지인 Elasticsearch로 **배달하는(Output)** '우체국 택배 기사님'입니다.

## 2. 이 폴더의 역할
- `config/logstash.yml`: 택배 기사님 자체의 기본 설정.
- `pipeline/logstash.conf`: **(가장 중요!)** 어디서 택배를 받고(5000번 포트), 어디로 배달할지(Elasticsearch 9200 포트) 규칙을 정해둔 작업 지시서입니다.

## 4. 실행 및 확인 방법
- MSA 철학에 따라, 이제 이 모듈은 **독자적인 `docker-compose.yml`**을 가집니다.

### ⚠️ 최초 1회 필수 작업
먼저 프로젝트 최상단 폴더에 있는 `setup-network.bat` 파일을 실행하여 공통 네트워크를 생성하고, `elasticsearch`가 켜져 있어야 합니다.

### 🚀 실행 순서
1. 터미널을 열고 **`logstash` 폴더 안으로 이동**합니다. (`cd logstash`)
2. 아래 명령어를 실행합니다:
   ```bash
   docker-compose up -d
   ```
3. 정상적으로 실행되면 `5000`번 포트에서 스프링 부트 서버들이 던지는 로그를 척척 받아내기 시작합니다.
