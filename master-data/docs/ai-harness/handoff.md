# GH-753 handoff

**HOLD: module implementation verified; runner H2 integration blocker remains.**

- Issue: https://github.com/skyg547/account/issues/753
- Branch: `agent/753-scd2-business-key`
- Worktree: `/tmp/account-753-scd2-business-key`
- Base: `origin/main@b06e7de3a2e26c1e14a15d73486c02fa7216e8b8`
- Draft PR: https://github.com/skyg547/account/pull/820 (`Refs #753`); published implementation `2f4a3bbc`; no Ready/merge/Issue close
- Scope: `master-data/**` only. Shared harness/history documents are unchanged under the user's explicit restriction.

## Behavior and verification

All four supported SCD2 types serialize absent/existing business keys. Version and current interval checks follow transaction-owned locks and targeted JPA refresh. A distinct losing approved request remains APPROVED without appliedAt; a retry of the already-applied same ID returns409 without a second mutation; rollback restores master/request state. PostgreSQL forward migrations reject invalid/inclusive overlapping history, including inactive rows. Unrelated keys proceed independently.

Final forced `./gradlew :master-data:test :master-data:api:bootJar :master-data:batch:bootJar --offline --no-daemon --console=plain --max-workers=2 --rerun-tasks` with the isolated PostgreSQL fixture: exit0/3m12s/22tasks executed, **602 tests** (core454/API143/Batch5), failures/errors/skips0. The API count includes **75 real PostgreSQL tests** (63 concurrency+12 migration). Both executable JARs contain core V8/V9 migrations and the PostgreSQL driver. Runner copied V8/V9 byte-identically.

Independent `/root/review_753` inspected all40 fixed source/build/test hashes and exact XML; no remaining module code findings. Scope, whitespace, conflict markers and local document links passed. It did not rerun tests or give a GitHub human approval.

## Open integration blocker and next owner

Existing `MigrationExecutorH2Test.masterDataBaselineContainsEveryJpaOwnedTableAndConstraint` and
`masterDataPublishedHistoryUpgradesForwardFromV5ToV6` fail on V9 PostgreSQL syntax. The explicit
compatibility command ran2 tests/2 failures, exit1/33s. The generic H2 context case also needs the
same vendor boundary when full runner tests are executed. This is a concrete regression, not an
environment excuse; overall Q1/integration remains FAIL.

A one-file **test-only** correction is prepared at `/tmp/account-753-runner-h2-compatibility.patch`: copy the actual packaged V1–V8 SQL into an isolated H2 fixture and keep the production runner's latest PostgreSQL migration discovery unchanged. The independent reviewer found no static issue in the proposal. The user was asked to authorize this exact test file outside `master-data/**`; no answer is treated as authorization. Do not apply it without that scope exception.

Next owner: parent Integrator applies the bounded proposal if approved, runs full `./gradlew :migration-runner:test --offline --no-daemon --console=plain --max-workers=2`, obtains a fresh independent review, and updates this Draft. If scope remains absolute, a separately authorized runner owner must resolve the fixture compatibility before Ready/merge.

## Quality evidence

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | FAIL | Exact business-key locks; fresh mutation/request reads; precise409; module602 PASS incl PostgreSQL75 | Not applicable: production logic changed | Existing runner H2 fixtures fail2/2; narrow scope exception + full runner test required | `/root/review_753`: module code verified, integration HOLD |
| Q2 | PASS | `master-data/docs/process-flow.md:111`, `schema.md:113`; concurrency/rollback/migration regressions | Not applicable: flow changed | Production deployment/load untested | Reviewer checked flow and boundaries |
| Q3 | PASS | `beginner-guide.md:96`, `local-run.md:103`, `schema.md:149` | Not applicable: feature docs changed | Record runner scope decision before integration | Reviewer checked documented commands/behavior/limits |
| Q4 | PASS | Lock adapter:39; request adapter:44; V9:19 rationale comments | Not applicable: nontrivial logic changed | Re-review any further fix | Reviewer checked code/comment agreement |

