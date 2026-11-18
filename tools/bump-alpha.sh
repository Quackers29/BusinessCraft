#!/usr/bin/env bash
# Bump the alpha build number in gradle.properties: 0.9.0-alphaN -> 0.9.0-alpha(N+1).
# Run once on a PR branch right before it merges to main, and commit the result.
# A version without an alpha number (e.g. 0.9.0-beta) becomes <base>-alpha1.
set -euo pipefail
cd "$(dirname "$0")/.."
cur=$(sed -n 's/^mod_version=//p' gradle.properties)
if [[ $cur =~ ^(.*-alpha)([0-9]+)$ ]]; then
  new="${BASH_REMATCH[1]}$((BASH_REMATCH[2] + 1))"
else
  new="${cur%%-*}-alpha1"
fi
sed -i "s/^mod_version=.*/mod_version=${new}/" gradle.properties
echo "$cur -> $new"
