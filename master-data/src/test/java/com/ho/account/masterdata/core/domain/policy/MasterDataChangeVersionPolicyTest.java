package com.ho.account.masterdata.core.domain.policy;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDataChangeVersionPolicyTest {

    @Test
    void acceptsCreateAsFirstVersion() {
        assertThatCode(() -> MasterDataChangeVersionPolicy.verify(ChangeType.CREATE, 1, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsUpdateAsNextScd2Version() {
        assertThatCode(() -> MasterDataChangeVersionPolicy.verify(ChangeType.UPDATE, 4, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsDeactivateAgainstCurrentVersionWithoutCreatingAnotherRow() {
        assertThatCode(() -> MasterDataChangeVersionPolicy.verify(ChangeType.DEACTIVATE, 3, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsCreateWhenHistoryAlreadyExists() {
        assertThatThrownBy(() -> MasterDataChangeVersionPolicy.verify(ChangeType.CREATE, 1, 2))
                .isInstanceOf(MasterDataVersionConflictException.class)
                .hasMessageContaining("only when no SCD2 history exists");
    }

    @Test
    void rejectsUpdateWithoutExistingHistory() {
        assertThatThrownBy(() -> MasterDataChangeVersionPolicy.verify(ChangeType.UPDATE, 1, 0))
                .isInstanceOf(MasterDataVersionConflictException.class)
                .hasMessageContaining("requires an existing SCD2 history");
    }
}