#!/usr/bin/env zsh
# Runs the eval. Free and offline by default (stub). Example:
#   zsh -ic './scripts/eval.sh'
#   zsh -ic './scripts/eval.sh --client anthropic --confirm-spend'   # real model, costs money
set -e
cd "$(dirname "$0")/.."

# Load local secrets (.env) without printing them.
if [ -f .env ]; then set -a; source .env; set +a; fi

mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
exec java -cp "target/classes:$(cat target/classpath.txt)" com.disputeintake.eval.EvalRunner "$@"
