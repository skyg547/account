package com.ho.account.audit.application.port.out;

import com.ho.account.audit.application.model.AuditActor;

public interface AuditActorProviderPort {

    AuditActor currentActor();
}

