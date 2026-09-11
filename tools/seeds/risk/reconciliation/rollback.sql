DELETE FROM reconciliation_stage_results WHERE reconciliation_run_id IN (SELECT id FROM reconciliation_runs WHERE reconciliation_unit_id=6900001 AND reconciliation_date=DATE '2090-01-15' AND run_by='GH690');
DELETE FROM reconciliation_differences WHERE reconciliation_run_id IN (SELECT id FROM reconciliation_runs WHERE reconciliation_unit_id=6900001 AND reconciliation_date=DATE '2090-01-15' AND run_by='GH690');
DELETE FROM reconciliation_runs WHERE reconciliation_unit_id=6900001 AND reconciliation_date=DATE '2090-01-15' AND run_by='GH690';
DELETE FROM recon_external_stage_record WHERE id=6900001 AND unit_id='6900001' AND audit_user='GH690';
DELETE FROM reconciliation_units WHERE id=6900001 AND name='GH690 source-to-journal';
