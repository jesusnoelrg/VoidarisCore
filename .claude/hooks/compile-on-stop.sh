#!/usr/bin/env bash
# Compiles at the end of a turn and feeds only the errors back to Claude.
input=$(cat)

# Avoid an endless loop when Claude already tried to fix the errors once.
if [ "$(jq -r '.stop_hook_active // false' <<<"$input")" = "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR" || exit 0
git status --porcelain | grep -qE '\.(java|kts|yml)$' || exit 0

out=$(./gradlew compileJava processResources -q 2>&1)
[ $? -eq 0 ] && exit 0

# Prefer javac errors (path relative to the project); fall back to Gradle's summary.
errors=$(grep -E '^/.*\.java:[0-9]+: error' -A2 <<<"$out" | sed "s|$CLAUDE_PROJECT_DIR/||" | head -20)
[ -z "$errors" ] && errors=$(grep -A6 'What went wrong' <<<"$out" | head -20)
[ -z "$errors" ] && errors=$(tail -n 20 <<<"$out")
printf 'La compilación falló:\n%s\n' "$errors" >&2
exit 2
