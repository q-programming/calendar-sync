#!/usr/bin/env bash
set -e

GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-YOUR_CLIENT_ID}"
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-YOUR_CLIENT_SECRET}"

java -jar calendarsync.jar \
  --server.port=9090 \
  --spring.datasource.url="jdbc:h2:file:./calendarsync;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" \
  --spring.security.oauth2.client.registration.google.client-id="$GOOGLE_CLIENT_ID" \
  --spring.security.oauth2.client.registration.google.client-secret="$GOOGLE_CLIENT_SECRET" \
  &

echo "Waiting for server to start..."
sleep 5

if command -v xdg-open &>/dev/null; then
  xdg-open http://localhost:9090/calendarsync
elif command -v open &>/dev/null; then
  open http://localhost:9090/calendarsync
else
  echo "Open http://localhost:9090/calendarsync in your browser"
fi
