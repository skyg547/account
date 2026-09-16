#!/usr/bin/env python3
"""
Codex Continuous Issue Runner (Harness v2)
- Dynamic Model Tiering:
  * Default: gpt-5.6-sol (effort="high") for balanced/standard tasks
  * Escalation: gpt-6-astra (effort="xhigh") for difficulty:very-high or model:astra
- Circuit Breaker:
  * Halts runner immediately upon detecting OpenAI Usage Limit / Quota Exhaustion
  * Blocks an issue (status:blocked) if it fails 2 consecutive times
- Prioritization:
  * priority:p0 > p1 > p2, dev runtime/infra prioritized
"""

import argparse
import json
import os
import subprocess
import sys
import time
from datetime import datetime

REPO_ROOT = "/home/ho/dev/account"
LOG_FILE = os.path.join(REPO_ROOT, "logs/codex-auto-run.log")
MAX_CONSECUTIVE_FAILURES = 2

EFFORT_MAP = {
    "difficulty:very-high": "xhigh",
    "difficulty:high": "high",
    "difficulty:medium": "high",
    "difficulty:low": "high",
}

def log(msg):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    formatted = f"[{timestamp}] {msg}"
    print(formatted)
    os.makedirs(os.path.dirname(LOG_FILE), exist_ok=True)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(formatted + "\n")

def get_ready_issues(target_num=None):
    cmd = [
        "gh", "issue", "list",
        "--state", "open",
        "--label", "status:ready",
        "--label", "agent-loop",
        "--json", "number,title,labels,body"
    ]
    res = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    if res.returncode != 0:
        log(f"Failed to fetch issues: {res.stderr}")
        return []

    issues = json.loads(res.stdout)

    if target_num:
        return [iss for iss in issues if iss["number"] == target_num]

    # Priority sorting: p0 > p1 > p2, with dev server runtime/infra given top priority
    def priority_key(iss):
        labels = [l['name'] for l in iss['labels']]
        prio = 3
        if 'priority:p0' in labels: prio = 0
        elif 'priority:p1' in labels: prio = 1
        elif 'priority:p2' in labels: prio = 2

        title = iss.get('title', '').lower()
        is_dev_runtime = any(kw in title for kw in [
            '[runtime]', '[dev]', '[infra]', '개발서버', '실기동', '컨테이너', 'compose'
        ]) or any(l in labels for l in ['module:infra', 'type:test'])
        runtime_tier = 0 if is_dev_runtime else 1

        return (prio, runtime_tier)

    filtered = []
    for iss in issues:
        labels = [l['name'] for l in iss['labels']]
        # Skip only if Gemini is actively working on it
        if 'agent:gemini' in labels and 'status:in-progress' in labels:
            continue
        filtered.append(iss)

    filtered.sort(key=priority_key)
    return filtered

def determine_model_and_effort(issue):
    labels = [l['name'] for l in issue['labels']]

    # Model tiering: default is gpt-5.6-sol. Escalate to gpt-6-astra for very-high or explicit astra label.
    is_astra = (
        'difficulty:very-high' in labels
        or 'model:astra' in labels
        or 'model:gpt-6-astra' in labels
    )
    model = "gpt-6-astra" if is_astra else "gpt-5.6-sol"

    effort = "high"
    diff_label = "difficulty:standard"
    for l in labels:
        if l in EFFORT_MAP:
            effort = EFFORT_MAP[l]
            diff_label = l
            break

    if is_astra:
        effort = "xhigh"

    return model, effort, diff_label

def extract_modules(issue):
    labels = [l['name'] for l in issue['labels']]
    modules = [l.replace('module:', '') for l in labels if l.startswith('module:') and l != 'module:cross-module']
    is_cross = 'module:cross-module' in labels or 'scope:cross-module' in labels or len(modules) > 1
    return modules, is_cross

def block_issue(issue_num, reason):
    log(f"Circuit Breaker Triggered: Blocking Issue #{issue_num}. Reason: {reason}")
    cmd = [
        "gh", "issue", "edit", str(issue_num),
        "--remove-label", "status:in-progress,status:ready",
        "--add-label", "status:blocked"
    ]
    subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    comment_cmd = [
        "gh", "issue", "comment", str(issue_num),
        "--body", f"### [Circuit Breaker] Issue #{issue_num} 일시 차단\n\n- 사유: {reason}\n- 조치: 무한 재시도 및 토큰 소진 방지를 위해 `status:blocked`로 전환했습니다. 환경 또는 진단 재검토 후 `status:ready`로 재개하세요."
    ]
    subprocess.run(comment_cmd, cwd=REPO_ROOT, capture_output=True, text=True)

