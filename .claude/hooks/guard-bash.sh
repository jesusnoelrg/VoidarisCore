#!/usr/bin/env bash
# Blocks Bash commands that would dump heavy, token-expensive paths into context.
cmd=$(jq -r '.tool_input.command // ""')

# Gradle tasks and javap signature lookups are allowed to reference these paths.
if grep -qE '(^|[;&|[:space:]])(\./gradlew|javap)[[:space:]]' <<<"$cmd"; then
  exit 0
fi

# Reading the tail of a log is fine; dumping the whole file is not.
if grep -qE '^tail -n [0-9]{1,3} [^;&]+\.log' <<<"$cmd"; then
  exit 0
fi

if grep -qE '(^|[[:space:]/])(run|build|\.gradle|\.idea)/|\.jar\b|latest\.log' <<<"$cmd"; then
  echo "Bloqueado: no leas run/, build/, .gradle/, .idea/, jars ni logs completos. Para firmas de la API usa 'javap -cp <jar> <clase>'. Para un log usa: tail -n 50 run/logs/latest.log | grep -iE 'error|exception|voidaris'" >&2
  exit 2
fi

exit 0
