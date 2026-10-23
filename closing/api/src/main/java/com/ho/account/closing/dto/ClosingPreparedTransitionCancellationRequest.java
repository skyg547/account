package com.ho.account.closing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An operator names the exact close operation and a durable cancellation reason. */
public record ClosingPreparedTransitionCancellationRequest(
        @NotBlank @Size(max = 36) String operationId,
        @NotBlank @Size(max = 300) String reason) { }
