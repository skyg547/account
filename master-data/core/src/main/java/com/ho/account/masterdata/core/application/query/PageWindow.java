package com.ho.account.masterdata.core.application.query;

/** Zero-based, bounded page request shared by inbound and outbound ports. */
public record PageWindow(int page, int size) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageWindow {
        if (page < 0 || size < 1 || size > MAX_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and " + MAX_SIZE);
        }
    }
}
