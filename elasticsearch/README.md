# 🔎 Elasticsearch (엘라스틱서치) - "로그 저장소 & 검색 엔진"

## 1. 초보자를 위한 개념 설명
Elasticsearch(줄여서 ES)는 우리가 쏟아내는 수많은 로그(데이터)를 모아두는 **'초고속 도서관'**입니다. 
일반적인 관계형 DB(MySQL, Oracle)와 달리, 텍스트 검색에 특화되어 있어서 수백만 건의 로그 중에서도 "NullPointerException"이라는 단어가 들어간 에러를 0.1초 만에 찾아줍니다.

## 2. 이 폴더의 역할
이 디렉토리는 Elasticsearch 서버가 어떻게 동작할지 결정하는 설정 파일(`elasticsearch.yml`)을 관리합니다.

## 3. 주요 설정
- **클러스터(Cluster):** 도서관의 이름입니다. 여기서는 `account-logs-cluster`로 지었습니다.
- **노드(Node):** 도서관에서 일하는 사서입니다. 로컬 개발 환경이므로 1명의 사서(`single-node`)만 둡니다.

## 4. 실행 및 확인 방법
- MSA 철학에 따라, 이제 이 모듈은 **독자적인 `docker-compose.yml`**을 가집니다.

### ⚠️ 최초 1회 필수 작업
모든 MSA 서비스가 서로 통신할 수 있도록, 가장 먼저 프로젝트 최상단 폴더에 있는 `setup-network.bat` 파일을 실행해 주세요. (공통 `account-network` 네트워크를 생성합니다.)

### 🚀 실행 순서
1. 터미널을 열고 **`elasticsearch` 폴더 안으로 이동**합니다. (`cd elasticsearch`)
2. 아래 명령어를 실행합니다:
   ```bash
   docker-compose up -d
   ```
3. 브라우저에서 [http://localhost:9200](http://localhost:9200) 에 접속했을 때, 환영 메시지(JSON)가 나오면 도서관이 정상 영업 중인 것입니다!
