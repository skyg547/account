package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;

/** Writes evidence in the caller's business transaction. */
public interface MasterDataAuditPort {
    void append(MasterDataAuditEvent event);
}
