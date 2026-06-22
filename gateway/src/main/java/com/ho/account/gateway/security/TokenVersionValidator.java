package com.ho.account.gateway.security;

import reactor.core.publisher.Mono;

/**
 * JWT 안의 roleVersion이 Auth의 현재 사용자 버전과 같은지 확인하는 포트입니다.
 *
 * <p>🐣 Gateway 필터는 "검문소" 역할만 하고, Auth와 통신하는 방법은 이 포트 뒤에 숨깁니다.</p>
 */
public interface TokenVersionValidator {

    Mono<Boolean> validate(String username, long roleVersion);
}
