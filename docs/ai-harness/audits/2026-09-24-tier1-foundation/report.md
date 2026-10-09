# Tier 1 Foundation architecture and code audit

**Audit date:** 2026-09-24 (Asia/Seoul). **Audited commit:** `a97d10ab6efc2570a88f83d630242cd748f8be57`. **Repository:** skyg547/account. **Scope:** `master-data:core`, `master-data:api`, `master-data:batch`, `auth:core`, `auth:api`; selected gateway/shared-kernel/Closing/contracts code only to trace foundation trust and persistence boundaries.

**Assessment:** the requested baseline is green, but the foundation is not ready for an unconditional security/financial-integrity sign-off. There are **23 actionable issue drafts: 9 P1 and 14 P2**. These include authorization defects, fail-open reference validation, temporal privilege leakage, SCD2 integrity races and deterministic metadata loss. No P0 was assigned: the review did not establish production internet exposure, an active compromise or a current financial loss.

P1 means resolve before relying on the affected capability in production. P2 means a material correctness, operability, scale or control gap with explicit acceptance work. F13/F14 are capability/policy gaps rather than asserted statutory violations; F23 is documentation drift. An existing TODO is not mitigation. A closed historical issue is not evidence that a related but different case is safe.

This is an inspection and issue-drafting deliverable. No production/test source, build configuration or migration was modified. No GitHub issues, comments, labels or PRs were created; no commits/pushes, deployment or operational database access occurred. Existing untracked user files in the primary checkout were preserved. The audit lives on `agent/tier1-foundation-audit-20260924` in `/tmp/account-tier1-audit-20260924`, based on the user's exact checkout rather than a newly fetched branch.

## Verification evidence

Requested tasks were actually executed in a fresh worktree using cached dependencies:

```bash
./gradlew :master-data:core:test :master-data:api:test :auth:core:test :auth:api:test \
  --offline --no-daemon --console=plain --max-workers=2
```

**BUILD SUCCESSFUL**, exit 0, 1m32s, 22 tasks executed. Offline mode avoided dependency installation/download. Gradle 8.7, Java toolchain 17; launching shell JVM reported OpenJDK 21.0.12.1. XML counts below are newly generated, not copied from historical worklogs.

| Module | Suites | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `master-data/core` | 11 | 444 | 0 | 0 | 0 |
| `master-data/api` | 11 | 68 | 0 | 0 | 0 |
| `auth/core` | 19 | 94 | 0 | 0 | 0 |
| `auth/api` | 6 | 21 | 0 | 0 | 0 |
| `master-data/batch` | 4 | 5 | 0 | 0 | 0 |
| `gateway` | 9 | 74 | 0 | 0 | 0 |

Requested baseline: **627 tests / 47 suites**, all passed. Additional master-data batch and gateway: **79 tests / 13 suites**, all passed. Overall inspected execution: **706 tests / 60 suites**, zero failures/errors/skips. Parameterized tests contribute heavily: 338 of master-data core's 444 tests are payload-validation cases, so the total must not be read as broad branch or security coverage. No JaCoCo/branch-coverage percentage was measured.

The supplemental command ran `:master-data:batch:test :gateway:test` plus an external classpath-export helper. Both test tasks succeeded, but the first combined invocation exited 1 because the audit helper used an invalid Gradle `project()` receiver. After fixing the external helper, the combined command exited 0 in 14s; the two successful test tasks were then UP-TO-DATE and the helper executed. This was an audit-tooling failure, not a product test failure, and the tests are counted only once.

Twelve synthetic observations additionally support the findings: eleven checks in `AuditProbes.java`, plus a full memory-mode application startup check. They are diagnostic reproductions, **not new passing regression tests**. The probe asserts the problematic behavior exists. The standalone probe initially needed an ambiguous Clock import fixed; final compilation and execution both exit 0. Source/probe logs are retained outside the checkout at `/tmp/account-tier1-audit-evidence/` and linked below. No dependency was added.

| Probe | Observed behavior | Limits |
|---|---|---|
| 1 | Department HTTP500 returns true | Mock HTTP, no network; actual adapter |
| 2 | Expired elevated role retained in JWT; version still valid with a basic role | Actual issuer/service; controlled clock; no live gateway request |
| 3 | Anonymous PAT admin list=200, revoke=204; default create owner=admin | Actual controller, mocked service, standalone MVC |
| 4 | Insufficient master approval role becomes HTTP500 | Actual controller + actual advice; no mutation called |
| 5 | Auth malformed JSON=500 and internal sentinel reflected | Actual advice/standalone MVC; synthetic text |
| 6 | Two sequential overlapping future products accepted | Actual service, date-aware fake persistence port |
| 7 | Core accepts blank product name/negative excessive scale | Actual service, fake persistence port |
| 8 | Account successor loses mapping and accountType | Actual service; persistence default confirmed in source |
| 9 | REJECTED/CRITICAL malformed-ID partner remains active | Domain observation; eligibility policy not assumed |
| 10 | Bulk partner lookup silently keeps first duplicate | Actual adapter, mocked output port |
| 11 | NUMERIC(19,4) stores 1.23456 as 1.2346 | Isolated H2 column, not a full entity/PostgreSQL test |
| 12 | Full AuthApplication memory-mode boot fails for missing PAT port | Local profile, web disabled, synthetic H2, generated credentials |

[Probe source](/tmp/account-tier1-audit-evidence/AuditProbes.java), [probe log](/tmp/account-tier1-audit-evidence/probes.log), [memory-mode probe](/tmp/account-tier1-audit-evidence/MemoryModeProbe.java), [memory-mode log](/tmp/account-tier1-audit-evidence/memory-probe.log), [machine-readable suite counts](test-summary.json).

## Architecture and control coverage

