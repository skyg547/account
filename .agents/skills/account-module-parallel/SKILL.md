---
name: account-module-parallel
description: Execute multi-module tasks in parallel across MSA microservices (e.g., journal-ledger, deposit, payable, closing) with disjoint module file allowlists, shared-kernel contract isolation, and parent integrator aggregation. Use when a task or issue spans multiple independent modules or when parallel module development is requested.
---

# Account Module-based Parallel Execution ($account-module-parallel)

## Purpose & Scope

This skill guides the execution of multi-module features, refactorings, or audits across the 20+ microservice modules in the `account` repository. It maximizes execution speed via subagent concurrency while ensuring strict file isolation and parent-controlled integration.

---

## 1. Preparation & Scope Decomposition

1. Read `AGENTS.md`, `docs/ai-harness/30-agents.md`, and `docs/ai-harness/80-file-ownership.md`.
2. Identify all target service modules involved (e.g., `journal-ledger`, `deposit`, `payable`, `tax`, `closing`).
3. Check for dependencies on `shared-kernel` or shared API contracts in `contracts/`.

---

## 2. Phase 1: Shared Contract Lock (If Applicable)

If the task requires changing shared domain models, common DTOs, or base utilities in `shared-kernel`:
1. Execute `shared-kernel` changes first in the parent context.
2. Verify contract build: `./gradlew :shared-kernel:build`.
3. Freeze shared contracts before dispatching parallel module subagents.

---

## 3. Phase 2: Dispatch Parallel Module Subagents

1. Launch concurrent subagents via `invoke_subagent` (one per target module).
2. Set explicit disjoint allowlists for each subagent:
   - Module Agent A: Allowlist `journal-ledger/**`
   - Module Agent B: Allowlist `deposit/**`
   - Module Agent C: Allowlist `payable/**`
3. Subagent Prompt Requirements:
   - Must follow Hexagonal Architecture principles inside its target module.
   - Must run module-scoped tests: `./gradlew :<module-name>:test`.
   - **MUST NOT** edit shared harness logs (`docs/ai-harness/*.md`, `CODEX_WORKLOG.md`) or files outside its designated module.
   - Must report completed files, test results, and any architectural decisions back to parent.

---

## 4. Phase 3: Parent Integration & Verification

1. Receive completed reports from all module subagents.
2. Parent Integrator runs cross-module integration tests: `./gradlew test` (or targeted multi-module test commands).
3. Record progress and status in shared harness files (`agent-status.md`, `worklog.md`, `handoff.md`).
4. Perform final review via `$account-review-handoff` before commit/PR gate.
