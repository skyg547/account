package com.ho.account.auth.core.application.port.in;

import com.ho.account.auth.api.dto.LoginResponse;

public interface AuthUseCase {

    LoginResponse login(String username, String password);
}

