# Issue #42: 거래처 등록·승인 화면과 Master Data API 연동

## 상태

- GitHub Issue: `#42`
- Branch: `agent/42-master-data-approval`
- Worktree: `C:\tmp\account-42-master-data-approval`
- 2026-07-29 closure audit에서 문서만 추가되고 실제 화면이 없음을 확인해 Issue가 다시 열렸습니다.
- PR `#234`가 merge commit `57aa1729`로 병합됐고 `Fixes #42`로 Issue가 닫혔습니다.

## 요구사항

1. `frontend/src/app/master-data/partner/page.tsx`에 실제 거래처 등록·승인 화면을 둡니다.
2. 거래처 목록은 `BusinessPartnerController`의 `GET /api/basic/businesspartners` 응답을 사용합니다.
3. 신규 거래처는 직접 쓰기 대신 `POST /api/master-data/change-requests`로 `BUSINESS_PARTNER / CREATE` 요청을 만듭니다.
4. 승인 대기 목록은 `GET /api/master-data/change-requests/pending`에서 거래처 요청만 표시합니다.
5. 승인과 반려는 core 유즈케이스가 소유한 상태 전이 API를 호출합니다.

## 아키텍처 결정

- 프런트엔드는 HTTP DTO 매핑과 로딩·오류·사용자 상호작용만 담당합니다.
- 승인자 분리, 현재 버전 확인, `REQUESTED -> APPROVED/REJECTED` 불변식은 기존 core 도메인/application 서비스에 남깁니다.
- 클라이언트가 `X-User-ID` 같은 신뢰 헤더를 만들지 않고 로그인 세션의 Bearer 토큰만 전달합니다.
- Backend actor와 역할은 Gateway가 JWT에서 재생성한 `X-Auth-User`/`X-Auth-Roles`만 사용하며 관리 역할이 없는 요청은 거절합니다.
- BUSINESS_PARTNER payload는 접수·승인 전에 typed command/domain 검증을 통과해야 합니다.
- API 실패를 빈 배열이나 Mock 데이터로 바꾸지 않아 운영 장애가 정상 화면처럼 보이지 않게 합니다.

## 검증 근거

- 변경 프런트 ESLint는 통과했고 Next source compilation 뒤 전체 type check는 Issue 밖의 기존 `PageHeader.breadcrumbs` 2건에서 중단됐습니다.
- Master Data Core 13, API 9, Gateway 30으로 총 52 tests와 API/Gateway `bootJar`가 통과했습니다.
- diff/충돌 표식/legacy header-path 정적 검사가 통과했습니다.
- 최초 독립 리뷰 finding을 수정한 뒤 재검토가 남은 P0-P3 없이 PASS했습니다.
- Docker CLI 부재로 Compose 실행 검증은 미실행했고 build argument 구조는 정적으로 검토했습니다.

## 잔여 위험

- pending API와 거래처 목록은 아직 pagination이 없어 대량 운영 전 포트부터 제한 계약을 추가해야 합니다.
- trusted header 계약을 지키려면 Master Data 서비스 포트를 외부에 직접 공개하지 않고 Gateway 뒤에 유지해야 합니다.
