---
description: Iterative frontend feature development loop (Issue -> Spec -> Dev -> Commit/Push)
---

# Frontend Feature Loop

Follow this iterative loop for all frontend UI developments to maintain traceability, design consistency, and manageable PR sizes.

1. **GitHub Issue Creation**
   - Use `gh issue create` to create a tracking issue for the specific frontend feature or domain.
   - Example: `gh issue create --title "feat(ui): 기준정보(Master) 도메인 화면 구현" --body "계정과목 체계, 거래처 관리 등 기준정보 화면 구현"`

2. **Design Specification (MD)**
   - Before coding, write or update a Markdown (`.md`) specification document in `frontend/docs/` (e.g., `frontend-redesign-spec.md` or domain-specific spec).
   - Document the layout, mock data structure, and component hierarchy.
   - Link the MD file to the GitHub Issue number.

3. **Implementation**
   - Implement the feature in a single logical unit (e.g., one page or one domain).
   - Use the `USE_MOCK` pattern to decouple UI from backend API during initial development.
   - Ensure the design aligns with the established Glassmorphism and Tailwind v4 system.

4. **Commit and Push**
   - After completing the unit, stage the changes.
   - Commit with Conventional Commits, referencing the issue number (e.g., `feat(ui): implement account code tree page (#8)`).
   - Push to the current feature branch immediately to ensure remote sync.

5. **Repeat**
   - Move to the next unit and repeat steps 3-4, or if starting a new domain, repeat steps 1-4.
