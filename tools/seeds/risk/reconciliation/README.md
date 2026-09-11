# Reconciliation synthetic package

DB: `reconciliation`. Run `reconciliationDailyJob reconciliationDate=2090-01-15 runBy=GH690 deepMode=false`. Unit 6900001 compares one external SOURCE row of KRW 1,000,000 to the accounting package's posted `GH690-CASH` debit of 1,000,000 on the same date. SOURCE product equals the journal account so the relaxed date/account matching key agrees. Expected business status is `SUCCESS`, one match, zero differences; Spring Batch metadata is checked independently by the runner.

Schema source: `reconciliation/core/src/main/resources/db/reconciliation-migration/V1__reconciliation_baseline.sql`. Shallow mode uses the real journal query adapter; it does not claim four-stage/deep reconciliation coverage. Existing active units are processed by this job too, so the runner must check the authorized synthetic scope before launching.

All records use reserved ID 6900001 and GH690 strings. Completed runs are reused by core on rerun. Rollback deletes owned run children first, never Spring Batch metadata or a remote journal. Roll back this package before journal-ledger to preserve reference order.
