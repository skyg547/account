package com.ho.account.internalaudit.core.application.port.out;

import com.ho.account.internalaudit.core.domain.CommandReceipt;
import java.util.Optional;

/**
 * Command receipts share the business transaction. Lock the normalized module-wide
 * key before checking receipts or audit history, then reserve and complete once.
 */
public interface CommandReceiptPort {

    void lockKey(String key);

    Optional<CommandReceipt> findByKey(String key);

    void reserve(String key, int fingerprintVersion, String fingerprint);

    void complete(String key, int snapshotVersion, String snapshotJson);
}
