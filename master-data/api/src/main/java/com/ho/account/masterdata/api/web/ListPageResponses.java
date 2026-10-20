package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
import java.util.List;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Retains the legacy JSON array while exposing page continuation in headers. */
final class ListPageResponses {
    private ListPageResponses() { }

    static PageWindow window(int page, int size) {
        try {
            return new PageWindow(page, size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    static <T, R> ResponseEntity<List<R>> respond(
            PageSlice<T> slice, PageWindow window, Function<T, R> mapper) {
        return ResponseEntity.ok()
                .header("X-Page", Integer.toString(window.page()))
                .header("X-Page-Size", Integer.toString(window.size()))
                .header("X-Has-Next", Boolean.toString(slice.hasNext()))
                .body(slice.content().stream().map(mapper).toList());
    }
}
