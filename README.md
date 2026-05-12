# Modern Financial System (MSA)

본 프로젝트는 DDD 및 헥사고날 아키텍처 기반의 모던 재무 시스템(엔터프라이즈 리스크 시스템)입니다.

## 아키텍처 및 도커 환경 구성

모든 개별 MSA 모듈 및 인프라스트럭처에 대해 독립적으로 실행 가능한 `Dockerfile`과 `docker-compose.yml`이 구성되어 있습니다.

### 포함된 모듈

**백엔드 모듈 (Spring Boot)**
* `asset-lease`, `auth`, `closing`, `config-server`, `discovery`, `expenditure-resolution`, `gateway`, `governance`, `journal-ledger`, `loan`, `master-data`, `payable`, `receivable`, `reconciliation`, `reporting`, `tax`

**인프라 및 기타 (Docker Compose 지원)**
* `elasticsearch`, `grafana`, `kafka`, `kibana`, `logstash`, `prometheus`, `redis`, `vault`, `zipkin`

### 개별 모듈 실행 방법

각 모듈 디렉토리로 이동하여 다음 명령어로 모듈별 컨테이너를 구동할 수 있습니다 (테스트 및 빌드는 수행하지 않은 상태로 구동만 진행).

```bash
# 예시: master-data 모듈 구동
cd master-data
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

*(참고: 애플리케이션 모듈의 경우 `./gradlew build` 등을 통해 `.jar` 파일이 `build/libs/`에 존재해야 정상적으로 `Dockerfile` 빌드가 완료됩니다.)*

## 문서 가이드

프로젝트 전반의 업무 흐름 및 아키텍처 구조에 대한 자세한 내용은 `docs/` 디렉토리를 참조하세요.

1. **[docs/README.md](docs/README.md)**: 전체 문서 허브 (초보자 필독)
2. **[docs/msa-execution-and-work-plan.md](docs/msa-execution-and-work-plan.md)**: MSA 실행 및 롤아웃 계획
3. **[docs/infrastructure-guide.md](docs/infrastructure-guide.md)**: 인프라 구성 및 운영 가이드

모든 비즈니스 모듈의 내부 폴더에도 별도의 문서를 유지하고 있습니다.
