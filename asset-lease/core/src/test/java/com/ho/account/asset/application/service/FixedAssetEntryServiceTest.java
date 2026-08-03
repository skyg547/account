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
}
