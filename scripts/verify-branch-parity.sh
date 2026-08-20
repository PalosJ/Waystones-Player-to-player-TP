#!/usr/bin/env bash
set -euo pipefail

readonly MODE="${1:-}"
shift || true

readonly BASELINE_FILE="gradle/canonical-main.properties"
readonly -a SHARED_PATHS=(
  "core"
  "common"
  "gradle/targets.json"
  "scripts/verify-branch-parity.sh"
  "scripts/runtime-matrix.py"
  "scripts/runtime-smoke.py"
  "scripts/release-manifest.py"
  "scripts/verify-release-manifest.py"
  "scripts/fetch-build-artifact.sh"
  "scripts/tests"
  "scripts/verify-target-matrix.py"
  "README.md"
  "CONTEXT.md"
  "docs"
  "LICENSE"
  "TEMPLATE_LICENSE.txt"
  "THIRD_PARTY_NOTICES.md"
)
readonly -a UNIFIED_ADAPTER_PATHS=(
  "adapters/balm"
  "adapters/client-balm"
  "adapters/durability"
  "adapters/identifier"
  "adapters/network"
  "adapters/profile"
  "adapters/screen"
  "adapters/teleport"
)

fail() {
  echo "Branch parity check failed: $*" >&2
  exit 1
}

read_baseline_from_text() {
  local text="$1"
  local baseline
  baseline="$(printf '%s\n' "$text" | sed -n 's/^canonicalMainCommit=//p')"
  [[ "$baseline" =~ ^[0-9a-f]{40}$ ]] || fail "invalid canonicalMainCommit"
  printf '%s\n' "$baseline"
}

verify_baseline_commit() {
  local baseline="$1"
  git cat-file -e "${baseline}^{commit}" 2>/dev/null || fail "canonical commit ${baseline} is unavailable"
}

verify_branch_against_baseline() {
  local baseline="$1"
  local branch_ref="$2"
  git diff --quiet "$baseline" "$branch_ref" -- "${SHARED_PATHS[@]}" \
    || fail "${branch_ref} shared files differ from recorded canonical commit ${baseline}"
}

verify_unified_adapters() {
  [[ "$#" -eq 2 ]] || fail "adapters mode requires exactly two unified branch refs"
  local left="$1"
  local right="$2"
  git diff --quiet "$left" "$right" -- "${UNIFIED_ADAPTER_PATHS[@]}" \
    || fail "unified adapter source families differ between ${left} and ${right}"
}

case "$MODE" in
  main)
    [[ "$#" -gt 0 ]] || fail "main mode requires at least one unified branch ref"
    for branch_ref in "$@"; do
      baseline_text="$(git show "${branch_ref}:${BASELINE_FILE}" 2>/dev/null)" \
        || fail "${branch_ref} does not contain ${BASELINE_FILE}"
      baseline="$(read_baseline_from_text "$baseline_text")"
      verify_baseline_commit "$baseline"
      git merge-base --is-ancestor "$baseline" HEAD \
        || fail "${baseline} is not an ancestor of canonical main HEAD"
      git diff --quiet "$baseline" HEAD -- "${SHARED_PATHS[@]}" \
        || fail "canonical main shared files changed after ${branch_ref} recorded ${baseline}"
      verify_branch_against_baseline "$baseline" "$branch_ref"
      echo "Verified shared parity for ${branch_ref} at canonical ${baseline}."
    done
    ;;
  branch)
    [[ "$#" -eq 0 ]] || fail "branch mode accepts no branch refs"
    [[ -f "$BASELINE_FILE" ]] || fail "missing ${BASELINE_FILE}"
    baseline="$(read_baseline_from_text "$(sed -n '1,20p' "$BASELINE_FILE")")"
    verify_baseline_commit "$baseline"
    verify_branch_against_baseline "$baseline" HEAD
    echo "Verified current branch shared parity at canonical ${baseline}."
    ;;
  adapters)
    verify_unified_adapters "$@"
    echo "Verified adapter parity for $1 and $2."
    ;;
  *)
    fail "usage: $0 <main <branch-ref>... | branch | adapters <neo-ref> <fabric-ref>>"
    ;;
esac