| Area | Controls verified | Remaining concerns |
|---|---|---|
| Domain/JPA separation | Pure domain models and separate persistence entities/mappers; business-partner aggregate copies child values into new SCD2 IDs | Many other masters remain mutable/anemic; defaults/classification are decided by JPA callbacks; validation differs by entrypoint |
| API/application boundary | Most controllers map DTOs into inbound use cases; core does not import API DTOs/JPA repositories | PAT controller directly depends on the application service; missing inbound port can be corrected with F01 without a broad refactor |
| Master-data approval | Requester != approver, guarded REQUESTED/APPROVED/REJECTED/APPLIED transitions; typed appliers; same-request pessimistic lock + @Version; sourceReference comparison | Authority headers are not authenticated at the service; direct administrator correction intentionally bypasses four eyes; cross-request business-key serialization and durable actor linkage incomplete |
| SCD2 | Inclusive date windows; invalid split/extension rejected; historical partner active semantics corrected; current queries apply effective dates | Future CREATE duplicates, concurrent overlaps, arbitrary bulk duplicate suppression; same-day version split is intentionally rejected and correction policy needs documentation |
| Partner accounts | Required bank/account/holder values; one main account in aggregate; defensive list; new child IDs on versioning | No verified bank ownership/account-verification workflow; no claim of encryption-at-rest or audit retention without deployment evidence |
| KYC/risk/registration | Enums persist and API exposes labels; normal partner DTO masks registration numbers | No screening decision evidence/expiry/eligibility state machine; arbitrary registration strings; raw change payload bypasses masking |
| Account/department hierarchy | Parent foreign keys and parent existence lookup | No explicit logical self-parent/cycle or effective-date hierarchy policy; recursive parent mapping depth/N+1 not load-tested; account fields lost on update |
| Currency/rates | Rate uses BigDecimal and rejects nonpositive domain values; latest-on-or-before query is DB-limited; pair/date unique | Currency code validator checks length, not ISO membership or alphabet; excessive rate scale/precision not ruled out by domain; no universal financial-policy guarantee |
| Fiscal period | Row lock, transaction, terminal permanent-close invariant; reconstitution preserves closed status | Caller-supplied service identity/audit actor; date/year/month construction constraints mostly storage/application conventions, not a complete aggregate factory |
| Auth domain | Immutable user snapshot, defensive roles, positive roleVersion; approved [validFrom, validTo) assignments | Temporal grants and scope are lost/incompletely enforced downstream; no defined role hierarchy/SoD matrix or password lifecycle workflows in inspected scope |
| Password/auth providers | Strict {bcrypt} structural validation; verifier fails closed; configured users prevalidated; local seed isolated; generated test credentials | This is encoded-format validation, not password strength/history/reset/rotation policy. Local SSO/TOTP exists; production requires real provider adapters and shared replay handling |
| JWT/internal auth | Issuer/signature/expiry/issued-at/claim validation in gateway; trusted headers stripped/rebuilt; version dependency fails closed; internal auth role-apply uses constant-time token comparison | Role expiry/scope findings; service receiver ingress trust absent; no deployment key-rotation or audience-separation certification |
| Auth persistence | Eager role reads within adapter transactions; role replacement uses per-user lock and trace/fingerprint deduplication | Login-counter race, memory-mode missing bean, identity limits mismatch. Missing @Version alone is not a role-write bug where pessimistic locking already applies |
| Transactions | Master mutations/application use cases transactional; fiscal period and role replacement locked; auth avoids a long remote-call transaction | Whole apply-due queue shares one transaction; no verified concurrent PostgreSQL failure/retry behavior |
| Batch | Required asOfDate; orchestration delegates to core; COUNT queries avoid loading all masters; primitive execution-context state | Only 5 tests; failed-run restart/same-instance retry and operational retention/metrics need verification. No specific confirmed job-orchestration defect found |
| MSA dependencies | Inspected application/domain code avoids inbound/API and persistence implementation dependencies | Some monolith composition still wires local adapters. Old documentation claiming specific Loan entity/Closing repository leaks is not sufficient proof of current violations |

The project has meaningful protective code; these findings do not mean every request is unprotected. In particular, gateway header stripping/internal-route denial, same-request locks, fiscal terminal states, role-change locks, strict password storage, and historical SCD2 tests are real controls. They have specific limits identified above.

## Database mapping and verification limits

The baseline exercises H2/JPA and Flyway context paths. Classes named `*PostgresqlSchemaContextTest` explicitly use H2 in PostgreSQL compatibility mode; they do not execute PostgreSQL. No production/development connection settings or personal data were inspected.

| Contract | Mapping inspected | Risk / next gate |
|---|---|---|
| Master codes/names | Account/department/partner code 20; product code 50; account/department/partner name 100; product name 200 | Validate before approval; max/max+1 tests (F12) |
| Partner details | Registration 20; CEO/business fields 50; account number/holder/bank 100; child FK + orphan-removal mapping | Bound and validate by jurisdiction/type; test version copy and sensitive response projections |
| Monetary precision | Product NUMERIC(19,4), rate NUMERIC(19,8), tax rate NUMERIC(7,4) | BigDecimal alone does not prevent rounding/overflow; exact storage round trips required |
| Auth identity | Auth user key 80; departmentCode 40; PAT owner 50; PAT name 100 | PAT mismatch F22; organization code lengths need an agreed shared contract |
| Audit identity | Master auditUser 50 vs requester/approver 80 | Actor propagation must resolve limits rather than truncate identifiers |
| Concurrency | Change-request lock_version + request lock; fiscal row lock; auth role user lock | SCD2 business-key exclusion and atomic login counters absent |
| DB integrity | Enumerated check constraints, pair/date and sourceReference uniqueness, parent/account FKs | No SCD2 exclusion/date-order constraints in reviewed baseline; constraints do not replace domain validation |

Before production acceptance, run clean and upgrade migrations plus Hibernate validate against an approved ephemeral PostgreSQL instance; execute concurrent transactions with deterministic barriers, deadlock/retry checks, precise numeric round trips and SQL-limit/query-plan tests. No package, image or dependency installation was undertaken here. No live full gateway-to-service topology, performance benchmark, browser/BFF journey, real provider integration or crash/restart test was performed.

## Prioritized issue catalogue

Suggested labels are metadata for creation, not new repository labels. The read-only `gh issue list --state open --limit 500` returned no open issues. A targeted closed-issue search identified related history; it is not an exhaustive semantic duplicate search of every historical issue/comment. The drafts distinguish already-fixed cases such as #667 future UPDATE from remaining cases such as future CREATE. Review drafts against any new issues before publication.

| ID | Priority | Title | Evidence class |
|---|---|---|---|
| [F01](issues/F01.md) | P1 | [auth] Bind PAT management to authenticated ownership and administrator authority | Reproduced/source-backed |
| [F02](issues/F02.md) | P1 | [auth][master-data] Authenticate trusted ingress and Closing service identity before accepting authority headers | Reproduced/source-backed |
| [F03](issues/F03.md) | P1 | [auth] End JWT privileges when individual approved role assignments expire | Reproduced/source-backed |
| [F04](issues/F04.md) | P1 | [auth] Preserve and enforce role data scopes through gateway authorization | Reproduced/source-backed |
| [F05](issues/F05.md) | P1 | [auth] Fail closed on unavailable department validation | Reproduced/source-backed |
| [F06](issues/F06.md) | P1 | [auth] Make login-failure counting and lock reset atomic across instances | Reproduced/source-backed |
| [F07](issues/F07.md) | P1 | [master-data] Serialize SCD2 writes by business key and reject overlapping database periods | Reproduced/source-backed |
| [F08](issues/F08.md) | P1 | [master-data] Reject sequential duplicate future-dated CREATE intervals | Reproduced/source-backed |
| [F09](issues/F09.md) | P1 | [master-data] Preserve account classification and regulatory mapping across SCD2 updates | Reproduced/source-backed |
| [F10](issues/F10.md) | P2 | [master-data] Fail closed on duplicate business-partner results in bulk reference queries | Reproduced/source-backed |
| [F11](issues/F11.md) | P2 | [master-data] Enforce product value precision and sign rules before approved persistence | Reproduced/source-backed |
| [F12](issues/F12.md) | P2 | [master-data] Validate master payloads against domain and storage limits before approval | Reproduced/source-backed |
| [F13](issues/F13.md) | P2 | [master-data] Define and implement screening eligibility separately from operational active status | Policy/control gap |
| [F14](issues/F14.md) | P2 | [master-data][auth] Preserve authenticated actors and durable security/change audit evidence | Policy/control gap |
| [F15](issues/F15.md) | P2 | [master-data] Mask sensitive change payloads in summary and mutation responses | Reproduced/source-backed |
| [F16](issues/F16.md) | P2 | [master-data] Preserve expected HTTP errors through the assembled exception advice | Reproduced/source-backed |
| [F17](issues/F17.md) | P2 | [auth] Sanitize unexpected errors and reject malformed requests as client errors | Reproduced/source-backed |
| [F18](issues/F18.md) | P2 | [auth] Restore memory-mode application startup with the PAT feature | Reproduced/source-backed |
| [F19](issues/F19.md) | P2 | [master-data] Isolate due-change failures and prevent poison requests from starving the queue | Reproduced/source-backed |
| [F20](issues/F20.md) | P2 | [master-data] Make identical concurrent submissions and apply retries idempotent | Reproduced/source-backed |
| [F21](issues/F21.md) | P2 | [master-data][auth] Bound list queries and avoid loading unused partner account aggregates | Reproduced/source-backed |
| [F22](issues/F22.md) | P2 | [auth] Align PAT identity and input limits with the auth schema | Reproduced/source-backed |
| [F23](issues/F23.md) | P2 | [master-data][auth] Synchronize foundation documentation with verified runtime behavior | Documentation drift |

