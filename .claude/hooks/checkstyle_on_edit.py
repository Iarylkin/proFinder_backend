#!/usr/bin/env python3
"""PostToolUse/Edit|Write hook: run checkstyle after a .java file is touched."""
import json
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

data = json.load(sys.stdin)
file_path = (
    data.get("tool_input", {}).get("file_path")
    or data.get("tool_response", {}).get("filePath")
    or ""
)

if not file_path.endswith(".java"):
    sys.exit(0)

result = subprocess.run(
    ["mvn", "-B", "checkstyle:checkstyle", "--fail-at-end"],
    cwd=REPO_ROOT,
    capture_output=True,
    text=True,
)
errors = [line for line in result.stdout.splitlines() if "ERROR" in line]
if errors:
    print("\n".join(errors))
