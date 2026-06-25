# AI Harness Overview

## Purpose

This harness defines how AI agents work safely in the account project. The project is a modular financial accounting and asset-management system built around Spring Boot, Java 17, hexagonal architecture, DDD, and high-volume batch processing.

초보자 설명: "하네스"는 여러 AI가 동시에 일해도 서로 충돌하지 않도록 작업 순서, 역할, 기록 위치, 금지 사항을 정해 둔 안전벨트다.

## Primary Tools

- Codex CLI: implementation, integration, verification, and repository-aware edits.
- Gemini CLI: independent review, risk finding, and prompt-based handoff review.
- Antigravity: planning, UI/IDE-assisted exploration, or human-supervised agent orchestration.
- Other local/open tools: non-sensitive local code search and draft generation only.

## Existing Project Guidance

- `Agents.md`: Codex-specific rules and common architecture rules.
- `GEMINI.md`: Gemini review role and read order.
- `CLAUDE.md`: Claude review role.
- `.clinerules`: legacy persona/R&R guidance.
- `SKILL.md`: repository-specific working sequence.
- `docs/WORKLOG.md`: project work history.
- `CODEX_WORKLOG.md`, `CLAUDE_WORKLOG.md`: agent-specific history.

## Harness Documents

- `10-rules.md`: mandatory rules.
- `20-workflow.md`: end-to-end loop.
- `30-agents.md`: agent roles.
- `40-test-checklist.md`: verification checklist.
- `50-git-worktree-guide.md`: branch/worktree guide.
- `60-rebase-merge-policy.md`: rebase, merge, conflict policy.
- `70-model-assignment-policy.md`: model capability assignment.
- `80-file-ownership.md`: role-based file ownership.
- `worklog.md`, `agent-status.md`, `decision-log.md`, `conflict-log.md`, `integration-log.md`, `handoff.md`: operating records.

## Operating Loop

1. Read current worklog and module docs.
2. Summarize the requirement.
3. Analyze impact.
4. Split work by agent role.
5. Work on a separate branch or worktree.
6. Update worklog and status.
7. Test and integrate.
8. Create a Draft MR/PR.
9. Receive human review.
10. Merge only after verified approval.

