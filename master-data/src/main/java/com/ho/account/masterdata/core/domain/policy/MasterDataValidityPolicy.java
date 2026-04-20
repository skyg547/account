package com.ho.account.masterdata.core.domain.policy;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class MasterDataValidityPolicy {

    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

    private MasterDataValidityPolicy() {
    }

    public static <T> Predicate<T> isActiveNow(
            Function<T, LocalDate> validFromExtractor,
            Function<T, LocalDate> validToExtractor) {
        LocalDate today = LocalDate.now();
        return candidate -> isActiveAt(today, validFromExtractor.apply(candidate), validToExtractor.apply(candidate));
    }

    public static boolean isActiveAt(LocalDate date, LocalDate validFrom, LocalDate validTo) {
        return isActive(date, validFrom, validTo);
    }

    public static void applyDefaultWindow(
            Supplier<LocalDate> validFromSupplier,
            Consumer<LocalDate> validFromSetter,
            Supplier<LocalDate> validToSupplier,
            Consumer<LocalDate> validToSetter) {
        if (validFromSupplier.get() == null) {
            validFromSetter.accept(LocalDate.now());
        }
        if (validToSupplier.get() == null) {
            validToSetter.accept(OPEN_ENDED_DATE);
        }
    }

    public static void closeIfActive(Supplier<LocalDate> validToSupplier, Consumer<LocalDate> validToSetter) {
        LocalDate today = LocalDate.now();
        LocalDate validTo = validToSupplier.get();
        if (validTo == null || validTo.isAfter(today)) {
            validToSetter.accept(today);
        }
    }

    private static boolean isActive(LocalDate date, LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null || validTo == null) {
            return false;
        }
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }
}
