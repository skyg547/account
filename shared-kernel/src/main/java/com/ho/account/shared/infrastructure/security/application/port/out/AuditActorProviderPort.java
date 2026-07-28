package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.application.model.AuditActor;

public interface AuditActorProviderPort {

    AuditActor currentActor();
}

