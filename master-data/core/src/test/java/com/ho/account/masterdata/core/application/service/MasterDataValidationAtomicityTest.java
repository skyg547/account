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
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Invalid direct updates must leave the existing SCD2 row open, even without a database rollback. */
class MasterDataValidationAtomicityTest {
    private static final String KEY = "MD-913";
    private static final LocalDate FROM = LocalDate.now().minusDays(30);
    private static final LocalDate UPDATE = LocalDate.now().plusDays(1);
    private static final LocalDate OPEN = LocalDate.of(9999, 12, 31);

    @Test
    void invalidAccountUpdateDoesNotCloseCurrentVersion() {
        AccountSubjectPersistencePort port = mock(AccountSubjectPersistencePort.class);
        AccountSubject current = new AccountSubject();
        current.setCode(KEY);
        current.setName("Current");
        current.setValidFrom(FROM);
        current.setValidTo(OPEN);
        when(port.findByCodeForUpdate(KEY)).thenReturn(Optional.of(current));
        AccountSubjectService service = new AccountSubjectService(port, mock(MasterDataBusinessKeyLockPort.class));

        assertThatThrownBy(() -> service.updateAccountSubject(KEY, new AccountSubjectCommand(
                KEY, "X".repeat(101), null, null, null, null, null, null, UPDATE, null,
                null, null, false))).isInstanceOf(IllegalArgumentException.class);

        assertThat(current.getValidTo()).isEqualTo(OPEN);
        verify(port, never()).save(any());
    }

    @Test
    void invalidDepartmentUpdateDoesNotCloseCurrentVersion() {
        DepartmentPersistencePort port = mock(DepartmentPersistencePort.class);
        Department current = new Department();
        current.setCode(KEY);
        current.setName("Current");
        current.setValidFrom(FROM);
        current.setValidTo(OPEN);
        when(port.findActiveByCodeForUpdate(KEY)).thenReturn(Optional.of(current));
        DepartmentService service = new DepartmentService(port, mock(MasterDataBusinessKeyLockPort.class));

        assertThatThrownBy(() -> service.updateDepartment(KEY, new DepartmentCommand(
                KEY, "X".repeat(101), null, null, UPDATE, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(current.getValidTo()).isEqualTo(OPEN);
        verify(port, never()).save(any());
    }

    @Test
    void invalidProductUpdateDoesNotCloseCurrentVersion() {
        ProductPersistencePort port = mock(ProductPersistencePort.class);
        Product current = new Product();
        current.setId(913L);
        current.setProductCode(KEY);
        current.setName("Current");
        current.setProductType(Product.ProductType.SERVICE);
        current.setValidFrom(FROM);
        current.setValidTo(OPEN);
        when(port.findBusinessKeyById(913L)).thenReturn(Optional.of(KEY));
        when(port.findByIdForUpdate(913L)).thenReturn(Optional.of(current));
        ProductService service = new ProductService(port, mock(MasterDataBusinessKeyLockPort.class));

        assertThatThrownBy(() -> service.updateProduct(913L, new ProductCommand(
                KEY, "X".repeat(201), null, null, null, null, UPDATE, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(current.getValidTo()).isEqualTo(OPEN);
        verify(port, never()).save(any());
    }

    @Test
    void invalidBusinessPartnerUpdateDoesNotCloseCurrentVersion() {
        BusinessPartnerPersistencePort port = mock(BusinessPartnerPersistencePort.class);
        BusinessPartner current = BusinessPartner.create(KEY, "Current", null, null, null, null,
                null, true, null, null, FROM, OPEN);
        when(port.findBusinessKeyById(913L)).thenReturn(Optional.of(KEY));
        when(port.findByIdForUpdate(913L)).thenReturn(Optional.of(current));
        BusinessPartnerService service = new BusinessPartnerService(port, mock(MasterDataBusinessKeyLockPort.class));

        assertThatThrownBy(() -> service.updateBusinessPartner(913L, new BusinessPartnerCommand(
                KEY, "X".repeat(101), null, null, null, null, null, null, null, null, UPDATE, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(current.getValidTo()).isEqualTo(OPEN);
        verify(port, never()).save(any());
    }
}
