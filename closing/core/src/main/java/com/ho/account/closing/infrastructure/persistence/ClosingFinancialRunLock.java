package com.ho.account.closing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "closing_financial_run_locks", uniqueConstraints =
        @UniqueConstraint(name = "uq_closing_run_lock", columnNames = {"scope", "execution_key"}))
public class ClosingFinancialRunLock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String scope;
    @Column(name = "execution_key", nullable = false, length = 100)
    private String executionKey;

    protected ClosingFinancialRunLock() { }
    public ClosingFinancialRunLock(String scope, String executionKey) {
        this.scope = scope;
        this.executionKey = executionKey;
    }
}
