# AI Harness Rules

## Branch And Review Rules

- All AI work must happen on a separate feature branch, agent branch, AI branch, or fix branch.
- Recommended branch prefixes:
  - `agent/`
  - `ai/`
  - `feature/`
  - `fix/`
- Do not push directly to `main`, `master`, or `develop`.
- Use Draft MR/PR for review. Do not treat direct commits on a base branch as the normal integration path.
- Do not mark merge complete without tests or an explicitly documented reason why tests could not run.
- 테스트 없이 merge 완료로 판단하지 않는다. 테스트를 못 돌렸으면 이유, 영향, 대체 검증을 기록한다.

초보자 설명: 기준 브랜치는 모두가 공유하는 원본이다. AI가 바로 밀어 넣으면 다른 사람 작업까지 깨질 수 있으므로, 먼저 작업 브랜치에서 검증하고 Draft PR로 리뷰를 받는다.

## Security Rules

- Do not read or print production/development DB credentials, passwords, tokens, personal information, private keys, or production URLs.
- 운영/개발 DB 접속정보, 비밀번호, 토큰, 개인정보, 운영 URL은 읽거나 출력하지 않는다.
- Do not paste secrets into worklogs, handoffs, commit messages, PR bodies, or chat.
- If a task needs secret-backed access, ask the user for an approved safe path instead of inspecting local secret files.

## Dangerous Command Rules

User approval is required before running:

- `rm -rf`
- `chmod -R`
- `curl | bash`
- external script execution
- package installation or dependency download
- destructive git operations
- permission changes

## Architecture Rules

- Preserve existing architecture and layer boundaries.
- Keep hexagonal architecture: inbound adapter, application service/use case, domain, outbound port, infrastructure adapter.
- Do not mix responsibilities across Controller, Service, ServiceImpl, DAO, Query XML, Mapper XML, Repository, DTO, VO, and domain model.
- Batch modules are orchestrators. Business rules, formulas, and state changes belong in core/application/domain.
- Large refactors must be split into a separate branch and reviewed independently.

## Completion Rules

Every meaningful change must document:

- changed files
- impact scope
- test method and result
- rollback method
- remaining risks

Also update:

- `docs/ai-harness/worklog.md`
- `docs/ai-harness/agent-status.md`
- `docs/ai-harness/handoff.md`
- `docs/ai-harness/conflict-log.md` when conflict occurs
- the agent-specific worklog when applicable

## Commit Rules

- Do not commit with conflict markers.
- Check for conflict markers before commit:

```powershell
rg -n "<<<<<<<|=======|>>>>>>>" .
```

- Do not commit unrelated generated logs, local settings, secrets, or temporary worktree folders.
