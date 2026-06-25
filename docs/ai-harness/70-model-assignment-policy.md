# Model Assignment Policy

Models are assigned by capability, not by vendor name.

## High Reasoning Model

Use for:

- Planner Agent
- SQL Agent
- Reviewer Agent
- Integrator Agent

Best for:

- architecture judgment
- conflict resolution
- security review
- performance review
- accounting correctness review
- large impact analysis

## Fast/Cheap Model

Use for:

- Explorer Agent
- Coder Agent
- Test Agent
- Documentation Agent

Best for:

- repetitive code search
- code draft within approved scope
- test draft
- documentation draft
- summarizing non-sensitive logs

## Local/Open Model

Use for:

- non-sensitive local code exploration
- simple refactoring candidate extraction
- log pattern grouping
- draft summaries that do not include secrets

Rules:

- Do not send data that is prohibited from external transfer.
- Do not process secrets, credentials, personal information, or production URLs.
- Escalate to a human when data classification is unclear.

초보자 설명: 모델 이름보다 "무슨 일을 맡길 수 있는 능력인가"가 중요하다. 어려운 판단은 높은 추론 모델, 반복 작업은 빠른 모델, 외부 전송 금지 데이터는 로컬 도구를 우선 사용한다.

