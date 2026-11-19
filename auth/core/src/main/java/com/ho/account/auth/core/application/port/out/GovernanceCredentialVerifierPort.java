package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.application.model.GovernanceActor;

public interface GovernanceCredentialVerifierPort {
    GovernanceActor verify(String authorization);
}
