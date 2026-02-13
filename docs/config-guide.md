# Role-Based Configuration Guide (Cline-Style)

This document explains how to configure Antigravity to act with specific roles and sequential tasks, similar to Cline.

## 1. Role Definitions (Personas)
**File**: [.clinerules](file:///c:/Users/gktjd/IdeaProjects/account/.clinerules) / [docs/GEMINI_SKILL.md](file:///c:/Users/gktjd/IdeaProjects/account/docs/GEMINI_SKILL.md)

- Add new personas under the `### 👥 1. 전문가 페르소나 및 R&R` section.
- Define specific responsibilities (R&R) and tools each role should prioritize.

## 2. Automated Workflows
**Directory**: [.agent/workflows/](file:///c:/Users/gktjd/IdeaProjects/account/.agent/workflows/)

- Create `.md` files in this directory to define sequential steps.
- Use the standard markdown list format for steps.
- These can be executed as holistic missions.

## 3. MCP (Model Context Protocol)
Antigravity supports MCP through server-side integration.
- **Tools**: `list_resources`, `read_resource`.
- **Configuration**: MCP servers are typically configured in the system environment or via `.vscode/settings.json` (if using specific extensions).
- **Verification**: Call `list_resources` with a `ServerName` to see what a specific server provides.

## 4. Skill Settings
**Files**: `**/SKILL.md`

- Place `SKILL.md` files in domain folders to provide localized "expert knowledge".
- Antigravity 자동으로 해당 디렉토리 상의 `SKILL.md`를 인지하여 해당 도메인 전문가로 동작합니다.
