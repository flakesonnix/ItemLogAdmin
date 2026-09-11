#!/bin/sh
# Minimal gradlew stub — delegates to system gradle or nix gradle
# Replace with full wrapper by running: gradle wrapper —gradle-version 8.10
set -e
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "gradle not found. Run: nix develop --command gradle \"\$@\"" >&2
  if command -v nix >/dev/null 2>&1; then
    exec nix develop --command gradle "$@"
  else
    echo "Install gradle or nix." >&2
    exit 1
  fi
fi
