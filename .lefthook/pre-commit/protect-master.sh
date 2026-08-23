#!/usr/bin/env bash
# Block creating direct commits on the protected branch.
set -euo pipefail

branch=$(git rev-parse --abbrev-ref HEAD)

case "$branch" in
  main | master)
    cat >&2 <<EOF
✖ Direct commits to '$branch' are blocked.
  Make changes on a feature/* branch, then open a PR into '$branch'
  so CI (lint + tests + assembleDebug) runs before merge.
  (Override: git commit --no-verify … — use with care.)
EOF
    exit 1
    ;;
esac

exit 0
