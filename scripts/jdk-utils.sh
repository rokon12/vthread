#!/bin/sh

java_major() {
    "$1/bin/java" -XshowSettings:properties -version 2>&1 \
        | awk -F '= ' '/java.specification.version/ {print $2; exit}'
}

valid_jdk() {
    candidate=$1
    expected_major=$2
    [ -n "$candidate" ] \
        && [ -x "$candidate/bin/java" ] \
        && [ -x "$candidate/bin/javac" ] \
        && [ -x "$candidate/bin/jfr" ] \
        && [ "$(java_major "$candidate")" = "$expected_major" ]
}

find_jdk() {
    expected_major=$1
    configured_home=$2

    if [ -n "$configured_home" ]; then
        if valid_jdk "$configured_home" "$expected_major"; then
            printf '%s\n' "$configured_home"
            return
        fi
        echo "JDK${expected_major}_HOME is not a complete JDK $expected_major: $configured_home" >&2
        exit 1
    fi

    if valid_jdk "${JAVA_HOME:-}" "$expected_major"; then
        printf '%s\n' "$JAVA_HOME"
        return
    fi

    if [ -x /usr/libexec/java_home ]; then
        candidate=$(/usr/libexec/java_home -v "$expected_major" 2>/dev/null || true)
        if valid_jdk "$candidate" "$expected_major"; then
            printf '%s\n' "$candidate"
            return
        fi
    fi

    sdkman_root=${SDKMAN_DIR:-${HOME:-}/.sdkman}
    for candidate in "$sdkman_root"/candidates/java/*; do
        if valid_jdk "$candidate" "$expected_major"; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    for candidate in /usr/lib/jvm/*; do
        if valid_jdk "$candidate" "$expected_major"; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    echo "Could not find JDK $expected_major." >&2
    echo "Set JDK${expected_major}_HOME to its installation directory and try again." >&2
    exit 1
}
