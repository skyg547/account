package com.risk.credit.core.application.service.crm;

import com.risk.credit.core.application.port.out.CrAccountCollateralRepository;
import com.risk.credit.core.application.port.out.CrAccountRepository;
import com.risk.credit.core.application.port.out.CrBulkOperationPort;
import com.risk.credit.core.application.port.out.CrCollateralRepository;
import com.risk.credit.core.application.port.out.CrCustomerRepository;
import com.risk.credit.core.domain.collateral.CrAccountCollateral;
import com.risk.credit.core.domain.collateral.CrCollateral;
import com.risk.credit.core.domain.exposure.CrAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * [QA] CRM 서비스의 우선순위 기반 담보배분 회귀 테스트.
 *
 * <p>계산 유즈케이스 패키지에 섞여 있던 시나리오를 CRM 패키지로 이동해
 * 테스트 의도가 패키지 구조와 일치하도록 정리했습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class CollateralAllocationServicePriorityTest {

    @Mock private CrCustomerRepository customerRepository;
    @Mock private CrAccountRepository accountRepository;
    @Mock private CrCollateralRepository collateralRepository;
    @Mock private CrAccountCollateralRepository accountCollateralRepository;
    @Mock private CrBulkOperationPort bulkOperationPort;
    @Mock private ApartmentCollateralService apartmentCollateralService;

    @InjectMocks
    private CollateralAllocationService allocationService;

    @Test
    @DisplayName("✅ RWA가 높은 계좌가 담보를 우선 배정받아야 한다")
    void allocateCollaterals_ShouldPrioritizeHighRiskWeightAccount() {
        Long customerId = 1001L;

        CrAccount mortgageAcc = CrAccount.builder()
                .id(1L)
                .accountNo("ACC-MORTGAGE")
                .productCode("MORTGAGE")
                .outstandingAmount(new BigDecimal("100000000"))
                .openDate(LocalDate.now().minusYears(2))
                .isActive(true)
                .build();

        CrAccount corpAcc = CrAccount.builder()
                .id(2L)
                .accountNo("ACC-CORP")
                .productCode("CORP_LOAN")
                .outstandingAmount(new BigDecimal("100000000"))
                .openDate(LocalDate.now().minusYears(1))
                .isActive(true)
                .build();

        CrCollateral collateral = CrCollateral.builder()
                .id(99L)
                .collateralCode("COLL-001")
                .appraisalAmount(new BigDecimal("100000000"))
                .baseHaircut(BigDecimal.ZERO)
                .isActive(true)
                .build();

        when(accountRepository.findByCustomer_IdAndIsActiveTrue(customerId)).thenReturn(List.of(mortgageAcc, corpAcc));
        when(collateralRepository.findByCustomer_IdAndIsActiveTrue(customerId)).thenReturn(List.of(collateral));

        allocationService.allocateCollateralsForCustomer(customerId);

        ArgumentCaptor<CrAccountCollateral> captor = ArgumentCaptor.forClass(CrAccountCollateral.class);
        verify(accountCollateralRepository, atLeastOnce()).save(captor.capture());

        List<CrAccountCollateral> savedMappings = captor.getAllValues();
        CrAccountCollateral corpMapping = savedMappings.stream()
                .filter(mapping -> "ACC-CORP".equals(mapping.getAccount().getAccountNo()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("100000000").compareTo(corpMapping.getAllocationAmount()));
    }
}
