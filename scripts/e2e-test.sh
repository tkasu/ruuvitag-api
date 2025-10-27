#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SERVER_PORT=8081
SERVER_HOST="localhost"
MAX_RETRIES=30
RETRY_DELAY=1

echo -e "${YELLOW}Starting e2e test...${NC}"

# Clean up function
cleanup() {
    if [ ! -z "$SERVER_PID" ]; then
        echo -e "${YELLOW}Stopping server (PID: $SERVER_PID)...${NC}"
        kill $SERVER_PID 2>/dev/null || true
        wait $SERVER_PID 2>/dev/null || true
    fi
}

# Register cleanup on exit
trap cleanup EXIT INT TERM

# Start the server in background
echo -e "${YELLOW}Starting server on port $SERVER_PORT...${NC}"
sbt run > /dev/null 2>&1 &
SERVER_PID=$!

echo "Server PID: $SERVER_PID"

# Wait for server to be ready
echo -e "${YELLOW}Waiting for server to be ready...${NC}"
RETRIES=0
while [ $RETRIES -lt $MAX_RETRIES ]; do
    if curl -s "http://$SERVER_HOST:$SERVER_PORT/health" > /dev/null 2>&1; then
        echo -e "${GREEN}Server is ready!${NC}"
        break
    fi
    RETRIES=$((RETRIES + 1))
    echo "Retry $RETRIES/$MAX_RETRIES..."
    sleep $RETRY_DELAY
done

if [ $RETRIES -eq $MAX_RETRIES ]; then
    echo -e "${RED}Server failed to start within $MAX_RETRIES seconds${NC}"
    exit 1
fi

# Test 1: Health check
echo -e "\n${YELLOW}Test 1: Health check${NC}"
HEALTH_RESPONSE=$(curl -s "http://$SERVER_HOST:$SERVER_PORT/health")
echo "Response: $HEALTH_RESPONSE"

if echo "$HEALTH_RESPONSE" | grep -q "persistenceLayerStatus"; then
    echo -e "${GREEN}✓ Health check passed${NC}"
else
    echo -e "${RED}✗ Health check failed${NC}"
    exit 1
fi

# Test 2: POST measurement
echo -e "\n${YELLOW}Test 2: POST measurement${NC}"
MAC_ADDRESS="FE:26:88:7A:66:66"
TIMESTAMP=$(date +%s)000  # Unix timestamp in milliseconds
TELEMETRY_DATA='[
    {
        "telemetry_type": "Temperature",
        "data": [
            {
                "mac_address": "'$MAC_ADDRESS'",
                "timestamp": '$TIMESTAMP',
                "value": 22.5
            }
        ]
    }
]'

POST_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    "http://$SERVER_HOST:$SERVER_PORT/telemetry" \
    -H "Content-Type: application/json" \
    -d "$TELEMETRY_DATA")

HTTP_CODE=$(echo "$POST_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$POST_RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $RESPONSE_BODY"

if [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓ POST measurement passed${NC}"
else
    echo -e "${RED}✗ POST measurement failed (expected 201, got $HTTP_CODE)${NC}"
    exit 1
fi

# Test 3: GET measurement
echo -e "\n${YELLOW}Test 3: GET measurement${NC}"
FROM_TIME="2020-01-01T00:00:00Z"
TO_TIME="2030-12-31T23:59:59Z"

GET_RESPONSE=$(curl -s -w "\n%{http_code}" \
    "http://$SERVER_HOST:$SERVER_PORT/telemetry/Temperature?macAddress=$MAC_ADDRESS&from=$FROM_TIME&to=$TO_TIME")

HTTP_CODE=$(echo "$GET_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$GET_RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $RESPONSE_BODY"

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ GET measurement passed (HTTP 200)${NC}"
else
    echo -e "${RED}✗ GET measurement failed (expected 200, got $HTTP_CODE)${NC}"
    exit 1
fi

# Verify the response contains the measurement
if echo "$RESPONSE_BODY" | grep -q "$MAC_ADDRESS"; then
    echo -e "${GREEN}✓ Response contains MAC address${NC}"
else
    echo -e "${RED}✗ Response does not contain MAC address${NC}"
    exit 1
fi

if echo "$RESPONSE_BODY" | grep -q "22.5"; then
    echo -e "${GREEN}✓ Response contains measurement value${NC}"
else
    echo -e "${RED}✗ Response does not contain measurement value${NC}"
    exit 1
fi

# Test 4: GET with missing parameters
echo -e "\n${YELLOW}Test 4: GET with missing parameters (should fail)${NC}"
GET_RESPONSE=$(curl -s -w "\n%{http_code}" \
    "http://$SERVER_HOST:$SERVER_PORT/telemetry/Temperature?macAddress=$MAC_ADDRESS")

HTTP_CODE=$(echo "$GET_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$GET_RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $RESPONSE_BODY"

if [ "$HTTP_CODE" = "400" ]; then
    echo -e "${GREEN}✓ Invalid request properly rejected${NC}"
else
    echo -e "${RED}✗ Invalid request not rejected (expected 400, got $HTTP_CODE)${NC}"
    exit 1
fi

# Test 5: GET /metrics endpoint
echo -e "\n${YELLOW}Test 5: Prometheus metrics endpoint${NC}"
METRICS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    "http://$SERVER_HOST:$SERVER_PORT/metrics")

HTTP_CODE=$(echo "$METRICS_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$METRICS_RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response (first 5 lines): $(echo "$RESPONSE_BODY" | head -n5)"

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Metrics endpoint accessible${NC}"
else
    echo -e "${RED}✗ Metrics endpoint failed (expected 200, got $HTTP_CODE)${NC}"
    exit 1
fi

echo -e "\n${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}All e2e tests passed successfully! ✓${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"

exit 0
