# Conflict Log

Merge and rebase conflict resolutions are recorded below.

## Template

| Date | Branches | Files | Conflict Type | Resolution | Reviewer |
| --- | --- | --- | --- | --- | --- |
| 2026-07-30 | `agent/243-postgres-actuator-runtime` + `origin/main@5c810422` | `CODEX_WORKLOG.md`, `docs/WORKLOG.md`, `docs/ai-harness/{agent-status,handoff,worklog}.md` | append-only text conflicts during rebase | Preserved both the integrated Issue #45 implementation records and the independently reviewed Issue #243 runtime dependency records; production/build changes had no semantic conflict. | Codex Integrator |
| 2026-08-11 | `agent/348-multi-tool-issue-ownership` + `origin/main@aaad0c0d` | `CODEX_WORKLOG.md`, `docs/ai-harness/{agent-status,handoff,worklog}.md` | append-only text conflicts during rebase | Preserved the merged Issue #340 evidence and the Issue #348 ownership-protocol records. No production, test, build, database, or runtime files conflicted. | Codex Integrator |
| YYYY-MM-DD | `agent/a` + `agent/b` | `path/file` | text/semantic/build | summary | name |
| 2026-07-29 | `agent/closing-consistency-pass` + `origin/main@b7c8aaa` | `docs/WORKLOG.md` | text / whole-file line-ending conflict during stash reapply | Kept the latest upstream worklog including Auth/Internal Audit/Issue 26-27 records, then appended the local Closing and Foundation review-ready sections. The named backup stash was retained. | Codex Integrator |
| 2026-07-29 | `agent/20-closing-consistency` + `origin/main@f3d33ea` | `internal-audit/.../InternalAuditApplication.java`, `InternalAuditApplicationContextTest.java`; `shared-kernel/build.gradle`; semantic rename in `AuditAspect.java` | modify/delete, text, and semantic rename during transfer-stash apply | Preserved Issue #29's split `internal-audit:core/api/batch` structure and did not resurrect deleted standalone application/test files. Kept the upstream AuditAspect API/package and the local Jackson BOM rationale; retained both named stashes. | Codex Integrator |
| 2026-07-29 | `agent/20-closing-consistency` + `origin/main@ce35ce5` | 82-path Closing/Foundation transfer snapshot | duplicate integration / stale-base reconciliation | Verified that all Closing production, test, contract bridge, and module documentation content already exists in main through `31be6f1`. Kept latest main, did not reapply duplicate code or regress Issue #66 infrastructure, and retained a third comparison stash. | Codex Integrator |
| 2026-07-30 | `agent/41-business-partner-ddd` + `main@39dabd4e` | `docs/WORKLOG.md`, `docs/ai-harness/agent-status.md`, `master-data/README.md`, `master-data/docs/{README,beginner-guide,local-run}.md`; API source/test paths | text, semantic, and directory-rename conflicts during rebase | Preserved Issue #40's physical API/Core/Batch split and latest upstream records, then reapplied the Business Partner domain/JPA boundary guidance. Controller/DTO changes followed the API path and the DTO serialization test moved from Core to API. The mixed-encoding upstream worklog bytes were retained after the follow-up entry. | Descartes reviewer + Codex Integrator |
| 2026-07-30 | `agent/228-container-images` + `origin/main@54352362` | `docs/ai-harness/agent-status.md` | text conflict during rebase | Kept Issue #43's final integrated status and the independent Issue #228 image-packaging status; discarded only the superseded pre-integration Issue #43 row. A later rebase to `origin/main@36a1be4f` completed without conflict. | Codex Integrator |
| 2026-07-30 | `agent/229-dev-postgres` + `origin/main@36a1be4f` | `CODEX_WORKLOG.md`, `docs/WORKLOG.md`, `docs/ai-harness/{worklog,agent-status,handoff}.md` | text conflicts in append-only shared records during rebase | Preserved the latest Issue #43/#44 integration records and appended the Issue #229 development PostgreSQL contract records. No database, migration, or application configuration conflict occurred. | Codex Integrator |
| 2026-07-30 | `agent/227-runtime-parity-audit` + `origin/main@36a1be4f` | `CODEX_WORKLOG.md`, `docs/WORKLOG.md`, `docs/ai-harness/{worklog,agent-status,handoff}.md` | text conflicts in append-only shared records during rebase | Preserved all latest Issue #43/#44 integration records and appended the Issue #227 audit snapshot/handoff entries without changing production or test code. | Codex Integrator |

## 2026-08-11 - Issue #344 latest-main harness sync

- Branch/worktree: `agent/344-internal-audit-local-h2` / `C:\tmp\account-344-internal-audit-local-h2`.
- Sync: named stash of the reviewed `cf50e4fc` diff, fast-forward to `origin/main@b2d5c6ef`, then stash replay.
- Conflict: only `docs/ai-harness/agent-status.md`; upstream added #312/#314 integration rows while the Issue branch added #344 and corrected #342 rows.
- Resolution: retained all four rows in newest-first order. `handoff.md` and `worklog.md` auto-merged append-only; the two Internal Audit implementation paths remained byte-identical to the independently reviewed stash.
- Verification: no unmerged entries or conflict markers, `git diff --check`, allowlist and latest-main focused policy test passed. The named stash remains temporarily as a recovery reference until the PR head is verified.

## 2026-08-12 - Issue #90 PR #359 latest-main sync

- Ready/merge was attempted only after final PR-head review, but main advanced from `43f5b36c` to `191c5c28` through PR #360 before GitHub completed the merge; GitHub rejected PR #359 as conflicting and Issue #90 remained open.
- Merged `origin/main@191c5c28` into `agent/90-asset-lease-batch-local`. Discovery production/test paths and `docs/WORKLOG.md` came from upstream unchanged.
- The only manual conflict was `docs/ai-harness/agent-status.md`; resolution preserved both the local Issue #90 row and upstream Issue #304/PR #360 row. `handoff.md` and `worklog.md` auto-merged append-only.
- Re-run focused/static gates after resolution before updating the PR. The four Issue #90 implementation paths did not overlap upstream.
