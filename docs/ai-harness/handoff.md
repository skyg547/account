# AI Harness Handoff

## Current State

- Branch: `main`
- Base branch: `main`
- Push status: main merge completed locally; origin push pending
- Current owner: Codex
- Verification: required document check, conflict marker check, trailing whitespace check, and diff check passed.

## What Changed

- Existing root guidance was preserved.
- New AI harness documents were added under `docs/ai-harness/`.
- Backups of existing harness files were created under `docs/ai-harness/_backup/2026-06-25/`.
- `.gitignore` now excludes local worktree folders.
- Beginner guide for AI agent coding with Git was added as `90-beginner-ai-agent-git-guide.md`.
- `ai-harness-upgrade-20260625` was merged into `main` by explicit user request.

## Next Recommended Steps

1. Push `main` to `origin/main`.
2. Verify `main...origin/main` is clean.
3. Use `docs/ai-harness/90-beginner-ai-agent-git-guide.md` as the beginner onboarding guide.

## Known Risks

- The repository has `.claude/settings.local.json`, but its content was not inspected to avoid local/private setting exposure.
- Slash-style branch prefixes failed locally, so the branch uses a slash-free name.
