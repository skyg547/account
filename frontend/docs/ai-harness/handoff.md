# Frontend handoff — Issue #811

## Review scope and result

The frontend portion is ready for Draft review. The change removes misleading local menu-permission state and uses the authenticated same-origin BFF. The full #811 feature is not verified until #838 supplies the actual Auth runtime/Gateway route and a local synthetic DB end-to-end read/write smoke passes.

Draft PR: [#943](https://github.com/skyg547/account/pull/943), `Refs #811`; branch `agent/811-menu-permission-frontend`, worktree `/tmp/account-811-menu-permission-frontend`.

| Q | Result | Evidence | Next gate |
| --- | --- | --- | --- |
| Q1 readable code and responsibilities | PASS | `src/context/NavContext.tsx` snapshot lifecycle; `src/app/system/menus/page.tsx` role/grant validation and authoritative reload; focused Node 22/22 | Human review |
| Q2 flow, exceptions, rationale | PASS | `docs/master-system-spec.md` sections 5–6 and loading/failure/approval tests | Actual Gateway/Auth/DB smoke |
| Q3 feature and beginner documentation | PASS | `docs/master-system-spec.md` request path, failure behavior, local commands and remaining contract; `README.md` BFF path and #838 dependency | Node 20/browser run |
| Q4 adjacent intent comments | PASS | Snapshot invalidation, complete-table validation, wildcard cell and approval/refetch comments in changed source | Human review |

## Verification and limitations

`node --test tests/menu-authorizations.test.mjs` 22/22, `npm run test:auth-bff` 16/16, `node --test tests/admin-role-request.test.mjs` 8/8, TypeScript, full frontend lint, diff/marker and allowlist checks passed. `./gradlew :frontend:test` cannot resolve because `frontend` is not a Gradle project. Tests used installed Node 22; the repository pins Node 20. No actual Gateway/Auth/DB/browser verification was claimed.

Follow-up independent review found and then cleared one P2: a missing target role after DELETE could have been shown as a confirmed revoke. The target role and its grant response are now required before confirmation. The new regression failed before the fix and passed afterward for empty and unrelated-only role lists. Final combined Node suite is 46/46 PASS (menu 22, BFF 16, admin 8); TypeScript, lint and scoped diff check PASS. Independent Astra high rereview found no remaining P0–P3, Q1–Q4 PASS. Draft CI Merge Guard reports `DRAFT_NOT_MERGE_READY, TRUST_POLICY_UNCONFIGURED`; the repository trust policy and final Ready gate remain outside this frontend change.

UI role selection and the `SYSTEM_ADMIN` display bypass do not prove the logged-in actor's role; backend actor binding, read/write policy, audit and approvals must be enforced by the #838 runtime. Idle same-route session changes are observed only on the next navigation. A write request may already have queued an approval when its response is lost, so the UI reports uncertainty and does not retry automatically.

Rollback is a reviewed revert of this Issue's frontend commit(s). Next owners are the #838 runtime/Gateway Integrator for actual contract proof and a human reviewer for Draft PR/Ready decision. Shared `docs/ai-harness/**` and `docs/history/**` are outside the user allowlist and were not changed.
