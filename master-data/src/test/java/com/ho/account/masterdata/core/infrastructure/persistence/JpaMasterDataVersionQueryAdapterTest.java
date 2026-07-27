package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaMasterDataVersionQueryAdapterTest {

    private final AccountSubjectRepository accountSubjectRepository = mock(AccountSubjectRepository.class);
    private final BusinessPartnerRepository businessPartnerRepository = mock(BusinessPartnerRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final JpaMasterDataVersionQueryAdapter adapter = new JpaMasterDataVersionQueryAdapter(
            accountSubjectRepository,
            businessPartnerRepository,
            departmentRepository,
            productRepository);

    @Test
    void delegatesVersionCountToRepositoryOwnedByTargetType() {
        when(departmentRepository.countByCode("D-001")).thenReturn(3L);

        assertThat(adapter.countPersistedVersions(MasterDataType.DEPARTMENT, "D-001")).isEqualTo(3L);
        verify(departmentRepository).countByCode("D-001");
    }

    @Test
    void rejectsTargetTypeWhoseTypedApplierAndVersionAdapterAreNotImplemented() {
        assertThatThrownBy(() -> adapter.countPersistedVersions(MasterDataType.CURRENCY, "KRW"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsupported targetType: CURRENCY");
    }
}