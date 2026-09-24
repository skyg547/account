package com.ho.account.closing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClosingTransitionRecoveryRequest(@NotBlank @Size(max = 36) String operationId,
        boolean remoteRequestTerminated) { }
