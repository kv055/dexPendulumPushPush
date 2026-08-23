#!/usr/bin/env bash
# Block creating a merge commit while on the protected branch.
set -euo pipefail

branch=$(git rev-parse --abbrev-ref HEAD)

case "$branch" in
  main | master)
    cat >&2 <<EOF
✖ Local merges into '$branch' are blocked.
  Merge feature branches via a reviewed PR so CI runs first.
  (Override: git merge --no-verify … — use with care.)
EOF
    exit 1
    ;;
esac

exit 0
