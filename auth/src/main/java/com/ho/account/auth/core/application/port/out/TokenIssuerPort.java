package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.AuthUser;

public interface TokenIssuerPort {

    IssuedToken issue(AuthUser user);

    record IssuedToken(
            String token,
            long expiresInSeconds) {
    }
}

