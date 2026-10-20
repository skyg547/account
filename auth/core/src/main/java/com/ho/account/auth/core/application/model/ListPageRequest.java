package com.ho.account.auth.core.application.model;

/** Zero-based, bounded page request shared by list use cases and persistence ports. */
public record ListPageRequest(int page, int size) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public ListPageRequest {
        if (page < 0 || size < 1 || size > MAX_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid page or size");
        }
    }
}
