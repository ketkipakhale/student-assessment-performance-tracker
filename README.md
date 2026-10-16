# Student Assessment and Performance Tracking System

A local, same-origin Spring Boot demo for creating assessments, taking published quizzes,
and tracking student performance. It uses Java 17, Spring Boot 3, Spring Web, Spring Data
JPA, Bean Validation, MySQL, Swagger/OpenAPI, JUnit 5, and a plain HTML/CSS/JavaScript
frontend served by the application.

## Architecture

Requests flow through `Controller -> Service -> Repository -> MySQL`. The frontend is in
[`src/main/resources/static/`](src/main/resources/static/). BCrypt password hashes and
roles are stored on the existing `students` table; passwords are never returned. Sessions
are server-side and CSRF remains enabled for browser state-changing requests.

Important relationships remain:

```text
Assessment 1 -> many Questions 1 -> many QuestionOptions
Student 1 -> many Submissions -> 1 Assessment
Submission 1 -> many SubmissionAnswers -> 1 Question
```

The application is a local monolith: Spring Boot serves both the REST API and the static
frontend. It is not deployed or hosted externally. Assessments cascade to their questions
and options; submissions and submission answers preserve the assessment and answer history.

## Roles and API permissions

- Public: `GET /`, Swagger, `POST /api/auth/register`, `POST /api/auth/login`,
  `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`, and the CSRF-token
  endpoint. Registration always creates a `STUDENT`; email and password validation and
  duplicate-email conflicts are handled by the API.
- `ADMIN`: create, list, inspect, replace, publish/unpublish, and delete assessments under
  `/api/admin/assessments`.
- Authenticated students: list/open published assessments, submit answers, and view only
  their own results and progress.
- Logout is `POST /api/auth/logout`. The browser sends the `X-XSRF-TOKEN` cookie value for
  state-changing requests.
- Unauthorized API requests return 401; authenticated users without the required role
  return 403.

Student submissions derive identity from the authenticated session. Any legacy
`studentId` field in an old submission payload is ignored when security is enabled.
Scores and percentages are calculated only from the stored answer key. The legacy
`POST /api/students` account-creation route is no longer public; use `/api/auth/register`.

## MySQL setup (no Docker)

1. Install and start MySQL Community Server 8.x.
2. Create the local database and user:

   ```sql
   CREATE DATABASE assessment_db;
   CREATE USER 'assessment_user'@'localhost' IDENTIFIED BY '<choose-a-local-password>';
   GRANT ALL PRIVILEGES ON assessment_db.* TO 'assessment_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

   Use `ALTER USER` if the account already exists.
3. Copy the ignored environment template and change local values:

   ```powershell
   Copy-Item .env.example .env
   ```

   `.env` is ignored by Git. Do not commit real database or admin credentials.
4. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in `.env` before first startup. When both are
   present, the application hashes the password and creates the admin account if that email
   does not exist. The application does not log the password. If the email already exists,
   it is left unchanged.

The default local profile also creates or repairs a published assessment named **Java Basics
Demo** if it does not already exist or has an incomplete unsubmitted definition. Any older
Java Basics demo records are retained but changed to `DRAFT`, so they are hidden from students.
For this demo, it contains exactly ten beginner-friendly Java questions, four options per
question, one correct option per question, and one point per question. Existing submitted
assessments are not rewritten, so saved results remain safe. Restarting the app repeats this
cleanup safely without creating or republishing older demo records.

The current local configuration intentionally keeps `ddl-auto: update` and
`createDatabaseIfNotExist=true`; it does not switch databases or require Docker.

## Prerequisites

- Java 17 or newer (`java -version`)
- Maven with the `mvn` command available (`mvn -version`)
- MySQL Community Server 8.x, running locally
- A database user with permission to create/update the local schema

## Run

From the nested project directory
`C:\Users\KETAKI\Downloads\student-assessment-tracker\student-assessment-tracker`:

```powershell
java -version
mvn -version
mvn spring-boot:run
```

Open the frontend at [http://localhost:8080/](http://localhost:8080/). Swagger is at
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
and the OpenAPI document is at `/v3/api-docs`.

For another local port, pass Spring Boot arguments separated by spaces, not commas:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082 --app.demo.seed-enabled=true"
```

Then use the matching frontend and Swagger URLs:

- Port 8080: `http://localhost:8080/` and
  `http://localhost:8080/swagger-ui/index.html`
- Port 8081: `http://localhost:8081/` and
  `http://localhost:8081/swagger-ui/index.html`
- Port 8082: `http://localhost:8082/` and
  `http://localhost:8082/swagger-ui/index.html`

If the default port is occupied, choose an available port rather than stopping an
unrelated process. The same port must be used for the frontend and Swagger URL.

## Full demo flow

1. Put `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the ignored local `.env` file, start the app,
   and sign in through the browser as the configured administrator. Do not commit or share
   the file. The application hashes the configured admin password and never logs it.
2. Create an assessment. The builder starts with 10 questions and four options per
   question; each question must have exactly one selected correct option. Add or remove
   questions as needed, then publish it.
3. Sign out and use **Create account** to register a student. Public registration can only
   create the `STUDENT` role. Use a valid email and the password required by the form;
   duplicate email addresses are rejected.
4. Sign in as the student and open **Java Basics Demo**. It contains exactly 10 questions,
   four options per question, and one correct answer per question. Navigate with Previous
   and Next; selected answers are retained. Answer all 10 questions and submit once. The
   result shows the server-calculated score and percentage. The Java Basics demo displays one
   question at a time with a `Question n of 10` indicator. Previous/Next navigation
   preserves answers; submission is blocked until all ten questions are answered. Review
   feedback appears only after the server accepts the single submission.
5. Review the student's progress history. Draft assessments are not visible or submittable.

The REST API also supports the same flow. Student-facing assessment responses never expose
the `correct` flag; saved result details can show answer feedback after submission. To
verify the demo in Swagger, authenticate as a student, call `GET /api/assessments`, find
`Java Basics Demo`, then call `GET /api/assessments/{id}` and inspect its 10 questions.

## Tests

Tests use an in-memory H2 database and do not need MySQL:

```powershell
mvn test
```

The existing suite covers assessment validation, published-only access, server-side
scoring (correct, incorrect, unanswered), duplicate and cross-question answer rejection,
saved results, progress, and persistence wiring. The test profile disables the security
filter only to keep the original service-level API regression tests deterministic; the
default application profile always enables the real session and CSRF security chain.

## Honest limitations

This is an internship/local demonstration, not a production identity platform. It has no
email verification, account lockout, MFA, audit trail, pagination, or database migration
history. It has a local password-reset flow but no email delivery. `ddl-auto: update` is
convenient locally but should be replaced with reviewed migrations and stronger operational
controls before production use.

### Local password reset limitation

Password reset tokens are cryptographically random, stored only as SHA-256 hashes, expire
after 30 minutes, invalidate older tokens for the same account, and are single-use. This
project does not configure SMTP or pretend to send email. For local development only, set
`PASSWORD_RESET_DEV_MODE=true` and set `APP_PUBLIC_URL` to the URL and port being used
(for example, `http://localhost:8082`). After requesting a reset in the UI, the generic
confirmation can display a local reset link. The development retrieval endpoint is disabled
by default and should remain disabled outside local development. Tokens are not logged and
are not returned by the production reset request endpoint.
