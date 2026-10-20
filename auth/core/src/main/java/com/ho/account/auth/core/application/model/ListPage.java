package com.ho.account.auth.core.application.model;

import java.util.List;

/** A count-free page; one extra SQL row establishes whether a later page exists. */
public record ListPage<T>(List<T> content, int page, int size, boolean hasNext) {
    public ListPage {
        content = List.copyOf(content);
    }
}