## Rollback and operational limits

Review an Issue-scoped code revert and forward schema correction together. Never delete financial/audit history, disable constraints, or use Flyway repair as data remediation. Old code may fail immediate exclusions without the closing-row flush. V9 table locks/GiST build duration and btree_gist/public permissions require a reviewed deployment window; no production database, runtime grants, deployed service network or production-scale throughput was tested. H2 intentionally stops atV8 and cannot certify exclusion.

`requestedVersion` remains history-row count; termination does not increment it. Compatible earlier termination remains a valid serial operation. The existing500-request scheduled chunk remains one transaction; per-request restart/partitioning is future work. PostgreSQL tests skip without the explicit synthetic fixture environment; local75 passing is separate evidence from future CI.

## Authority separation

Parent Integrator owns Git/GitHub publication and these module-local records. Service/SQL/Test writers had disjoint ownership; the reviewer was read-only and returned defects to the owners. Internal review is procedural separation, not a separate human GitHub approval. User-authorized actions end at branch push and Draft PR. Ready, merge, Issue closure and branch/worktree deletion require separate authority.

## Changed-file inventory

- `master-data/README.md`
- `master-data/api/build.gradle`
- `master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataExceptionHandler.java`
- `master-data/api/src/main/resources/application-local.yaml`
- `master-data/api/src/test/java/com/ho/account/masterdata/MasterDataPostgresqlSchemaContextTest.java`
- `master-data/api/src/test/java/com/ho/account/masterdata/MasterDataScd2PostgresqlConcurrencyTest.java`
- `master-data/api/src/test/java/com/ho/account/masterdata/MasterDataScd2PostgresqlMigrationTest.java`
- `master-data/batch/src/main/resources/application-local.yaml`
- `master-data/batch/src/test/java/com/ho/account/masterdata/batch/MasterDataBatchPostgresqlSchemaContextTest.java`
- `master-data/build.gradle`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/port/out/AccountSubjectPersistencePort.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/port/out/BusinessPartnerPersistencePort.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/port/out/DepartmentPersistencePort.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/port/out/MasterDataBusinessKeyLockPort.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/port/out/ProductPersistencePort.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectMasterDataChangeApplier.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerMasterDataChangeApplier.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentMasterDataChangeApplier.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeApplierSupport.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductMasterDataChangeApplier.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/domain/policy/MasterDataValidityPolicy.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaAccountSubjectPersistenceAdapter.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaBusinessPartnerPersistenceAdapter.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaDepartmentPersistenceAdapter.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaMasterDataBusinessKeyLockAdapter.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaMasterDataChangeRequestPersistenceAdapter.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaProductPersistenceAdapter.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/MasterDataBusinessKeyLockEntity.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java`
- `master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/ProductRepository.java`
- `master-data/core/src/main/resources/db/migration/V8__master_data_business_key_lock_and_validity.sql`
- `master-data/core/src/main/resources/db/migration/V9__master_data_scd2_non_overlap.sql`
- `master-data/core/src/test/java/com/ho/account/masterdata/core/application/service/BusinessPartnerServiceTest.java`
- `master-data/core/src/test/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestPayloadValidationTest.java`
- `master-data/core/src/test/java/com/ho/account/masterdata/core/application/service/MasterDataFutureVersionOverlapTest.java`
- `master-data/core/src/test/java/com/ho/account/masterdata/core/infrastructure/persistence/BusinessPartnerHistoricalActiveIntegrationTest.java`
- `master-data/docs/README.md`
- `master-data/docs/ai-harness/agent-status.md`
- `master-data/docs/ai-harness/handoff.md`
- `master-data/docs/ai-harness/worklog.md`
- `master-data/docs/beginner-guide.md`
- `master-data/docs/local-run.md`
- `master-data/docs/process-flow.md`
- `master-data/docs/schema.md`
