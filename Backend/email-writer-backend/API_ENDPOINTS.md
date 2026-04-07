# Smart Email Assistant API Documentation

## Base URL
```text
http://localhost:8080
```

## API Prefix
All application APIs are versioned under:
```text
/api/v1
```

## Authentication
Use JWT for protected endpoints:
```text
Authorization: Bearer <token>
```

---

## Authentication Endpoints (`/api/v1/auth`)

### Register User
```http
POST /api/v1/auth/register
```

#### Request Body
```json
{
  "username": "string (required, 3-50)",
  "email": "string (required, valid email)",
  "password": "string (required, min 6)",
  "firstName": "string (optional)",
  "lastName": "string (optional)"
}
```

#### Success Response (200)
```json
{
  "token": "string",
  "type": "Bearer",
  "id": 1,
  "username": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string"
}
```

---

### Login
```http
POST /api/v1/auth/login
```

#### Request Body
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

#### Success Response (200)
```json
{
  "token": "string",
  "type": "Bearer",
  "id": 1,
  "username": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string"
}
```

---

### Auth Test (Public)
```http
GET /api/v1/auth/test
```

#### Success Response (200)
```json
{
  "message": "Auth service running"
}
```

---

## Email Generation Endpoints (`/api/v1/email`)

### Generate Multiple Replies (JWT required)
```http
POST /api/v1/email/generate
```

#### Request Body
```json
{
  "subject": "string",
  "emailContent": "string",
  "tone": "string (optional)",
  "language": "string (optional, default: en)"
}
```

#### Success Response (200)
```json
{
  "replies": ["string", "string", "string"],
  "summary": "string"
}
```

#### Response Header
```text
X-RateLimit-Remaining: <number>
```

#### Rate Limit Response (429)
```json
{
  "error": "rate_limit_exceeded",
  "message": "You have reached the request limit (8 per minute / 200 per day). Please wait before trying again."
}
```

---

### Regenerate Replies (JWT required)
```http
POST /api/v1/email/regenerate
```

#### Request Body
Same as `/api/v1/email/generate`

#### Success Response (200)
```json
{
  "replies": ["string", "string", "string"],
  "summary": "string"
}
```

#### Rate Limit Response (429)
```json
{
  "error": "rate_limit_exceeded",
  "message": "You have reached the request limit. Please wait before trying again."
}
```

---

### Generate Single Reply (JWT required)
```http
POST /api/v1/email/generate-single
```

#### Request Body
Same as `/api/v1/email/generate`

#### Success Response (200)
```json
{
  "summary": "string",
  "reply": "string"
}
```

#### Rate Limit Response (429)
```json
{
  "error": "rate_limit_exceeded",
  "message": "Request limit reached. Please wait."
}
```

---

### Ping (Public, no Gemini call)
```http
GET /api/v1/email/ping
```

#### Success Response (200)
```json
{
  "status": "ok",
  "service": "email-generator"
}
```

---

### Email Connectivity Test (Public, calls Gemini)
```http
GET /api/v1/email/test
```

#### Success Response (200)
```json
{
  "status": "ok",
  "summary": "string",
  "reply": "string"
}
```

---

## Saved Replies Endpoints (`/api/v1/replies`) - JWT required

### Save Reply
```http
POST /api/v1/replies/save
```

#### Request Body
```json
{
  "emailSubject": "string (required, max 500)",
  "emailContent": "string (required)",
  "tone": "string (optional)",
  "language": "string (optional)",
  "replyText": "string (required)",
  "summary": "string (optional)"
}
```

#### Success Response (200)
```json
{
  "message": "Reply saved successfully",
  "id": 123,
  "createdAt": "2026-04-07T10:20:30"
}
```

---

### Get Reply History
```http
GET /api/v1/replies/history
```

#### Query Parameters
- `page` (optional, default: `0`)
- `size` (optional, default: `20`)
- `tone` (optional)
- `fromDate` (optional, ISO date-time)
- `toDate` (optional, ISO date-time)

#### Success Response (200) - Paginated (when no filters)
```json
{
  "content": ["SavedReply objects"],
  "totalPages": 1,
  "totalElements": 10,
  "currentPage": 0,
  "size": 20
}
```

#### Success Response (200) - Filtered (when `tone`/`fromDate`/`toDate` provided)
```json
{
  "content": ["SavedReply objects"],
  "total": 4,
  "filtered": true
}
```

---

### Search Replies
```http
GET /api/v1/replies/search?q=<query>&tone=<tone>
```

#### Query Parameters
- `q` (required, non-empty)
- `tone` (optional)

#### Success Response (200)
```json
{
  "results": ["SavedReply objects"],
  "query": "meeting",
  "tone": "professional",
  "total": 2
}
```

#### Missing Query Response (400)
```json
{
  "error": "missing_query_param",
  "message": "Query parameter 'q' is required and cannot be empty."
}
```

---

### Get Favorites
```http
GET /api/v1/replies/favorites
```

#### Success Response (200)
```json
{
  "favorites": ["SavedReply objects"],
  "total": 3
}
```

---

### Toggle Favorite
```http
PUT /api/v1/replies/{id}/favorite
```

#### Success Response (200)
```json
{
  "message": "Added to favorites",
  "isFavorite": true
}
```

---

### Delete Reply
```http
DELETE /api/v1/replies/{id}
```

#### Success Response (200)
```json
{
  "message": "Reply deleted successfully"
}
```

---

### Get Reply Statistics
```http
GET /api/v1/replies/stats
```

#### Success Response (200)
```json
{
  "username": "string",
  "totalReplies": 20,
  "favoriteCount": 5,
  "toneDistribution": {
    "professional": 12,
    "casual": 8
  },
  "recentActivity": ["2026-04-07T10:20:30"]
}
```

---

### Export Replies CSV
```http
GET /api/v1/replies/export
```

#### Success Response (200)
- Header: `Content-Type: text/csv`
- Header: `Content-Disposition: attachment; filename=saved_replies_<username>.csv`
- Body: CSV text

---

## Other Public Health Endpoints

### Actuator Health
```http
GET /actuator/health
```

### Actuator Info
```http
GET /actuator/info
```

### Custom Health
```http
GET /custom-health
```

#### `GET /custom-health` Success (200)
```json
{
  "status": "UP",
  "services": {
    "database": "UP"
  }
}
```

#### `GET /custom-health` Degraded (503)
```json
{
  "status": "DOWN",
  "services": {
    "database": "DOWN"
  }
}
```

---

## Common Error Responses

### Validation Error (400)
```json
{
  "error": "validation_error",
  "message": "Validation failed for one or more fields.",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Email must be valid"
    }
  ]
}
```

### Unauthorized (401)
```json
{
  "error": "request_error",
  "message": "User not authenticated"
}
```

### Not Found (404)
```json
{
  "error": "request_error",
  "message": "Reply not found with ID: 5"
}
```

### Internal Error (500)
```json
{
  "error": "internal_error",
  "message": "An unexpected error occurred. Please try again."
}
```
