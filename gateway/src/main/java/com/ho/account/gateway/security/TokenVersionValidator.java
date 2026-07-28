package com.ho.account.gateway.security;

import reactor.core.publisher.Mono;

/**
 * JWT 안의 roleVersion이 Auth의 현재 사용자 버전과 같은지 확인하는 출력 포트입니다.
 *
 * <p>초보자 설명: Gateway 필터는 "검문소" 역할만 하고, Auth와 통신하는 방법은 이 포트 뒤에 숨깁니다.
 * 네트워크 장애와 실제 권한 변경을 서로 다른 결과로 돌려주므로 호출자가 올바른 응답을 만들 수 있습니다.</p>
 */
@FunctionalInterface
public interface TokenVersionValidator {

    Mono<TokenVersionValidationResult> validate(String username, long roleVersion);
}
