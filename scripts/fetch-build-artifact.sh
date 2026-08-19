#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 5 ]]; then
  echo "usage: $0 <branch> <requested-run-id-or-empty> <artifact-name> <artifact-file> <output-directory>" >&2
  exit 2
fi

branch=$1
requested_run_id=$2
artifact_name=$3
artifact_file=$4
output_directory=$5
expected_workflow='Build'

if [[ -z ${GH_TOKEN:-} ]]; then
  echo "GH_TOKEN is required to resolve and download Build artifacts." >&2
  exit 2
fi

head_sha=$(git rev-parse HEAD)
run_id=$requested_run_id
if [[ -z $run_id ]]; then
  if [[ ${GITHUB_EVENT_NAME:-} == workflow_dispatch ]]; then
    echo "Manual runtime runs must provide the matching Build run ID." >&2
    exit 2
  fi
  run_id=$(gh run list \
    --workflow build.yml \
    --branch "$branch" \
    --commit "$head_sha" \
    --status success \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId // empty')
fi

if [[ -z $run_id ]]; then
  echo "No successful Build run with an available artifact was found for $branch at $head_sha." >&2
  exit 1
fi

run_head_sha=$(gh run view "$run_id" --json headSha --jq '.headSha')
run_status=$(gh run view "$run_id" --json status --jq '.status')
run_conclusion=$(gh run view "$run_id" --json conclusion --jq '.conclusion')
run_workflow=$(gh run view "$run_id" --json workflowName --jq '.workflowName')
run_event=$(gh run view "$run_id" --json event --jq '.event')
run_branch=$(gh run view "$run_id" --json headBranch --jq '.headBranch')
if [[ $run_workflow != "$expected_workflow" || $run_status != completed || $run_conclusion != success ]]; then
  echo "Run $run_id is not a completed successful $expected_workflow workflow run." >&2
  exit 1
fi
if [[ $run_event != push && $run_event != workflow_dispatch ]]; then
  echo "Run $run_id came from untrusted or unsupported event $run_event." >&2
  exit 1
fi
if [[ $run_branch != "$branch" ]]; then
  echo "Run $run_id belongs to branch $run_branch, expected $branch." >&2
  exit 1
fi
if [[ $run_head_sha != "$head_sha" ]]; then
  echo "Run $run_id was built from $run_head_sha, expected checked-out commit $head_sha." >&2
  exit 1
fi

mkdir -p "$output_directory"
gh run download "$run_id" --name "$artifact_name" --dir "$output_directory"
binary_path="$output_directory/$artifact_file"
if [[ ! -f $binary_path ]]; then
  echo "Build artifact $artifact_name did not contain $artifact_file." >&2
  exit 1
fi
binary_sha256=$(sha256sum "$binary_path" | awk '{print $1}')

if [[ -n ${GITHUB_OUTPUT:-} ]]; then
  {
    echo "run_id=$run_id"
    echo "binary=$binary_path"
    echo "sha256=$binary_sha256"
    echo "commit=$head_sha"
  } >> "$GITHUB_OUTPUT"
else
  printf 'run_id=%s\nbinary=%s\nsha256=%s\ncommit=%s\n' \
    "$run_id" "$binary_path" "$binary_sha256" "$head_sha"
fi
