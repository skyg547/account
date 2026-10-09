package com.ho.account.closing.application.port.out;

import java.util.function.Supplier;

/** Runs one keyed financial command while a database lock excludes other workers on that key. */
public interface ClosingFinancialRunLockPort {
    void ensureLockRow(String scope, String key);
    <T> T withExclusiveRun(String scope, String key, Supplier<T> action);
}
