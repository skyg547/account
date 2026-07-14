package com.ho.account.masterdata.core.domain.policy;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 모든 SCD2 기준정보가 같은 유효기간 규칙을 사용하도록 모아 둔 도메인 정책입니다.
 */
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

    /**
     * 종료 요청이 기존 유효기간을 축소하는 정상적인 SCD2 변경인지 확인합니다.
     *
     * <p>종료일이 시작일보다 빠르면 이력이 뒤집히고, 기존 종료일보다 늦으면 과거 버전을
     * 다시 연장하는 결과가 되므로 둘 다 거부합니다.</p>
     */
    public static LocalDate requireTerminationDate(
            LocalDate requestedEndDate,
            LocalDate validFrom,
            LocalDate currentValidTo) {
        if (requestedEndDate == null) {
            throw new IllegalArgumentException("Termination effective date is required.");
        }
        if (validFrom != null && requestedEndDate.isBefore(validFrom)) {
            throw new IllegalArgumentException("Termination effective date cannot be before validFrom.");
        }
        if (currentValidTo != null && requestedEndDate.isAfter(currentValidTo)) {
            throw new IllegalArgumentException("Termination effective date cannot extend the current validity window.");
        }
        return requestedEndDate;
    }

    private static boolean isActive(LocalDate date, LocalDate validFrom, LocalDate validTo) {
        if (date == null || validFrom == null || validTo == null) {
            return false;
        }
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }
}