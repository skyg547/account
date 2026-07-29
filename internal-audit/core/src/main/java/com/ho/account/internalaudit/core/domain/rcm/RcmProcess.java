package com.ho.account.internalaudit.core.domain.rcm;

import lombok.Builder;

@Builder
public record RcmProcess(
        String processId,
        String processName,
        String description,
        String ownerId
) {}
