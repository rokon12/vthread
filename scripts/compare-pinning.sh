#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
classes_dir=$repo_root/target/pinning-demo/classes
source_file=$repo_root/src/main/java/demo/pinning/PinningDemo.java
. "$repo_root/scripts/jdk-utils.sh"

run_demo() {
    runtime_major=$1
    jdk_home=$2
    recording=$repo_root/target/pinning-jdk$runtime_major.jfr
    events=$repo_root/target/pinning-jdk$runtime_major-events.txt
    output=$repo_root/target/pinning-jdk$runtime_major-output.txt

    rm -f "$recording" "$events" "$output"

    echo
    echo "=== JDK $runtime_major ==="
    "$jdk_home/bin/java" -version 2>&1 | sed -n '1p'

    "$jdk_home/bin/java" \
        -Djdk.tracePinnedThreads=short \
        -Djdk.virtualThreadScheduler.parallelism=1 \
        -Djdk.virtualThreadScheduler.maxPoolSize=1 \
        "-XX:StartFlightRecording=filename=$recording,settings=profile,dumponexit=true" \
        -cp "$classes_dir" \
        demo.pinning.PinningDemo > "$output" 2>&1

    cat "$output"
    "$jdk_home/bin/jfr" print --events jdk.VirtualThreadPinned "$recording" \
        > "$events" 2>/dev/null || true

    pinned_count=$(awk '/^jdk\.VirtualThreadPinned \{/ {count++} END {print count + 0}' "$events")
    elapsed=$(awk -F 'elapsedMs=' '/tasks=/ {print $2; exit}' "$output")
    printf 'summary: elapsedMs=%s, jdk.VirtualThreadPinned events=%s\n' "$elapsed" "$pinned_count"
}

jdk21_home=$(find_jdk 21 "${JDK21_HOME:-}")
jdk25_home=$(find_jdk 25 "${JDK25_HOME:-}")

mkdir -p "$classes_dir"
rm -rf "$classes_dir"
mkdir -p "$classes_dir"

echo "Compiling the demo once with JDK 21 (--release 21)."
"$jdk21_home/bin/javac" --release 21 -d "$classes_dir" "$source_file"

run_demo 21 "$jdk21_home"
run_demo 25 "$jdk25_home"

echo
echo "The source and bytecode were identical in both runs."
echo "JDK 21 pins the carrier during the synchronized blocking call."
echo "JDK 25 removes monitor pinning, but Exercise 4 still has a separate lock-serialization bug."
echo "Recordings and extracted events are under target/pinning-jdk*."
