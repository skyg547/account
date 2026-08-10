-- Local H2 (PostgreSQL mode) baseline for the executable Internal Audit API.
-- Scope is intentionally limited to the six JpaEntity types owned by
-- com.ho.account.internalaudit.core.infrastructure.persistence.entity.

CREATE TABLE rcm_process (
    process_id VARCHAR(255) NOT NULL PRIMARY KEY,
    process_name VARCHAR(255),
    description VARCHAR(255),
    owner_id VARCHAR(255)
);

CREATE TABLE rcm_risk (
    risk_id VARCHAR(255) NOT NULL PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    risk_description VARCHAR(255),
    impact_level VARCHAR(255),
    likelihood VARCHAR(255),
    CONSTRAINT fk_rcm_risk_process
        FOREIGN KEY (process_id) REFERENCES rcm_process(process_id)
);

CREATE INDEX idx_rcm_risk_process ON rcm_risk(process_id);

CREATE TABLE rcm_control_activity (
    control_id VARCHAR(255) NOT NULL PRIMARY KEY,
    risk_id VARCHAR(255) NOT NULL,
    control_description VARCHAR(255),
    control_type VARCHAR(255),
    execution_method VARCHAR(255),
    frequency VARCHAR(255),
    owner_id VARCHAR(255),
    CONSTRAINT fk_rcm_control_activity_risk
        FOREIGN KEY (risk_id) REFERENCES rcm_risk(risk_id)
);

CREATE INDEX idx_rcm_control_activity_risk ON rcm_control_activity(risk_id);

CREATE TABLE eval_design (
    evaluation_id VARCHAR(255) NOT NULL PRIMARY KEY,
    control_id VARCHAR(255) NOT NULL,
    evaluator_id VARCHAR(255),
    evaluation_date VARCHAR(255),
    result VARCHAR(255),
    remarks VARCHAR(255),
    CONSTRAINT fk_eval_design_control
        FOREIGN KEY (control_id) REFERENCES rcm_control_activity(control_id)
);

CREATE INDEX idx_eval_design_control ON eval_design(control_id);

CREATE TABLE eval_operating (
    evaluation_id VARCHAR(255) NOT NULL PRIMARY KEY,
    control_id VARCHAR(255) NOT NULL,
    evaluator_id VARCHAR(255),
    evaluation_date VARCHAR(255),
    sample_size INTEGER,
    exception_count INTEGER,
    result VARCHAR(255),
    remarks VARCHAR(255),
    CONSTRAINT fk_eval_operating_control
        FOREIGN KEY (control_id) REFERENCES rcm_control_activity(control_id)
);

CREATE INDEX idx_eval_operating_control ON eval_operating(control_id);

CREATE TABLE operating_evaluation_jpa_entity_evidence_file_paths (
    operating_evaluation_jpa_entity_evaluation_id VARCHAR(255) NOT NULL,
    evidence_file_paths VARCHAR(255),
    CONSTRAINT fk_eval_operating_evidence_evaluation
        FOREIGN KEY (operating_evaluation_jpa_entity_evaluation_id)
        REFERENCES eval_operating(evaluation_id)
);

CREATE INDEX idx_eval_operating_evidence_evaluation
    ON operating_evaluation_jpa_entity_evidence_file_paths(
        operating_evaluation_jpa_entity_evaluation_id);

CREATE TABLE eval_deficiency (
    deficiency_id VARCHAR(255) NOT NULL PRIMARY KEY,
    evaluation_id VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    remediation_plan VARCHAR(255),
    status VARCHAR(255)
);

-- evaluation_id may identify either a design or an operating evaluation, so a
-- single relational foreign key would reject one of the two valid parent types.
CREATE INDEX idx_eval_deficiency_evaluation ON eval_deficiency(evaluation_id);
