package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountSubjectServiceTest {

    @Mock
    private AccountSubjectPersistencePort persistencePort;

    @Test
    void activeListDelegatesValidityFilteringToPersistence() {
        LocalDate today = LocalDate.now();
        AccountSubject account = new AccountSubject();
        account.setCode("110000");
        account.setName("Cash");
        when(persistencePort.findAllActive(today)).thenReturn(List.of(account));

        AccountSubjectService service = new AccountSubjectService(persistencePort);

        assertThat(service.findAllActiveAccountSubjects()).containsExactly(account);
        verify(persistencePort).findAllActive(today);
    }
}
