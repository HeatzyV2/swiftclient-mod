#!/usr/bin/env bash
# Builds a release of the mod for every Minecraft version, and publishes it on GitHub only when asked.
#
#   tools/release-all.sh 2.7.0                       bump to 2.7.0, test + build every version, collect the jars
#   tools/release-all.sh 2.7.0 --audit               same, then launch every version and check its mixins
#   tools/release-all.sh 2.7.0 --publish notes.md    same, then create the GitHub release v2.7.0 with every jar
#   tools/release-all.sh --publish notes.md          publish the version already in stonecutter.properties.toml
#
# Jars: build/libs/<version>/swiftclient-mod-<version>+<minecraft>.jar, plus swiftclient-mod-<version>.jar
# (the 26.2 build under its old name, for launchers that predate the multi-version releases).
# Publishing needs the GitHub CLI (gh) logged in, a clean git tree and the version commit pushed: the tag is
# created at the exact tested commit, after every artifact has been uploaded to a draft.
set -euo pipefail
cd "$(dirname "$0")/.."

PROPS=stonecutter.properties.toml
REPO=HeatzyV2/swiftclient-mod
# The Minecraft version of Swift instances (launcher: SWIFT_MC_VERSION), shipped under the legacy name too.
LEGACY_MC=26.2

version=""
audit=false
notes=""
while [ $# -gt 0 ]; do
  case "$1" in
    --audit) audit=true ;;
    --publish) notes="${2:?--publish needs a release notes file}"; shift ;;
    -*) echo "unknown option $1" >&2; exit 2 ;;
    *) version="$1" ;;
  esac
  shift
done

current=$(sed -nE 's/^mod\.version = "([^"]+)"/\1/p' "$PROPS")
if [ -n "$version" ] && [ "$version" != "$current" ]; then
  sed -i -E "s/^mod\.version = \"[^\"]+\"/mod.version = \"$version\"/" "$PROPS"
  echo "mod.version: $current -> $version"
else
  version="$current"
fi

echo "== Building and testing Swift Client mod $version for every Minecraft version"
./gradlew buildAll --console=plain

out="build/libs/$version"
cp "$out/swiftclient-mod-$version+$LEGACY_MC.jar" "$out/swiftclient-mod-$version.jar"
ls -1 "$out"

if $audit; then
  echo "== Launching every version to check the mixins"
  tools/audit-mixins.sh all
fi

if [ -n "$notes" ]; then
  [ -f "$notes" ] || { echo "notes file $notes not found" >&2; exit 2; }
  if [ -n "$(git status --porcelain)" ]; then
    echo "The git tree has uncommitted changes: commit (and push) the release first." >&2
    exit 1
  fi
  commit=$(git rev-parse HEAD)
  git fetch origin main
  git merge-base --is-ancestor "$commit" origin/main || {
    echo "Push the tested commit to origin/main before publishing." >&2
    exit 1
  }
  for mc in $(grep -oE 'versions\(([^)]*)\)' settings.gradle.kts | grep -oE '"[^"]+"' | tr -d '"'); do
    test -s "$out/swiftclient-mod-$version+$mc.jar"
  done
  (cd "$out" && sha256sum *.jar > SHA256SUMS.txt)
  echo "== Publishing v$version on $REPO"
  # The untagged 26.2 jar goes first: older launchers take the first swiftclient-mod-*.jar of a release.
  gh release create "v$version" "$out/swiftclient-mod-$version.jar" --repo "$REPO" \
    --title "Swift Client mod $version" --notes-file "$notes" --target "$commit" --draft
  gh release upload "v$version" "$out"/swiftclient-mod-"$version"+*.jar --repo "$REPO"
  gh release upload "v$version" "$out/SHA256SUMS.txt" --repo "$REPO"
  gh release edit "v$version" --draft=false --latest --repo "$REPO"
  echo "Published: https://github.com/$REPO/releases/tag/v$version"
fi
