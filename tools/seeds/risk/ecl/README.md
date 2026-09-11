# ECL synthetic package

DB: `ecl`. Run `allowanceEclJob baseDate=2090-01-15 runId=GH690 modelVersion=GH690`. The input is an isolated copy of the contract fixture from `AllowanceEclBatchIntegrationTest`, using schema ECL V1–V4. No cross-database join is used.

One Stage 1 corporate exposure has drawn 1,000,000, undrawn 200,000, CCF .5, PD .01 and unsecured LGD .45. An inactive, unpledged collateral sample has appraisal 500,000 and does not reduce this exposure. Expected EAD is 1,100,000; probability-weighted ECL is `1,100,000 × .45 × .01 × (.8×.2 + 1×.6 + 1.3×.2) / 1.05 = 4808.5714`. The result query checks exact EAD, Stage, LGD and closing summary lineage/amount.

Runtime parameter keys and the corporate/unsecured segment are shared. Injection uses `ON CONFLICT DO NOTHING`; preflight rejects incompatible values instead of overwriting them. GH690 descriptions identify newly inserted shared rows for rollback. There must be exactly the three seeded macro scenarios for 2090. Read-only existing unrelated rows are never normalized by these files.

The ECL job synchronizes its snapshot into customer/account tables and calculates results. Rollback removes this reserved date's outputs before its fixture and only deletes shared parameters carrying GH690 ownership. Do not rollback ECL while closing still depends on its summary. A separately reserved date is necessary because the existing job replaces results for its entire base date.
