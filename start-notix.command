#!/bin/zsh
SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR" || exit 1

./scripts/start-all-services.sh --tail

echo
echo "Press any key to close this window."
read -k 1
