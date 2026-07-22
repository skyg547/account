# Decision Log

| Date | Decision | Reason | Owner |
| --- | --- | --- | --- |
| 2026-06-25 | Keep existing `Agents.md` instead of creating separate `AGENTS.md`. | Repository and Codex rules already use `Agents.md`; on Windows a second case-only filename is unsafe. | Codex |
| 2026-06-25 | Use capability-based model assignment. | User requested no vendor-locked model names. | Codex |
| 2026-06-25 | Use `ai-harness-upgrade-20260625` branch. | Slash branch prefixes failed in the local Git ref layout; base branch direct push remains prohibited. | Codex |


| 2026-07-14 | Canonicalize the root guide as `AGENTS.md` and move repeatable procedures to repository skills and custom agents. | Current Codex discovery requires uppercase `AGENTS.md`; progressive disclosure reduces always-loaded context. This supersedes the 2026-06-25 filename decision. | Codex |
| 2026-07-14 | Keep the custom Integrator agent read-only and advisory. | One parent Integrator must own conflict edits, shared logs, Git, PR, and Issue mutations to prevent parallel state races. | Codex |