Every draft contains title, labels, exact commit/file/line references, problem, business/architecture impact, solution, acceptance criteria, evidence limits and a bounded execution/verification/rollback contract. The following condensed findings are self-contained; the individual files add workflow details.

### F01 — [auth] Bind PAT management to authenticated ownership and administrator authority (P1)

**Labels:** `area:api`, `type:bug`, `priority:p1`

**Problem:** PAT creation accepts a caller-selected username and defaults to admin. Listing and revocation accept a supplied owner; administrator listing has no authorization and force-revoke passes isAdmin=true. The service trusts these inputs and uses case-insensitive owner comparison even though user identities are stored as distinct keys.

**References:** [auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java:31](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java#L31); [auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java:64](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java#L64); [auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java:85](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java#L85)

**Impact:** A caller reaching the auth service can anonymously create token records for arbitrary users, list token metadata, and revoke tokens. This is an authorization defect independently of whether PAT authentication is implemented. Current gateway routes do not expose PAT endpoints; public-gateway exploitation and downstream PAT impersonation are not claimed.

**Solution:** Derive identity and privileges from a verified principal, remove admin defaults and caller-controlled administrator flags, and enforce canonical ownership in an inbound PAT use case. Add actor-attributed creation/revocation events.

**Acceptance:**

- Anonymous callers cannot create/list/revoke PATs; ordinary users cannot act for another identity or invoke administrator actions.
- Use the same canonical identity semantics as AuthUser; case variants must not authorize another account.
- Full HTTP tests include spoofed body/query identities, forged headers, owner success and verified administrator success. Secure the boundary before adding gateway routes.

**Evidence/limits:** Reproduced with actual controller and mocked service in standalone MockMvc: anonymous administrator list=200, revoke=204, empty create delegates owner=admin. No real token or user accessed.

### F02 — [auth][master-data] Authenticate trusted ingress and Closing service identity before accepting authority headers (P1)

**Labels:** `area:api`, `type:bug`, `priority:p1`

**Problem:** Auth administrator and master-data write/approval endpoints trust raw X-Auth-* headers. Fiscal-period mutation trusts only X-Service-Identity=closing and accepts auditUser from the body. No receiving servlet authentication layer establishes those identities; the Closing HTTP adapter sends the identity header alone.

**References:** [auth/api/src/main/java/com/ho/account/auth/api/web/AdminUserController.java:25](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/AdminUserController.java#L25); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:51](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L51); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataDirectWritePolicy.java:34](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataDirectWritePolicy.java#L34); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/InternalFiscalPeriodController.java:36](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/InternalFiscalPeriodController.java#L36); [closing/core/src/main/java/com/ho/account/closing/infrastructure/external/HttpFiscalPeriodControlAdapter.java:140](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/closing/core/src/main/java/com/ho/account/closing/infrastructure/external/HttpFiscalPeriodControlAdapter.java#L140)

**Impact:** Any caller with direct service connectivity can forge administrator/approver identities or the Closing service identity. Two fabricated actors can defeat the intended four-eyes provenance. Fiscal-period transition invariants still apply. Gateway stripping and internal-route denial protect the gateway path, not lateral/direct service access; deployment mTLS/network policies were not assessed.

**Solution:** Authenticate the receiver boundary with validated audience-bound tokens or verified mTLS/service assertions. Derive roles and actors from authenticated context; retain both service principal and authorized delegated actor. Keep login publicly reachable under its own contract.

**Acceptance:**

- A correct-looking role/service header without credentials is rejected before any use case executes.
- Wrong-service, expired, invalid-audience and forged credentials fail; verified gateway and Closing calls succeed.
- Gateway spoofed-header stripping/internal-route denial remains effective; full service HTTP tests exercise real filters and advice.

**Evidence/limits:** Source-confirmed trust boundary; no production network penetration attempted. Existing internal-controller tests explicitly treat the header alone as sufficient.

### F03 — [auth] End JWT privileges when individual approved role assignments expire (P1)

**Labels:** `area:core`, `type:bug`, `priority:p1`

**Problem:** JWT expiry uses a fixed TTL and embedded assignments omit validity windows. Token-version validation only requires an unchanged roleVersion and any currently effective role. A permanent basic role therefore keeps a token valid after an elevated role in the same token expires.

**References:** [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JwtTokenIssuer.java:37](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JwtTokenIssuer.java#L37); [auth/core/src/main/java/com/ho/account/auth/core/application/service/AuthService.java:216](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/AuthService.java#L216); [auth/core/src/main/java/com/ho/account/auth/core/domain/model/RoleAssignment.java:42](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/domain/model/RoleAssignment.java#L42); [gateway/src/main/java/com/ho/account/gateway/security/JjwtAccessTokenVerifier.java:129](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/gateway/src/main/java/com/ho/account/gateway/security/JjwtAccessTokenVerifier.java#L129)

**Impact:** Time-limited privileged access can remain usable until token expiry, beyond its approval window.

**Solution:** Cap token lifetime at the earliest included authorization expiry, or validate and project current assignments on every request. Define exact expiry and clock-skew behavior. Do not assume a roleVersion changes merely because time passes.

**Acceptance:**

- With permanent USER plus temporary ADMIN, elevated access fails at validTo even while USER remains effective.
- Clock-controlled issuer/version/gateway tests cover before, exactly at and after expiry, including cached validation.
- Newly effective grants and refresh behavior are explicitly defined without silently extending old privileges.

**Evidence/limits:** Synthetic actual issuer plus actual AuthService validation reproduced the defect with a controlled validation clock; generated signing material/token remained in memory and was not printed.

### F04 — [auth] Preserve and enforce role data scopes through gateway authorization (P1)

**Labels:** `area:core`, `type:bug`, `priority:p1`

**Problem:** Auth retains and signs dataScope, but the gateway principal and forwarded authorization context flatten assignments into role names. Service checks use only those names. Non-GLOBAL assignments consequently receive the same role-based authority as GLOBAL ones.

**References:** [auth/core/src/main/java/com/ho/account/auth/core/domain/model/RoleAssignment.java:26](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/domain/model/RoleAssignment.java#L26); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JwtTokenIssuer.java:49](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JwtTokenIssuer.java#L49); [gateway/src/main/java/com/ho/account/gateway/security/JjwtAccessTokenVerifier.java:129](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/gateway/src/main/java/com/ho/account/gateway/security/JjwtAccessTokenVerifier.java#L129); [gateway/src/main/java/com/ho/account/gateway/filter/JwtAuthenticationFilter.java:122](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/gateway/src/main/java/com/ho/account/gateway/filter/JwtAuthenticationFilter.java#L122); [auth/api/src/main/java/com/ho/account/auth/api/web/AdminUserController.java:25](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/AdminUserController.java#L25)

**Impact:** The system cannot honor scoped privilege approvals; a scoped administrator role can receive the unfiltered all-user projection. The exact organizational scope policy still needs definition.

**Solution:** Define supported scope types and scope subjects, preserve authenticated assignments, and enforce scope in queries and commands. Until implemented, reject unsupported scoped grants instead of silently broadening them.

**Acceptance:**

- Equal role codes with different scopes yield different permitted records/actions according to an approved policy.
- Missing, malformed and unsupported scopes fail closed; no free-text scope is silently promoted to GLOBAL.
- End-to-end tests prove gateway/service propagation and cross-scope denial, including administrator listing and master-data writes.

**Evidence/limits:** Source-confirmed loss of signed scope information. No live tenant/department data was queried.

### F05 — [auth] Fail closed on unavailable department validation (P1)

**Labels:** `area:core`, `type:bug`, `priority:p1`

**Problem:** The remote department adapter returns true for every non-404 HTTP error and RestClient connection failure. The fallback is active whenever auth.master-data.enabled=true and is not restricted to a local profile.

**References:** [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/MasterDataDepartmentValidationAdapter.java:32](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/MasterDataDepartmentValidationAdapter.java#L32); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/MasterDataDepartmentValidationAdapter.java:38](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/MasterDataDepartmentValidationAdapter.java#L38); [auth/core/src/main/java/com/ho/account/auth/core/application/service/AuthService.java:105](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/AuthService.java#L105)

**Impact:** An unavailable or unauthorized reference lookup becomes permission to issue a JWT for an unverified department.

**Solution:** Separate invalid-reference from unavailable-dependency outcomes, reject token issuance for either, return a sanitized retriable availability error where appropriate, and configure bounded timeouts. Keep explicitly local behavior in the local adapter.

**Acceptance:**

- 401/403/500, connection failure, timeout and malformed success responses issue no token.
- 404 reports invalid reference; a verified valid department succeeds.
- Adapter and login integration tests assert fail-closed behavior and no credential/input leakage.

**Evidence/limits:** MockRestServiceServer returned HTTP500 to the actual adapter; it returned true. No external connection.

### F06 — [auth] Make login-failure counting and lock reset atomic across instances (P1)

**Labels:** `area:core`, `type:bug`, `priority:p1`

**Problem:** Failure recording reads, increments in Java and saves without a lock or @Version. Two transactions reading count=3 can both persist count=4, losing a failure. First-row insert and expiry/success deletion have additional races.

**References:** [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JpaLoginAttemptAdapter.java:58](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JpaLoginAttemptAdapter.java#L58); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JpaLoginAttemptAdapter.java:75](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JpaLoginAttemptAdapter.java#L75); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/LoginAttemptJpaRepository.java:5](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/LoginAttemptJpaRepository.java#L5); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/LoginAttemptJpaEntity.java:14](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/LoginAttemptJpaEntity.java#L14)

**Impact:** The configured brute-force threshold is not reliably enforced under concurrency. The documented first-insert TODO understates the existing-row problem.

**Solution:** Use an atomic upsert/increment or serialized per-identity state transition with bounded conflict retries. Coordinate successful-login resets and expired-lock cleanup with the same policy.

**Acceptance:**

- Barrier-based PostgreSQL tests count every committed concurrent failure and lock exactly at the configured threshold.
- Exercise first insert, existing row, expiry cleanup and successful-login races across separate transactions.
- Document allowed in-flight attempts and identity normalization; failures do not disappear or unexpectedly clear a refreshed lock.

**Evidence/limits:** Source-confirmed read-modify-write race; actual multi-transaction PostgreSQL execution was not performed.

### F07 — [master-data] Serialize SCD2 writes by business key and reject overlapping database periods (P1)

**Labels:** `area:core`, `type:financial`, `priority:p1`

**Problem:** Decision/apply locks protect one request ID, not its master-data business key. Distinct approved requests can both pass the history-count check and write overlapping CREATE or UPDATE versions. Master rows lack optimistic versions, and code/date indexes do not enforce interval exclusion.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java:138](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java#L138); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java:145](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java#L145); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/MasterDataChangeRequestJpaRepository.java:16](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/MasterDataChangeRequestJpaRepository.java#L16); [master-data/core/src/main/resources/db/migration/V6__master_data_postgresql_baseline.sql:156](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/resources/db/migration/V6__master_data_postgresql_baseline.sql#L156)

**Impact:** Two requests may both become APPLIED for an ambiguous financial reference. Singular lookups can fail and bulk/count paths may silently use or count duplicates.

**Solution:** Serialize every mutation of (type,key), including direct writes and absent-row CREATE; recheck version under that lock. Add database nonoverlap/valid-interval constraints through forward migrations and define existing-data remediation.

**Acceptance:**

- Concurrent distinct request IDs and direct-versus-approved writes cannot commit overlapping periods.
- PostgreSQL tests cover CREATE/UPDATE/DEACTIVATE, absent keys, rollback and precise 409 conflicts.
- Failed contenders never become APPLIED; unaffected business keys retain parallel throughput.

**Evidence/limits:** Source-confirmed concurrent execution trace; PostgreSQL race not executed. Same-request locking is a positive control, not a remedy for this race.

### F08 — [master-data] Reject sequential duplicate future-dated CREATE intervals (P1)

**Labels:** `area:core`, `type:financial`, `priority:p1`

**Problem:** Direct/core CREATE checks only whether a row is active today. The first future-dated creation is invisible to that check, so a second identical future interval is accepted. The same pattern exists for all four supported masters, including BusinessPartner whose existsByBusinessPartnerCode default checks today.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java:42](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java#L42); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java:26](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/DepartmentService.java#L26); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:31](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L31); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java:38](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/BusinessPartnerService.java#L38); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java:93](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java#L93)

**Impact:** A single administrator can create overlapping scheduled master rows through ordinary sequential calls; no race or special database failure is required.

**Solution:** Define CREATE as new business-key creation, or explicitly permit disjoint reactivation with an interval-overlap query. Apply that policy consistently to all entrypoints and back it with F07 constraints.

**Acceptance:**

- Two sequential future CREATE requests with the same key/overlapping dates reject the second and preserve the first.
- Cover all four types, historical overlap, inclusive adjacent dates and legitimate nonoverlap policy.
- Direct/core entrypoints and approved flows use consistent semantics; approved CREATE already has a history-count guard and must retain it.

**Evidence/limits:** Actual ProductService reproduced two accepted identical future intervals with a date-aware in-memory port. Other three paths source-confirmed; this probe did not execute their database adapters.

### F09 — [master-data] Preserve account classification and regulatory mapping across SCD2 updates (P1)

**Labels:** `area:core`, `type:financial`, `priority:p1`

**Problem:** The successor account is built from a command that has no accountType or regulatoryMappingCode. Neither value is copied from the current version. Persistence defaults null accountType from category, while regulatoryMappingCode stays null.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java:100](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectService.java#L100); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/command/AccountSubjectCommand.java:6](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/command/AccountSubjectCommand.java#L6); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/mapper/AccountSubjectMapper.java:50](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/mapper/AccountSubjectMapper.java#L50); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/AccountSubjectEntity.java:91](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/AccountSubjectEntity.java#L91)

**Impact:** Renaming an account while retaining category REVENUE, with NON_OPERATING_INCOME and an existing regulatory mapping, can turn the successor into generic REVENUE and erase its reporting mapping. Old history survives, but current/future classification is corrupted.

**Solution:** Create successors from the current aggregate and apply explicitly supplied changes. Preserve fields absent from the command; introduce explicit authorized commands if those fields should be editable.

**Acceptance:**

- A name-only update preserves regulatoryMappingCode and both non-operating account classifications.
- Persistence round trips verify successor values and unchanged historical values.
- Tests cover direct and approved updates; intentional clearing/changing has explicit semantics and audit evidence.

**Evidence/limits:** Actual AccountSubjectService probe produced a successor with both fields null; JPA defaulting is source-confirmed, not a live PostgreSQL observation.

### F10 — [master-data] Fail closed on duplicate business-partner results in bulk reference queries (P2)

**Labels:** `area:core`, `type:bug`, `priority:p2`

**Problem:** The bulk adapter collects duplicate codes using (a,b)->a. Unlike singular Optional queries, it silently suppresses overlapping active versions. Repository order normally makes this the oldest overlapping validFrom; equal-date order is unspecified.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/adapter/MonolithMasterDataQueryAdapter.java:66](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/adapter/MonolithMasterDataQueryAdapter.java#L66); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java:37](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java#L37)

**Impact:** Batch consumers can receive a plausible but incorrect partner name/type/status while data corruption is hidden. F07 prevents new overlaps but does not make legacy/imported corrupt data safe to read.

**Solution:** Detect duplicates and raise a typed integrity error, or return an explicit per-key failure under a documented bulk contract. Never pick a version silently.

**Acceptance:**

- Two active versions of one requested key cause a deterministic error with no successful substitute value.
- Test equal and unequal start dates, repeated input codes, normal distinct keys and empty input.
- Consumers cannot treat partial/corrupt reference data as a successful financial validation.

**Evidence/limits:** Actual adapter probe returned one entry (first partner) for two same-code results.

### F11 — [master-data] Enforce product value precision and sign rules before approved persistence (P2)

**Labels:** `area:core`, `type:financial`, `priority:p2`

**Problem:** The direct HTTP DTO checks nonnegative price, but approved commands and core services do not. All paths accept more than four meaningful fractional digits even though storage is NUMERIC(19,4). The schema has no nonnegative-price check.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductMasterDataChangeApplier.java:29](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductMasterDataChangeApplier.java#L29); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/command/ProductCommand.java:19](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/command/ProductCommand.java#L19); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:31](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L31); [master-data/api/src/main/java/com/ho/account/masterdata/api/dto/ProductRequestDto.java:26](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/dto/ProductRequestDto.java#L26); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/ProductEntity.java:44](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/ProductEntity.java#L44); [master-data/core/src/main/resources/db/migration/V6__master_data_postgresql_baseline.sql:113](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/resources/db/migration/V6__master_data_postgresql_baseline.sql#L113)

**Impact:** An approved payload can persist a negative price; a positive value such as 1.23456 can change to 1.2346 on storage without a deliberate business rounding decision.

**Solution:** Put sign, precision and representability rules in the core domain. Reject lossy scale/overflow, or apply an explicitly approved rounding policy before exposing a result. Use a forward database constraint as a second line of defense.

**Acceptance:**

- Direct, approved and core paths enforce identical price rules before any old version is closed.
- Cover negative, zero, max magnitude, overflow, excess meaningful scale and harmless trailing zeros.
- Database save/reload preserves the validated value exactly; tests include actual PostgreSQL before release.

**Evidence/limits:** Actual core accepted negative over-scale price. An isolated H2 column with the mapped NUMERIC(19,4) type rounded 1.23456 to 1.2346; this was not a full JPA/PostgreSQL round trip.

### F12 — [master-data] Validate master payloads against domain and storage limits before approval (P2)

**Labels:** `area:core`, `type:bug`, `priority:p2`

**Problem:** Account/product approval validation checks null names but allows blank names; direct/core models largely use unrestricted setters. BusinessPartner trims mandatory text but has no storage-length bounds. Change-request targetKey permits 100 characters while account/department/partner codes permit 20 and product codes 50. Invalid lengths can therefore be accepted and approved before persistence fails.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectMasterDataChangeApplier.java:29](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/AccountSubjectMasterDataChangeApplier.java#L29); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductMasterDataChangeApplier.java:29](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductMasterDataChangeApplier.java#L29); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java:70](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java#L70); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/AccountSubjectEntity.java:34](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/AccountSubjectEntity.java#L34); [master-data/core/src/main/resources/db/migration/V6__master_data_postgresql_baseline.sql:35](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/resources/db/migration/V6__master_data_postgresql_baseline.sql#L35)

**Impact:** Approvals can authorize impossible records, blank descriptions weaken reference quality, and deferred persistence errors can poison apply-due. DTO-only validation cannot protect governance and other inbound adapters.

**Solution:** Centralize mandatory fields, canonical code/text rules and length constraints in domain factories/value objects. Validate the effective merged UPDATE before approval and application, keeping direct API constraints aligned.

**Acceptance:**

- Blank names and oversized keys/text are rejected before request persistence/approval, with stable sanitized validation errors.
- Boundary tests cover exactly max and max+1 for each persisted field and distinguish omitted-update fields from blank values.
- No failed validation closes a current version; all inbound paths share the same rules.

**Evidence/limits:** Blank product name accepted in the actual-service probe; length and approval gaps source-confirmed.

### F13 — [master-data] Define and implement screening eligibility separately from operational active status (P2)

**Labels:** `area:core`, `type:feature`, `priority:p2`

**Problem:** KYC and risk are writable enums with no screening evidence, transition policy, expiry or dedicated reviewer authority. New partners default to active/PENDING/LOW. REJECTED or CRITICAL partners can still be operationally active; reference DTOs expose active but no screening eligibility. Registration numbers are trimmed, not validated.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java:70](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java#L70); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java:118](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java#L118); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java:241](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java#L241); [master-data/api/src/main/java/com/ho/account/masterdata/api/dto/BusinessPartnerRequestDto.java:21](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/dto/BusinessPartnerRequestDto.java#L21); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/adapter/MonolithMasterDataQueryAdapter.java:109](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/adapter/MonolithMasterDataQueryAdapter.java#L109)

**Impact:** The foundation currently stores screening labels rather than proving eligibility for financial use. This is a business-control capability gap, not a finding that active is contractually synonymous with KYC-approved, and not a claimed statutory violation or proven downstream payment bypass.

**Solution:** Obtain the approved jurisdiction/partner-type eligibility policy; model screening lifecycle, reason/evidence, reviewer separation, expiry and escalation. Expose explicit eligibility to relevant consumers instead of overloading active. Validate identifiers according to jurisdiction/type, without assuming a universal registration format.

**Acceptance:**

- A policy decision table covers PENDING/APPROVED/REJECTED/REVIEW_REQUIRED, risk levels, expired evidence and missing identifiers.
- Only authorized screening decisions alter approval/risk; generic metadata edits cannot assert approval without the required evidence.
- Consumer tests enforce the agreed eligibility contract; audit records identify evidence, reviewer and decision time.

**Evidence/limits:** Actual domain probe confirms REJECTED/CRITICAL plus malformed registration remains active. Downstream transaction eligibility was not tested.

### F14 — [master-data][auth] Preserve authenticated actors and durable security/change audit evidence (P2)

**Labels:** `area:core`, `type:feature`, `priority:p2`

**Problem:** Master commands do not propagate authenticated actors into master-row auditUser; rows commonly default to SYSTEM/system. Change requests retain requester/approver but are not explicitly linked to each resulting version, and reject overwrites the original reason. Login-attempt state is deleted on success and PAT revocation has no actor/reason/time event record.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:37](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L37); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java:116](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/ProductService.java#L116); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java:126](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java#L126); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/AccountSubjectEntity.java:88](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/entity/AccountSubjectEntity.java#L88); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/changerequest/MasterDataChangeRequest.java:112](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/changerequest/MasterDataChangeRequest.java#L112); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JpaLoginAttemptAdapter.java:89](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/security/JpaLoginAttemptAdapter.java#L89); [auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java:85](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java#L85)

**Impact:** Database fields alone cannot reconstruct who changed sensitive references or credentials and why. Login log messages exist; absence of a durable DB event stream is not proof that external log retention is absent.

**Solution:** Define audit ownership/retention, append actor-attributed events for sensitive changes, link request/source reference to resulting master versions, preserve request and decision reasons separately, and validate external audit durability if logs are the chosen record.

**Acceptance:**

- Direct/admin changes record the authenticated actor; service-delegated changes record service and authorized business actor separately.
- Approval/apply/reject/PAT events retain timestamps, reason, stable identity and before/after identifiers without credentials/raw sensitive payloads.
- Rollback cannot leave a success audit for an uncommitted change; retention/integrity/export behavior has explicit tests and operational evidence.

**Evidence/limits:** Source-confirmed attribution/history gaps; centralized audit infrastructure and retention were outside this inspection.

### F15 — [master-data] Mask sensitive change payloads in summary and mutation responses (P2)

**Labels:** `area:api`, `type:bug`, `priority:p2`

**Problem:** Normal partner responses apply registration-number masking, but the shared change-request response returns payloadJson unchanged, including on pending lists, approval and apply. Raw data inside the escaped JSON string bypasses the normal DTO masking path.

**References:** [master-data/api/src/main/java/com/ho/account/masterdata/api/dto/MasterDataChangeRequestDto.java:55](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/dto/MasterDataChangeRequestDto.java#L55); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:61](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L61); [master-data/api/src/main/java/com/ho/account/masterdata/api/dto/BusinessPartnerDto.java:19](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/dto/BusinessPartnerDto.java#L19)

**Impact:** Every permitted approval reader can receive raw submitted registration data across pending records without a separate sensitive-detail authorization decision. No actual customer-data disclosure was exercised.

**Solution:** Use metadata/typed masked summaries for list and mutation responses. If raw evidence is necessary, expose it through a separately authorized and audited detail contract while preserving immutable stored source payload.

**Acceptance:**

- Synthetic raw registration values never appear anywhere in summary JSON, including nested escaped payload strings.
- Raw detail access is explicitly allowed/denied by verified authorization and audited.
- Masking changes neither the signed/approved command nor idempotency comparison.

**Evidence/limits:** Source-confirmed bypass; existing normal-partner serialization tests do not cover change-request payload responses.

### F16 — [master-data] Preserve expected HTTP errors through the assembled exception advice (P2)

**Labels:** `area:api`, `type:bug`, `priority:p2`

**Problem:** Approval/internal controllers throw ResponseStatusException(403), but global master-data advice inherits the generic Exception handler returning 500. The status-preserving advice is scoped only to the four direct-write controllers. Current standalone controller tests omit that assembled advice.

**References:** [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:105](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L105); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/InternalFiscalPeriodController.java:39](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/InternalFiscalPeriodController.java#L39); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataExceptionHandler.java:15](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataExceptionHandler.java#L15); [shared-kernel/src/main/java/com/ho/account/shared/finance/exception/GlobalExceptionAdvice.java:63](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/shared-kernel/src/main/java/com/ho/account/shared/finance/exception/GlobalExceptionAdvice.java#L63); [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataDirectWritePolicy.java:21](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataDirectWritePolicy.java#L21)

**Impact:** Denied requests stay blocked, but clients and monitoring see server failures instead of permission errors; malformed headers/types and other expected failures also need consistent mapping.

**Solution:** Handle expected framework/business exceptions centrally with sanitized 400/403/404/409 contracts and preserve unexpected 500 errors. Add tests using real advice and security configuration.

**Acceptance:**

- Insufficient role and missing/wrong service identity produce the intended 403/401 contract, never 500.
- Missing required input/malformed types=400, missing record=404, version/state conflict=409 under a documented API contract.
- Rejected requests invoke no mutation; full-context HTTP tests include the real exception-advice stack.

**Evidence/limits:** Actual controller plus actual MasterDataExceptionHandler reproduced HTTP500 for an insufficient approval role; mocked use case was never called.

### F17 — [auth] Sanitize unexpected errors and reject malformed requests as client errors (P2)

**Labels:** `area:api`, `type:bug`, `priority:p2`

**Problem:** Unexpected exception.getMessage() is reflected into the public response. Unreadable JSON lacks a specific handler and becomes 500. Login username has no maximum length despite the 80-character persistence contract; PAT input is also unvalidated.

**References:** [auth/api/src/main/java/com/ho/account/auth/api/web/AuthExceptionHandler.java:48](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/AuthExceptionHandler.java#L48); [auth/api/src/main/java/com/ho/account/auth/api/dto/LoginRequest.java:5](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/dto/LoginRequest.java#L5); [auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java:25](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java#L25); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/AuthUserJpaEntity.java:24](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/AuthUserJpaEntity.java#L24)

**Impact:** Malformed input is misclassified and implementation/parser/SQL details may be disclosed. Oversized identities can fail while recording authentication failure rather than returning a clean validation response.

**Solution:** Use constant public unexpected-error messages with correlation IDs, map malformed input safely to 400, and enforce bounded inputs before service/persistence calls. Keep diagnostic logging free of credential-bearing exception payloads.

**Acceptance:**

- Malformed JSON/type mismatch returns 400 and does not expose submitted sensitive content.
- A synthetic exception sentinel never appears in a public 500 response.
- Username and PAT field max/max+1 cases are tested against actual schema limits; successful login error behavior remains stable.

**Evidence/limits:** Actual advice/MVC probes reproduced malformed JSON=500 and reflection of a synthetic internal-message sentinel.

### F18 — [auth] Restore memory-mode application startup with the PAT feature (P2)

**Labels:** `area:core`, `type:bug`, `priority:p2`

**Problem:** PAT service/controller are unconditional, while their only persistence-port implementation is conditional on auth.persistence.mode=jpa. The documented memory mode therefore has an unsatisfied required PersonalAccessTokenPort dependency.

**References:** [auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java:37](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java#L37); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/PersonalAccessTokenPersistenceAdapter.java:26](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/PersonalAccessTokenPersistenceAdapter.java#L26); [auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java:20](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java#L20)

**Impact:** The advertised local/in-memory application mode fails to start.

**Solution:** Provide the intended memory adapter or conditionally disable the complete PAT feature for unsupported modes. State feature availability explicitly.

**Acceptance:**

- Full AuthApplication context starts in both documented JPA and memory modes with runtime-generated safe test credentials.
- PAT endpoints are either functional with the intended adapter or intentionally absent with a documented contract.
- Tests include the whole bean graph rather than only configuration binding.

**Evidence/limits:** Reproduced using full local AuthApplication, web disabled, synthetic in-memory H2 and runtime-generated unprinted credentials: startup fails specifically for missing PersonalAccessTokenPort.

### F19 — [master-data] Isolate due-change failures and prevent poison requests from starving the queue (P2)

**Labels:** `area:core`, `type:bug`, `priority:p2`

**Problem:** Up to 500 due requests execute in one transaction. A stale version or invalid payload late in that batch rolls back earlier valid changes. The failing APPROVED request remains eligible for the next earliest-first selection, so repeated runs can make no progress.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java:114](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java#L114); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaMasterDataChangeRequestPersistenceAdapter.java:50](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaMasterDataChangeRequestPersistenceAdapter.java#L50)

**Impact:** One invalid request can indefinitely delay unrelated effective-date changes needed by other financial services.

**Solution:** Use an explicitly separate transactional executor per request, or a restartable chunk/job design with classified retry/quarantine and a durable result record. Avoid self-invocation that bypasses transaction advice. Coordinate locking with F07.

**Acceptance:**

- Given valid A, invalid B and valid C, an approved policy allows independent valid work to progress and reports B explicitly.
- Retry/restart never applies a successful request twice and does not mark failed work APPLIED.
- Exercise partial failure, process restart, two workers, queue ordering and maximum selection size.

**Evidence/limits:** Source-confirmed transaction/selection behavior; process-crash and actual PostgreSQL execution untested. Existing documentation acknowledges this limitation.

### F20 — [master-data] Make identical concurrent submissions and apply retries idempotent (P2)

**Labels:** `area:core`, `type:bug`, `priority:p2`

**Problem:** sourceReference lookup followed by insert is not atomic: concurrent identical first submissions race into a unique-key failure. The constraint protects data but does not recover the existing result. Repeating apply after a committed success fails the APPROVED readiness check instead of returning the completed result.

**References:** [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java:59](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java#L59); [master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java:133](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/application/service/MasterDataChangeRequestService.java#L133); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/changerequest/MasterDataChangeRequest.java:135](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/changerequest/MasterDataChangeRequest.java#L135); [master-data/core/src/main/resources/db/migration/V5__master_data_change_request_lineage.sql:1](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/resources/db/migration/V5__master_data_change_request_lineage.sql#L1)

**Impact:** A lost response or duplicate delivery can appear as failure after successful processing, complicating governance reconciliation and retries. Current sequential request deduplication is a positive control.

**Solution:** Introduce atomic create-or-load with immutable command comparison and transaction-safe conflict recovery. Define repeated application of an already APPLIED identical request as a stable result; retain conflict rejection for mismatched commands.

**Acceptance:**

- Concurrent identical submissions produce one row and the same result for all callers; mismatched reuse returns 409.
- Retry after successful apply/response loss does not create another SCD2 version or change appliedAt.
- PostgreSQL tests cover rollback-only unique violations and recovery in an appropriate transaction; no blind catch-and-continue.

**Evidence/limits:** Source-confirmed retry gaps; concurrent sourceReference race unexecuted.

### F21 — [master-data][auth] Bound list queries and avoid loading unused partner account aggregates (P2)

**Labels:** `area:core`, `type:refactor`, `priority:p2`

**Problem:** Partner history/active/search, pending changes, user lists and PAT lists use unbounded List contracts. Partner list queries fetch every child account despite summary DTOs not needing accounts. SCD2 history increases both query work and response size.

**References:** [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:47](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L47); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java:73](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java#L73); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java:109](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/repository/BusinessPartnerRepository.java#L109); [master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaMasterDataChangeRequestPersistenceAdapter.java:43](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/infrastructure/persistence/JpaMasterDataChangeRequestPersistenceAdapter.java#L43); [auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java:72](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java#L72); [auth/core/src/main/java/com/ho/account/auth/core/application/service/AdminUserQueryService.java:31](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/AdminUserQueryService.java#L31)

**Impact:** Routine foundation reads can consume increasing heap/latency and amplify outages across dependent services. No measured production load failure is claimed.

**Solution:** Add capped stable pagination or cursor contracts and summary projections. Avoid collection-fetch pagination that applies limits in memory; load aggregates only where needed.

**Acceptance:**

- Database tests demonstrate actual SQL limits, stable tie-breaking and no omissions/duplicates between pages.
- Large synthetic partner/account history produces bounded queries/allocations and does not load unused account data.
- API defaults/max sizes and consumer migration are explicit; filtering retains effective-date semantics.

**Evidence/limits:** Source-confirmed unbounded paths; no load benchmark or production query plan was run.

### F22 — [auth] Align PAT identity and input limits with the auth schema (P2)

**Labels:** `area:core`, `type:bug`, `priority:p2`

**Problem:** Auth usernames permit 80 characters, while PAT username permits 50. PAT tokenName permits 100 in storage with no request/domain upper bound; TTL has a default but no approved maximum.

**References:** [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/AuthUserJpaEntity.java:24](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/AuthUserJpaEntity.java#L24); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/PersonalAccessTokenJpaEntity.java:19](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/PersonalAccessTokenJpaEntity.java#L19); [auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/PersonalAccessTokenJpaEntity.java:22](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/infrastructure/persistence/PersonalAccessTokenJpaEntity.java#L22); [auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java:25](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/api/src/main/java/com/ho/account/auth/api/web/PersonalAccessTokenController.java#L25); [auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java:57](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/PersonalAccessTokenService.java#L57)

**Impact:** A valid 51–80 character auth identity cannot reliably create a PAT, and oversized input reaches database failures. Credential lifetime has no enforceable upper policy.

**Solution:** Use one canonical identity-length policy and a forward PAT migration; validate names and explicitly define allowed lifetime bounds before creation.

**Acceptance:**

- Persist/reload PATs for valid 50/51/80-character identities; reject 81 under the common policy.
- Name max/max+1 and TTL zero/negative/max/max+1 behave deterministically before persistence.
- Existing tokens survive the forward migration; owner authentication is provided by F01, not by input validation.

**Evidence/limits:** Source-confirmed schema mismatch and absent upper bounds; real-schema boundary reproduction not executed.

### F23 — [master-data][auth] Synchronize foundation documentation with verified runtime behavior (P2)

**Labels:** `type:docs`, `priority:p2`

**Problem:** Master docs still say terminate clears useYn, while current code preserves it. Schema guidance still describes V2 CLOB although V2 now uses TEXT. Auth README says only NORMAL succeeds and SSO/LDAP always fail, while current local provider/OTP implementations can succeed. Lockout docs mention only first inserts, not existing-row lost updates.

**References:** [master-data/README.md:7](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/README.md#L7); [master-data/docs/process-flow.md:42](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/docs/process-flow.md#L42); [master-data/docs/schema.md:102](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/docs/schema.md#L102); [auth/README.md:23](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/README.md#L23); [auth/README.md:56](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/README.md#L56); [master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java:274](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/java/com/ho/account/masterdata/core/domain/model/BusinessPartner.java#L274); [master-data/core/src/main/resources/db/migration/V2__master_data_change_requests.sql:1](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/master-data/core/src/main/resources/db/migration/V2__master_data_change_requests.sql#L1); [auth/core/src/main/java/com/ho/account/auth/core/application/service/AuthService.java:76](https://github.com/skyg547/account/blob/a97d10ab6efc2570a88f83d630242cd748f8be57/auth/core/src/main/java/com/ho/account/auth/core/application/service/AuthService.java#L76)

**Impact:** Operators and future maintainers receive incorrect explanations of historical status, migration readiness and supported authentication modes. Stale cross-module dependency TODOs can also trigger unnecessary refactors.

**Solution:** Update only the inspected module guides using actual code/test evidence. Distinguish local provider implementations from missing production providers, verified H2 behavior from PostgreSQL evidence, and operational active status from screening eligibility.

**Acceptance:**

- README/process/schema/local-run agree with the current implementation and executed commands.
- Remove obsolete exact defect claims only after confirming their replacements; retain unresolved defects with concrete references.
- Independent documentation review verifies local versus production limitations, historical active semantics and current test evidence.

**Evidence/limits:** Source/document comparison; current baseline tests independently verified. No documentation changes beyond this audit were made.

## Remediation order and release gates

1. Establish receiver-side identity trust (F02), secure PAT ownership/admin operations before exposing it (F01), and close department/JWT/scope authorization gaps (F03–F05). These are prerequisites for treating service headers and approved roles as authority.
2. Make lockout atomic (F06); implement shared-key SCD2 exclusion/serialization (F07) and sequential CREATE protection (F08). Preserve account metadata (F09) and reject bulk ambiguity (F10). Verify with real PostgreSQL concurrent transactions before claiming multi-node readiness.
3. Align core validation/precision and screening policy (F11–F13), then complete actor/evidence controls and safe responses (F14–F17). KYC eligibility requires a product/compliance policy decision; no arbitrary universal rule is proposed.
4. Repair documented runtime mode (F18), due-work progress/idempotency (F19–F20), bounded reads (F21), PAT input/schema limits (F22), and synchronized guidance (F23). Each issue remains separately reviewable rather than one foundation rewrite.

Do not add PAT gateway routes until authorization is fixed. PAT currently has creation/list/revocation scaffolding but no production token-hash authentication consumer; operational PAT authentication must be designed, bounded and tested separately. Likewise TaxProfile has no complete governed use-case pipeline, and SppiTestRule is an explicitly simplified, currently unconnected rule: it must not be presented as production classification coverage. SPPI policy adequacy and regulatory interpretation were not evaluated against external standards in this repository audit.

## Missing tests and explicit acceptance evidence

| Missing evidence | Why green tests do not establish it | Required targeted verification |
|---|---|---|
| Receiver authentication/PAT HTTP authorization | Mocked or standalone controllers do not enforce a real trusted-ingress boundary; no PAT controller test exists | Real filter/advice HTTP tests for anonymous, forged, ordinary, scoped and privileged actors |
| Role lifecycle and scopes | Issuer snapshot tests do not advance a mixed-role session through expiry; gateway flattens scopes | Clock-controlled login→gateway→protected-action tests and current-scope comparisons |
| Remote department failures | No remote error-matrix test protecting fail-closed semantics | 401/403/404/500, timeout, connection failure, malformed response; no token issuance |
| Concurrent counters/SCD2/trace | Sequential mock/H2 tests do not establish distinct-transaction interleavings | PostgreSQL barrier tests, one-winner/conflict results, rollback and first-insert races |
| Master invariant parity | 338 parameterized payload cases focus selected shapes, not every domain/storage boundary | Direct/approved/core max-length, blank, sign, precision and effective-interval cases |
| Mapping preservation | Current SCD2 tests do not populate every metadata field | Full aggregate save/reload and successor-vs-predecessor field comparison |
| Real database baseline | PostgreSQL-named tests actually run H2 | Clean/upgrade PostgreSQL migration, JPA validate, exact numeric and FK/check behavior |
| Due/batch recovery | Successful runs and date validation are not crash/restart proofs | Poison-item progress, crash restart, same job instance, committed-effect deduplication |
| Load behavior | No branch coverage or load measurement | SQL limits, no collection-fetch pagination surprise, large history query plans |
| Security audit retention | Mutable lock state and application log calls are not a retention test | Approved immutable-event/log collection, access controls, retention and retrieval evidence |

## Q1–Q4 assessment and review separation

This table evaluates the **inspected baseline**, not approval of a code change. Audit delivery can be complete while remediation remains open. No implementation is declared production-ready or independently approved for merge.

| Contract | Verdict | Evidence | N/A reason | Risk / next gate | Independent review |
|---|---|---|---|---|---|
| Q1 — clear responsibilities and correctness | FAIL | F01–F12, F18–F22; 627 baseline + 79 supplemental tests pass but miss these conditions | Not applicable: existing behavior was explicitly inspected | Resolve authorized issues and rerun acceptance suites; passing baseline is insufficient | Separate read-only auth and master persistence reviewers found matching boundary/atomicity risks |
| Q2 — accurate input→processing→output/failure explanations | FAIL | Remote adapter's unrestricted allow-on-error rationale; role/scope lifetime and apply-due limitations | Not applicable: workflow explanations inspected | Correct code/contracts and explain errors/retries/time semantics | Reviewers supplied code-path evidence; report distinguishes static and executed evidence |
| Q3 — current module docs and beginner guidance | FAIL | F23: terminate/useYn, V2 TEXT/CLOB, auth providers, lockout limitation | Not applicable: source/docs drift confirmed | Update module documentation after verified behavior and independent review | Both reviewers identified stale claims; obsolete findings were excluded |
| Q4 — accurate nearby intent comments | FAIL | MasterDataDepartmentValidationAdapter.java:45 calls its fail-open fallback standalone without a profile restriction | Not applicable: existing nontrivial logic/comments inspected | Correct misleading rationale alongside fixes; preserve useful existing comments | Useful SCD2/locking/batch comments recognized; comments are not verification |

Parent owns test execution, diagnostic probes, report/issue drafts and shared records. `/root/auth_audit` and `/root/master_persistence_audit` performed independent read-only source review and did not edit code or run the parent's Gradle tasks. The latter also challenged the parent-created account-field, future-CREATE, bulk-ambiguity, product and KYC findings. Both reviewers approved the substantive audit accuracy. Their requested source-reference and cross-module verification-plan corrections were applied. For the documentation deliverable itself, Q1/Q4 are N/A because no implementation changed, and Q2/Q3 pass for evidence-labelled explanations and corrected references/plans. This is procedural review separation, not a GitHub APPROVED review or evidence of deployment isolation.

## Delivery and handoff

Final documentation checks: harness quality 32/32 passed; all 23 issue bodies and 102 exact-commit source links validated; approved-file conflict markers, unmerged index and whitespace checks are clean. Primary checkout HEAD/status are unchanged. These are document/static checks, additional to the 706 application tests.

- [23 individual issue bodies](issues/) and [title/label/body manifest](issue-manifest.json) are ready for review/creation.
- Suggested issue labels have not been created or modified. Publication was not explicitly authorized, so drafts are the completed deliverable permitted by the request; there is no pending permission needed to finish this audit.
- Only audit documentation and the four required append-only harness/history records are written. No repair is included, and no new regression test is added to production source trees.
- Rollback of this deliverable means discarding/reverting only these audit documents/record additions through a reviewed change; no schema/data/runtime rollback is necessary.
- Next owner: repository maintainers prioritize and approve executable issue scopes, then implementation owners and independent reviewers close each stated acceptance gap.
