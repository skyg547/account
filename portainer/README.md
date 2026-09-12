# Portainer CE

컨테이너 상태·로그·콘솔·재기동을 웹에서 관리하는 독립 인프라 모듈입니다.
업무 API/core/batch 코드에는 의존하지 않습니다.

- 이미지: `docker.io/portainer/portainer-ce:2.39.7` (LTS 고정 버전)
- 진입점: 기존 Nginx의 `http://localhost:8080/portainer/`
- 통신: `account-network` → `account-portainer:9000`, 추가 host port 없음
- 영속 데이터: `account-portainer-data` → `/data`
- 관리자 초기화: 필수 외부 파일 `PORTAINER_ADMIN_PASSWORD_FILE`
- 엔진 소켓: `PORTAINER_SOCKET_PATH`, 기본 `/var/run/docker.sock`

실행 전 [Portainer 런북](../docs/guides/portainer.md)을 따르세요. 소켓 권한은
컨테이너 생성·exec·호스트 파일 접근까지 가능한 엔진 관리자 권한입니다.
소켓을 `:ro`로 마운트해도 API 쓰기를 막을 수 없으므로 읽기 전용 보안으로
표현하지 않습니다. Portainer 계정은 회계 애플리케이션 계정과 별개입니다.

정적 회귀 검사: `python3 -m unittest discover -s portainer/tests -v`
(Python 3 + PyYAML 필요). 실기동 검증 범위와 제약은 런북을 참고하세요.
