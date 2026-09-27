#!/usr/bin/env sh
set -eu
if [ "$#" -ne 3 ]; then
  echo "usage: $0 <patient-id> <user-id> <credential-id>" >&2
  exit 2
fi
BUILD_DIR="${TMPDIR:-/tmp}/patient-account-erasure-run"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java -name '*.java' -print)
java -cp "$BUILD_DIR" dev.healthtech.deletion.DeletionRunner "$1" "$2" "$3"
