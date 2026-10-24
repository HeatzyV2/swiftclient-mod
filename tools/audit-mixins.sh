#!/usr/bin/env bash
# Launches the game for one or more Minecraft versions, applies every mixin (-PauditMixins), closes the game
# and reports the mixin problems found in the log.
#   tools/audit-mixins.sh 26.2            one version
#   tools/audit-mixins.sh 1.21.4 26.3     several
#   tools/audit-mixins.sh all             every version of settings.gradle.kts
set -uo pipefail
cd "$(dirname "$0")/.."

versions=("$@")
if [ ${#versions[@]} -eq 0 ] || [ "${versions[0]}" = "all" ]; then
  mapfile -t versions < <(grep -oE 'versions\(([^)]*)\)' settings.gradle.kts | grep -oE '"[^"]+"' | tr -d '"')
fi

failed=()
mkdir -p build
for mc in "${versions[@]}"; do
  if [ "$mc" = "26.2" ]; then dir=run; else dir="run/$mc"; fi
  echo "=== $mc (log: $dir/logs/latest.log)"
  touch "build/audit-$mc.txt.start"
  ./gradlew ":$mc:runClient" -PauditMixins -PauditExit --console=plain -q > "build/audit-$mc.txt" 2>&1
  log="$dir/logs/latest.log"
  if [ ! -f "$log" ] || [ ! "$log" -nt "build/audit-$mc.txt.start" ]; then
    echo "No fresh game log: see build/audit-$mc.txt"
    failed+=("$mc")
    continue
  fi
  # Mixin problems: failed mixins and injections (one line each, without the log prefix), then the verdict.
  problems=$(grep -E "Mixin apply for mod swiftclient failed|Critical injection failure|Audit des mixins en echec" "$log" 2>/dev/null     | sed -E 's/^\[[0-9:]+\] \[[^]]*\] \([^)]*\) //' | cut -c1-400)
  if grep -q "Audit des mixins termine sans erreur" "$log" 2>/dev/null && [ -z "$problems" ]; then
    echo "OK"
  else
    [ -n "$problems" ] && echo "$problems" | head -40
    if ! grep -q "Audit des mixins" "$log" 2>/dev/null; then
      echo "(no audit line: the game did not reach the first tick, see build/audit-$mc.txt and $log)"
      crash=$(ls -t "$dir"/crash-reports/*.txt 2>/dev/null | head -1)
      [ -n "$crash" ] && [ "$crash" -nt "build/audit-$mc.txt.start" ] && grep -E "^(Caused by|Description|java\.)" "$crash" | head -8
    fi
    failed+=("$mc")
  fi
done

if [ ${#failed[@]} -gt 0 ]; then
  echo "Mixin problems on: ${failed[*]}"
  exit 1
fi
echo "Every mixin applied on: ${versions[*]}"
