# TraceNest — Backend API Contract

This document is the source of truth for the backend team. It describes every request the TraceNest frontend will send and every response it expects. Follow it exactly so the frontend can drop the mock service layer (`src/services/reports.service.ts`) and switch to the real API with no UI changes.

> Base URL (proposed): `https://api.tracenest.app/api/v1`
> Content type: `application/json; charset=utf-8` (except file uploads: `multipart/form-data`)
> Auth: `Authorization: Bearer <JWT>` on every protected route.
> Time format: ISO-8601 UTC (`2026-07-21T10:15:30Z`).
> IDs: opaque strings (max 64 chars). Frontend never parses them.

---

## 1. Global Conventions

### 1.1 Success envelope

Return the resource directly (no wrapper) for single-resource endpoints:

```json
{ "id": "r_123", "type": "pet", "...": "..." }
```

For collections, return a paginated envelope:

```json
{
  "items": [{ "...": "..." }],
  "page": 0,
  "size": 20,
  "totalItems": 137,
  "totalPages": 7
}
```

### 1.2 Error envelope (ALL non-2xx responses)

```json
{
  "timestamp": "2026-07-21T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Human-readable summary",
  "fieldErrors": {
    "email": "must be a valid email",
    "lat": "must be between -90 and 90"
  },
  "traceId": "b1f2..."
}
```

| HTTP | `code` values the frontend handles                      |
| ---- | ------------------------------------------------------- |
| 400  | `VALIDATION_ERROR`, `BAD_REQUEST`                       |
| 401  | `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `TOKEN_INVALID` |
| 403  | `FORBIDDEN`, `NOT_OWNER`                                |
| 404  | `NOT_FOUND`                                             |
| 409  | `EMAIL_TAKEN`, `DUPLICATE_RESOURCE`                     |
| 413  | `PAYLOAD_TOO_LARGE`                                     |
| 422  | `UNPROCESSABLE`                                         |
| 429  | `RATE_LIMITED`                                          |
| 500  | `INTERNAL_ERROR`                                        |

### 1.3 CORS

Allow `GET, POST, PUT, PATCH, DELETE, OPTIONS` from the deployed web origin and `http://localhost:8080` for dev. Expose `Authorization` header.

### 1.4 Pagination / sorting query params

- `page` (int, default 0), `size` (int, default 20, max 100)
- `sort` — one of the values documented per endpoint (e.g. `newest`, `reward`).

---

## 2. Authentication

JWT-based. Access token TTL ~15 min, refresh token TTL ~30 days (httpOnly cookie recommended, or returned in body if cookies are not used).

### 2.1 `POST /auth/register`

**Request**

```json
{
  "name": "Aarav Sharma",
  "email": "aarav@example.com",
  "password": "P@ssw0rd!",
  "phone": "+919812345678", // optional
  "city": "Bengaluru" // optional
}
```

Validation: `name` 2–80 chars; `email` RFC-5322; `password` ≥8 chars, ≥1 letter + ≥1 digit.

