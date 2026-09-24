package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalActor;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Parses Gateway-rebuilt identity headers and fails closed for journal write commands. */
final class JournalCommandAuthorization {

    static final String AUTH_USER_HEADER = "X-Auth-User";
    static final String AUTH_ROLES_HEADER = "X-Auth-Roles";

    private static final Set<String> MAKER_ROLES = Set.of(
            "ROLE_JOURNAL_MAKER", "ROLE_ACCOUNTING_ADMIN", "ROLE_ADMIN");
    private static final Set<String> APPROVER_ROLES = Set.of(
            "ROLE_JOURNAL_APPROVER", "ROLE_ACCOUNTING_ADMIN", "ROLE_ADMIN");
    private static final Set<String> POSTER_ROLES = Set.of(
            "ROLE_JOURNAL_POSTER", "ROLE_ACCOUNTING_ADMIN", "ROLE_ADMIN");

    private JournalCommandAuthorization() {
    }

    static String requireMaker(String actor, String roles) {
        return require(actor, roles, MAKER_ROLES, "A journal maker role is required.");
    }

    static String requireApprover(String actor, String roles) {
        return require(actor, roles, APPROVER_ROLES, "A journal approver role is required.");
    }

    static String requirePoster(String actor, String roles) {
        return require(actor, roles, POSTER_ROLES, "A journal posting role is required.");
    }

    private static String require(String actor, String roles, Set<String> allowed, String message) {
        final String canonicalActor;
        try {
            canonicalActor = JournalActor.canonicalize(actor);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated journal actor is required.");
        }
        boolean authorized = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .anyMatch(allowed::contains);
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        return canonicalActor;
    }
}
