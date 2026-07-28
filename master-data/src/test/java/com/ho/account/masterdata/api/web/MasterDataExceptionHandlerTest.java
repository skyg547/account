package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.shared.finance.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataExceptionHandlerTest {

    @Test
    void mapsStaleBusinessVersionToConflictInsteadOfServerError() {
        MasterDataExceptionHandler handler = new MasterDataExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleVersionConflict(
                new MasterDataVersionConflictException("expected=3, requested=2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("expected=3").contains("requested=2");
    }

    @Test
    void mapsReusedSourceReferenceToConflict() {
        MasterDataExceptionHandler handler = new MasterDataExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleIdempotencyConflict(
                new MasterDataIdempotencyConflictException("governance-approval-id=55"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("governance-approval-id=55");
    }
}
