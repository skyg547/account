package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class BatchMetadataVerifierTest {

    @Test
    void readsPresentMaximumLengthWithJdbcLongAccess() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(4)).thenReturn(2500L);
        when(resultSet.wasNull()).thenReturn(false);

        Long maximumLength = BatchMetadataVerifier.readNullableLong(resultSet, 4);

        assertThat(maximumLength).isEqualTo(2500L);
        InOrder accessOrder = inOrder(resultSet);
        accessOrder.verify(resultSet).getLong(4);
        accessOrder.verify(resultSet).wasNull();
        verify(resultSet, never()).getObject(4, Long.class);
    }

    @Test
    void preservesZeroWhenMaximumLengthIsNotSqlNull() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(4)).thenReturn(0L);
        when(resultSet.wasNull()).thenReturn(false);

        Long maximumLength = BatchMetadataVerifier.readNullableLong(resultSet, 4);

        assertThat(maximumLength).isZero();
        InOrder accessOrder = inOrder(resultSet);
        accessOrder.verify(resultSet).getLong(4);
        accessOrder.verify(resultSet).wasNull();
        verify(resultSet, never()).getObject(4, Long.class);
    }

    @Test
    void returnsNullWhenMaximumLengthIsSqlNull() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(4)).thenReturn(0L);
        when(resultSet.wasNull()).thenReturn(true);

        Long maximumLength = BatchMetadataVerifier.readNullableLong(resultSet, 4);

        assertThat(maximumLength).isNull();
        InOrder accessOrder = inOrder(resultSet);
        accessOrder.verify(resultSet).getLong(4);
        accessOrder.verify(resultSet).wasNull();
        verify(resultSet, never()).getObject(4, Long.class);
    }
}