def run_issue(issue, dry_run=False):
    num = issue["number"]
    title = issue["title"]
    body = issue.get("body", "") or ""
    model, effort, diff_label = determine_model_and_effort(issue)
    modules, is_cross = extract_modules(issue)

    if is_cross:
        mod_summary = f"다중 모듈 연계 작업 ({', '.join(modules)})"
        scope_guide = f"- [모듈 스코프]: 다중 모듈 연동 ({', '.join(modules)})\n- [허용 범위(Allowlist)]: {', '.join(f'{m}/**' for m in modules)} (지정 모듈 외 수정 금지)\n- [검증 명령]: ./gradlew {' '.join(f':{m}:test' for m in modules)}"
    elif modules:
        target_mod = modules[0]
        mod_summary = f"단일 모듈 작업 ({target_mod})"
        scope_guide = f"- [모듈 스코프]: 단일 모듈 ({target_mod})\n- [허용 범위(Allowlist)]: {target_mod}/** (해당 모듈 디렉터리 내 헥사고날 수직 슬라이스 자유 수정/추가, 타 모듈 월경 금지)\n- [검증 명령]: ./gradlew :{target_mod}:test"
    else:
        mod_summary = "일반 작업"
        scope_guide = "- [모듈 스코프]: 이슈 본문에 명시된 대상 파일 Allowlist 준수"

    log(f"=== Starting Task #{num}: {title} [{mod_summary}] ===")
    log(f"Assigned Model: {model} | Difficulty: {diff_label} | Reasoning Effort: {effort}")

    if dry_run:
        log(f"[DRY-RUN] Task #{num} verified: model={model}, effort={effort}, scope={mod_summary}")
        return "DRY_RUN"

    prompt = f"""GitHub Issue #{num} ({title}) 작업을 $account-issue-loop, $account-hexagonal-change, $account-module-parallel 규약에 따라 진행해주세요.
- 작업 유형: {mod_summary}
- 모델/추론 등급: {model} (추론 강도: {effort})
{scope_guide}
- [사용자 전권 부여 / Full Access]: 사용자가 전체 액세스 권한(Full Authority)을 공식 부여했습니다. 지정된 모듈 디렉터리 내에서 이슈 해결 및 실기동/테스트 검증에 필요한 모든 관련 파일(빌드 스크립트, 설정, 소스 코드 등)을 능동적으로 수정하여 끝까지 완주하세요. (타 모듈 및 공용 하네스 문서는 수정 금지)
- 이슈 본문의 요구사항, 수용 조건(Acceptance Criteria), 검증 계획을 철저히 준수하세요.
- 전용 브랜치(agent/{num}-...) 및 /tmp 하위의 격리 워크트리를 생성하여 작업하세요.
- 검증 통과 후 Draft PR을 생성(Refs #{num})하고 AI harness(agent-status.md, worklog.md, handoff.md)를 갱신하세요.
- 완료 후 PR 본문에 검증 결과와 권한 분리 문구를 남겨주세요.

---
[Issue #{num} 원문 내용]
{body[:3500]}
"""

    cmd = [
        "codex", "exec",
        "-m", model,
        "-c", f"model_reasoning_effort=\"{effort}\"",
        "--sandbox", "danger-full-access",
        "--dangerously-bypass-approvals-and-sandbox",
        "--dangerously-bypass-hook-trust",
        prompt
    ]

    start_time = time.time()
    res = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    elapsed = time.time() - start_time

    output = res.stdout + "\n" + res.stderr
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(output + "\n")

    if "You've hit your usage limit" in output or "rate limit reached" in output.lower():
        log(f"OpenAI usage limit hit during Issue #{num}. Elapsed: {elapsed:.1f}s")
        return "RATE_LIMIT"

    if res.returncode == 0:
        log(f"=== Issue #{num} finished successfully in {elapsed:.1f}s ===")
        return "SUCCESS"
    else:
        log(f"=== Issue #{num} exited with code {res.returncode} in {elapsed:.1f}s ===")
        return "ERROR"

def main():
    parser = argparse.ArgumentParser(description="Codex Continuous Issue Runner (Harness v2)")
    parser.add_argument("--dry-run", action="store_true", help="Inspect and route candidate issues without invoking Codex")
    parser.add_argument("--list", action="store_true", help="List ready issues and assigned model tiers")
    parser.add_argument("--issue", type=int, help="Run a specific issue number")
    args = parser.parse_args()

    log("Codex Continuous Issue Runner (Harness v2) Started.")
    issues = get_ready_issues(target_num=args.issue)
    if not issues:
        log("No status:ready issues found for Codex.")
        return

    if args.list:
        log(f"Listing {len(issues)} ready issues:")
        for iss in issues:
            model, effort, diff = determine_model_and_effort(iss)
            print(f"  #{iss['number']}: {iss['title']} -> Model: {model} (effort: {effort}, {diff})")
        return

    log(f"Found {len(issues)} ready issues for Codex: {[i['number'] for i in issues]}")
    failure_counts = {}

    for iss in issues:
        num = iss["number"]
        status = run_issue(iss, dry_run=args.dry_run)

        if status == "RATE_LIMIT":
            log("Rate limit / Usage limit reached. Terminating runner immediately to preserve resources.")
            sys.exit(1)

        if status == "ERROR":
            failure_counts[num] = failure_counts.get(num, 0) + 1
            if failure_counts[num] >= MAX_CONSECUTIVE_FAILURES:
                block_issue(num, f"{MAX_CONSECUTIVE_FAILURES}회 연속 실행 실패로 인한 자동 차단")
        elif status == "SUCCESS":
            failure_counts.pop(num, 0)

        if not args.dry_run:
            time.sleep(5)

if __name__ == "__main__":
    main()
