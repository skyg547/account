package com.ho.account.gateway.security;

/**
 * 외부 액세스 토큰을 기술 독립적인 내부 신원으로 바꾸는 보안 포트입니다.
 *
 * <p>필터는 JJWT 같은 라이브러리를 직접 알지 않고, 이 계약의 성공/실패만 사용합니다.</p>
 */
@FunctionalInterface
public interface AccessTokenVerifier {

    AuthenticatedPrincipal verify(String token);
}
