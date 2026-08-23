#!/usr/bin/env bash
# Block pushing to the protected branch from a dev machine.
# Git feeds pre-push one line per ref on stdin:
#   <local ref> <local sha> <remote ref> <remote sha>
set -euo pipefail

blocked=0
while read -r _local_ref _local_sha remote_ref _remote_sha; do
  case "$remote_ref" in
    refs/heads/main | refs/heads/master)
      blocked=1
      ;;
  esac
done

if [ "$blocked" -eq 1 ]; then
  cat >&2 <<'EOF'
✖ Direct pushes to 'main'/'master' are blocked.
  Push your feature/* branch and open a PR instead.
  (Override: git push --no-verify … — use with care.)
EOF
  exit 1
fi

exit 0
