package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.application.model.ListPage;
import com.ho.account.auth.core.application.model.ListPageRequest;
import java.util.Optional;

public interface AuthUserQueryPort {

    Optional<AuthUser> findByUsername(String username);

    /** Returns users in a deterministic username order for read-only administrative views. */
    ListPage<AuthUser> findUsers(ListPageRequest pageRequest);
}
