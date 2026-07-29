package com.ho.account.closing.domain;

/**
 * State of one business date during end-of-day and beginning-of-day processing.
 *
 * <p>A closed business date is immutable. Beginning-of-day processing therefore
 * starts on a new aggregate rather than reopening the closed row.</p>
 */
public enum EodState {
    OPEN("영업 중", true),
    PRE_CLOSING("마감 준비", false),
    CLOSING_IN_PROGRESS("EOD 마감 진행 중", false),
    CLOSED("마감 완료", false),
    BOD_IN_PROGRESS("영업 개시 준비 중", false);

    private final String description;
    private final boolean transactionAllowed;

    EodState(String description, boolean transactionAllowed) {
        this.description = description;
        this.transactionAllowed = transactionAllowed;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTransactionAllowed() {
        return transactionAllowed;
    }

    public boolean canTransitionTo(EodState nextState) {
        if (nextState == null) {
            return false;
        }
        return switch (this) {
            case OPEN -> nextState == PRE_CLOSING;
            case PRE_CLOSING -> nextState == OPEN || nextState == CLOSING_IN_PROGRESS;
            case CLOSING_IN_PROGRESS -> nextState == CLOSED;
            case BOD_IN_PROGRESS -> nextState == OPEN;
            case CLOSED -> false;
        };
    }
}