**Response 201**

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 900,
  "user": {
    "id": "u_abc123",
    "name": "Aarav Sharma",
    "email": "aarav@example.com",
    "phone": "+919812345678",
    "city": "Bengaluru",
    "role": "USER",
    "avatarUrl": null,
    "createdAt": "2026-07-21T10:15:30Z"
  }
}
```

Errors: `409 EMAIL_TAKEN`, `400 VALIDATION_ERROR`.

### 2.2 `POST /auth/login`

**Request**

```json
{ "email": "aarav@example.com", "password": "P@ssw0rd!" }
```

**Response 200** — same shape as register.
Errors: `401 INVALID_CREDENTIALS`.

### 2.3 `POST /auth/refresh`

**Request**

```json
{ "refreshToken": "eyJhbGci..." }
```

**Response 200**

```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 900 }
```

### 2.4 `POST /auth/logout`

Auth required. Server invalidates refresh token.
**Response 204** (no body).

### 2.5 `POST /auth/forgot-password`

**Request** `{ "email": "aarav@example.com" }`
**Response 202** `{ "message": "If the email exists, a reset link was sent." }`
(Always 202 — do not reveal whether the email exists.)

### 2.6 `POST /auth/reset-password`

**Request**

```json
{ "token": "<from email link>", "newPassword": "N3wPass!" }
```

**Response 200** `{ "message": "Password updated." }`

### 2.7 `GET /auth/me`

Auth required. Returns the current user object (same shape as `user` in login response). Used on app boot to hydrate the session.

---

## 3. User Profile

### 3.1 `GET /users/me` — same as `/auth/me`.

### 3.2 `PATCH /users/me`

**Request** (any subset)

```json
{ "name": "Aarav S.", "phone": "+91...", "city": "Mumbai", "avatarUrl": "https://..." }
```

**Response 200** — updated `User` object.

### 3.3 `POST /users/me/avatar` (multipart)

Field: `file` (image, ≤5 MB, jpeg/png/webp).
**Response 200** `{ "avatarUrl": "https://cdn.tracenest.app/u/abc.webp" }`.

### 3.4 `POST /users/me/change-password`

```json
{ "currentPassword": "old", "newPassword": "new" }
```

Response 200 `{ "message": "Password updated." }`. Errors: `401 INVALID_CREDENTIALS`.

---

## 4. Reports (Core domain)

Four report types drive most of the UI:
`person | pet | lost-vehicle | stolen-vehicle`
Status lifecycle: `missing → found → resolved`.

### 4.1 The `Report` object (canonical response shape)

```json
{
  "id": "r_9f3a2b",
  "type": "pet",
  "title": "Golden Retriever missing near Cubbon Park",
  "description": "Wearing a red collar, responds to 'Bruno'.",
  "status": "missing",
  "reward": 5000,
  "contact": "+919812345678",
  "images": [
    "https://cdn.tracenest.app/r/9f3a2b/1.jpg",
    "https://cdn.tracenest.app/r/9f3a2b/2.jpg"
  ],
  "location": {
    "lat": 12.9762,
    "lng": 77.5993,
    "address": "Cubbon Park, MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "postcode": "560001",
    "country": "India",
    "source": "gps" // "gps" | "manual"
  },
  "createdAt": "2026-07-21T10:15:30Z",
  "updatedAt": "2026-07-21T10:15:30Z",
  "lastSeen": "2026-07-20T18:00:00Z",
  "reporter": {
    "id": "u_abc123",
    "name": "Aarav Sharma",
    "avatarUrl": null
  },
  "details": {
    // Only fields relevant to `type` are populated. Others may be null/absent.
    "age": null,
    "gender": null,
    "clothing": null,
    "species": "Dog",
    "breed": "Golden Retriever",
    "color": "Golden",
    "vehicleType": null,
    "vehicleNumber": null,
    "manufacturer": null,
    "model": null,
    "year": null,
    "firNumber": null,
    "policeStation": null,
    "policeComplaint": null
  }
}
```

> Frontend currently flattens these fields (see `src/services/types.ts`). Prefer the nested `location` + `details` shape above; the frontend adapter will map to its flat type. If the backend cannot nest, return everything flat with the exact field names from `types.ts`.

### 4.2 Type-specific field requirements

| Type             | Required extra fields                                            | Optional                  |
| ---------------- | ---------------------------------------------------------------- | ------------------------- |
| `person`         | `age`, `gender`, `clothing`                                      | `lastSeen`                |
| `pet`            | `species`, `breed`, `color`                                      | `lastSeen`                |
| `lost-vehicle`   | `vehicleType`, `vehicleNumber`, `manufacturer`, `model`, `color` | `year`, `lastSeen`        |
| `stolen-vehicle` | Same as lost-vehicle **+** `firNumber`, `policeStation`          | `policeComplaint`, `year` |

Common required for all: `title`, `description`, `contact`, `location.lat`, `location.lng`, `location.city`.

### 4.3 `GET /reports` — list / search

Query params (all optional):

| Param                    | Type   | Notes                                                                           |
| ------------------------ | ------ | ------------------------------------------------------------------------------- |
| `q`                      | string | Free-text over title, description, city, vehicleNumber, breed, model, firNumber |
| `type`                   | enum   | `person                                                                         | pet   | lost-vehicle | stolen-vehicle | all` |
| `status`                 | enum   | `missing                                                                        | found | resolved     | all`           |
| `city`                   | string | Case-insensitive `contains`                                                     |
| `lat`, `lng`, `radiusKm` | number | Optional geo filter                                                             |
| `sort`                   | enum   | `newest` (default) or `reward` (desc)                                           |
| `page`, `size`           | int    | Pagination                                                                      |

**Response 200** — paginated envelope of `Report`.

### 4.4 `GET /reports/{id}` → `Report`. Errors: `404 NOT_FOUND`.

### 4.5 `POST /reports` (auth required, multipart or JSON)

**JSON body** (when images are uploaded separately or via URL):

```json
{
  "type": "pet",
  "title": "Golden Retriever missing near Cubbon Park",
  "description": "Wearing a red collar...",
  "contact": "+919812345678",
  "reward": 5000,
  "lastSeen": "2026-07-20T18:00:00Z",
  "location": {
    "lat": 12.9762,
    "lng": 77.5993,
    "address": "Cubbon Park, MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "postcode": "560001",
    "country": "India",
    "source": "gps"
  },
  "images": ["https://cdn.tracenest.app/uploads/tmp/abc.jpg"],
  "details": {
    "species": "Dog",
    "breed": "Golden Retriever",
    "color": "Golden"
  }
}
```

**Multipart alternative** (single request with images):

- part `payload` — JSON as above (without `images`)
- parts `images` (repeated) — image files, ≤5 MB each, max 6

**Response 201** — full `Report` object. `status` defaults to `"missing"`.

**Location rules (very important — the UI supports two flows):**

- If `source == "gps"`, the frontend has already reverse-geocoded via Nominatim; backend MUST persist all address components verbatim.
- If `source == "manual"`, `lat`/`lng` may still be present (from map click). Backend must store whatever fields the client sent; do not overwrite.
- Backend SHOULD forward-geocode when `lat`/`lng` is missing but address components are present, and vice versa, to keep the record complete.

### 4.6 `PATCH /reports/{id}` (auth required, owner or ADMIN)

Any subset of the create body. Cannot change `type` or `reporter`.
**Response 200** — updated `Report`.

### 4.7 `PATCH /reports/{id}/status`

```json
{ "status": "found" } // or "resolved" | "missing"
```

Auth: owner or ADMIN. Response 200 — updated `Report`. Emits notification + email to subscribers.

### 4.8 `DELETE /reports/{id}` (owner or ADMIN) → 204.

### 4.9 `GET /reports/mine` — auth required. Paginated list of caller's reports.

### 4.10 `GET /reports/stats` (dashboard tiles)

**Response 200**

```json
{
  "total": 137,
  "active": 82, // status == "missing"
  "found": 34,
  "resolved": 21,
  "byType": { "person": 20, "pet": 60, "lost-vehicle": 30, "stolen-vehicle": 27 }
}
```

### 4.11 `GET /reports/map` — lightweight list for map view

Same filters as `/reports` but returns a compact payload (no description/details) for perf:

```json
{
  "items": [
    {
      "id": "r_1",
      "type": "pet",
      "title": "...",
      "status": "missing",
      "lat": 12.97,
      "lng": 77.59,
      "city": "Bengaluru",
      "createdAt": "..."
    }
  ]
}
```

No pagination; cap at 500. Frontend calls this from `/reports/map`.

---

## 5. Image Uploads

### 5.1 `POST /uploads/images` (multipart, auth required)

Field: `files` (repeated, ≤6 files, ≤5 MB each, `image/jpeg|png|webp`).
**Response 201**

```json
{
  "files": [
    {
      "url": "https://cdn.tracenest.app/uploads/tmp/abc.jpg",
      "width": 1200,
      "height": 800,
      "sizeBytes": 234123
    }
  ]
}
```

Frontend uploads first, then references URLs in `POST /reports`. Backend should garbage-collect `tmp/*` files not referenced within 24 h.

Errors: `413 PAYLOAD_TOO_LARGE`, `415 UNSUPPORTED_MEDIA_TYPE`.

---

## 6. Notifications & Email Alerts (MVP scope: email only)

### 6.1 Subscriptions

Users can subscribe to alerts for a city + optional report types + optional radius.

`POST /alerts/subscriptions`

```json
{
  "city": "Bengaluru",
  "types": ["pet", "person"],
  "radiusKm": 10,
  "lat": 12.97,
  "lng": 77.59
}
```

**Response 201**

```json
{
  "id": "sub_123",
  "city": "Bengaluru",
  "types": ["pet", "person"],
  "radiusKm": 10,
  "createdAt": "..."
}
```

`GET /alerts/subscriptions` → `{ "items": [ ... ] }`
`DELETE /alerts/subscriptions/{id}` → 204.

### 6.2 In-app notifications

`GET /notifications?unreadOnly=true&page=0&size=20`

```json
{
  "items": [
    {
      "id": "n_1",
      "type": "NEW_REPORT_NEARBY", // or REPORT_STATUS_CHANGED, REPORT_COMMENT
      "title": "New pet reported near you",
      "body": "Golden Retriever missing at Cubbon Park",
      "reportId": "r_9f3a2b",
      "read": false,
      "createdAt": "2026-07-21T10:15:30Z"
    }
  ],
  "unreadCount": 3,
  "page": 0,
  "size": 20,
  "totalItems": 42,
  "totalPages": 3
}
```

`POST /notifications/{id}/read` → 204
`POST /notifications/read-all` → 204

### 6.3 Server behaviour when a report is created / status changes

Backend enqueues:

1. Email to every matching subscription (`type` overlap + within radius or same city).
2. In-app notification record for the same users.

Frontend does not call these — they are server-side side effects. This section exists so backend implements them.

---

## 7. Dashboard Aggregates

`GET /dashboard/summary` (auth required)

```json
{
  "myReports": { "total": 4, "active": 2, "resolved": 2 },
  "communityStats": { "total": 137, "active": 82, "resolved": 21 },
  "recent": [/* up to 5 Report objects authored by the user, newest first */]
}
```

---

## 8. Geocoding (optional server helper)

Currently the frontend calls OpenStreetMap Nominatim directly (see `src/lib/geocode.ts`). If Nominatim rate limits become a problem, expose:

`GET /geo/reverse?lat=12.97&lng=77.59`

```json
{
  "address": "Cubbon Park, MG Road",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postcode": "560001",
  "country": "India"
}
```

`GET /geo/forward?q=Cubbon+Park` → same shape + `lat`, `lng`.

Backend may cache Nominatim responses for 24 h to comply with their usage policy.

---

## 9. Admin (post-MVP, sketch only)

- `GET /admin/reports?flagged=true` — moderation queue.
- `POST /admin/reports/{id}/takedown` — soft delete, notifies reporter.
- `GET /admin/users`, `PATCH /admin/users/{id}` — role changes, ban.
  All require `role == "ADMIN"`; otherwise `403 FORBIDDEN`.

---

## 10. Security Requirements (must-have)

1. **Passwords** hashed with BCrypt (cost ≥ 12) or Argon2id. Never returned in any response.
2. **JWT** signed with RS256 (preferred) or HS256 with a ≥256-bit secret from environment. Include `sub` (userId), `role`, `iat`, `exp`. Do not put PII in claims beyond email.
3. **Rate limiting**:
   - `/auth/login`, `/auth/forgot-password`: 5 req / 15 min / IP.
   - `POST /reports`, `POST /uploads/images`: 20 req / hour / user.
   - Return `429 RATE_LIMITED` with `Retry-After` header.
4. **Ownership checks** on every mutating report/user endpoint. `403 NOT_OWNER` otherwise.
5. **Input validation** with Bean Validation (JSR-380). Reject unknown fields with `400 VALIDATION_ERROR`.
6. **Sanitize** `description`, `clothing`, `policeComplaint` before storing (strip HTML). The frontend will render them as plain text but defend in depth.
7. **File uploads**: verify magic bytes (not just Content-Type). Strip EXIF GPS unless `location.source == "gps"` (privacy).
8. **PII**: never expose reporter `email` or `phone` in public list/detail responses. Only expose `contact` (which the reporter deliberately publishes) and `reporter.name` + `avatarUrl`.
9. **CORS** — strict allow-list of origins; no wildcard with credentials.
10. **HTTPS** everywhere in production; HSTS enabled.
11. **Logging** — log `traceId`, path, status, userId; never log request bodies containing passwords or tokens.
12. **Password reset tokens**: single-use, ≥128-bit random, 30-min TTL.

---

## 11. Field Reference (canonical names & types)

| Field             | Type   | Notes                                 |
| ----------------- | ------ | ------------------------------------- |
| `id`              | string | Server-generated, opaque              |
| `type`            | enum   | `person                               | pet     | lost-vehicle | stolen-vehicle` |
| `status`          | enum   | `missing                              | found   | resolved`    |
| `title`           | string | 3–120 chars                           |
| `description`     | string | 10–4000 chars                         |
| `contact`         | string | phone or email, ≤120 chars            |
| `reward`          | number | integer ≥ 0, currency = INR (assumed) |
| `images[]`        | string | absolute HTTPS URLs                   |
| `location.lat`    | number | -90 to 90                             |
| `location.lng`    | number | -180 to 180                           |
| `location.source` | enum   | `gps                                  | manual` |
| `age`             | number | integer 0–130                         |
| `year`            | number | integer 1900–current+1                |
| `vehicleNumber`   | string | uppercase, ≤20 chars                  |
| `firNumber`       | string | ≤40 chars                             |
| `lastSeen`        | string | ISO-8601 datetime, must be ≤ now      |

---

## 12. Endpoint Cheat-Sheet

| Method | Path                       | Auth | Purpose                       |
| ------ | -------------------------- | ---- | ----------------------------- |
| POST   | /auth/register             | -    | Create account                |
| POST   | /auth/login                | -    | Get JWT                       |
| POST   | /auth/refresh              | -    | New access token              |
| POST   | /auth/logout               | ✓    | Invalidate refresh            |
| POST   | /auth/forgot-password      | -    | Send reset email              |
| POST   | /auth/reset-password       | -    | Consume reset token           |
| GET    | /auth/me                   | ✓    | Current user                  |
| GET    | /users/me                  | ✓    | Profile                       |
| PATCH  | /users/me                  | ✓    | Update profile                |
| POST   | /users/me/avatar           | ✓    | Upload avatar                 |
| POST   | /users/me/change-password  | ✓    | Change password               |
| GET    | /reports                   | -    | List/search reports           |
| GET    | /reports/{id}              | -    | Report detail                 |
| POST   | /reports                   | ✓    | Create report                 |
| PATCH  | /reports/{id}              | ✓    | Edit report                   |
| PATCH  | /reports/{id}/status       | ✓    | Change status                 |
| DELETE | /reports/{id}              | ✓    | Delete                        |
| GET    | /reports/mine              | ✓    | Caller's reports              |
| GET    | /reports/stats             | -    | Global stats                  |
| GET    | /reports/map               | -    | Compact geo list              |
| POST   | /uploads/images            | ✓    | Upload images                 |
| POST   | /alerts/subscriptions      | ✓    | Subscribe to city/type alerts |
| GET    | /alerts/subscriptions      | ✓    | List subscriptions            |
| DELETE | /alerts/subscriptions/{id} | ✓    | Unsubscribe                   |
| GET    | /notifications             | ✓    | List notifications            |
| POST   | /notifications/{id}/read   | ✓    | Mark read                     |
| POST   | /notifications/read-all    | ✓    | Mark all read                 |
| GET    | /dashboard/summary         | ✓    | Dashboard tiles               |
| GET    | /geo/reverse               | ✓    | Optional reverse-geocode      |
| GET    | /geo/forward               | ✓    | Optional forward-geocode      |

---

## 13. Frontend Integration Notes (for the backend team)

- The frontend currently mocks these endpoints in `src/services/reports.service.ts` and `src/lib/auth-context.tsx`. Once the backend is live, only those files change; UI components remain untouched.
- Frontend types live in `src/services/types.ts`. If backend chooses the nested `location`/`details` shape (recommended), the frontend adapter will map to that flat type.
- Dark/light mode, filters, sorting, and map filtering are all client-side after data loads — backend does not need to know about theme or UI state.
- Image URLs must be absolute HTTPS (no relative paths) so `<img>` tags render across environments.
- All list endpoints must return stable ordering when `sort` is the same, so pagination is deterministic (tie-break by `id` desc).

---

**Owner:** Frontend team. Any change to request/response shape MUST be coordinated here first; the frontend adapter depends on this contract.
