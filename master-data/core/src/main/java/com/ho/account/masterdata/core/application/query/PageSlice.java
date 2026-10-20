package com.ho.account.masterdata.core.application.query;

import java.util.List;

/** Page contents and continuation flag, without an additional COUNT query. */
public record PageSlice<T>(List<T> content, boolean hasNext) {
    public PageSlice {
        content = List.copyOf(content);
    }
}
