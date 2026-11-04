# Visa Application Portal — Full-stack React + Spring Boot + PostgreSQL

This repository is the separated full-stack migration of the uploaded Visa Portal project. The running application is a standalone React SPA; Spring Boot owns the REST API, authentication, validation, file storage, admin workflow and Gemini integration; PostgreSQL stores users and applications.

## Technology stack

- **Frontend:** React 19, Vite, Axios, Lucide icons
- **Backend:** Java 21, Spring Boot 3.4, Spring Security, JWT, Spring Data JPA
- **Database:** PostgreSQL 16, Hibernate JSONB fields
- **AI:** Existing Google Gemini `generateContent` API flow, invoked only by the backend
- **Deployment:** Docker Compose (PostgreSQL, API, frontend)

## Features migrated

### Applicant
- Registration and login
- Email verification (SMTP configurable; local development auto-verifies when `MAIL_ENABLED=false`)
- Forgot-password and reset-password flow
- Applicant dashboard and application history
- Create, continue, view and delete draft applications
- Five-step application form: visa details, personal details, passport details, address details and security declaration
- Applicant photo, PAN, Aadhaar and passport file uploads
- Server-side section validation and final submission validation
- Submitted application status and administrator feedback
- Profile update, password change, account deletion and light/dark appearance setting

### Administrator
- Role-protected admin access
- Dashboard counts: total applications, pending review, approved today and rejected today
- Application list with search, status filter, date range, AI risk filter and sorting
- Full application details and all five form sections
- Trigger AI analysis on demand
- AI findings, summary and raw JSON inspection
- Change application status, require applicant feedback for rejection / attention, and maintain internal notes
- Human administrator retains final approval/rejection authority

### AI integration
- Existing Gemini API family and vision model setting retained
- Face-presence checks for applicant and passport photos
- Passport OCR extraction
- Applicant-entered vs OCR consistency checks
- Risk category and human-readable AI summary
- Liveness/tampering are marked unavailable when the configured Gemini flow does not provide those capabilities

**Important AI integrity note:** The Laravel source generated a random placeholder `photo_match_score`; it was not biometric face comparison. The migration does not fabricate that score. The result explicitly reports it as unavailable. Gemini face-presence detection is not a biometric match or liveness check. AI output is advisory only.

## Run with Docker (recommended)

1. Copy `.env.example` to `.env` in the project root.
2. Set a strong `JWT_SECRET`, a PostgreSQL password, and your **newly rotated** Gemini API key.
3. Start the application:

```bash
docker compose up --build
```

4. Open the frontend at `http://localhost:5173`. The API is at `http://localhost:8080/api`.

The first start creates the PostgreSQL tables through Hibernate. Uploaded files are stored in the `visa_uploads` Docker volume and are not publicly served; access goes through authenticated API endpoints.

## Run locally without Docker

### PostgreSQL

Create a database named `visa_app` and ensure PostgreSQL is running.

### Backend

Requirements: Java 21+, Maven 3.9+, PostgreSQL 14+.

```bash
cd backend
export DATABASE_URL=jdbc:postgresql://localhost:5432/visa_app
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=your_password
export JWT_SECRET='replace-with-a-long-random-secret-of-at-least-32-characters'
export AI_SERVICE_API_KEY='your-new-gemini-api-key'
export GEMINI_MODEL_VISION='gemini-1.5-pro-latest'
export MAIL_ENABLED=false
mvn spring-boot:run
```

When `MAIL_ENABLED=false`, accounts are auto-verified for local development. For real email verification and password reset, set `MAIL_ENABLED=true` and configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and `FRONTEND_URL`.

### Frontend

Requirements: Node.js 20+.

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`.

## Create an administrator

Register a user through the app, then run this SQL in PostgreSQL:

```sql
UPDATE users SET role = 'admin' WHERE email = 'admin@example.com';
```

Sign out and sign back in. Admin access also requires a verified email when SMTP verification is enabled.

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register applicant |
| POST | `/api/auth/login` | Login and receive JWT |
| GET | `/api/auth/me` | Current account |
| POST | `/api/auth/verify-email?token=...` | Verify email |
| POST | `/api/auth/resend-verification` | Resend verification |
| POST | `/api/auth/forgot-password` | Request password reset |
| POST | `/api/auth/reset-password` | Reset password using token |
| PATCH | `/api/auth/profile` | Update profile |
| PATCH | `/api/auth/password` | Change password |
| DELETE | `/api/auth/account` | Delete account and applications |
| GET / POST | `/api/applications` | List / create applicant applications |
| GET | `/api/applications/{id}` | Read own application |
| PATCH | `/api/applications/{id}/sections/{section}` | Save one of five form sections |
| POST | `/api/applications/{id}/files` | Upload evidence |
| GET | `/api/applications/{id}/files/{filename}` | Authenticated file access (owner/admin) |
| POST | `/api/applications/{id}/submit` | Submit application |
| DELETE | `/api/applications/{id}` | Delete a draft |
| GET | `/api/admin/dashboard` | Admin metrics |
| GET | `/api/admin/applications` | Admin list and filters |
| GET | `/api/admin/applications/{id}` | Admin application detail |
| PATCH | `/api/admin/applications/{id}` | Update status, feedback and notes |
| POST | `/api/admin/applications/{id}/trigger-ai` | Run Gemini analysis |

Section slugs: `visa-details`, `personal-details`, `passport-details`, `address-details`, `security-declaration`.

## Data model

`users` stores accounts, role and email-verification state. `applications` stores the owner, status, timestamps, AI findings, summary and admin feedback/notes. The five form sections are stored as PostgreSQL `jsonb` columns (`visa_details`, `personal_details`, `passport_details`, `address_details`, `security_declaration`) so the original section field names remain intact. `action_tokens` stores expiring email-verification and password-reset tokens.

## Legacy source reference

- `migration-reference/legacy-react-pages/` retains the original React/Inertia pages, components, layouts and hooks for visual/behavior comparison.
- `migration-reference/legacy-laravel/` retains the original Laravel controllers, models, validation requests, routes, migrations and AI service for migration traceability.

The running app does not depend on Laravel, PHP, Inertia or Ziggy.

## Data migration from an existing deployment

The uploaded archive contains application source code but no live production database dump or user-upload directory. Therefore, this project creates a new PostgreSQL schema; it does not invent or silently migrate user records. Before replacing a live system, export the existing `users`, `applications` and five detail tables, map them to the JSONB columns described above, copy uploaded files into the matching `uploads/application-{id}` directories, and verify record counts and file access.

## Security and production checklist

- Rotate any API key that was present in the uploaded source archive. Never commit `.env` files.
- Set a unique strong `JWT_SECRET` and strong PostgreSQL credentials.
- Enable SMTP and email verification in production.
- Keep uploads on private storage; the API does not expose `/uploads/**` as public static content.
- Add malware scanning, backup/restore tests, audit logs, rate limiting, monitoring and a reviewed retention policy before production use.
- Replace Hibernate `ddl-auto: update` with managed Flyway/Liquibase migrations for production deployments.
- Validate AI results with human review; do not use the AI risk label as an automated visa decision.
