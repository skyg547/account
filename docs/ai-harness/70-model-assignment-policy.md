# Model Assignment Policy

Assign models by capability and task risk, not by vendor name. Project custom agent files specify reasoning effort only and inherit the available parent model.

## High Reasoning Capacity

Use for:

- Planner, SQL, Reviewer, and advisory Integrator roles;
- architecture or cross-module changes;
- financial calculations and state transitions;
- conflict resolution, security, accounting correctness, and performance review;
- batch restartability, partitioning, and high-volume persistence decisions.

## Balanced Capacity

Use for:

- bounded Coder, Controller, Service, Batch, Test, and Documentation work with clear acceptance criteria;
- implementation inside an explicit allowlist;
- focused test and documentation updates.

Escalate a normally balanced role to high reasoning when ambiguity, financial risk, concurrency, migration risk, or broad blast radius is present.

## Fast/Low-Cost Capacity

Use for:

- read-heavy exploration and candidate-file searches;
- repetitive summaries of non-sensitive code or logs;
- mechanical draft generation that will receive independent review.

Do not use lower-capacity execution for high-risk core logic solely because the role is named Coder.

## Local/Open Capacity

Use for non-sensitive local exploration, simple refactoring candidates, log pattern grouping, and draft summaries.

- Never process secrets, credentials, personal data, or production URLs.
- Do not send data prohibited from external transfer.
- Escalate to a human when data classification is unclear.
