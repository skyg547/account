package com.ho.account.mart.core.application.service.ods;

import com.ho.account.mart.core.application.port.out.OdsApartCollDetailRepository;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.audit.processor.CollateralDataQualityProcessor;
import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollateralDataQualityInspectionServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

    private final OdsApartCollDetailRepository repository = mock(OdsApartCollDetailRepository.class);
    private final CollateralDataQualityInspectionService service = new CollateralDataQualityInspectionService(
            repository,
            new CollateralDataQualityProcessor());

    @Test
    void realEstateCollateralLoadsApartmentDetailThroughPort() {
        when(repository.findByCollateralId("COLL-100")).thenReturn(Optional.of(validDetail()));

        OdsDqAudit audit = service.inspect(realEstate(), BASE_DATE);

        assertNull(audit);
        verify(repository).findByCollateralId("COLL-100");
    }

    @Test
    void cashDepositDoesNotLoadApartmentDetail() {
        OdsDqAudit audit = service.inspect(cashDeposit(), BASE_DATE);

        assertNull(audit);
        verify(repository, never()).findByCollateralId("COLL-CASH");
    }

    private OdsCollateralMst realEstate() {
        return OdsCollateralMst.builder()
                .collateralNo("COLL-100")
                .collateralType("REAL_ESTATE")
                .appraisedValue(new BigDecimal("100000000.0000"))
                .build();
    }

    private OdsCollateralMst cashDeposit() {
        return OdsCollateralMst.builder()
                .collateralNo("COLL-CASH")
                .collateralType("CASH_DEPOSIT")
                .appraisedValue(new BigDecimal("50000000.0000"))
                .build();
    }

    private OdsApartCollDetail validDetail() {
        return OdsApartCollDetail.builder()
                .collateralId("COLL-100")
                .districtCode("11680")
                .kbMarketPrice(new BigDecimal("120000000.0000"))
                .houseType("APARTMENT")
                .exclusiveArea(new BigDecimal("84.50"))
                .floorNo(12)
                .isSpeculativeArea(false)
                .build();
    }
}
