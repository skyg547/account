# Cloudflare Tunnel

외부 호스트 포트를 추가하지 않고 `account-network`의
`http://account-frontend-nginx:80`으로 연결하는 독립 Compose 모듈입니다.

- `docker-compose.yml`: 계정·토큰 없이 임시 공용 URL을 만드는 Quick Tunnel.
- `docker-compose.named.yml`: `CLOUDFLARE_TUNNEL_TOKEN`을 사용하는 원격 관리 Named Tunnel.
- `.env.example`: 비밀값 없는 환경 파일 예시.

두 Compose 파일은 **각각 단독 실행**합니다. 같은 project/service를 사용하므로
모드 전환 시 기존 커넥터를 먼저 중지합니다.
[실행·Access 보호·검증·롤백 가이드](../docs/guides/cloudflare-tunnel-guide.md)를 따라 진행하세요.
