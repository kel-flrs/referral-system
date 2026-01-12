# OAuth 2.0 Authentication - Bullhorn Mock Service

## Overview

The Bullhorn Mock Service now implements **OAuth 2.0 authentication** that matches the real Bullhorn CRM API flow. This provides enterprise-grade security for API consumers.

## OAuth Flow (Bullhorn-Compatible)

The authentication follows Bullhorn's 3-step OAuth process:

### Step 1: Get Authorization Code
```bash
GET /oauth/authorize?client_id=CLIENT_ID&username=USERNAME&password=PASSWORD&response_type=code&action=Login
```

**Response:**
```json
{
  "code": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Step 2: Exchange Code for Access Token
```bash
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTH_CODE&client_id=CLIENT_ID&client_secret=CLIENT_SECRET
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 600,
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "scope": "read:candidates,write:candidates,..."
}
```

### Step 3: Login to REST API
```bash
POST /rest-services/login?version=*&access_token=ACCESS_TOKEN
```

**Response:**
```json
{
  "BhRestToken": "d86275b357a84a8aa7e021248af11e38",
  "restUrl": "http://localhost:8080/rest-services/",
  "sessionExpires": 3600
}
```

### Step 4: Use Session Token for API Calls
```bash
GET /api/v1/candidates?BhRestToken=SESSION_TOKEN
```

The `BhRestToken` can be provided in:
- Query parameter: `?BhRestToken=TOKEN`
- HTTP Header: `BhRestToken: TOKEN` or `BHRestToken: TOKEN`
- Cookie: `BhRestToken=TOKEN`

## Pre-Configured OAuth Clients

Three sample clients are automatically created on startup:

### 1. Test Client (Full Access)
- **Client ID:** `test-client-1`
- **Client Secret:** `test-secret-1`
- **Username:** `admin@bullhorn.local`
- **Password:** `password123`
- **Scopes:** Full read/write access to all entities
- **Use Case:** Development and testing

### 2. Reporting Service (Read-Only)
- **Client ID:** `reporting-service`
- **Client Secret:** `reporting-secret-456`
- **Username:** `reports@bullhorn.local`
- **Password:** `reports123`
- **Scopes:** Read-only access to all entities
- **Use Case:** Analytics and reporting applications

### 3. Integration Service
- **Client ID:** `integration-service`
- **Client Secret:** `integration-secret-789`
- **Username:** `integration@bullhorn.local`
- **Password:** `integration123`
- **Scopes:** Read/write to candidates and jobs
- **Use Case:** External integrations

## Token Expiration

- **Authorization Code:** 5 minutes
- **Access Token:** 10 minutes (matches Bullhorn)
- **Refresh Token:** 24 hours (48 hours for integration-service)
- **REST Session:** 1 hour

## Refresh Token Flow

To get a new access token using a refresh token:

```bash
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=REFRESH_TOKEN&client_id=CLIENT_ID&client_secret=CLIENT_SECRET
```

## Complete Example

```bash
# Step 1: Get authorization code
AUTH_RESPONSE=$(curl -s "http://localhost:8080/oauth/authorize?client_id=test-client-1&username=admin@bullhorn.local&password=password123&response_type=code&action=Login")
AUTH_CODE=$(echo $AUTH_RESPONSE | jq -r '.code')

# Step 2: Exchange for access token
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8080/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=$AUTH_CODE&client_id=test-client-1&client_secret=test-secret-1")
ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')

# Step 3: Login to REST API
LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/rest-services/login?version=*&access_token=$ACCESS_TOKEN")
REST_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.BhRestToken')

# Step 4: Call API
curl "http://localhost:8080/api/v1/candidates?page=0&size=10&BhRestToken=$REST_TOKEN"
```

## Testing

Run the automated OAuth test:
```bash
bash test-oauth.sh
```

Expected output:
```
=========================================
Testing Bullhorn OAuth 2.0 Flow
=========================================

STEP 1: Get Authorization Code
-----------------------------------------
✓ Authorization code received

STEP 2: Exchange Code for Access Token
-----------------------------------------
✓ Access token received

STEP 3: Login to REST API with Access Token
-----------------------------------------
✓ REST session token received

STEP 4: Test API Call with Session Token
-----------------------------------------
✓ API call successful!
Total candidates: 1000

=========================================
OAuth 2.0 Flow Test Complete - SUCCESS!
=========================================
```

## Security Features

**Industry-Standard OAuth 2.0** - Authorization Code flow
**JWT Tokens** - Stateless, signed tokens
**BCrypt Password Hashing** - Secure credential storage
**Token Expiration** - Automatic timeout
**Refresh Tokens** - Long-lived access without re-authentication
**Scope-Based Access Control** - Granular permissions
**Session Management** - Track active API sessions
**Secure by Default** - All API endpoints protected except OAuth endpoints

## Protected Endpoints

All `/api/**` endpoints now require authentication via `BhRestToken`.

**Public endpoints (no authentication required):**
- `/oauth/authorize`
- `/oauth/token`
- `/rest-services/login`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/actuator/health`

## Database Tables

OAuth implementation adds three new tables:

1. **oauth_clients** - Registered API consumers
2. **rest_sessions** - Active REST API sessions
3. **refresh_tokens** - Long-lived refresh tokens

## Configuration

OAuth settings in `application-dev.yml`:

```yaml
app:
  oauth:
    jwt-secret: dev-secret-key-change-in-production-12345678
    issuer: bullhorn-mock-service-dev
    session-validity-seconds: 3600  # 1 hour
```

**IMPORTANT:** Change `jwt-secret` in production!

## Differences from Real Bullhorn

| Feature | Bullhorn CRM | Mock Service |
|---------|--------------|--------------|
| OAuth Flow | 3-step OAuth 2.0 | ✓ Same |
| Authorization Code | Short-lived JWT | ✓ Same concept |
| Access Token | 10 min expiry | ✓ Same |
| Refresh Token | Long-lived | ✓ Same |
| Session Token (BhRestToken) | Required for API calls | ✓ Same |
| Token in Query/Header/Cookie | All supported | ✓ Same |

## Next Steps

1. **Test Integration:** Use the mock service to test your Bullhorn integrations
2. **Client Registration:** Create additional OAuth clients as needed
3. **Monitoring:** Check session and token tables for active connections
4. **Production:** Change JWT secret before deploying to production

## Support

- Run `bash test-oauth.sh` to verify OAuth is working
- Check application logs for authentication errors
- Use Swagger UI at http://localhost:8080/swagger-ui.html (no auth required)
- Use pgAdmin at http://localhost:5050 to inspect OAuth tables
