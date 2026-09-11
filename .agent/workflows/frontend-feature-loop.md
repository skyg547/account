---
description: Issue-scoped frontend development with verification, independent review and parent-approved Draft PR
---

# Frontend Feature Loop

Follow [AGENTS.md](../../AGENTS.md), the [common workflow](../../docs/ai-harness/20-workflow.md) and [feature workflow](feature-dev.md). Roles and ownership follow [30](../../docs/ai-harness/30-agents.md), [80](../../docs/ai-harness/80-file-ownership.md), [85](../../docs/ai-harness/85-github-issue-agent-loop.md) and [86](../../docs/ai-harness/86-multi-tool-issue-ownership.md).

Implementers and read-only Reviewers return evidence and change requests to the parent Integrator. Only the parent updates shared records and Git/GitHub state within user or explicit workflow approval. Implementation or review alone does not authorize publication; model tier does not grant authority.

1. **Issue and specification**
   - Read the existing Issue and comments; return any new Issue draft or claim request to the parent. The parent confirms ready status, dependencies, ownership and acceptance criteria before assigning implementation.
   - Before coding, write or update the approved Markdown specification in `frontend/docs/`. Link the Issue and describe layout, mock data structure, component hierarchy, verification and rollback.

2. **Branch and worktree isolation**
   - The parent fetches the latest base and creates `agent/<issue>-<slug>`; use an external worktree for dirty checkouts, parallel work or explicit isolation requests. Preserve the original checkout and other workers' changes.
   - The parent records the Issue, branch, worktree, exact base and disjoint file allowlists before editing. Follow the [branch/worktree guide](../../docs/ai-harness/50-git-worktree-guide.md).

3. **Implementation**
   - Implement one logical unit (one page or domain) within the assigned scope.
   - Use the `USE_MOCK` pattern for initial UI development when the approved specification calls for it, and follow the established design system and frontend module documentation.

4. **Verification and independent review**
   - Run the targeted frontend tests, lint/type checks and affected build required by the Issue/module docs; inspect scoped diff, whitespace and conflict markers. Report commands, results and any skipped checks with reasons and risks.
   - Freeze the diff and return changed files, base/head identity, verification, rollback and a PR body draft to the parent. A separate read-only Reviewer checks the acceptance criteria and returns findings; required fixes go back to the implementer for verification and re-review.
   - Failed or unverifiable required checks and unresolved findings hold publication. Local success is not CI evidence.

5. **Parent-approved Draft PR**
   - After verification and independent review, the parent alone updates shared records, stages/commits the approved files and, within explicit publication approval, pushes the Issue branch and creates a Draft PR with `Refs #<issue>`.
   - Return evidence without publishing if approval is absent. Follow the [PR lifecycle](../../docs/ai-harness/88-pr-review-and-merge-runbook.md): frozen Draft pre-review is allowed; Draft-stage checks and Ready-only checks are distinct. Ready, merge, Issue close and cleanup each require their applicable approval gate. Changes to head/base or verification require re-review.

6. **Next unit**
   - Repeat implementation, verification and independent review only within the same approved Issue scope. Every publication remains parent-owned and gated; a new domain or out-of-scope unit returns to Issue/specification and ownership confirmation. Do not automatically continue or publish beyond the assignment.
