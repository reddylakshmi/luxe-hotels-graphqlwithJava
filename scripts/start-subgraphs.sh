#!/bin/bash
ROOT=/Users/lmotati/Workspace-IntelliJ/luxe-hotels-graphqlwithJava
mkdir -p /tmp/luxe-run
> /tmp/luxe-run/pids
SUBGRAPHS=(
  "property:4001:PropertyApplication"
  "guest:4002:GuestApplication"
  "pricing:4003:PricingApplication"
  "reservations:4004:ReservationsApplication"
  "loyalty:4005:LoyaltyApplication"
  "content:4006:ContentApplication"
  "experiences:4007:ExperiencesApplication"
  "meetings:4008:MeetingsApplication"
  "notifications:4009:NotificationsApplication"
  "corporate:4010:CorporateApplication"
)
for entry in "${SUBGRAPHS[@]}"; do
  IFS=':' read -r name port appclass <<<"$entry"
  log=/tmp/luxe-run/${name}.log
  : > "$log"
  jar="$ROOT/subgraph-${name}/target/subgraph-${name}-1.0.0-SNAPSHOT.jar"
  java -jar "$jar" --server.port=$port > "$log" 2>&1 &
  echo "$!" >> /tmp/luxe-run/pids
done
echo "all 10 launched, waiting for Started..."
START=$(date +%s)
while true; do
  ready=0
  for entry in "${SUBGRAPHS[@]}"; do
    IFS=':' read -r name port appclass <<<"$entry"
    grep -q "Started ${appclass}" /tmp/luxe-run/${name}.log && ready=$((ready+1))
  done
  if [ "$ready" -eq 10 ]; then
    echo "all 10 READY in $(( $(date +%s) - START ))s"
    break
  fi
  if [ $(( $(date +%s) - START )) -gt 60 ]; then
    echo "timeout waiting for subgraphs (only $ready ready)"
    for entry in "${SUBGRAPHS[@]}"; do
      IFS=':' read -r name port appclass <<<"$entry"
      grep -q "Started ${appclass}" /tmp/luxe-run/${name}.log || echo "  $name NOT READY"
    done
    exit 1
  fi
  sleep 1
done
echo
echo "=== port health ==="
for entry in "${SUBGRAPHS[@]}"; do
  IFS=':' read -r name port appclass <<<"$entry"
  printf "%-15s :%d  /actuator/health=%s\n" "$name" "$port" "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health)"
done
