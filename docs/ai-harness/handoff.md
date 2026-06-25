# AI Harness Handoff

## Current State

- Branch: `ai-harness-upgrade-20260625`
- Base branch: `main`
- Push status: not pushed
- Current owner: Codex
- Verification: required document check, conflict marker check, trailing whitespace check, and tracked-file diff check passed.

## What Changed

- Existing root guidance was preserved.
- New AI harness documents were added under `docs/ai-harness/`.
- Backups of existing harness files were created under `docs/ai-harness/_backup/2026-06-25/`.
- `.gitignore` now excludes local worktree folders.

## Next Recommended Steps

1. Review `docs/ai-harness/10-rules.md` and `20-workflow.md`.
2. Confirm whether Antigravity needs a dedicated root config file or only this shared harness.
3. Create a Draft PR from `ai-harness-upgrade-20260625` after human review.
4. Do not push directly to `main`, `master`, or `develop`.

## Known Risks

- The repository has `.claude/settings.local.json`, but its content was not inspected to avoid local/private setting exposure.
- Slash-style branch prefixes failed locally, so the branch uses a slash-free name.
