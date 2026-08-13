# AI Harness Overview

## Purpose

This harness defines how AI agents work safely in the account project. The project is a modular financial accounting and asset-management system built around Spring Boot, Java 17, hexagonal architecture, DDD, and high-volume batch processing.

초보자 설명: "하네스"는 여러 AI가 동시에 일해도 서로 충돌하지 않도록 작업 순서, 역할, 기록 위치, 금지 사항을 정해 둔 안전벨트다.

## Instruction Layers

1. `AGENTS.md`: short, always-loaded repository contract.
2. `.agents/skills/`: task-specific reusable workflows loaded only when triggered.
3. `.codex/agents/`: project-scoped custom subagent personas and sandbox boundaries.
4. `docs/ai-harness/`: detailed policy, runbooks, and operating records.
5. Module `README.md` and `docs/*.md`: local domain and execution context.

## Primary Tools

- Codex: implementation, integration, verification, repository skills, and custom subagents.
- Gemini: independent review by default; explicit implementation assignments follow the shared Issue ownership protocol.
- Claude Code: architecture review by default; explicit implementation assignments follow the shared Issue ownership protocol.
- Antigravity/Cline-compatible workflow: `.agent/workflows/feature-dev.md`, subordinate to the common harness.
- GitHub Issues/Projects and `gh`: task contract, traceability, Draft PR, and review gates.
- Local/open tools: non-sensitive local exploration and draft generation only.

## Existing Project Guidance

- `AGENTS.md`: Codex repository contract and architecture invariants.
- `.agents/skills/`: Issue loop, architecture change, and review/handoff skills.
- `.codex/agents/`: Planner, Explorer, implementation, test, review, and integration personas.
- `GEMINI.md`: Gemini independent review role.
- `CLAUDE.md`: Claude role guidance.
- `.clinerules`: legacy persona guidance; common safety and architecture rules in `AGENTS.md` take precedence.
- `SKILL.md`: compatibility pointer to the discoverable repository skills.
- `docs/WORKLOG.md`: durable project milestones.
- `CODEX_WORKLOG.md`, `CLAUDE_WORKLOG.md`: agent-specific history.

## Harness Documents

- `05-issue-1-compliance-audit.md`: Issue #1 compliance audit and remediation notes.
- `10-rules.md`: mandatory rules.
- `20-workflow.md`: end-to-end loop.
- `30-agents.md`: agent roles.
- `40-test-checklist.md`: verification checklist.
- `45-ci-module-validation.md`: GitHub Actions changed-module test selection.
- `50-git-worktree-guide.md`: branch/worktree guide.
- `60-rebase-merge-policy.md`: rebase, merge, conflict policy.
- `70-model-assignment-policy.md`: capability and risk based model assignment.
- `80-file-ownership.md`: role-based file ownership.
- `85-github-issue-agent-loop.md`: Issue/branch/worktree/PR runbook.
- `86-multi-tool-issue-ownership.md`: Codex/Gemini/Claude Code Issue status, claim, ownership, and handoff protocol.
- `90-beginner-ai-agent-git-guide.md`: beginner guide.
- `95-codex-skills-subagents.md`: skill and custom subagent map.
- `worklog.md`, `agent-status.md`, `decision-log.md`, `conflict-log.md`, `integration-log.md`, `handoff.md`: operating records.

## Operating Loop

1. Select or create an Issue when durable tracking is appropriate, then check its `status:*` and `agent:*` labels before assigning a tool.
2. Use `$account-issue-loop` to establish branch/worktree traceability.
3. Read the target module docs and use `$account-hexagonal-change` for code work.
4. Delegate disjoint exploration, implementation, test, and review tasks to `.codex/agents/`.
5. Let the parent Integrator collect results and update shared records once.
6. Use `$account-review-handoff` for verification and handoff.
7. Open a Draft PR, receive human review, merge through the PR gate, then synchronize the Issue and cleanup.
