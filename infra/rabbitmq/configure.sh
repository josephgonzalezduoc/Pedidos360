#!/bin/sh
set -eu
api='http://rabbit1:15672/api'
attempt=0
while [ "$attempt" -lt 30 ]; do
    nodes=$(curl --silent --show-error --fail --connect-timeout 3 --max-time 10 \
        --user "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" "$api/nodes" || true)
    if printf '%s' "$nodes" | grep -q '"name"[[:space:]]*:[[:space:]]*"rabbit@rabbit1"' \
        && printf '%s' "$nodes" | grep -q '"name"[[:space:]]*:[[:space:]]*"rabbit@rabbit2"'; then
        curl --silent --show-error --fail --connect-timeout 3 --max-time 30 \
            --user "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" \
            --header 'Content-Type: application/json' \
            --data-binary @/config/definitions.json "$api/definitions"
        printf '\nTopologia y politicas importadas; se detectaron los dos miembros.\n'
        exit 0
    fi
    attempt=$((attempt + 1))
    sleep 2
done
printf 'No se detectaron ambos nodos. Revisar logs y cluster_status.\n' >&2
exit 1
