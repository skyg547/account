package com.ho.account.auth.api.dto;

import java.util.List;

public record RoleAssignmentApplyResponse(
        String username,
        long roleVersion,
        List<String> roles) {
}
