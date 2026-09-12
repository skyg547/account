package com.ho.account.masterdata.core.domain.policy;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 모든 SCD2 기준정보가 같은 유효기간 규칙을 사용하도록 모아 둔 도메인 정책입니다.
 */
public final class MasterDataValidityPolicy {

    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

    private MasterDataValidityPolicy() {
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
        requireValidityWindow(validFromSupplier.get(), validToSupplier.get());
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

    /**
     * 신규 SCD2 버전의 시작일과 종료일이 뒤집히지 않았는지 저장 전에 확인합니다.
     */
    public static void requireValidityWindow(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null || validTo == null) {
            throw new IllegalArgumentException("SCD2 validFrom and validTo are required.");
        }
        if (validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("SCD2 validTo cannot be before validFrom.");
        }
    }

    /**
     * 기존 유효구간 안에서만 새 버전을 분할하도록 변경 전에 확인합니다.
     *
     * <p>기존 종료일 뒤에는 이미 예약된 버전이 있을 수 있으므로 새 구간을 그 밖으로
     * 확장하지 않습니다. 시작일은 기존 시작일보다 늦어야 이전 버전도 최소 하루를 유지합니다.</p>
     */
    public static void requireVersionSplit(
            LocalDate currentValidFrom,
            LocalDate currentValidTo,
            LocalDate newValidFrom,
            LocalDate newValidTo) {
        requireValidityWindow(currentValidFrom, currentValidTo);
        requireValidityWindow(newValidFrom, newValidTo);
        if (!newValidFrom.isAfter(currentValidFrom)) {
            throw new IllegalArgumentException("SCD2 new validFrom must be after the current validFrom.");
        }
        if (newValidTo.isAfter(currentValidTo)) {
            throw new IllegalArgumentException("SCD2 new validity window cannot extend the current validity window.");
        }
    }

    private static boolean isActive(LocalDate date, LocalDate validFrom, LocalDate validTo) {
        if (date == null || validFrom == null || validTo == null) {
            return false;
        }
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }
}
