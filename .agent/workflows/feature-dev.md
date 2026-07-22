---
description: issue-driven role-based feature development workflow
---

# Feature Development Workflow

Follow `AGENTS.md` and `docs/ai-harness/10-rules.md`. Treat legacy `.clinerules` as supplemental persona context only when it does not conflict with the current harness.

1. **Planner / Explorer**
   - Confirm the Issue or task contract, acceptance criteria, affected module/layer, and candidate files.
   - Do not edit code.

2. **Branch / Worktree**
   - Create a non-base branch.
   - Use an external worktree for dirty checkouts or parallel writers.
   - Record Issue, branch, worktree, and base branch.

3. **Role-Based Implementation**
   - Assign disjoint allowlists to Controller, Service, Batch, SQL, Test, and Documentation roles.
   - Preserve API/core/batch and port/adapter boundaries.
   - Keep shared logs with the parent Integrator.

4. **Verification / Review**
   - Run targeted tests, affected builds, diff checks, and conflict-marker checks.
   - Use an independent read-only Reviewer.

5. **Integration**
   - Let the parent Integrator update shared records and prepare the Draft PR.
   - Merge only through the approved PR gate.
   - Update Issue and handoff before cleanup.
