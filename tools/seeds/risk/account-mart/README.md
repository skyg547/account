# Account Mart synthetic package

DB: `account-mart`. Run `integratedPositionEtlJob baseDate=2090-01-15` with `mart.batch.cdm-event.enabled=false` to validate the local database pipeline without publishing a downstream event. One valid corporate loan source has ODS balance 1,000,000 and matching GL 1,000,000. It produces Stage 1 position/exposure snapshots and a zero-difference GL–SL audit.

Schema source: Account Mart V1–V6 (the local baseline documents the same columns); fixture source: `IntegratedPositionEtlJobTest`. The account master is inserted before its product FK. ODS interest rate uses percentage units, matching that test; exposure conversion belongs to core. The ECL database has its own contract fixture, so this seed does not imply cross-database propagation was tested.

All strings and predicates reserve GH690. Repeated seed injection never resets outputs; repeated job output verification requires the same business values and permits additional audit history. Rollback deletes snapshots and children before source masters. Existing date-wide job replacement means the runner must reserve this entire base date.
