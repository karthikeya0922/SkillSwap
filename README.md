# SkillSwap

**Learn. Teach. Exchange. Grow.**

SkillSwap is a peer-to-peer skill exchange platform for college students. Students list what they can teach and what they want to learn, get matched with classmates (ideally two-way swaps), chat in real time, book sessions, and pay with **time credits** — one completed teaching hour earns one credit.

````
Register → Build profile → Add skills → Discover matches → Connect → Chat
        → Book session → Credits held → Session completed → Rate → Reputation
````

## Features

| Area | What it does |
|---|---|
| Auth | Registration, login, JWT (BCrypt-hashed passwords), role-based access (`STUDENT`, `ADMIN`), login throttling, persistent sessions |
| Profiles | Photo upload, bio, college/department/year, weekly availability, teach/learn skills with proficiency and experience, stats, badges, reviews |
| Skill catalogue | 14 categories, 50+ skills, admin-managed (unused skills are deleted, used ones deactivated) |
| Matching | Deterministic 0–100 score with plain-English reasons ("You want to learn UI/UX and Rahul teaches UI/UX…") |
| Discover | Search plus filters for skill, category, proficiency, rating, department and availability; sort by match, rating, sessions, newest |
| Connections | Request, accept, decline, cancel, remove. Only connected students can chat |
| Learning requests | "I want to learn X" posts; teachers of X offer help; the learner accepts one and books a session |
| Sessions | Booking with overlap, self-booking, past-time and length checks; accept/decline/cancel/complete lifecycle; reminders; auto-expiry and auto-completion |
| Time wallet | Ledger-backed balance, credits held when a session is accepted, released on completion, refunded on cancellation; overspending prevented |
| Ratings | 1–5 stars plus teaching quality, communication and knowledge; one review per session; averages recomputed from the table |
| Real-time | STOMP over WebSocket: messages, typing indicator, read receipts, online presence, live notifications |
| Community | Learning groups (public/private, invitations, discussion, group sessions and events) and skill challenges (submissions, peer reviews) |
| Reputation | XP for teaching, learning, good ratings, helping and challenges; ranks from Beginner to Community Master; 9 badges |
| Admin | Analytics dashboard (Recharts), user search/suspend/reactivate, skill and category management, report review with warn / remove content / suspend actions |
| API docs | Swagger UI at `/swagger-ui.html` (OpenAPI 3 at `/v3/api-docs`), with a JWT **Authorize** button |
| Performance | Cached skill catalogue (`@Cacheable` / `@CacheEvict`), async WebSocket notification delivery (`@Async`), automatic retry of failed reads in the frontend |
| Monitoring | Spring Boot Actuator health and metrics, per-request timing logs, structured JSON log files |
| Resilience | React error boundaries with a recovery screen; chat messages support Markdown (bold, italic, code, lists, links) |

## Tech stack

- **Frontend:** React 19, Vite, Tailwind CSS 4, React Router, Axios, React Hook Form, Recharts, Lucide icons, `@stomp/stompjs`
- **Backend:** Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA/Hibernate, Validation, WebSocket, Cache, Actuator), JJWT, springdoc-openapi, Logback, Maven wrapper
- **Database:** MySQL 8 (H2 in MySQL mode for tests only)
- **Ops:** Dockerfiles and Docker Compose

## Running locally

**Prerequisites:** JDK 21+, Node 20+, and MySQL 8 running locally. Maven is not required because the project includes the Maven wrapper.

### 1. Backend

```bash
cd backend
cp .env.example .env        # then edit DB_PASSWORD and JWT_SECRET
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

`backend/.env` is git-ignored and is loaded automatically. Settings you can put in it:

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL connection (the database is created if missing) | `localhost:3306/skillswap`, `skillswap`, — |
| `JWT_SECRET` | HMAC signing key, at least 32 characters (**required**) | — |
| `SERVER_PORT` | API port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins | `http://localhost:5173` |
| `SEED_ENABLED` | Seed demo data into an empty database | `true` |
| `SEED_DEMO_PASSWORD` | Password for all demo students (no students are seeded if this is blank) | — |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin account (no admin is seeded if the password is blank) | `admin@skillswap.dev` |

### 2. Frontend

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173
```

Vite proxies `/api`, `/ws` and `/uploads` to the backend. If the backend is not on port 8080, create `frontend/.env.local` containing `VITE_BACKEND_URL=http://localhost:<port>`.

### Demo accounts

With seeding enabled, every demo student signs in as `<firstname>@skillswap.dev` using `SEED_DEMO_PASSWORD`. Good accounts to start with:

- `karthikeya@skillswap.dev`: the most complete profile, with sessions, matches, chats, a pending group invite and incoming requests
- `rahul@skillswap.dev`: a two-way match with Karthikeya (UI/UX ↔ Java)
- `admin@skillswap.dev` (with `ADMIN_PASSWORD`): the analytics and moderation console. One reported spam account is waiting for review.

## Docker

```bash
cp .env.example .env        # fill in secrets
docker compose up --build   # open http://localhost:3000
```

This starts MySQL 8.4, the API and an nginx container that serves the built frontend and proxies the API and WebSocket traffic.

## Deploying for free without a card (Render + Aiven)

