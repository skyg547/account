package com.ho.account.masterdata.api.web;

import java.util.Arrays;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gateway가 검증해 전달한 역할 헤더를 직접 쓰기 경계에서 확인해, 역할이 없거나 허용되지 않으면
 * 승인 우회 mutation을 fail-closed 합니다.
 *
 * <p>높은 우선순위의 scoped advice가 정책의 {@link ResponseStatusException}을 먼저 처리해 공통
 * {@code Exception} handler가 의도한 403을 500으로 바꾸지 못하게 합니다. 기존 API의 예외 계약에는
 * 영향을 주지 않도록 직접 쓰기를 제공하는 네 Controller에만 적용합니다.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        AccountSubjectController.class,
        BusinessPartnerController.class,
        ProductController.class,
        DepartmentController.class
})
final class MasterDataDirectWritePolicy {

    private static final Set<String> ADMIN_ROLES = Set.of(
            "ADMIN", "SYSTEM_ADMIN", "MASTER_MANAGER", "ACCOUNTING_ADMIN", "PARTNER_MANAGER");

    MasterDataDirectWritePolicy() {
    }

    static void requireAdminRole(String authenticatedRoles) {
        boolean authorized = authenticatedRoles != null && Arrays.stream(authenticatedRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .anyMatch(ADMIN_ROLES::contains);
        if (!authorized) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Master Data administrator role is required.");
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Void> handleResponseStatusException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).build();
    }
}
