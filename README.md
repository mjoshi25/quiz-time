# Quizora Phase 1C Full Stack

Modern React/Vite frontend plus Spring Boot/MongoDB/JWT backend.

## Run backend

Requirements: Java 17, Maven 3.9+, MongoDB or MongoDB Atlas.

Set environment variables:

```text
MONGODB_URI=mongodb://localhost:27017/quiz_competition
JWT_SECRET=change-this-secret-to-at-least-32-characters-long
ADMIN_EMAIL=admin@quizora.com
ADMIN_PASSWORD=Admin@12345
FRONTEND_ORIGIN=http://localhost:5173
```

Then run:

```bash
cd backend
mvn spring-boot:run
```

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Optional `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080/api
```

## Test sequence

1. Register a participant and log in.
2. Register a host. The host remains PENDING.
3. The pending host cannot access `/api/host/dashboard`; backend authorization enforces approval.
4. Log in as the bootstrap admin.
5. Open Admin approvals and approve the host.
6. Log in as the host again and open the Host Centre.

Admin is created separately at startup; public registration cannot create an admin.


## Quiz editor improvements and Class 1 seed quiz

- The host quiz editor now keeps **Add question** after the question list and scrolls to a newly added question.
- On Create Quiz, hosts can select one of their existing quizzes and clone its questions/settings into a new draft. The copied title is editable, and scheduled start/end times are cleared so the new draft does not inherit an old schedule.
- On backend startup, an idempotent seed creates **Class 1 Mathematics - Basics (30 Questions)** for the existing `host1@quiztime.com` account, if that account exists with role `HOST`. The quiz starts as `DRAFT`; it does not bypass host approval or publish automatically. If the account does not exist yet, register/create that host account and restart the backend to seed the quiz.
