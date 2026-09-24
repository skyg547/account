package com.ho.account.journalledger.domain.journal.domain;

import java.util.Locale;

/**
 * Canonical identity policy for journal command actors.
 *
 * <p>Trusted adapters authenticate the principal, while the domain normalizes every human or
 * service principal before it is persisted or compared. Case and surrounding whitespace cannot
 * therefore be used to bypass maker-checker separation.</p>
 */
public final class JournalActor {

    private static final int MAX_LENGTH = 50;

    private JournalActor() {
    }

    public static String canonicalize(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Journal actor is required.");
        }
        String canonical = actor.trim().toLowerCase(Locale.ROOT);
        if (canonical.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Journal actor must not exceed " + MAX_LENGTH + " characters.");
        }
        return canonical;
    }

    public static boolean sameIdentity(String first, String second) {
        return canonicalize(first).equals(canonicalize(second));
    }
}
