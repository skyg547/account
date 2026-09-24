-- Fail-closed publication control for the in-place GL/SL balance rebuild.
-- The singleton is persistent so a crashed job deliberately leaves readers and writers blocked.
CREATE TABLE ledger_reaggregation_control (
    control_id SMALLINT PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    owner_job_instance_id BIGINT,
    range_start DATE,
    range_end DATE,
    epoch BIGINT NOT NULL,
    CONSTRAINT ck_ledger_reaggregation_singleton CHECK (control_id = 1),
    CONSTRAINT ck_ledger_reaggregation_status CHECK (status IN ('OPEN', 'REBUILDING')),
    CONSTRAINT ck_ledger_reaggregation_epoch CHECK (epoch >= 0),
    CONSTRAINT ck_ledger_reaggregation_state CHECK (
        (status = 'OPEN'
            AND owner_job_instance_id IS NULL AND range_start IS NULL AND range_end IS NULL)
        OR
        (status = 'REBUILDING'
            AND owner_job_instance_id IS NOT NULL
            AND range_start IS NOT NULL AND range_end IS NOT NULL
            AND range_start <= range_end)
    )
);

INSERT INTO ledger_reaggregation_control
    (control_id, status, owner_job_instance_id, range_start, range_end, epoch)
VALUES (1, 'OPEN', NULL, NULL, NULL, 0);
