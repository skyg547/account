package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(ints = {10, 21, 30})
    void sequentialFutureUpdateRejectsOverlapBeforeAnyFurtherSave(int secondStart) {
        LocalDate today = LocalDate.now();
        List<BusinessPartner> rows = stubHistory(today);
        BusinessPartner scheduled = service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Scheduled", today.plusDays(21), BusinessPartner.OPEN_ENDED_VALID_TO));
        List<VersionState> before = states(rows);
        BusinessPartnerAccount originalAccount = rows.get(0).getAccounts().get(0);
        BusinessPartnerAccount scheduledAccount = scheduled.getAccounts().get(0);

        assertThatIllegalArgumentException().isThrownBy(() -> service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Rejected", today.plusDays(secondStart), BusinessPartner.OPEN_ENDED_VALID_TO)));

        assertThat(states(rows)).isEqualTo(before);
        assertThat(rows).hasSize(2);
        verify(persistencePort, times(2)).save(any(BusinessPartner.class));
        assertThat(rows.get(0).getAccounts()).containsExactly(originalAccount);
        assertThat(originalAccount.getId()).isEqualTo(77L);
        assertThat(originalAccount.getBusinessPartner()).isSameAs(rows.get(0));
        assertThat(scheduled.getAccounts()).containsExactly(scheduledAccount);
        assertThat(scheduledAccount.getBusinessPartner()).isSameAs(scheduled);
        assertContinuousSingleVersion(rows, today);
    }

    @Test
    void omittedEndIsRejectedWhenCurrentWindowAlreadyEndsBeforeScheduledHistory() {
        LocalDate today = LocalDate.now();
        List<BusinessPartner> rows = stubHistory(today);
        service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Scheduled", today.plusDays(21), BusinessPartner.OPEN_ENDED_VALID_TO));
        List<VersionState> before = states(rows);

        assertThatIllegalArgumentException().isThrownBy(() -> service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Rejected", today.plusDays(10), null)));

        assertThat(states(rows)).isEqualTo(before);
        verify(persistencePort, times(2)).save(any(BusinessPartner.class));
        assertContinuousSingleVersion(rows, today);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20})
    void containedSecondSplitPreservesScheduledHistoryAndAccountOwnership(int secondStart) {
        LocalDate today = LocalDate.now();
        List<BusinessPartner> rows = stubHistory(today);
        BusinessPartner scheduled = service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Scheduled", today.plusDays(21), BusinessPartner.OPEN_ENDED_VALID_TO));
        VersionState scheduledBefore = states(rows).get(1);

        BusinessPartner inserted = service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Inserted", today.plusDays(secondStart), today.plusDays(20)));

        assertThat(rows).hasSize(3);
        verify(persistencePort, times(4)).save(any(BusinessPartner.class));
        assertThat(rows.get(0).getValidTo()).isEqualTo(today.plusDays(secondStart - 1));
        assertThat(states(rows).get(1)).isEqualTo(scheduledBefore);
        assertThat(inserted.getValidFrom()).isEqualTo(today.plusDays(secondStart));
        assertThat(inserted.getValidTo()).isEqualTo(today.plusDays(20));
        assertThat(inserted.getAccounts().get(0)).isNotSameAs(scheduled.getAccounts().get(0));
        assertThat(inserted.getAccounts().get(0).getBusinessPartner()).isSameAs(inserted);
        assertContinuousSingleVersion(rows, today);
    }

    @Test
    void invalidSplitDatesDoNotMutateOrSaveCurrentVersion() {
        LocalDate today = LocalDate.now();
        BusinessPartner current = currentPartner(today);
        List<VersionState> before = states(List.of(current));
        when(persistencePort.findById(10L)).thenReturn(Optional.of(current));

        assertThatIllegalArgumentException().isThrownBy(() -> service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Rejected", current.getValidFrom(), BusinessPartner.OPEN_ENDED_VALID_TO)));
        assertThatIllegalArgumentException().isThrownBy(() -> service.updateBusinessPartner(10L,
                command("BP-SERVICE", "Rejected", today.plusDays(10), today.plusDays(9))));

        assertThat(states(List.of(current))).isEqualTo(before);
        verify(persistencePort, never()).save(any(BusinessPartner.class));
    }

    private List<BusinessPartner> stubHistory(LocalDate today) {
        BusinessPartner original = currentPartner(today);
        List<BusinessPartner> rows = new ArrayList<>(List.of(original));
        when(persistencePort.findById(10L)).thenAnswer(invocation -> {
            // The ID still refers to today's original after scheduling a future version.
            List<BusinessPartner> current = rows.stream().filter(row -> row.isActiveAt(LocalDate.now())).toList();
            assertThat(current).containsExactly(original);
            return rows.stream().filter(row -> Long.valueOf(10L).equals(row.getId())).findFirst();
        });
        when(persistencePort.save(any(BusinessPartner.class))).thenAnswer(invocation -> {
            BusinessPartner row = invocation.getArgument(0);
            if (rows.stream().noneMatch(existing -> existing == row)) {
                rows.add(row);
            }
            return row;
        });
        return rows;
    }

    private record VersionState(BusinessPartner row, LocalDate from, LocalDate to, LocalDateTime updatedAt) { }

    private List<VersionState> states(List<BusinessPartner> rows) {
        return rows.stream().map(row -> new VersionState(row, row.getValidFrom(), row.getValidTo(), row.getUpdatedAt()))
                .toList();
    }

    private void assertContinuousSingleVersion(List<BusinessPartner> rows, LocalDate today) {
        for (LocalDate date = today.minusDays(30); !date.isAfter(today.plusDays(60)); date = date.plusDays(1)) {
            LocalDate asOf = date;
            assertThat(rows.stream().filter(row -> row.isValid(asOf)).count())
                    .as("valid versions at %s", date).isEqualTo(1);
        }
        assertThat(rows.stream().filter(row -> row.isValid(BusinessPartner.OPEN_ENDED_VALID_TO)).count()).isEqualTo(1);
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
