#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
. "$repo_root/scripts/jdk-utils.sh"

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <21|25> [command ...]" >&2
    exit 2
fi

requested_major=$1
shift

case "$requested_major" in
    21) configured_home=${JDK21_HOME:-} ;;
    25) configured_home=${JDK25_HOME:-} ;;
    *)
        echo "Unsupported workshop JDK: $requested_major (expected 21 or 25)" >&2
        exit 2
        ;;
esac

workshop_jdk_home=$(find_jdk "$requested_major" "$configured_home")
JAVA_HOME=$workshop_jdk_home
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

if [ "$#" -gt 0 ]; then
    exec "$@"
fi

workshop_shell=${SHELL:-/bin/sh}
echo "Opening a JDK $requested_major shell with JAVA_HOME=$JAVA_HOME"
echo "Run 'exit' to return to your previous shell."
"$JAVA_HOME/bin/java" -version
exec "$workshop_shell" -i
