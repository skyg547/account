package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.Optional;

public interface AuthUserQueryPort {

    Optional<AuthUser> findByUsername(String username);
}

