#!/bin/sh
set -eu

required_java=25
failed=0

check_command() {
    if command -v "$1" >/dev/null 2>&1; then
        echo "ok: found $1"
    else
        echo "missing: $1" >&2
        failed=1
    fi
}

check_command java
check_command mvn
check_command git

if command -v java >/dev/null 2>&1; then
    java_spec=$(java -XshowSettings:properties -version 2>&1 | awk -F '= ' '/java.specification.version/ {print $2; exit}')
    if [ "$java_spec" = "$required_java" ]; then
        echo "ok: Java specification version $java_spec"
    else
        echo "warning: Java specification version is $java_spec; this lab expects $required_java" >&2
        failed=1
    fi
fi

if [ "$failed" -ne 0 ]; then
    echo "doctor check failed" >&2
    exit 1
fi

mvn -q -DskipTests compile
echo "doctor check passed"
