#!/usr/bin/env bash
set -euo pipefail

# Deliberately requires visibility + authentication; never defaults to public.
if [[ $# -ne 2 || ( "$2" != "private" && "$2" != "public" ) ]]; then
  echo "Usage: $0 OWNER/REPO private|public" >&2
  echo "Example: $0 your-username/NovaFetch private" >&2
  exit 2
fi
repo="$1"
visibility="$2"
if [[ ! "$repo" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]]; then
  echo "Invalid OWNER/REPO" >&2; exit 2
fi
command -v gh >/dev/null || { echo "Install GitHub CLI (gh) first." >&2; exit 1; }
gh auth status >/dev/null || { echo "Run gh auth login first." >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo "Commit/stash changes before publishing." >&2; exit 1; }
[[ "$(git branch --show-current)" == "main" ]] || { echo "Checkout main before publishing." >&2; exit 1; }
if git remote get-url github >/dev/null 2>&1; then
  echo "Remote 'github' already exists; refusing to overwrite." >&2
  exit 1
fi
printf 'Creating %s as %s. This action publishes code. Continue? [y/N] ' "$repo" "$visibility"
read -r answer
[[ "$answer" == "y" || "$answer" == "Y" ]] || exit 0

gh repo create "$repo" "--$visibility" --source=. --remote=github --push
