#!/bin/bash

echo "========================================="
echo "Testing Bullhorn OAuth 2.0 Flow"
echo "========================================="
echo ""

# Step 1: Get authorization code
echo "STEP 1: Get Authorization Code"
echo "-----------------------------------------"
AUTH_RESPONSE=$(curl -s "http://localhost:8080/oauth/authorize?client_id=test-client-1&username=admin@bullhorn.local&password=password123&response_type=code&action=Login")
AUTH_CODE=$(echo $AUTH_RESPONSE | grep -o '"code":"[^"]*"' | cut -d'"' -f4)

if [ -z "$AUTH_CODE" ]; then
    echo "ERROR: Failed to get authorization code"
    echo "Response: $AUTH_RESPONSE"
    exit 1
fi

echo "✓ Authorization code received"
echo "Code (first 50 chars): ${AUTH_CODE:0:50}..."
echo ""

# Step 2: Exchange code for access token
echo "STEP 2: Exchange Code for Access Token"
echo "-----------------------------------------"
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8080/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=$AUTH_CODE&client_id=test-client-1&client_secret=test-secret-1")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
REFRESH_TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"refresh_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "ERROR: Failed to get access token"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo "✓ Access token received"
echo "Access Token (first 50 chars): ${ACCESS_TOKEN:0:50}..."
echo "Refresh Token (first 50 chars): ${REFRESH_TOKEN:0:50}..."
echo ""

# Step 3: Login to REST API
echo "STEP 3: Login to REST API with Access Token"
echo "-----------------------------------------"
LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/rest-services/login?version=*&access_token=$ACCESS_TOKEN")

REST_TOKEN=$(echo $LOGIN_RESPONSE | grep -o -E '"[Bb]hRestToken":"[^"]*"' | cut -d'"' -f4)
REST_URL=$(echo $LOGIN_RESPONSE | grep -o '"restUrl":"[^"]*"' | cut -d'"' -f4)

if [ -z "$REST_TOKEN" ]; then
    echo "ERROR: Failed to get REST session token"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo "✓ REST session token received"
echo "BhRestToken: $REST_TOKEN"
echo "REST URL: $REST_URL"
echo ""

# Step 4: Test API call with session token
echo "STEP 4: Test API Call with Session Token"
echo "-----------------------------------------"
API_RESPONSE=$(curl -s "http://localhost:8080/api/v1/candidates?page=0&size=2&BhRestToken=$REST_TOKEN")

CANDIDATE_COUNT=$(echo $API_RESPONSE | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)

if [ -z "$CANDIDATE_COUNT" ]; then
    echo "ERROR: Failed to call API"
    echo "Response: ${API_RESPONSE:0:200}..."
    exit 1
fi

echo "✓ API call successful!"
echo "Total candidates: $CANDIDATE_COUNT"
echo ""
echo "========================================="
echo "OAuth 2.0 Flow Test Complete - SUCCESS!"
echo "========================================="
