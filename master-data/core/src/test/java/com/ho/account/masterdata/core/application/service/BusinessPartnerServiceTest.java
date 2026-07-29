package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 거래처 application service가 도메인 검증과 두 번의 SCD2 저장을 올바른 순서로 조정하는지 검증합니다.
 *
 * <p>도메인 단위 테스트만으로는 service가 현재 행을 먼저 닫아 버리거나 새 행만 저장하는 회귀를
 * 찾을 수 없습니다. 여기서는 저장 기술을 mock으로 격리하고, 새 버전 검증 뒤 과거·신규 버전을
 * 순서대로 포트에 전달하는 application 계층의 책임만 확인합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class BusinessPartnerServiceTest {

    @Mock
    private BusinessPartnerPersistencePort persistencePort;

    private BusinessPartnerService service;

    @BeforeEach
    void setUp() {
        service = new BusinessPartnerService(persistencePort);
    }

    @Test
    void updateValidatesThenSavesClosedHistoryBeforeNewVersion() {
        LocalDate today = LocalDate.now();
        BusinessPartner current = currentPartner(today);
        BusinessPartnerCommand command = command(
                "BP-SERVICE",
                "새봄상사 신사명",
                today.plusDays(1),
                BusinessPartner.OPEN_ENDED_VALID_TO);
        when(persistencePort.findById(10L)).thenReturn(Optional.of(current));
        when(persistencePort.save(any(BusinessPartner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessPartner next = service.updateBusinessPartner(10L, command);

        InOrder order = inOrder(persistencePort);
        order.verify(persistencePort).findById(10L);
        order.verify(persistencePort).save(current);
        order.verify(persistencePort).save(next);
        assertThat(current.getValidTo()).isEqualTo(today);
        assertThat(current.getUseYn()).isTrue();
        assertThat(next.getId()).isNull();
        assertThat(next.getBusinessPartnerCode()).isEqualTo("BP-SERVICE");
        assertThat(next.getBusinessPartnerName()).isEqualTo("새봄상사 신사명");
        assertThat(next.getValidFrom()).isEqualTo(today.plusDays(1));
        assertThat(next.getAccounts()).singleElement()
                .satisfies(account -> {
                    assertThat(account.getId()).isNull();
                    assertThat(account.getAccountNumber()).isEqualTo("110-123-456789");
                    assertThat(account.getBusinessPartner()).isSameAs(next);
                });
    }

    @Test
    void updateDoesNotSaveAnythingWhenTheNewVersionIsInvalid() {
        LocalDate today = LocalDate.now();
        BusinessPartner current = currentPartner(today);
        BusinessPartnerCommand command = command(
                "CHANGED-CODE",
                "잘못된 변경",
                today.plusDays(1),
                BusinessPartner.OPEN_ENDED_VALID_TO);
        when(persistencePort.findById(10L)).thenReturn(Optional.of(current));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.updateBusinessPartner(10L, command))
                .withMessage("거래처 코드는 SCD2 버전 수정 시 변경할 수 없습니다.");

        // 새 버전 검증이 끝나기 전에는 과거 행을 닫거나 포트에 저장하지 않습니다.
        assertThat(current.getValidTo()).isEqualTo(BusinessPartner.OPEN_ENDED_VALID_TO);
        verify(persistencePort, never()).save(any(BusinessPartner.class));
    }

    private BusinessPartner currentPartner(LocalDate today) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 9, 30);
        BusinessPartnerAccount account = BusinessPartnerAccount.reconstitute(
                77L,
                "한국은행",
                "110-123-456789",
                "새봄상사",
                "BOKRKRSE",
                true,
                timestamp,
                timestamp);
        return BusinessPartner.reconstitute(
                10L,
                "BP-SERVICE",
                "새봄상사",
                "123-45-67890",
                "김새봄",
                "도매업",
                "원자재",
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                today.minusDays(30),
                BusinessPartner.OPEN_ENDED_VALID_TO,
                timestamp,
                timestamp,
                "tester",
                List.of(account));
    }

    private BusinessPartnerCommand command(
            String code,
            String name,
            LocalDate validFrom,
            LocalDate validTo) {
        return new BusinessPartnerCommand(
                code,
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                validFrom,
                validTo);
    }
}
