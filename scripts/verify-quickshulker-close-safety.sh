#!/usr/bin/env bash
set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
quick_shulker="$repo_root/src/main/java/me/aleksilassila/litematica/printer/utils/QuickShulkerUtils.java"

require_text() {
    local file=$1
    local text=$2
    if ! grep -Fq "$text" "$file"; then
        echo "Missing QuickShulker close-safety contract: $text" >&2
        exit 1
    fi
}

reject_text() {
    local file=$1
    local text=$2
    if grep -Fq "$text" "$file"; then
        echo "Unsafe QuickShulker close fallback found: $text" >&2
        exit 1
    fi
}

require_text "$quick_shulker" "player.containerMenu != deferredCloseContainer"
require_text "$quick_shulker" "deferredCloseContainer.getCarried().isEmpty()"
require_text "$quick_shulker" "if (deferredCloseContainer != null) return;"
require_text "$quick_shulker" "!inventoryTransferPerformed && container.getCarried().isEmpty()"
reject_text "$quick_shulker" "MAX_DEFERRED_CLOSE_RETRIES"
reject_text "$quick_shulker" "deferredCloseRetries"

close_count=$(grep -Fc "player.closeContainer();" "$quick_shulker")
if [[ $close_count -ne 2 ]]; then
    echo "Expected exactly two guarded shulker close sites, found $close_count" >&2
    exit 1
fi

if ! awk '
    /player\.closeContainer\(\);/ && index(previous, "getCarried().isEmpty()") == 0 { exit 1 }
    { previous = $0 }
' "$quick_shulker"; then
    echo "Every shulker close site must be directly guarded by an empty carried stack" >&2
    exit 1
fi

handler_clear_count=$(grep -Ec '^[[:space:]]*isOpenHandler = false;' "$quick_shulker")
if [[ $handler_clear_count -ne 2 ]]; then
    echo "Pending shulker closes must retain the open-handler lock" >&2
    exit 1
fi

echo "QuickShulker close-safety source contract passed"
