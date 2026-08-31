package com.ho.account.asset.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.FixedAssetPersistencePort;
import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FixedAssetEntryServiceTest {

    @Mock
    private FixedAssetPersistencePort persistencePort;
    @Mock
    private AssetEventPort eventPort;
    @InjectMocks
    private FixedAssetEntryService service;

    @Test
    void registrationInitializesBookValueBeforePersistence() {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode("FA-001");
        asset.setAcquisitionDate(LocalDate.of(2026, 8, 1));
        asset.setAcquisitionCost(new BigDecimal("12000000.00"));
        asset.setResidualValue(new BigDecimal("1200000.00"));
        when(persistencePort.save(asset)).thenReturn(asset);

        FixedAsset registered = service.registerAsset(asset, "asset-admin");

        assertThat(registered.getCurrentBookValue()).isEqualByComparingTo("12000000.00");
        assertThat(registered.getAccumulatedDepreciation()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(registered.getStatus()).isEqualTo("ACTIVE");
        verify(persistencePort).saveHistory(any(AssetHistory.class));
        verify(eventPort).sendAssetEvent(any(), any());
    }

    @Test
    void processMonthlyDepreciationSkipsAlreadyDepreciatedAssets() {
        FixedAsset alreadyDepreciated = new FixedAsset();
        alreadyDepreciated.setAssetCode("FA-002");
        alreadyDepreciated.setStatus("ACTIVE");
        alreadyDepreciated.setAcquisitionCost(new BigDecimal("12000000.00"));
        alreadyDepreciated.setCurrentBookValue(new BigDecimal("11800000.00"));
        alreadyDepreciated.setAccumulatedDepreciation(new BigDecimal("200000.00"));
        alreadyDepreciated.setDepreciationAmountPerPeriod(new BigDecimal("200000.00"));
        alreadyDepreciated.setLastDepreciationDate(LocalDate.of(2026, 8, 31));

        when(persistencePort.findByStatus("ACTIVE")).thenReturn(java.util.List.of(alreadyDepreciated));

        service.processMonthlyDepreciation(LocalDate.of(2026, 8, 31), "asset-admin");

        org.mockito.Mockito.verify(persistencePort, org.mockito.Mockito.never()).save(any(FixedAsset.class));
        org.mockito.Mockito.verify(persistencePort, org.mockito.Mockito.never()).saveHistory(any(AssetHistory.class));
        org.mockito.Mockito.verify(eventPort, org.mockito.Mockito.never()).sendAssetEvent(any(), any());
    }
}
