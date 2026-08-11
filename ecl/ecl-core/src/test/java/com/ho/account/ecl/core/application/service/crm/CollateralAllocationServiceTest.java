package com.ho.account.ecl.core.application.service.crm;

import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.port.out.CrCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrCustomerRepository;
import com.ho.account.ecl.core.domain.calculator.CollateralAllocationCalculator;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;

import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * [QA] CRM 담보배분 서비스의 대표 최적화 시나리오 테스트.
 *
 * <p>담보가 한정되어 있을 때 손실완화 효과가 큰 계좌에 먼저 담보가 배정되는지 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class CollateralAllocationServiceTest {

    @Mock private CrCustomerRepository customerRepository;
    @Mock private CrAccountRepository accountRepository;
    @Mock private CrCollateralRepository collateralRepository;
    @Mock private CrAccountCollateralRepository accountCollateralRepository;
    @Mock private CrBulkOperationPort bulkOperationPort;
    @Mock private ApartmentCollateralService apartmentCollateralService;

    @org.mockito.Spy
    private CollateralAllocationCalculator collateralAllocationCalculator = new CollateralAllocationCalculator();

    @InjectMocks
    private CollateralAllocationService allocationService;


    @Test
    @DisplayName("✅ LGD/ECL 완화 목적 담보 배분 검증")
    void shouldAllocateCollateralToHighLossPriorityAccountFirst() {
        // [Given]
        // 담보 1개 (가치 100)
        // 계좌 A(기업대출), 계좌 B(담보대출)에 대해 손실완화 효과가 큰 계좌를 우선 배정한다.
        
        Long customerId = 1L;
        CrAccount accA = CrAccount.builder().id(101L).accountNo("ACC-A").productCode("CORP").outstandingAmount(new BigDecimal("100")).build();
        CrAccount accB = CrAccount.builder().id(102L).accountNo("ACC-B").productCode("MORTGAGE").outstandingAmount(new BigDecimal("100")).build();
        
        CrCollateral collateral = CrCollateral.builder()
                .id(201L).collateralCode("COLL-1").collateralType("CASH")
                .appraisalAmount(new BigDecimal("100")).baseHaircut(BigDecimal.ZERO).build();

        when(accountRepository.findByCustomer_IdAndIsActiveTrue(customerId)).thenReturn(List.of(accA, accB));
        when(collateralRepository.findByCustomer_IdAndIsActiveTrue(customerId)).thenReturn(List.of(collateral));

        // [When]
        allocationService.allocateCollateralsForCustomer(customerId);

        // [Then]
        verify(bulkOperationPort, atLeastOnce()).deleteAllocationByAccountId(anyLong());

        ArgumentCaptor<CrAccountCollateral> captor = ArgumentCaptor.forClass(CrAccountCollateral.class);
        verify(accountCollateralRepository, atLeastOnce()).save(captor.capture());

        List<CrAccountCollateral> results = captor.getAllValues();
        // 최적화 결과: 계좌 A에 담보 100이 모두 배분되어야 함
        CrAccountCollateral resultA = results.stream()
                .filter(r -> r.getAccount().getAccountNo().equals("ACC-A"))
                .findFirst().orElseThrow();
        
        assertEquals(0, new BigDecimal("100").compareTo(resultA.getAllocationAmount()), "기업대출에 담보가 우선 배분되어야 함");
        
        long countB = results.stream().filter(r -> r.getAccount().getAccountNo().equals("ACC-B")).count();
        assertEquals(0, countB, "담보대출에는 남은 담보가 없어야 함");
    }
}
