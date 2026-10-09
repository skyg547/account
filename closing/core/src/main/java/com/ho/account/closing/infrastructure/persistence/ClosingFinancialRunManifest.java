package com.ho.account.closing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "closing_financial_run_manifests", uniqueConstraints =
        @UniqueConstraint(name = "uq_closing_run_manifest", columnNames = {"scope", "batch_id"}))
public class ClosingFinancialRunManifest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String scope;
    @Column(name = "batch_id", nullable = false)
    private Long batchId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String commandsJson;

    protected ClosingFinancialRunManifest() { }
    public ClosingFinancialRunManifest(String scope, Long batchId, String commandsJson) {
        this.scope = scope;
        this.batchId = batchId;
        this.commandsJson = commandsJson;
    }
    public String getCommandsJson() { return commandsJson; }
}
