#!/usr/bin/env bash
set -euo pipefail

CHART_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$CHART_DIR/../../.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"

[[ -f "$ENV_FILE" ]] || { echo "error: $ENV_FILE not found" >&2; exit 1; }

# --- ELASTIC_PASSWORD ---
ELASTIC_PASSWORD="$(grep -E '^ELASTIC_PASSWORD=' "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '"' | tr -d "'" | xargs)"
[[ -n "$ELASTIC_PASSWORD" ]] || { echo "error: ELASTIC_PASSWORD not set in $ENV_FILE" >&2; exit 1; }

# --- vaultSeed из .env ---
SECRETS_VALUES="$(mktemp)"
trap 'rm -f "$SECRETS_VALUES"' EXIT

{
  echo "vaultSeed:"
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$line" ]] && continue
    key="${line%%=*}"
    val="${line#*=}"
    val="${val#"${val%%[![:space:]]*}"}"
    val="${val%"${val##*[![:space:]]}"}"
    val="${val#\"}"; val="${val%\"}"
    printf '  %s: "%s"\n' "$key" "$val"
  done < "$ENV_FILE"
} > "$SECRETS_VALUES"

# --- vault-seed ---
kubectl delete secret vault-seed --namespace eventreg --ignore-not-found >/dev/null 2>&1 || true

# --- Elasticsearch ---
helm upgrade --install elasticsearch "$CHART_DIR/charts/elasticsearch-8.5.1.tgz" \
  --namespace eventreg --create-namespace \
  --values "$CHART_DIR/es-values.yaml" \
  --set secret.password="$ELASTIC_PASSWORD"

kubectl rollout status statefulset/elasticsearch-master --namespace eventreg --timeout=300s

# --- Kibana service token ---
kubectl exec -n eventreg elasticsearch-master-0 -- \
  bin/elasticsearch-service-tokens delete elastic/kibana kibana-token >/dev/null 2>&1 || true

KIBANA_TOKEN="$(kubectl exec -n eventreg elasticsearch-master-0 -- \
  bin/elasticsearch-service-tokens create elastic/kibana kibana-token \
  | grep -oP '=\s*\K.*' | tr -d ' ')"

[[ -n "$KIBANA_TOKEN" ]] || { echo "error: failed to create Kibana service token" >&2; exit 1; }

# --- Umbrella chart ---
helm upgrade --install eventreg "$CHART_DIR" \
  --namespace eventreg --create-namespace \
  --values "$SECRETS_VALUES" \
  --set kibana.elasticsearch.serviceAccountToken="$KIBANA_TOKEN" \
  "$@"