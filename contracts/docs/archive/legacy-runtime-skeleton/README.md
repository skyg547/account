# Contracts legacy runtime skeleton archive

이 폴더의 `Dockerfile`과 `docker-compose.yml`은 과거 모든 모듈의 디렉터리 모양을 맞추기 위해
만든 빈 실행 뼈대입니다. Alpine 컨테이너가 안내 문구만 출력하고 종료하므로 실제 Contracts
기능이나 검증을 제공하지 않습니다.

2026-07-22 점검에서 Anti-Skeleton 정책과 “library는 실행 서버가 아니다”라는 문서에 맞춰 활성
모듈 루트에서 이 archive로 이동했습니다. 내용은 이력 확인을 위해 보존하지만 빌드·배포·로컬
실행에 사용하지 않습니다.