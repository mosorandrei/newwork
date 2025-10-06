# NEWWORK – Core API + SPA (BFF) – Candidate Submission

This repository contains a **Java Spring Boot** backend (`apps/core-api`) and a **TypeScript NestJS + React** web app (`apps/web`) packaged to run with **Docker Compose**.  
The scope focuses on the “Employee Profile (HR App)” scenario with clear role‑based access, optimistic concurrency, and AI‑polished feedback using a HuggingFace model.

---

## Quick Start (Docker)

### Prerequisites
- Docker Desktop 4.x+ (or Docker Engine 24+)
- Docker Compose v2
- Optional: a HuggingFace Inference API Token (free) if you want AI polishing to work

### 1) Run
```bash
docker compose up --build
```
This will start:
- `db` (Postgres 16) on **localhost:5432**
- `core-api` (Spring Boot) on **localhost:8081**
- `web` (NestJS + React SPA) on **localhost:3000**

### 2) Open the apps
- Swagger UI:  
  - http://localhost:8081/swagger-ui.html (redirects to …/index.html on some setups)  
  - or http://localhost:8081/swagger-ui/index.html
- Web SPA: http://localhost:3000

---

## Demo Users (seeded)

| Role     | Email                    | Password  | Notes                                   |
|----------|--------------------------|-----------|-----------------------------------------|
| MANAGER  | manager@newwork.test     | Passw0rd! | Links to Alice (Manager/Owner)          |
| EMPLOYEE | bob@newwork.test         | Passw0rd! | Used for absence requests / own profile |
| COWORKER | carol@newwork.test       | Passw0rd! | Can give feedback to others             |

> Seed data includes 3 employees (Alice, Bob, Carol), sample profiles (with sensitive fields), feedback samples, and absence examples.

---

## Endpoints Overview (Core API)

- **Auth** – `POST /auth/login` → `{ token, role, employeeId }`
- **Employees** – `GET/POST /api/employees`, `GET/PUT/DELETE /api/employees/{id}`  
  Uses **ETag / If-Match** for concurrency.
- **Profiles** – `GET/PUT /api/employees/{id}/profile`  
  Sensitivity masking based on role/ownership.
- **Feedback** – `GET/POST /api/employees/{id}/feedback`  
  Create will **polish** text using HuggingFace.
- **Absences** – `POST /api/employees/{eid}/absences`, `GET /api/employees/{eid}/absences`,  
  `GET /api/absences/{id}`, `PUT /api/absences/{id}/approve|reject|cancel` (ETag protected).

Security is **JWT (HMAC)**. Send `Authorization: Bearer <token>` with every API call.  
CORS is configured for `http://localhost:3000` (the SPA origin).

---

## Step‑by‑Step Swagger Scenario

1) **Login (any user)**  
   - `POST /auth/login` with body:
     ```json
     { "email": "manager@newwork.test", "password": "Passw0rd!" }
     ```
   - Copy the `token` from the response.
   - In Swagger click **“Authorize”**, paste `Bearer <token>`.

2) **Manager – list employees**  
   - `GET /api/employees` → 200 OK with all employees.

3) **Manager – view an employee (ETag)**  
   - `GET /api/employees/{id}` → response header contains `ETag: "N"`  
     Save this value. It’s required for updates/deletes.

4) **Manager – update an employee (optimistic concurrency)**  
   - `PUT /api/employees/{id}` with header `If-Match: "N"`  
     Body:
     ```json
     { "firstName": "Robert" }
     ```
   - If the `If-Match` does not equal server version you’ll get:
     - `412` for bad/missing `If-Match` or
     - `409` with body `{ "currentVersion": X }`. Re‑GET and retry.

5) **Owner vs Coworker – profile visibility**  
   - Login as `bob@newwork.test` and `GET /api/employees/{bobId}/profile` → full data (owns it).
   - Login as `carol@newwork.test` and `GET /api/employees/{bobId}/profile` → sensitive fields masked (`salary=null`, `ssnMasked=null`, `address=null`).

6) **Owner – update profile (ETag)**  
   - As Bob: `GET /api/employees/{bobId}/profile` → take `ETag: "M"`  
   - `PUT /api/employees/{bobId}/profile` with `If-Match: "M"` body:
     ```json
     {
       "bio": "Full‑stack engineer focusing on product impact.",
       "skillsJson": "{\"skills\":[\"Java\",\"Spring\",\"React\",\"NestJS\"]}",
       "contactEmail": "bob@newwork.test"
     }
     ```

7) **Coworker/Manager – create feedback with AI polish**  
   - Login as `carol@newwork.test`.  
   - `POST /api/employees/{bobId}/feedback` body:
     ```json
     { "text": "Bob deliver feature fast and communicate clear" }
     ```
   - Response contains both `textOriginal` and `textPolished`.  
     > If `HF_API_TOKEN` isn’t set or the model is down, API returns `502 hf_unavailable`.

8) **Employee – create absence**  
   - Login as `bob@newwork.test`.
   - `POST /api/employees/{bobId}/absences` body:
     ```json
     {
       "type": "VACATION",
       "startDate": "2025-11-03",
       "endDate": "2025-11-08",
       "reason": "Family trip"
     }
     ```
   - Response includes `ETag: "K"` and `Location: /api/absences/{id}`.

9) **Manager – approve/reject (ETag)**  
   - As manager: `GET /api/absences/{id}` → take `ETag: "K"`  
   - `PUT /api/absences/{id}/approve` with header `If-Match: "K"` and optional body:
     ```json
     { "comment": "Enjoy!" }
     ```

---

## Architectural Decisions (highlights)

- **Clean layering** – `web` (controllers), `service` (use‑cases), `repo` (JPA), `domain` (entities). Each layer has single responsibility and small public surface.
- **Role‑aware access helpers** – `Access` encapsulates all “who can do what” checks (manager/owner/coworker). This keeps controllers lean and testable.
- **Optimistic concurrency everywhere it matters** – ETag + If‑Match support lives in a reusable `Etags` helper, with a global handler for version mismatch returning `409 { currentVersion }` to enable client reconciliation.
- **Clear data privacy** – `ProfileView` ensures coworker responses **omit** sensitive fields; masking (`****1234`) is explicit and centralized in the service.
- **JWT with explicit claims** – subject = userId, custom claims for role and employeeId. `JwtAuthFilter` is tiny and safe: invalid tokens simply yield anonymous context.
- **HuggingFace integration with resilient retry** – `HfClientRest` + `HfRetryProps` + `HuggingFacePolishService` implement backoff, jitter and status‑based retry, surfacing proper HTTP codes.
- **Swagger groups** – separate groups (`employees`, `profiles`, `feedback`, `absences`, `auth`) for demo clarity.
- **Transaction boundaries** – write flows use service‑level transactional work; seed data done via `DataSeederRunner` in a single transaction for deterministic startup.
- **Dev‑friendly defaults** – schema auto‑update, CORS for `http://localhost:3000`, seed data and demo users.

> In short: small, composable classes, behavior close to the domain, and HTTP semantics that make the UI robust.
