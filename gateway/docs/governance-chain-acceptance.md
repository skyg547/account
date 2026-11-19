# 거버넌스 실제 체인 검증

`/api/audit/**`는 패키지와 Config Server의 `audit-governance-api`에서 `lb://auth-service`로
전달됩니다. Auth가 승인 기록과 역할 변경을 같은 데이터베이스에서 처리하며, 내부감사 업무의
`/api/v1/internalaudit/**`는 별도 서비스가 담당합니다.

## 검증 범위

`gateway/tests/governance-chain-acceptance.mjs`는 실제 Auth/Gateway bootJar와 현재 프런트엔드의
Next BFF route 소스를 사용합니다. BFF 소스는 임시 Next 프로젝트에 그대로 복사하며 저장소의
`.env`를 로드하지 않습니다. Gateway 라우트 목록과 JWT/요청 제한 필터는 패키지에서 읽고,
SimpleDiscoveryClient에 이번 실행의 Auth 주소만 등록합니다. 원격 Config/Eureka는 끕니다.
Gateway의 현재 권한 버전 검증은 켠 상태에서 실제 Auth HTTP API를 호출합니다.

token-version WebClient의 패키지 기본값은 `http://auth-service`이며 하네스에서 주소를 바꾸지
않습니다. 서비스 ID는 LoadBalancer가 해석하고 전송은 HTTP입니다. SimpleDiscovery의 인스턴스가
별도 스킴을 제공하지 않아도 유효한 HTTP URL을 구성하도록 합니다. 기존 Eureka 인스턴스는 자신의
스킴을 제공하므로, 이 호환성 변경을 기존 Eureka 장애의 증거로 해석하지 않습니다.

초보자 관점에서 이 검사는 브라우저 대신 HTTP 요청을 보내며, 앞문인 BFF부터 Gateway를 거쳐
실제 Auth까지 통과하는지 확인합니다. 테스트용 Gateway 응답이나 메모리 승인 receipt는 만들지
않습니다. H2 파일은 실제 Flyway 마이그레이션과 JPA가 사용하고, 서비스 종료 후 JDBC로 다시
읽어 승인·역할 변경이 저장되었는지 확인합니다.

- BFF와 Gateway의 미인증 요청 401.
- 실제 로그인과 HttpOnly 쿠키, 현재 역할의 권한 조회 및 다른 역할/사용자 대상 요청 403.
- 프런트엔드 모양의 `AUTH_USER_ROLE` 요청에서 양수 id와 `PENDING`, 검증된 요청자 확인.
- pending API로 같은 기록 재조회, 자기 승인 403, 서로 다른 관리자 승인, 반복 승인 409.
- 대상 재로그인에서 새 역할과 증가한 roleVersion, 기존 토큰 401.
- Auth 종료 후 H2에서 승인자·요청자·APPROVED 및 실제 역할·roleVersion 직접 확인.

## 전제 조건과 실행

Java 17, 프런트엔드 계약의 Node 20, lockfile과 일치하는 기존 frontend 의존성, 현재 소스로 만든
두 bootJar가 필요합니다. 패키지 설치·다운로드는 실행자가 승인한 별도 준비 단계입니다.
하네스는 설치를 실행하지 않고 필수 파일이나 Node 버전이 맞지 않으면 실패합니다.

저장소 루트에서 빌드와 정책 테스트를 먼저 실행합니다.

```powershell
.\gradlew :gateway:test :gateway:bootJar :auth:api:test :auth:api:bootJar --offline --no-daemon --console=plain --max-workers=1
node gateway/tests/governance-chain-acceptance.mjs
```

기본 JAR 파일명은 `gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar`와
`auth/api/build/libs/api-0.0.1-SNAPSHOT.jar`입니다. 다른 이름은 `CHAIN_GATEWAY_JAR`,
`CHAIN_AUTH_JAR`에 로컬 파일 경로를 지정하며 Java 실행 파일은 `CHAIN_JAVA`로 지정할 수 있습니다.
실행은 부모 검증 작업과 조정해서 진행합니다. 문서의 실행 명령 자체가 실행 성공 근거는 아닙니다.

하네스는 임시 디렉터리, 임의의 비밀번호·JWT키·BFF키, loopback의 임시 포트만 사용합니다.
운영 자격증명이나 URL을 상속하지 않으며 자식 프로세스는 숨김 창으로 실행합니다. 로그와 토큰은
출력하지 않고 실패 단계만 알립니다. 시작한 프로세스는 성공·실패 모두 종료하며 합성 H2와 복사된
BFF는 출력된 임시 경로에 보존합니다. 자동 재귀 삭제는 하지 않습니다. 재실행은 새 디렉터리와
DB를 생성하므로 이전 승인 기록과 섞이지 않습니다.

성공 메시지는 모든 필수 검증을 마친 경우에만 `PASS`를 표시합니다. 누락 전제 조건, 서버 시작
실패, HTTP/DB 검증 실패는 종료 코드 1입니다. H2 체인 성공은 PostgreSQL SQL/잠금/동시성,
실제 Eureka/Config 배포, TLS·브라우저 동작의 검증을 대신하지 않습니다. PostgreSQL의 마이그레이션과
승인 경합 검증은 Auth의 별도 테스트 게이트로 실행하고 결과를 구분해 기록합니다.

## 변경 되돌리기

라우트만 먼저 되돌려야 할 경우 패키지와 Config Server의 `audit-governance-api`를 함께 되돌리고
Gateway를 재시작합니다. Auth 스키마와 승인 기록은 자동 삭제하지 않습니다. 전체 변경은 검토된
Issue 단위 revert로 처리하며 실제 운영 데이터 복구는 별도 절차입니다.
