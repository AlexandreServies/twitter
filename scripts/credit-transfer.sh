#!/usr/bin/env bash
#
# credit-transfer.sh
#
# Moves credits between two API-key accounts by calling the live relay
# service's /credits REST endpoint. No direct DynamoDB access is used --
# everything goes through the same authenticated HTTP API that clients use.
#
# The x-api-key header identifies the account whose balance is changed
# (it is the DynamoDB partition key), so each call authenticates *as* the
# account it modifies. Both keys below are configured in application.yml.
#
#   Add    -> POST   /credits   {"amount": N}   (returns {added, balance})
#   Remove -> DELETE /credits   {"amount": N}   (returns {removed, balance})
#
# Usage:
#   BASE_URL=https://<relay-host> ./scripts/credit-transfer.sh            # dry run: prints the plan only
#   BASE_URL=https://<relay-host> ./scripts/credit-transfer.sh --execute  # performs the transfer
#
# Optional overrides:
#   AMOUNT=2500000   # credits to move (default 2500000)
#
set -euo pipefail

# ----- configuration -------------------------------------------------------

BASE_URL="${BASE_URL:-}"
AMOUNT="${AMOUNT:-2500000}"

# Account that RECEIVES credits.
CREDIT_KEY="2e54206b02cb6ed997f42f5bb10d7d19ff59e80f19b334339d38d08a92fb1b71"  # AXIOM
CREDIT_LABEL="AXIOM"

# Account that LOSES credits.
DEBIT_KEY="7f3a91c8d4e6b2f0a5c7e9d1b3f8a2c4e6d0b5f7a9c3e1d5b7f2a8c0e4d6b9f1"   # AXIOM_PULSE
DEBIT_LABEL="AXIOM_PULSE"

EXECUTE=0
[[ "${1:-}" == "--execute" ]] && EXECUTE=1

# ----- validation ----------------------------------------------------------

if [[ -z "$BASE_URL" ]]; then
  echo "ERROR: BASE_URL is not set. Example:" >&2
  echo "  BASE_URL=https://relay.example.com $0 --execute" >&2
  exit 1
fi
BASE_URL="${BASE_URL%/}"  # strip trailing slash

if ! [[ "$AMOUNT" =~ ^[0-9]+$ ]] || [[ "$AMOUNT" -le 0 ]]; then
  echo "ERROR: AMOUNT must be a positive integer (got '$AMOUNT')." >&2
  exit 1
fi

# ----- plan ----------------------------------------------------------------

echo "Credit transfer plan"
echo "  endpoint : $BASE_URL/credits"
echo "  amount   : $AMOUNT"
echo "  +$AMOUNT -> $CREDIT_LABEL  ($CREDIT_KEY)"
echo "  -$AMOUNT <- $DEBIT_LABEL   ($DEBIT_KEY)"
echo

if [[ "$EXECUTE" -ne 1 ]]; then
  echo "Dry run only. Re-run with --execute to apply."
  exit 0
fi

# ----- helpers -------------------------------------------------------------

# call METHOD KEY -> prints the JSON response body; fails the script on HTTP >= 400
call() {
  local method="$1" key="$2" body http status
  body="$(printf '{"amount": %s}' "$AMOUNT")"
  # Capture body + trailing HTTP status code.
  local out
  out="$(curl -sS -w $'\n%{http_code}' -X "$method" \
    -H "x-api-key: $key" \
    -H "Content-Type: application/json" \
    -d "$body" \
    "$BASE_URL/credits")"
  status="${out##*$'\n'}"
  body="${out%$'\n'*}"
  echo "  HTTP $status  $body"
  if [[ "$status" -ge 400 ]]; then
    echo "ERROR: request failed (HTTP $status). Aborting." >&2
    exit 1
  fi
}

# ----- execute -------------------------------------------------------------

echo "Adding $AMOUNT credits to $CREDIT_LABEL ..."
call POST "$CREDIT_KEY"

echo "Removing $AMOUNT credits from $DEBIT_LABEL ..."
call DELETE "$DEBIT_KEY"

echo
echo "Done. Transfer of $AMOUNT credits from $DEBIT_LABEL to $CREDIT_LABEL complete."
