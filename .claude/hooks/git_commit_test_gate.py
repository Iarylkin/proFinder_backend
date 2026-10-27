#!/usr/bin/env python3
"""PreToolUse/Bash hook: gate `git commit` behind a consistent staged state.

Blocking checks (run only when the bash command invokes `git commit`):
  1. Every staged .json file parses.
  2. If docker-compose.yml is staged and a working docker-compose/docker
     compose binary is available in this environment, `... config` succeeds.
  3. `mvn -B verify` passes (tests + checkstyle, same order as CI).

Non-blocking: if changed code or a known infra file has no matching docs/
file staged in the same commit, print a reminder. Never blocks the commit -
docs/code sync needs a human judgment call, not an automatic gate.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

INFRA_DOC_MAP = {
    "docker-compose.yml": ["docs/3 - System Design.md"],
    "src/main/resources/application.properties": [
        "docs/3 - System Design.md",
        "docs/6 - Database Schema.md",
    ],
}


def staged_files(repo_root=REPO_ROOT):
    out = subprocess.run(
        ["git", "diff", "--cached", "--name-status"],
        cwd=repo_root, capture_output=True, text=True,
    ).stdout
    files = []
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) >= 2 and parts[0] != "D":
            files.append(parts[-1])
    return files


def check_json(files, repo_root=REPO_ROOT):
    errors = []
    for f in files:
        if f.endswith(".json"):
            result = subprocess.run(
                ["git", "show", f":{f}"], cwd=repo_root, capture_output=True, text=True,
            )
            try:
                json.loads(result.stdout)
            except json.JSONDecodeError as e:
                errors.append(f"{f}: invalid JSON ({e})")
    return errors


def check_docker_compose(files, repo_root=REPO_ROOT):
    if not any(Path(f).name == "docker-compose.yml" for f in files):
        return [], None
    for binary in (["docker-compose"], ["docker", "compose"]):
        try:
            probe = subprocess.run(binary + ["version"], capture_output=True, text=True)
        except FileNotFoundError:
            continue
        if probe.returncode == 0:
            result = subprocess.run(
                binary + ["config"], cwd=repo_root, capture_output=True, text=True,
            )
            if result.returncode != 0:
                return [f"docker-compose.yml: {result.stderr.strip()[-1000:]}"], None
            return [], None
    return [], "docker-compose/docker compose not usable in this environment - syntax check skipped"


def leading_word(java_basename):
    name = java_basename[: -len(".java")]
    m = re.match(r"^[A-Z][a-z0-9]*", name)
    return m.group(0) if m else name


def docs_warning(files, repo_root=REPO_ROOT):
    if any(f.startswith("docs/") for f in files):
        return None

    docs_dir = repo_root / "docs"
    doc_files = list(docs_dir.glob("*.md")) if docs_dir.is_dir() else []
    lines = []

    for f in files:
        is_domain_java = (
            f.startswith("src/main/java/fit/biejk/") or f.startswith("src/test/java/fit/biejk/")
        ) and f.endswith(".java")
        if is_domain_java:
            word = leading_word(Path(f).name)
            matches = []
            for doc in doc_files:
                try:
                    text = doc.read_text(encoding="utf-8", errors="ignore")
                except OSError:
                    continue
                if re.search(re.escape(word), text, re.IGNORECASE):
                    matches.append(f"docs/{doc.name}")
            if matches:
                lines.append(f"  - {f} (entity: {word}) -> possibly relevant: {', '.join(matches)}")
            else:
                lines.append(f"  - {f} (entity: {word}) -> no matching docs/ file found")
        elif f in INFRA_DOC_MAP:
            lines.append(f"  - {f} -> possibly relevant: {', '.join(INFRA_DOC_MAP[f])}")

    if not lines:
        return None

    return (
        "Commit changes code/infra without touching docs/:\n"
        + "\n".join(lines)
        + "\nConsider reviewing these docs or running /sync-docs."
    )


def deny(reason):
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }))


def allow(message=None):
    payload = {
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "allow",
        }
    }
    if message:
        payload["systemMessage"] = message
    print(json.dumps(payload))


def main():
    data = json.load(sys.stdin)
    command = data.get("tool_input", {}).get("command", "")

    if not re.search(r"(^|[;&|]|\s)git\s+commit(\s|$)", command):
        return

    files = staged_files()

    errors = check_json(files)
    dc_errors, dc_notice = check_docker_compose(files)
    errors += dc_errors

    if errors:
        deny("Pre-commit checks failed:\n" + "\n".join(errors))
        return

    result = subprocess.run(["mvn", "-B", "verify"], cwd=REPO_ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        tail = (result.stdout + result.stderr)[-4000:]
        deny("mvn verify failed, commit blocked:\n" + tail)
        return

    parts = []
    if dc_notice:
        parts.append(dc_notice)
    warning = docs_warning(files)
    if warning:
        parts.append(warning)

    allow("\n\n".join(parts) if parts else None)


if __name__ == "__main__":
    main()
