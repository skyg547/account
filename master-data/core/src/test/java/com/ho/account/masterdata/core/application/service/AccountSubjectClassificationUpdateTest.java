package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountType;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AccountSubjectClassificationUpdateTest {

    private static final LocalDate OLD_FROM = LocalDate.of(2025, 1, 1);
    private static final LocalDate NEW_FROM = LocalDate.of(2026, 8, 1);

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void nameOnlyDirectUpdatePreservesClassificationAndOriginalHistory(AccountType type) {
        AccountSubjectPersistencePort persistence = mock(AccountSubjectPersistencePort.class);
        AccountSubject original = original(type);
        List<AccountSubject> saved = new ArrayList<>();
        when(persistence.findByCode("AC-755")).thenReturn(Optional.of(original));
        when(persistence.findByCodeForUpdate("AC-755")).thenReturn(Optional.of(original));
        when(persistence.save(org.mockito.ArgumentMatchers.any(AccountSubject.class))).thenAnswer(invocation -> {
            AccountSubject account = invocation.getArgument(0);
            saved.add(account);
            return account;
        });

        AccountSubject successor = new AccountSubjectService(persistence, mock(MasterDataBusinessKeyLockPort.class)).updateAccountSubject("AC-755",
                new AccountSubjectCommand("AC-755", "Renamed", null, null, null, null,
                        null, null, NEW_FROM, null, null, null, false));

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0)).isSameAs(original);
        assertThat(original.getValidFrom()).isEqualTo(OLD_FROM);
        assertThat(original.getValidTo()).isEqualTo(NEW_FROM.minusDays(1));
        assertThat(original.getName()).isEqualTo("Original");
        assertThat(original.getAccountType()).isEqualTo(type);
        assertThat(original.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(saved.get(1)).isSameAs(successor);
        assertThat(successor.getValidFrom()).isEqualTo(NEW_FROM);
        assertThat(successor.getName()).isEqualTo("Renamed");
        assertThat(successor.getCategory()).isEqualTo(original.getCategory());
        assertThat(successor.getAccountType()).isEqualTo(type);
        assertThat(successor.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(successor.isUnsettled()).isTrue();
        assertThat(successor.isFixedAsset()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void approvedNameOnlyUpdatePreservesClassificationAndRequestAudit(AccountType type) {
        AccountSubjectPersistencePort accounts = mock(AccountSubjectPersistencePort.class);
        AccountSubject original = original(type);
        List<AccountSubject> saved = new ArrayList<>();
        when(accounts.findByCode("AC-755")).thenReturn(Optional.of(original));
        when(accounts.findByCodeForUpdate("AC-755")).thenReturn(Optional.of(original));
        when(accounts.save(org.mockito.ArgumentMatchers.any(AccountSubject.class))).thenAnswer(invocation -> {
            AccountSubject account = invocation.getArgument(0);
            saved.add(account);
            return account;
        });
        MasterDataChangeRequestPersistencePort requests = mock(MasterDataChangeRequestPersistencePort.class);
        MasterDataVersionQueryPort versions = mock(MasterDataVersionQueryPort.class);
        when(versions.countPersistedVersions(MasterDataType.ACCOUNT_SUBJECT, "AC-755")).thenReturn(1L);
        String payload = "{\"name\":\"Approved rename\"}";
        MasterDataChangeRequest request = MasterDataChangeRequest.reconstitute(755L,
                MasterDataType.ACCOUNT_SUBJECT, "AC-755", ChangeType.UPDATE, ChangeStatus.REQUESTED,
                1L, NEW_FROM, 2, "requester", null, LocalDateTime.of(2026, 7, 1, 9, 0), null,
                "name correction", payload, "source-755-" + type, null);
        when(requests.findById(755L)).thenReturn(Optional.of(request));
        when(requests.findByIdForUpdate(755L)).thenReturn(Optional.of(request));
        when(requests.save(org.mockito.ArgumentMatchers.any(MasterDataChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AccountSubjectService accountService = new AccountSubjectService(accounts, mock(MasterDataBusinessKeyLockPort.class));
        AccountSubjectMasterDataChangeApplier applier = new AccountSubjectMasterDataChangeApplier(
                accountService, new JacksonMasterDataChangePayloadDecoder(
                        new ObjectMapper().registerModule(new JavaTimeModule())), accounts);
        MasterDataChangeRequestService requestService = new MasterDataChangeRequestService(
                requests, versions, List.of(applier), mock(MasterDataBusinessKeyLockPort.class));

        requestService.approve(755L, "approver");
        assertThat(saved).isEmpty();
        MasterDataChangeRequest applied = requestService.applyApprovedChange(755L);

        assertThat(saved).hasSize(2);
        AccountSubject successor = saved.get(1);
        assertThat(original.getName()).isEqualTo("Original");
        assertThat(original.getValidTo()).isEqualTo(NEW_FROM.minusDays(1));
        assertThat(original.getAccountType()).isEqualTo(type);
        assertThat(original.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(successor.getName()).isEqualTo("Approved rename");
        assertThat(successor.getValidFrom()).isEqualTo(NEW_FROM);
        assertThat(successor.getAccountType()).isEqualTo(type);
        assertThat(successor.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(applied.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(applied.getRequestedBy()).isEqualTo("requester");
        assertThat(applied.getApprovedBy()).isEqualTo("approver");
        assertThat(applied.getReason()).isEqualTo("name correction");
        assertThat(applied.getPayloadJson()).isEqualTo(payload);
        assertThat(applied.getAppliedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void rejectsIncompatibleExplicitTypeWithoutClosingCurrentVersion(AccountType currentType) {
        AccountSubjectPersistencePort persistence = mock(AccountSubjectPersistencePort.class);
        AccountSubject original = original(currentType);
        when(persistence.findByCode("AC-755")).thenReturn(Optional.of(original));
        when(persistence.findByCodeForUpdate("AC-755")).thenReturn(Optional.of(original));
        AccountType incompatible = currentType == AccountType.NON_OPERATING_INCOME
                ? AccountType.NON_OPERATING_EXPENSES : AccountType.NON_OPERATING_INCOME;

        assertThatThrownBy(() -> new AccountSubjectService(persistence, mock(MasterDataBusinessKeyLockPort.class)).updateAccountSubject("AC-755",
                new AccountSubjectCommand("AC-755", "Invalid", null, null, null, null,
                        null, null, NEW_FROM, null, incompatible, null, false)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(original.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
        assertThat(original.getAccountType()).isEqualTo(currentType);
        verify(persistence, never()).save(org.mockito.ArgumentMatchers.any(AccountSubject.class));
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void rejectsMappingReplacementAndClearTogetherWithoutClosingCurrentVersion(AccountType type) {
        AccountSubjectPersistencePort persistence = mock(AccountSubjectPersistencePort.class);
        AccountSubject original = original(type);
        when(persistence.findByCode("AC-755")).thenReturn(Optional.of(original));
        when(persistence.findByCodeForUpdate("AC-755")).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> new AccountSubjectService(persistence, mock(MasterDataBusinessKeyLockPort.class)).updateAccountSubject("AC-755",
                new AccountSubjectCommand("AC-755", "Invalid", null, null, null, null,
                        null, null, NEW_FROM, null, null, "REG-NEW", true)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(original.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
        assertThat(original.getRegulatoryMappingCode()).isEqualTo("REG-755");
        verify(persistence, never()).save(org.mockito.ArgumentMatchers.any(AccountSubject.class));
    }

    private static AccountSubject original(AccountType type) {
        AccountSubject account = new AccountSubject();
        account.setCode("AC-755");
        account.setName("Original");
        account.setCategory(type == AccountType.NON_OPERATING_INCOME
                ? AccountSubject.AccountCategory.REVENUE : AccountSubject.AccountCategory.EXPENSES);
        account.setAccountType(type);
        account.setBalanceType(type == AccountType.NON_OPERATING_INCOME
                ? AccountSubject.BalanceType.CREDIT : AccountSubject.BalanceType.DEBIT);
        account.setRegulatoryMappingCode("REG-755");
        account.setUnsettled(true);
        account.setFixedAsset(true);
        account.setValidFrom(OLD_FROM);
        account.setValidTo(LocalDate.of(9999, 12, 31));
        return account;
    }
}
