#!/bin/sh
set -eu

repo_root=$(pwd)
project_dir=$repo_root
ref_name=

if [ "$#" -gt 0 ] && git rev-parse -q --verify "$1^{commit}" >/dev/null 2>&1; then
    ref_name=$1
    safe_ref=$(printf '%s' "$ref_name" | tr '/:' '__')
    recording=${2:-target/pinning-$safe_ref.jfr}
    events=${3:-target/pinning-$safe_ref-events.txt}
    tmp_base=${TMPDIR:-/tmp}
    tmp_base=${tmp_base%/}
    project_dir=$(mktemp -d "$tmp_base/vithread-pinning.XXXXXX")

    git archive "$ref_name" | tar -x -C "$project_dir"
    cp "$repo_root/pom.xml" "$project_dir/pom.xml"
    echo "running checkpoint $ref_name in $project_dir"
else
    recording=${1:-target/pinning.jfr}
    events=${2:-target/pinning-events.txt}
fi

case "$recording" in
    /*) recording_path=$recording ;;
    *) recording_path=$repo_root/$recording ;;
esac

case "$events" in
    /*) events_path=$events ;;
    *) events_path=$repo_root/$events ;;
esac

mkdir -p "$(dirname "$recording_path")"
mkdir -p "$(dirname "$events_path")"

echo "recording JFR to $recording_path"

set +e
(
    cd "$project_dir" &&
    mvn -q -Dtest=PinningDetectionTest "-Dsurefire.extraArgLine=-XX:StartFlightRecording=filename=$recording_path,settings=profile,dumponexit=true" test
)
test_status=$?
set -e

if [ "$test_status" -ne 0 ]; then
    if [ -n "$ref_name" ]; then
        echo "PinningDetectionTest exited with status $test_status for $ref_name."
    else
        echo "PinningDetectionTest exited with status $test_status; this is expected on 04-pinning-start."
    fi
fi

if [ ! -f "$recording_path" ]; then
    echo "JFR recording was not created: $recording_path" >&2
    exit 1
fi

if command -v jfr >/dev/null 2>&1; then
    jfr print --events jdk.VirtualThreadPinned "$recording_path" > "$events_path" 2>/dev/null || true
    if [ -s "$events_path" ]; then
        echo "jdk.VirtualThreadPinned events:"
        cat "$events_path"
    else
        echo "No jdk.VirtualThreadPinned events printed by this JDK."
        echo "Recording kept at $recording_path for inspection in JDK Mission Control."
    fi
else
    echo "The jfr tool is not on PATH. Open $recording_path in JDK Mission Control."
fi

exit 0
