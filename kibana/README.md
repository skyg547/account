# 📊 Kibana (키바나) - "로그 시각화 대시보드"

## 1. 초보자를 위한 개념 설명
Kibana는 Elasticsearch에 쌓인 방대한 텍스트 로그들을 웹 브라우저에서 예쁜 표, 그래프, 검색창으로 보여주는 **'모니터 요원(대시보드)'**입니다. 개발자는 콘솔의 까만 화면 대신 키바나의 하얀 화면에서 마우스 클릭 몇 번으로 에러를 찾아낼 수 있습니다.

## 2. 이 폴더의 역할
`kibana.yml`을 통해 키바나 서버가 어느 Elasticsearch 도서관과 연결되어 있는지 설정합니다.

## 3. 실행 및 확인 방법
- MSA 철학에 따라, 이제 이 모듈은 **독자적인 `docker-compose.yml`**을 가집니다.

### ⚠️ 최초 1회 필수 작업
먼저 프로젝트 최상단 폴더에 있는 `setup-network.bat` 파일을 실행하여 공통 네트워크를 생성하고, `elasticsearch`가 켜져 있어야 합니다.

### 🚀 실행 순서
1. 터미널을 열고 **`kibana` 폴더 안으로 이동**합니다. (`cd kibana`)
2. 아래 명령어를 실행합니다:
   ```bash
   docker-compose up -d
   ```
3. 브라우저에서 [http://localhost:5601](http://localhost:5601) 에 접속하면 화려한 대시보드가 열립니다.

## 4. 처음 접속 시 해야 할 일 (Index Pattern 만들기)
키바나가 어떤 서랍(Index)의 데이터를 보여줄지 알려줘야 합니다.
1. 왼쪽 메뉴 끝의 톱니바퀴 ⚙️ (Stack Management) 클릭
2. `Index Patterns` 클릭 -> `Create index pattern` 클릭
3. Name에 `account-logs-*` 입력 (로그스태시가 만든 상자 이름 패턴)
4. Timestamp 필드를 선택하고 완료!
5. 이제 돋보기 🔍 (Discover) 메뉴에서 마음껏 로그를 검색하세요!
