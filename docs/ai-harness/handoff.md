# AI Harness Handoff

## Current State

- Branch: `ai-harness-upgrade-20260625`
- Base branch: `main`
- Push status: branch previously pushed; new guide commit ready for push and main integration
- Current owner: Codex
- Verification: required document check, conflict marker check, trailing whitespace check, and diff check passed.

## What Changed

- Existing root guidance was preserved.
- New AI harness documents were added under `docs/ai-harness/`.
- Backups of existing harness files were created under `docs/ai-harness/_backup/2026-06-25/`.
- `.gitignore` now excludes local worktree folders.
- Beginner guide for AI agent coding with Git was added as `90-beginner-ai-agent-git-guide.md`.

## Next Recommended Steps

1. Verify the new beginner guide.
2. Push `ai-harness-upgrade-20260625`.
3. Merge into `main` because the user explicitly requested mainstream integration.
4. Push and sync `main`.

## Known Risks

- The repository has `.claude/settings.local.json`, but its content was not inspected to avoid local/private setting exposure.
- Slash-style branch prefixes failed locally, so the branch uses a slash-free name.
