-- Receipts and source rows commit together. Remote identity uses the immutable incoming business key.
-- Missing receipts after rollback are safe; never manufacture keys from generated database IDs.
CREATE TABLE payable_source_creation_locks (
    stripe_id INTEGER PRIMARY KEY,
    CONSTRAINT ck_payable_creation_stripe CHECK (stripe_id >= 0 AND stripe_id < 64)
);
INSERT INTO payable_source_creation_locks(stripe_id) VALUES
(0), (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25), (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37), (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49), (50), (51), (52), (53), (54), (55), (56), (57), (58), (59), (60), (61), (62), (63);
CREATE TABLE payable_source_creations (
    source_type VARCHAR(50) NOT NULL,
    business_key VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    source_id BIGINT NOT NULL,
    PRIMARY KEY (source_type, business_key),
    CONSTRAINT ck_payable_creation_digest CHECK (LENGTH(request_fingerprint) = 64),
    CONSTRAINT ck_payable_creation_source CHECK (source_id > 0)
);
