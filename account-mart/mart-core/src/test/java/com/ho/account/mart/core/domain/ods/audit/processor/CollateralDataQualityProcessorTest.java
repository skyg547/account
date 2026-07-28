package com.ho.account.mart.core.domain.ods.audit.processor;

import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CollateralDataQualityProcessorTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

    private final CollateralDataQualityProcessor processor = new CollateralDataQualityProcessor();

    @Test
    void cashDepositWithPositiveAppraisalPassesWithoutApartmentDetail() {
        OdsDqAudit audit = processor.inspect(cashDeposit(), Optional.empty(), BASE_DATE);

        assertNull(audit);
    }

    @Test
    void realEstateMissingApartmentDetailReturnsMissingReferenceAudit() {
        OdsDqAudit audit = processor.inspect(realEstate(), Optional.empty(), BASE_DATE);

        assertEquals("ods_apart_coll_detail", audit.getTableName());
        assertEquals("MISSING_REFERENCE", audit.getAuditType());
        assertEquals("COLL-100", audit.getAccountNo());
    }

    @Test
    void realEstateInvalidApartmentInputsReturnsLgdInputAudit() {
        OdsApartCollDetail invalidDetail = OdsApartCollDetail.builder()
                .collateralId("COLL-100")
                .districtCode(" ")
                .kbMarketPrice(BigDecimal.ZERO)
                .exclusiveArea(new BigDecimal("84.50"))
                .build();

        OdsDqAudit audit = processor.inspect(realEstate(), Optional.of(invalidDetail), BASE_DATE);

        assertEquals("LGD_INPUT_ERROR", audit.getAuditType());
        assertEquals("ods_apart_coll_detail", audit.getTableName());
    }

    @Test
    void realEstateWithValidApartmentInputsPasses() {
        OdsApartCollDetail validDetail = OdsApartCollDetail.builder()
                .collateralId("COLL-100")
                .districtCode("11680")
                .kbMarketPrice(new BigDecimal("120000000.0000"))
                .houseType("APARTMENT")
                .exclusiveArea(new BigDecimal("84.50"))
                .floorNo(12)
                .isSpeculativeArea(false)
                .build();

        OdsDqAudit audit = processor.inspect(realEstate(), Optional.of(validDetail), BASE_DATE);

        assertNull(audit);
    }

    @Test
    void invalidMasterAppraisalIsReportedBeforeApartmentDetailCheck() {
        OdsCollateralMst collateral = OdsCollateralMst.builder()
                .collateralNo("COLL-100")
                .collateralType("REAL_ESTATE")
                .appraisedValue(BigDecimal.ZERO)
                .build();

        OdsDqAudit audit = processor.inspect(collateral, Optional.empty(), BASE_DATE);

        assertEquals("ods_coll_mst", audit.getTableName());
        assertEquals("LOGIC_ERROR", audit.getAuditType());
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
}
