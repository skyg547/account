package com.ho.account.auth.core.application.port.in;

import com.ho.account.auth.core.application.model.AdminUserView;
import java.util.List;

public interface AdminUserQueryUseCase {

    List<AdminUserView> findAllUsers();
}
