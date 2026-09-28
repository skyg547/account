package com.ho.account.closing.web;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves command authority only from identity headers rebuilt by the Gateway.
 * Closing API ports must therefore remain unavailable for direct public access.
 */
final class ClosingCommandAuthority {

    static final String AUTH_USER_HEADER = "X-Auth-User";
    static final String AUTH_ROLES_HEADER = "X-Auth-Roles";

    private static final int DEFAULT_ACTOR_MAX_LENGTH = 50;
    private static final Set<String> COMMAND_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_ACCOUNTING_ADMIN",
            "ROLE_CLOSING_MANAGER");

    private ClosingCommandAuthority() {
    }

    static String requireAuthenticatedActor(String actor) {
        return requireAuthenticatedActor(actor, DEFAULT_ACTOR_MAX_LENGTH);
    }

    static String requireAuthenticatedActor(String actor, int maxActorLength) {
        if (actor == null || actor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated actor is required.");
        }
        String trustedActor = actor.trim();
        // Each inbound boundary supplies its persisted audit-column limit; general Closing defaults to 50.
        if (trustedActor.length() > maxActorLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authenticated actor must not exceed " + maxActorLength + " characters.");
        }
        return trustedActor;
    }

    static String requireCommandAuthority(String actor, String roles) {
        return requireCommandAuthority(actor, roles, DEFAULT_ACTOR_MAX_LENGTH);
    }

    static String requireCommandAuthority(String actor, String roles, int maxActorLength) {
        String trustedActor = requireAuthenticatedActor(actor, maxActorLength);
        boolean authorized = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .anyMatch(COMMAND_ROLES::contains);
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A closing command role is required.");
        }
        return trustedActor;
    }
}
