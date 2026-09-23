package com.ho.account.auth.core.application.model;

/** Technology-neutral user projection for the administrative user list. */
public record AdminUserView(
        long id,
        String name,
        String email,
        String role,
        String status,
        String lastLogin,
        String department) {
}
