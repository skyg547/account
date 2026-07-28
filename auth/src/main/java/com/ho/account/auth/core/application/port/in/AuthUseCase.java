package com.ho.account.auth.core.application.port.in;

import com.ho.account.auth.core.application.model.AuthenticationResult;

public interface AuthUseCase {

    AuthenticationResult login(LoginCommand command);

    boolean validateTokenVersion(String username, long roleVersion);

    /**
     * API DTO와 분리된 core 로그인 입력입니다. 비밀번호는 공백 제거를 하지 않습니다.
     */
    record LoginCommand(String username, String password) {
    }
}
