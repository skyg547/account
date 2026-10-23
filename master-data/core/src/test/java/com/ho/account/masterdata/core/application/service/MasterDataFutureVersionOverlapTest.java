package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Port-backed histories reproduce sequential requests without relying on transaction rollback. */
class MasterDataFutureVersionOverlapTest {

    private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);
    private static final String CODE = "SCD2-667";

    enum MasterType { ACCOUNT_SUBJECT, DEPARTMENT, PRODUCT }

    @ParameterizedTest
    @CsvSource({
            "ACCOUNT_SUBJECT, 10", "ACCOUNT_SUBJECT, 21", "ACCOUNT_SUBJECT, 30",
            "DEPARTMENT, 10", "DEPARTMENT, 21", "DEPARTMENT, 30",
            "PRODUCT, 10", "PRODUCT, 21", "PRODUCT, 30"
    })
    void secondUnboundedFutureChangeIsRejectedWithoutChangingEitherVersion(MasterType type, int secondStart) {
        Fixture<?> fixture = fixture(type);
        fixture.update(fixture.today.plusDays(21), OPEN_END);
        List<VersionState> before = fixture.states();
        assertThat(fixture.history.rows).hasSize(2);
        assertThat(fixture.history.saveAttempts).isEqualTo(2);

        assertThatIllegalArgumentException().isThrownBy(() ->
                fixture.update(fixture.today.plusDays(secondStart), OPEN_END));

        assertThat(fixture.states()).isEqualTo(before);
        assertThat(fixture.history.saveAttempts).isEqualTo(2);
        fixture.assertContinuousSingleVersion();
    }

    @ParameterizedTest
    @EnumSource(MasterType.class)
    void omittedEndDoesNotSilentlyExtendTheShortenedCurrentWindow(MasterType type) {
        Fixture<?> fixture = fixture(type);
        fixture.update(fixture.today.plusDays(21), OPEN_END);
        List<VersionState> before = fixture.states();

        assertThatIllegalArgumentException().isThrownBy(() ->
                fixture.update(fixture.today.plusDays(10), null));

        assertThat(fixture.states()).isEqualTo(before);
        assertThat(fixture.history.saveAttempts).isEqualTo(2);
        fixture.assertContinuousSingleVersion();
    }

    @ParameterizedTest
    @CsvSource({
            "ACCOUNT_SUBJECT, 10", "ACCOUNT_SUBJECT, 20",
            "DEPARTMENT, 10", "DEPARTMENT, 20", "PRODUCT, 10", "PRODUCT, 20"
    })
    void containedSplitPreservesScheduledVersionIncludingOneDayBoundary(MasterType type, int secondStart) {
        Fixture<?> fixture = fixture(type);
        fixture.update(fixture.today.plusDays(21), OPEN_END);
        VersionState scheduled = fixture.states().get(1);

        fixture.update(fixture.today.plusDays(secondStart), fixture.today.plusDays(20));

        assertThat(fixture.history.rows).hasSize(3);
        assertThat(fixture.history.saveAttempts).isEqualTo(4);
        assertThat(fixture.states().get(0).to()).isEqualTo(fixture.today.plusDays(secondStart - 1));
        assertThat(fixture.states().get(1)).isEqualTo(scheduled);
        assertThat(fixture.states().get(2).from()).isEqualTo(fixture.today.plusDays(secondStart));
        assertThat(fixture.states().get(2).to()).isEqualTo(fixture.today.plusDays(20));
        fixture.assertContinuousSingleVersion();
    }

    @ParameterizedTest
    @EnumSource(MasterType.class)
    void invalidRangesAreRejectedBeforeTheFirstMutationOrSave(MasterType type) {
        Fixture<?> fixture = fixture(type);
        List<VersionState> before = fixture.states();

        assertThatIllegalArgumentException().isThrownBy(() ->
                fixture.update(fixture.today.minusDays(30), OPEN_END));
        assertThatIllegalArgumentException().isThrownBy(() ->
                fixture.update(fixture.today.plusDays(10), fixture.today.plusDays(9)));

        assertThat(fixture.states()).isEqualTo(before);
        assertThat(fixture.history.saveAttempts).isZero();
    }

    private Fixture<?> fixture(MasterType type) {
        LocalDate today = LocalDate.now();
        return switch (type) {
            case ACCOUNT_SUBJECT -> accountSubjectFixture(today);
            case DEPARTMENT -> departmentFixture(today);
            case PRODUCT -> productFixture(today);
        };
    }

    private Fixture<AccountSubject> accountSubjectFixture(LocalDate today) {
        AccountSubject original = new AccountSubjectCommand(CODE, "Original", null,
                AccountSubject.AccountCategory.ASSETS, AccountSubject.BalanceType.DEBIT,
                null, false, false, today.minusDays(30), OPEN_END).toEntity();
        original.setId(1L);
        History<AccountSubject> history = new History<>(original, AccountSubject::getValidFrom,
                AccountSubject::getValidTo, AccountSubject::getUpdatedAt);
        AccountSubjectPersistencePort port = mock(AccountSubjectPersistencePort.class);
        // Like the real query, each lookup resolves today's row from the evolving history.
        when(port.findByCodeForUpdate(CODE)).thenAnswer(invocation -> history.current(LocalDate.now()));
        when(port.save(any(AccountSubject.class))).thenAnswer(invocation -> history.save(invocation.getArgument(0)));
        AccountSubjectService service = new AccountSubjectService(port, mock(MasterDataBusinessKeyLockPort.class));
        return new Fixture<>(today, history, (from, to) -> service.updateAccountSubject(CODE,
                new AccountSubjectCommand(CODE, "Revised", null, AccountSubject.AccountCategory.ASSETS,
                        AccountSubject.BalanceType.DEBIT, null, false, false, from, to)));
    }

    private Fixture<Department> departmentFixture(LocalDate today) {
        Department original = new DepartmentCommand(CODE, "Original", null,
                Department.DepartmentType.COST_CENTER, today.minusDays(30), OPEN_END).toEntity();
        original.setId(1L);
        History<Department> history = new History<>(original, Department::getValidFrom,
                Department::getValidTo, Department::getUpdatedAt);
        DepartmentPersistencePort port = mock(DepartmentPersistencePort.class);
        when(port.findActiveByCodeForUpdate(CODE)).thenAnswer(invocation -> history.current(LocalDate.now()));
        when(port.save(any(Department.class))).thenAnswer(invocation -> history.save(invocation.getArgument(0)));
        DepartmentService service = new DepartmentService(port, mock(MasterDataBusinessKeyLockPort.class));
        return new Fixture<>(today, history, (from, to) -> service.updateDepartment(CODE,
                new DepartmentCommand(CODE, "Revised", null, Department.DepartmentType.COST_CENTER, from, to)));
    }

    private Fixture<Product> productFixture(LocalDate today) {
        Product original = new ProductCommand(CODE, "Original", null, "EA", BigDecimal.ONE,
                Product.ProductType.PHYSICAL, today.minusDays(30), OPEN_END).toEntity();
        original.setId(1L);
        History<Product> history = new History<>(original, Product::getValidFrom,
                Product::getValidTo, Product::getUpdatedAt);
        ProductPersistencePort port = mock(ProductPersistencePort.class);
        when(port.findBusinessKeyById(1L)).thenReturn(Optional.of(CODE));
        // Product updates address a row ID; assert that the reused original is still today's row.
        when(port.findByIdForUpdate(1L)).thenAnswer(invocation -> {
            assertThat(history.current(LocalDate.now())).containsSame(original);
            return Optional.of(original);
        });
        when(port.save(any(Product.class))).thenAnswer(invocation -> history.save(invocation.getArgument(0)));
        ProductService service = new ProductService(port, mock(MasterDataBusinessKeyLockPort.class));
        return new Fixture<>(today, history, (from, to) -> service.updateProduct(1L,
                new ProductCommand(CODE, "Revised", null, "EA", BigDecimal.ONE,
                        Product.ProductType.PHYSICAL, from, to)));
    }

    private record VersionState(Object row, LocalDate from, LocalDate to, LocalDateTime updatedAt) { }

    private record Fixture<T>(LocalDate today, History<T> history, BiFunction<LocalDate, LocalDate, T> updater) {
        T update(LocalDate from, LocalDate to) {
            return updater.apply(from, to);
        }

        List<VersionState> states() {
            return history.rows.stream().map(row -> new VersionState(row, history.from.apply(row),
                    history.to.apply(row), history.updatedAt.apply(row))).toList();
        }

        void assertContinuousSingleVersion() {
            // Cover both sides of every changed boundary and the open-ended final date.
            for (LocalDate date = today.minusDays(30); !date.isAfter(today.plusDays(60)); date = date.plusDays(1)) {
                assertThat(history.validRows(date)).as("valid versions at %s", date).hasSize(1);
            }
            assertThat(history.validRows(OPEN_END)).hasSize(1);
        }
    }

    private static final class History<T> {
        private final List<T> rows = new ArrayList<>();
        private final Function<T, LocalDate> from;
        private final Function<T, LocalDate> to;
        private final Function<T, LocalDateTime> updatedAt;
        private int saveAttempts;

        private History(T original, Function<T, LocalDate> from, Function<T, LocalDate> to,
                Function<T, LocalDateTime> updatedAt) {
            rows.add(original);
            this.from = from;
            this.to = to;
            this.updatedAt = updatedAt;
        }

        private T save(T row) {
            saveAttempts++;
            if (rows.stream().noneMatch(existing -> existing == row)) {
                rows.add(row);
            }
            return row;
        }

        private List<T> validRows(LocalDate date) {
            return rows.stream().filter(row -> !date.isBefore(from.apply(row)) && !date.isAfter(to.apply(row))).toList();
        }

        private Optional<T> current(LocalDate date) {
            List<T> valid = validRows(date);
            assertThat(valid).hasSize(1);
            return Optional.of(valid.get(0));
        }
    }
}