1. **Database:** create a free MySQL service at [aiven.io](https://aiven.io). Note the host, port, user and password from its overview page.
2. **App:** on [render.com](https://render.com) choose **New → Blueprint**, pick this repo and fill in the prompted values:
   `DB_URL=jdbc:mysql://<host>:<port>/defaultdb?sslMode=REQUIRED&serverTimezone=Asia/Kolkata`, `DB_USERNAME`, `DB_PASSWORD`, `SEED_DEMO_PASSWORD` and `ADMIN_PASSWORD`.
3. **Stay awake:** free Render services sleep after 15 idle minutes. Add a free monitor on [cron-job.org](https://cron-job.org) or [UptimeRobot](https://uptimerobot.com) that requests `https://skillswap-api-karthikeya.onrender.com/api/categories` every 10 minutes.

On Render's free plan, uploaded profile photos are lost whenever the API restarts or redeploys.

## Deploying (free, always on)

The whole stack runs on one [Oracle Cloud Always Free](https://www.oracle.com/cloud/free/) VM (Ampere A1, Ubuntu). In the VM's subnet security list, allow inbound TCP 80 and 443. Then SSH in and run:

```bash
git clone https://github.com/karthikeya0922/SkillSwap.git && cd SkillSwap
bash deploy/setup.sh
```

The script installs Docker, opens the firewall, generates `.env` with random secrets and starts everything behind Caddy. Caddy serves HTTPS on `https://<ip-with-dashes>.sslip.io`. It prints the URL and the demo and admin passwords when it finishes. To update the server later, run `git pull && bash deploy/setup.sh`.

## Tests

```bash
cd backend && ./mvnw test
```

There are 42 tests:

- unit tests for the match algorithm
- Mockito unit tests for `WalletService` and `NotificationService` (repositories and the WebSocket pusher are mocked)
- cache tests that check reads fill the catalogue caches and every relevant write evicts them
- JPA relation tests for the `@OneToMany` and `@ManyToMany` mappings
- endpoint tests for Swagger, Actuator access rules and request IDs
- HTTP tests for auth and security (validation, duplicate email, 401 and 403 responses)
- session and credit-ledger integration tests (overlap, past bookings, self-booking, insufficient credits, escrow, refunds, duplicate ratings)
- a full seed run that checks every wallet's ledger sums to its balance

## Architecture

```
backend/src/main/java/com/skillswap
├── auth, user, skill, match, connection, request, session,
│   wallet, rating, chat, notification, group, challenge,
│   reputation, report, admin, dashboard      ← one package per feature
│       Controller → Service → Repository → MySQL, with DTOs and mappers
├── security    JWT filter, token authenticator, JSON 401/403 handlers
├── common      Error envelope, global exception handler, paging, file storage
└── config      Security, WebSocket (STOMP), CORS, seed data
```

- **Responses.** Successes use `{ success, status, message?, data, timestamp }`. Errors use `{ success: false, status, message, errors?, path, timestamp }`.
- **Credits.** Only `WalletService` changes a balance. Every change writes a ledger row in the same transaction, and debits take a row lock.
- **Authorization.** User IDs always come from the JWT principal and never from request bodies. Each service checks ownership or participation, and admin routes are restricted at the filter chain.
- **Real-time.** The STOMP `CONNECT` frame carries the JWT, and clients may only subscribe to their own `/user/queue/*` destinations. Pushes are sent after the database transaction commits.
- **Relations.** Most links are `@ManyToOne` from the owning entity. `User.skills` and `SkillGroup.memberships` are their lazy `@OneToMany` inverse sides. `SkillGroup.activeMembers` is a read-only `@ManyToMany` over the existing `group_members` table. None of them add columns or tables.

## API docs, monitoring and logs

| What | Where | Access |
|---|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` | Public. Log in with `POST /api/auth/login`, then paste the token into **Authorize** |
| OpenAPI JSON | `/v3/api-docs` | Public (set `SWAGGER_ENABLED=false` to turn both off) |
| Health | `/actuator/health` | Public status; database and disk details for admins |
| Metrics | `/actuator/metrics`, `/actuator/metrics/{name}` | Admin token only |
| Caches | `/actuator/caches` | Admin token only |

- **Caching.** The category list and the full skill catalogue are cached in memory. Creating, editing or deleting a category or skill, or adding or removing a profile skill, evicts them once the transaction commits. Filtered searches are not cached, so the cache stays small.
- **Async.** Notification rows are saved inside the request's transaction, and the WebSocket push runs afterwards on the `skillswap-async-` thread pool.
- **Logging.** The console uses Spring Boot's normal format, tagged with a request ID. `logs/skillswap.json` (set by `LOG_PATH`) holds one JSON object per line, rolls daily or at 10 MB, and keeps 14 days. Every `/api` call logs its method, path, status and duration, and calls slower than `SLOW_REQUEST_MS` (default 1000) are logged as warnings. Responses carry an `X-Request-Id` header that matches the log lines.
- **Frontend.** `GET` requests are retried up to twice, with backoff, on network errors and 502/503/504 responses. Writes are never retried automatically.

### Match score (0–100)

| Signal | Points |
|---|---|
| They teach a skill you want (40 for the first, +5 for each extra, −3 for each skill where their level is below your target) | up to 50 |
| They want a skill you teach (a two-way swap) | 25–30 |
| Shared interest categories | up to 8 |
| Overlapping weekly availability | up to 7 |
| Their teacher rating (2.5 if unrated) | up to 5 |

Scores of 80 and above are labelled 🔥 **Great SkillSwap Match**.

### Session lifecycle

`REQUESTED → ACCEPTED` (credits held; logistics pending) `→ SCHEDULED` (link or location set) `→ ONGOING → COMPLETED` (credits released to the teacher). `REJECTED` and `CANCELLED` (held credits refunded) are terminal. Unanswered requests expire at their start time. Sessions the learner never confirms complete automatically 48 hours after they end.

## Production notes

- Set `SEED_ENABLED=false` and use a strong `JWT_SECRET`.
- The schema uses `ddl-auto=update` for convenience. Switch to a migration tool before running with real data at scale.
- Presence tracking and login throttling are in-memory, so they assume a single backend instance.
