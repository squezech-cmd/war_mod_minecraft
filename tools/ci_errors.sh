#!/usr/bin/env bash
# Fetches the newest check-run summary of the CI build (GitHub strips run logs for this sandbox,
# but the step summary is part of the check-run output and readable over the API).
set -euo pipefail
REPO=squezech-cmd/war_mod_minecraft
SHA=$(git rev-parse HEAD)
gh api "repos/$REPO/commits/$SHA/check-runs" -q '.check_runs[] | select(.name | test("build")) | .output.summary' 2>/dev/null | tail -70
