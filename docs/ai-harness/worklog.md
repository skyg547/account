# AI Harness Worklog

## 2026-06-25 - Harness Upgrade Bootstrap

- Owner: Codex acting as AI Harness Architect and Integrator Agent.
- Branch: `ai-harness-upgrade-20260625`.
- Scope:
  - Preserved existing root harness files.
  - Created `docs/ai-harness/` operating documentation.
  - Added backup copies under `_backup/2026-06-25/`.
  - Added worktree ignore entries.
- Verification:
  - Required `docs/ai-harness` file existence check passed.
  - Conflict marker check passed.
  - Trailing whitespace check passed.
  - `git diff --check` on modified tracked guidance files passed with CRLF conversion warnings only.
- Risks:
  - Branch prefixes with slash failed in local Git ref layout, so this task uses `ai-harness-upgrade-20260625` without slash.
- Rollback:
  - Restore root guidance files from `docs/ai-harness/_backup/2026-06-25/`.
  - Remove or revert `docs/ai-harness/` additions and `.gitignore` worktree entries if the harness upgrade is rejected.

## 2026-06-26 - Beginner AI Agent Git Guide

- Owner: Codex acting as AI Harness Architect and Integrator Agent.
- Branch: `ai-harness-upgrade-20260625`.
- Scope:
  - Added `90-beginner-ai-agent-git-guide.md`.
  - Linked the guide from `Agents.md` and `00-overview.md`.
  - Covered Git branch, worktree, Draft PR/MR, multi-agent roles, prompt templates, verification, and safety rules.
- Verification:
  - Required file existence check passed.
  - Conflict marker check passed.
  - Trailing whitespace check passed.
  - `git diff --check` passed with CRLF conversion warnings only.
- Integration intent:
  - Push current branch.
  - Merge into `main` because the user explicitly requested mainstream merge.
  - Push `main` and verify clean sync.

## 2026-06-26 - Mainstream Merge

- Owner: Codex acting as Integrator Agent.
- Source branch: `ai-harness-upgrade-20260625`.
- Target branch: `main`.
- Result:
  - Source branch pushed to origin.
  - `main` updated from `origin/main`.
  - Source branch merged into `main` with `--no-ff`.
  - No merge conflicts occurred.
- Final sync:
  - `main` pushed to origin.
  - Clean synchronization check expected after this log commit is pushed.

## 2026-06-30 - Asset Lease Split

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Split `asset-lease` into `core`, `api`, and `batch` Gradle subprojects.
  - Kept `:asset-lease` as a compatibility wrapper for `:asset-lease:core`.
  - Added separate API and Batch Spring Boot entry points.
  - Updated run configs, Dockerfile, local docs, and dependent module reference.
- Verification:
  - `.\gradlew projects --console=plain` passed.
  - `.\gradlew :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :expenditure-resolution:core:compileJava --console=plain --max-workers=1` passed.
  - `.\gradlew :asset-lease:test :asset-lease:compileJava --console=plain --max-workers=1` passed.
- Risks:
  - Long-running bootRun smoke and real `assetDepreciationJob` execution are not run yet.
## 2026-07-02 - Full Local Build/API/BATCH Verification

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Verified current Gradle project graph, full build, API bootRun smoke, Batch context smoke, and representative Spring Batch Jobs.
  - Fixed Batch bootstrap behavior for asset-lease, account-mart, and ecl so Batch apps run non-web in local CLI mode.
  - Fixed asset-lease batch paging reader repository signature.
  - Updated local development and module run documents with H2/PostgreSQL separation and verified local flags.
- Verification:
  - `.\gradlew projects --console=plain` passed.
  - `.\gradlew compileJava --console=plain --max-workers=1` passed.
  - `.\gradlew build --console=plain --max-workers=1` passed.
  - API bootRun smoke passed for all current API/server modules.
  - Batch context smoke passed for all current Batch modules.
  - Representative Job smoke passed for all current Job-bearing Batch modules; journal-ledger batch remains context-only because no Job definition exists.
  - Java TODO search returned no matches.
- Risks:
  - PostgreSQL path is documented but not executed against a live local PostgreSQL instance in this pass.
  - Smoke data is empty/demo H2, so business-result correctness under production-like volume still needs seeded integration tests.
- Rollback:
  - Revert the batch bootstrap files and docs changed in this verification pass if the non-web CLI behavior is rejected.

## 2026-07-02 - Account Mart Core/Batch Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed direct Spring Batch interface usage from `mart-core` processors.
  - Added `mart-batch` processor adapters and a StepExecution-to-core parameter helper.
  - Removed a JPA-leaking unused application port skeleton.
  - Corrected ODS-GL reconciliation balance summary to aggregate by base date, subject/account, and currency.
  - Updated account-mart docs with beginner-friendly core/batch responsibility boundaries.
- Verification:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-batch:compileJava --console=plain --max-workers=1` passed.
  - `.\gradlew :account-mart:mart-batch:test --console=plain --max-workers=1` passed.
  - Core Spring Batch type search returned only explanatory comments, no imports or implemented interfaces.
  - Unused skeleton port search returned no matches.
- Risks:
  - Batch test shutdown still logs existing step-scope reader close warnings.
  - PostgreSQL high-volume reconciliation plan is not verified in this pass.
- Rollback:
  - Revert this account-mart commit if the boundary refactor is rejected.
