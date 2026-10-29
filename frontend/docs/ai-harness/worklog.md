# Frontend worklog — Issue #811

## 2026-10-29 — authenticated menu-permission frontend handoff

- Draft PR [#943](https://github.com/skyg547/account/pull/943), `Refs #811`; the frontend scope is reviewable but #811 end-to-end acceptance remains open.
- Issue #811 frontend scope was expanded by its latest user comment to `frontend/**`; the request forbids edits to other modules and shared harness docs. The original parent Issue's runtime/Gateway decisions remain a separate integration gate. #838 already owns that work and has an active worktree, so this branch changes no Gateway/Auth/shared-kernel/config-repo files.
- `NavContext` now requests role grants through the same-origin HttpOnly BFF path, exposes only validated `READ` grants, invalidates snapshots on role/path changes, clears on `/login`, and refetches after navigation back into the app. `SYSTEM_ADMIN` remains a UI-only display bypass without authenticated actor binding.
- The menu page no longer displays Mock roles/grants or simulates writes. It requires complete valid role/grant reads, sends POST/DELETE through the BFF, and refetches authoritative state before saying a change is reflected. A 202 approval request or unchanged state is reported as pending/unverified.
- Backend-compatible lowercase/hyphen role codes are accepted, while values that cannot safely form one BFF path segment are rejected. Failure, empty results, stale reads, accepted-but-unconfirmed writes, and duplicate in-flight writes have focused regression coverage.
- Verification on available Node 22.23.2 / npm 10.9.8: `node --test tests/menu-authorizations.test.mjs` 21/21 PASS; `npm run test:auth-bff` 16/16 PASS; `node --test tests/admin-role-request.test.mjs` 8/8 PASS; `npx --no-install tsc --noEmit --incremental false` PASS; `npm run lint -- --quiet` PASS; `git diff --check -- frontend` and exact changed-file conflict-marker check PASS. Node 20 parity and browser/runtime/DB smoke were not run.
- Requested `./gradlew :frontend:test --offline --console=plain --no-daemon` failed before tests: Gradle root has no `:frontend` project. The Node commands above are the applicable frontend checks; this task did not change root build configuration.
- Independent read-only review corrected two P2 issues before approval: valid lowercase/hyphen role codes and stale grants across login/navigation. Final review found no remaining P0–P3 findings and Q1–Q4 PASS on the four changed code/test/functional-doc files.
- Rollback: revert the Issue #811 frontend commit(s). No DB schema, runtime service, operational data or other module was changed.
