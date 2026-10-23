package com.ho.account.expenditure.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

/** Database identity scoped to one payable, allowing the same client operation ID on another payable. */
public class PayableOffsetOperationId implements Serializable {
    private Long payableId;
    private String operationId;

    public PayableOffsetOperationId() {
    }

    public PayableOffsetOperationId(Long payableId, String operationId) {
        this.payableId = payableId;
        this.operationId = operationId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PayableOffsetOperationId that)) return false;
        return Objects.equals(payableId, that.payableId)
                && Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payableId, operationId);
    }
}
