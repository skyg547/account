package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Direct CREATE must reserve a business key across its entire SCD2 history. */
class MasterDataCreateHistoryTest {

    private static final String CODE = "MD-CREATE-HISTORY";
    private static final LocalDate TODAY = LocalDate.now();

    private record Window(int firstFrom, int firstTo, int secondFrom, int secondTo) {
        LocalDate firstFromDate() { return TODAY.plusDays(firstFrom); }
        LocalDate firstToDate() { return TODAY.plusDays(firstTo); }
        LocalDate secondFromDate() { return TODAY.plusDays(secondFrom); }
        LocalDate secondToDate() { return TODAY.plusDays(secondTo); }
    }

    static Stream<Window> windows() {
        return Stream.of(
                new Window(21, 30, 21, 30),   // Identical future reservation.
                new Window(-60, -30, -45, -20), // Overlap with expired history.
                new Window(21, 30, 30, 40),   // Dates are inclusive: day 30 overlaps.
                new Window(21, 30, 31, 40),   // Adjacent, disjoint intervals still reuse a key.
                new Window(21, 30, 35, 40)); // A gap does not make CREATE a reactivation API.
    }

    @ParameterizedTest
    @MethodSource("windows")
    void accountSubjectRejectsSecondCreateAndPreservesFirst(Window window) {
        AccountSubjectPersistencePort port = mock(AccountSubjectPersistencePort.class);
        List<AccountSubject> rows = new ArrayList<>();
        when(port.existsByCode(CODE)).thenAnswer(invocation -> !rows.isEmpty());
        when(port.findByCode(CODE)).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.isValid(TODAY)).findFirst());
        when(port.save(any(AccountSubject.class))).thenAnswer(invocation -> {
            AccountSubject row = invocation.getArgument(0);
            rows.add(row);
            return row;
        });
        AccountSubjectService service = new AccountSubjectService(port);

        AccountSubject first = service.createAccountSubject(accountCommand(window.firstFromDate(), window.firstToDate()));
        assertThatThrownBy(() -> service.createAccountSubject(
                accountCommand(window.secondFromDate(), window.secondToDate())))
                .isInstanceOf(MasterDataVersionConflictException.class);

        assertThat(rows).containsExactly(first);
        assertThat(first.getValidFrom()).isEqualTo(window.firstFromDate());
        assertThat(first.getValidTo()).isEqualTo(window.firstToDate());
    }

    @ParameterizedTest
    @MethodSource("windows")
    void departmentRejectsSecondCreateAndPreservesFirst(Window window) {
        DepartmentPersistencePort port = mock(DepartmentPersistencePort.class);
        List<Department> rows = new ArrayList<>();
        when(port.existsByCode(CODE)).thenAnswer(invocation -> !rows.isEmpty());
        when(port.findActiveByCode(CODE)).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.isValid(TODAY)).findFirst());
        when(port.save(any(Department.class))).thenAnswer(invocation -> {
            Department row = invocation.getArgument(0);
            rows.add(row);
            return row;
        });
        DepartmentService service = new DepartmentService(port);

        Department first = service.createDepartment(departmentCommand(window.firstFromDate(), window.firstToDate()));
        assertThatThrownBy(() -> service.createDepartment(
                departmentCommand(window.secondFromDate(), window.secondToDate())))
                .isInstanceOf(MasterDataVersionConflictException.class);

        assertThat(rows).containsExactly(first);
        assertThat(first.getValidFrom()).isEqualTo(window.firstFromDate());
        assertThat(first.getValidTo()).isEqualTo(window.firstToDate());
    }

    @ParameterizedTest
    @MethodSource("windows")
    void productRejectsSecondCreateAndPreservesFirst(Window window) {
        ProductPersistencePort port = mock(ProductPersistencePort.class);
        List<Product> rows = new ArrayList<>();
        when(port.existsByProductCode(CODE)).thenAnswer(invocation -> !rows.isEmpty());
        when(port.findActiveByProductCode(CODE)).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.isValid(TODAY)).findFirst());
        when(port.save(any(Product.class))).thenAnswer(invocation -> {
            Product row = invocation.getArgument(0);
            rows.add(row);
            return row;
        });
        ProductService service = new ProductService(port);

        Product first = service.createProduct(productCommand(window.firstFromDate(), window.firstToDate()));
        assertThatThrownBy(() -> service.createProduct(
                productCommand(window.secondFromDate(), window.secondToDate())))
                .isInstanceOf(MasterDataVersionConflictException.class);

        assertThat(rows).containsExactly(first);
        assertThat(first.getValidFrom()).isEqualTo(window.firstFromDate());
        assertThat(first.getValidTo()).isEqualTo(window.firstToDate());
    }

    @ParameterizedTest
    @MethodSource("windows")
    void businessPartnerRejectsSecondCreateAndPreservesFirst(Window window) {
        BusinessPartnerPersistencePort port = mock(BusinessPartnerPersistencePort.class);
        List<BusinessPartner> rows = new ArrayList<>();
        when(port.existsByBusinessPartnerCode(CODE)).thenAnswer(invocation -> !rows.isEmpty());
        when(port.save(any(BusinessPartner.class))).thenAnswer(invocation -> {
            BusinessPartner row = invocation.getArgument(0);
            rows.add(row);
            return row;
        });
        BusinessPartnerService service = new BusinessPartnerService(port);

        BusinessPartner first = service.createBusinessPartner(partnerCommand(window.firstFromDate(), window.firstToDate()));
        assertThatThrownBy(() -> service.createBusinessPartner(
                partnerCommand(window.secondFromDate(), window.secondToDate())))
                .isInstanceOf(MasterDataVersionConflictException.class);

        assertThat(rows).containsExactly(first);
        assertThat(first.getValidFrom()).isEqualTo(window.firstFromDate());
        assertThat(first.getValidTo()).isEqualTo(window.firstToDate());
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class,
            names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void approvedRequestFlowStillRejectsCreateWhenHistoryExists(MasterDataType type) {
        MasterDataChangeRequestPersistencePort requests = mock(MasterDataChangeRequestPersistencePort.class);
        MasterDataVersionQueryPort versions = mock(MasterDataVersionQueryPort.class);
        MasterDataChangeApplier applier = mock(MasterDataChangeApplier.class);
        when(applier.targetType()).thenReturn(type);
        when(versions.countPersistedVersions(type, CODE)).thenReturn(1L);
        MasterDataChangeRequestService service = new MasterDataChangeRequestService(
                requests, versions, List.of(applier));
        MasterDataChangeRequestCommand command = new MasterDataChangeRequestCommand(
                type, CODE, ChangeType.CREATE, TODAY.plusDays(21), 1,
                "requester", "new key", "{}", null);

        assertThatThrownBy(() -> service.requestChange(command))
                .isInstanceOf(MasterDataVersionConflictException.class);
        verify(applier).validate(any(MasterDataChangeRequest.class));
        verify(requests, never()).save(any(MasterDataChangeRequest.class));
    }

    private static AccountSubjectCommand accountCommand(LocalDate from, LocalDate to) {
        return new AccountSubjectCommand(CODE, "테스트 계정", null, AccountSubject.AccountCategory.ASSETS,
                AccountSubject.BalanceType.DEBIT, null, false, false, from, to);
    }

    private static DepartmentCommand departmentCommand(LocalDate from, LocalDate to) {
        return new DepartmentCommand(CODE, "테스트 부서", null, Department.DepartmentType.COST_CENTER, from, to);
    }

    private static ProductCommand productCommand(LocalDate from, LocalDate to) {
        return new ProductCommand(CODE, "테스트 상품", null, null, BigDecimal.ONE,
                Product.ProductType.SERVICE, from, to);
    }

    private static BusinessPartnerCommand partnerCommand(LocalDate from, LocalDate to) {
        return new BusinessPartnerCommand(CODE, "테스트 거래처", null, null, null, null,
                BusinessPartner.PartnerType.VENDOR, true, null, null, from, to);
    }
}
