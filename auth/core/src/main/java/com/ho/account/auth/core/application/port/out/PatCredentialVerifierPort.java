package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.application.model.PatActor;

/** Verifies a bearer credential against its signature and the current AuthUser. */
public interface PatCredentialVerifierPort {

    PatActor verify(String authorizationHeader);
}
