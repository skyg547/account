package com.ho.account.auth.api.dto;

import com.ho.account.auth.core.application.model.AdminUserView;

public record AdminUserDto(
        Long id,
        String name,
        String email,
        String role,
        String status,
        String lastLogin,
        String dept) {

    public static AdminUserDto from(AdminUserView view) {
        return new AdminUserDto(
                view.id(),
                view.name(),
                view.email(),
                view.role(),
                view.status(),
                view.lastLogin(),
                view.department());
    }
}
