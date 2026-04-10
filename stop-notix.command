#!/bin/zsh
SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR" || exit 1

./scripts/stop-all-services.sh

echo
echo "Press any key to close this window."
read -k 1
