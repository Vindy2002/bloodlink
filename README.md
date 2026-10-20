# BloodLink – Emergency Blood Donor Finder

Spring Boot 3 (Java 17) REST API + React (Vite) web app.

## Run it with Docker (easiest)
Only Docker Desktop is needed:
```bash
docker compose up --build
```
Open **http://localhost:3000** (API on :8080). First build takes a few minutes. Stop with `Ctrl+C`; `docker compose down -v` also wipes the database.

### See the database in your browser
Open **http://localhost:8081** (Adminer). Log in with: System `PostgreSQL`, Server `db`, Username `bloodlink`, Password `bloodlink`, Database `bloodlink`. Then click a table (e.g. `donor_profiles`) → **Select data**.

## Run it without Docker

**Backend** (needs Java 17+ and Maven)
```bash
cd backend
mvn spring-boot:run          # http://localhost:8080  (H2 file database, demo data auto-created)
```
**Frontend** (needs Node 18+)
```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173  (proxies /api to :8080)
```

## Demo logins (created on first start)
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@bloodlink.lk | Admin@123 |
| Verified hospital | hospital@kurunegala.lk | Hospital@123 |
| Verified hospital | hospital@colombo.lk | Hospital@123 |
| Pending hospital | pending@kandy.lk | Hospital@123 |
| Donor (A+, Kurunegala) | donor1@demo.lk | Donor@123 |
| Donors 2..60 | donorN@demo.lk | Donor@123 |

Try this flow: log in as the Kurunegala hospital → send an A+ request → log in as donor1 → accept it →
back as the hospital, see the phone number, record the donation → donor becomes "verified" and is paused for 4 months.
Then log in as admin to approve the pending hospital.

## Features
- Donor registration (blood group + city/GPS), profile, **availability toggle**, automatic 4-month eligibility (max 3 donations a year)
- **Geo search**: bounding-box query in the DB + Haversine distance in Java, nearest first, compatible blood groups, map view
- **Request alerts**: verified hospital sends a request → all eligible compatible donors in radius get in-app + SMS + email (async). One-tap "widen +10 km" re-alerts new donors only
- **Hospital verification workflow**: register → PENDING → admin approves/declines (with reason) → only VERIFIED hospitals can send requests, see phones in search, record donations
- **Donation history**: hospital-confirmed (makes donor "verified") and donor self-reported entries
- **Privacy controls**: hide from search, stronger location blur, phone hidden unless donor opts in or accepts a request, names shortened, exact coordinates never leave the server, rate-limited search/login
- JWT auth with roles DONOR / HOSPITAL / ADMIN, BCrypt passwords

## Donor eligibility (enforced when registering)
- Age 18 to 55 (56 to 60 only if a regular donor), weight over 50 kg
- Cannot register: blood-borne infections, diabetes, high blood pressure, heart/kidney/liver disease, asthma, epilepsy
- Cannot register right now: pregnant/breastfeeding, tattoo or piercing in last 12 months, recent surgery, antibiotics or special medicines, fever/infection, recent vaccine, not feeling well
- Switching availability ON asks the donor to confirm they are currently fit
- Donation gap: 4 months

## Configuration (backend/src/main/resources/application.yml, or environment variables)
| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | **Change before going live** (32+ random chars) |
| `SEED_DEMO` | `false` to stop creating demo data |
| `CORS_ORIGINS` | Your frontend URL(s), comma separated |
| `SMS_ENABLED`, `SMS_URL`, `SMS_TOKEN`, `SMS_SENDER` | Real SMS. See below |
| `spring.mail.*` | SMTP settings for real email (commented example in application.yml) |
| `SPRING_PROFILES_ACTIVE=postgres` | Use PostgreSQL (already set in docker-compose.yml) |

### SMS
By default SMS and email are **logged to the console** so you can test without accounts.
`SecurityConfig.smsGateway()` posts `{to, message, from}` as JSON with a Bearer token to `SMS_URL`.
Most Sri Lankan providers (e.g. Notify.lk, Dialog, Hutch bulk SMS) use a slightly different request format,
so adjust that one small method to match your provider's API docs.

## API summary
```
POST /api/auth/register/donor | /register/hospital | /login          GET /api/me
GET  /api/search/donors?bloodGroup=A_POS&lat=7.48&lng=80.36&radiusKm=25&compatible=true   (public)
GET  /api/public/stats
DONOR     GET/PUT /api/donor/profile   PATCH /api/donor/availability   GET /api/donor/alerts
          POST /api/donor/alerts/{requestId}/accept|decline   GET/POST /api/donor/donations
HOSPITAL  GET /api/hospital/me   POST/GET /api/hospital/requests   GET /requests/{id}/responses
          POST /requests/{id}/escalate   PATCH /requests/{id}/status   POST/GET /api/hospital/donations
ADMIN     GET /api/admin/hospitals?status=PENDING   POST /hospitals/{id}/verify | /reject   GET /api/admin/stats
ANY AUTH  GET /api/notifications   POST /api/notifications/read-all
```
Blood groups in the API: `A_POS A_NEG B_POS B_NEG AB_POS AB_NEG O_POS O_NEG`.

## Before real-world use
- Serve over HTTPS and set a strong `JWT_SECRET`.
- Donor data is health-related personal data. Add a consent checkbox + privacy policy, and review Sri Lanka's Personal Data Protection Act.
- Mobile-number verification (OTP) for donors would reduce fake sign-ups.
- Hospital approval is manual: confirm registration numbers by calling the hospital's official line.
- Rate limiting is in-memory (one server). Use Redis or a gateway if you run several instances.
- Eligibility rules (age 18-55, up to 60 for regular donors, weight over 50 kg, health declaration, 4-month gap) are in `Eligibility.java` and `data.js`. Confirm them with the National Blood Transfusion Service. Haemoglobin 12.5 g/dl is tested at the blood bank, not online.
