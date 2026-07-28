package com.ho.account.gateway.security;

/**
 * 액세스 토큰의 서명, 필수 claim 또는 업무상 신원 값이 유효하지 않을 때 발생합니다.
 */
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
