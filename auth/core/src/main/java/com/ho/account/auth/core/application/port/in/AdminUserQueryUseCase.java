package com.ho.account.auth.core.application.port.in;

import com.ho.account.auth.core.application.model.AdminUserView;
import com.ho.account.auth.core.application.model.ListPage;
import com.ho.account.auth.core.application.model.ListPageRequest;

public interface AdminUserQueryUseCase {

    ListPage<AdminUserView> findUsers(String authenticatedUsername, ListPageRequest pageRequest);
}
