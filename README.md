# 📚 Library Management System

A multi-tenant SaaS REST API for managing library operations — built with Java 21 and Spring Boot 3. Each institution operates in full isolation: its own catalogue, members, loans, and reservations, all secured behind JWT authentication.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Multi-Tenancy Model](#multi-tenancy-model)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [API Reference](#api-reference)
- [Scheduler Automation](#scheduler-automation)
- [Copy Availability Model](#copy-availability-model)
- [Security](#security)
- [Response Format](#response-format)
- [Development Status](#development-status)
- [Roadmap](#roadmap)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| ORM | JPA / Hibernate |
| Security | Spring Security + JWT + HttpOnly cookies |
| Build | Maven |
| Email | Spring JavaMailSender (Gmail SMTP) |
| Utilities | Lombok, Bean Validation (`jakarta.validation`) |

---

## Architecture Overview

The backend exposes a clean REST API under the base path `/api/v1`. All responses — success or error — use a consistent envelope format (see [Response Format](#response-format)).

**Public IDs** are generated using a `SecureRandom`-based generator with the charset `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (visually ambiguous characters `0`, `O`, `1`, `I`, `L` are excluded). Entity IDs follow the pattern `PREFIX-XXXXXX` (e.g. `INST-9F3K2L`, `BOOK-K3MP9R`).

---

## Multi-Tenancy Model

Every entity belongs to an **institution**. There is no shared global data pool.

```
Institution (tenant)
   ├── Users (ADMIN, LIBRARIAN)         ← manage the library
   ├── Books (with Authors, Categories)
   ├── Members                           ← library patrons (do not log in)
   ├── Loans                             ← borrow records
   └── Reservations                      ← queue positions for unavailable books
```

The `institution_id` is extracted from the authenticated user's JWT — never accepted from the request body. All repository queries include `AND institution_id = :institutionId` as a hard filter, making cross-institution data access structurally impossible.

### User Roles

| Role | Description |
|---|---|
| `ADMIN` | Full control: manage users, books, members, loans, reservations |
| `LIBRARIAN` | Daily operations: manage books, members, loans, reservations |
| `MEMBER` | Library patron — does not log in; managed as a data entity only |

---

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL
- Maven
- A Gmail account configured for SMTP (or another mail provider)

### Configuration

Set the following in `application.properties` or as environment variables:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lms_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@gmail.com
spring.mail.password=your_app_password

jwt.secret=your_jwt_secret
```

### Run

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api/v1`.

### Institution Onboarding (3-Step Flow)

Before any library operations can begin, an institution must complete registration. No authentication token is required for these steps.

**Step 1 — Register**

```http
POST /api/v1/auth/institution/register
Content-Type: application/json

{
  "name":    "Dar es Salaam City Library",
  "email":   "admin@dslibrary.ac.tz",
  "phone":   "+255712345678",
  "address": "Samora Avenue, Dar es Salaam"
}
```

Returns a `publicId` (e.g. `INST-9F3K2L`) and sends a 6-character verification code to the provided email.

**Step 2 — Verify Email**

```http
POST /api/v1/auth/institution/verify
Content-Type: application/json

{
  "institutionPublicId": "INST-9F3K2L",
  "verificationCode":    "A9K2LM"
}
```

The code expires after 24 hours.

**Step 3 — Create Admin Account**

```http
POST /api/v1/auth/institution/setup-admin
Content-Type: application/json

{
  "institutionPublicId": "INST-9F3K2L",
  "password":            "SecurePass123!",
  "confirmPassword":     "SecurePass123!"
}
```

The institution is now ready to use. Log in to obtain a JWT.

---

## Authentication

The system uses a **dual-token** approach:

- **Access token** — short-lived JWT (5 minutes), sent in the `Authorization: Bearer` header.
- **Refresh token** — UUID stored in an HttpOnly cookie. Used only to obtain a new access token.

Both tokens are **SHA-256 hashed** before being stored in the database. Raw values never touch persistent storage.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email":    "admin@dslibrary.ac.tz",
  "password": "SecurePass123!"
}
```

Returns `{ accessToken, user }` in the response body. Sets the refresh token as an HttpOnly cookie.

### Refresh Token

```http
POST /api/v1/auth/refresh
```

No body required — the refresh token is read from the cookie automatically. Returns a new access token and rotates the refresh token.

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

Blacklists the access token immediately and clears the refresh cookie.

---

## API Reference

**Base URL:** `http://localhost:8080/api/v1`

All protected endpoints require: `Authorization: Bearer <accessToken>`

### Institution Registration (Public)

| Method | Path | Description |
|---|---|---|
| POST | `/auth/institution/register` | Step 1 — Register institution |
| POST | `/auth/institution/verify` | Step 2 — Verify email code |
| POST | `/auth/institution/setup-admin` | Step 3 — Create first admin |

### Authentication (Public)

| Method | Path | Description |
|---|---|---|
| POST | `/auth/login` | Login — returns JWT + sets refresh cookie |
| POST | `/auth/refresh` | Refresh access token using cookie |
| POST | `/auth/logout` | Logout — blacklists token, clears cookie |

### User Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/users` | ADMIN | Create a librarian |
| GET | `/users` | ADMIN | List all users (paginated) |
| GET | `/users/{publicId}` | ADMIN, LIBRARIAN | Get single user |
| PATCH | `/users/{publicId}` | ADMIN, LIBRARIAN | Update user details |
| DELETE | `/users/{publicId}` | ADMIN | Soft delete user |

### Book Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/books` | ADMIN, LIBRARIAN | Create book |
| GET | `/books` | ADMIN, LIBRARIAN | List books (paginated) |
| GET | `/books/{publicId}` | ADMIN, LIBRARIAN | Get single book |
| PATCH | `/books/{publicId}` | ADMIN, LIBRARIAN | Update book (partial) |
| DELETE | `/books/{publicId}` | ADMIN, LIBRARIAN | Hard delete (blocked if active loans exist) |

### Author Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/authors` | ADMIN, LIBRARIAN | Create author |
| GET | `/authors` | ADMIN, LIBRARIAN | List authors (paginated) |
| GET | `/authors/{publicId}` | ADMIN, LIBRARIAN | Get single author |
| PATCH | `/authors/{publicId}` | ADMIN, LIBRARIAN | Update author |
| DELETE | `/authors/{publicId}` | ADMIN, LIBRARIAN | Soft delete |

### Book–Author Links

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/book-authors` | ADMIN, LIBRARIAN | Link a book to an author |
| GET | `/book-authors/book/{publicId}` | ADMIN, LIBRARIAN | Get all authors for a book |
| GET | `/book-authors/author/{publicId}` | ADMIN, LIBRARIAN | Get all books for an author |
| PATCH | `/book-authors/{id}` | ADMIN, LIBRARIAN | Replace an existing link |
| DELETE | `/book-authors/{id}` | ADMIN, LIBRARIAN | Remove a link |

### Category Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/categories` | ADMIN, LIBRARIAN | Create category |
| GET | `/categories` | ADMIN, LIBRARIAN | List categories (paginated) |
| GET | `/categories/{publicId}` | ADMIN, LIBRARIAN | Get single category |
| PATCH | `/categories/{publicId}` | ADMIN, LIBRARIAN | Rename category |
| DELETE | `/categories/{publicId}` | ADMIN, LIBRARIAN | Delete (unlinks books, does not delete them) |

### Member Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/members` | ADMIN, LIBRARIAN | Register a member |
| GET | `/members` | ADMIN, LIBRARIAN | List members (paginated) |
| GET | `/members/{publicId}` | ADMIN, LIBRARIAN | Get single member |
| PATCH | `/members/{publicId}` | ADMIN, LIBRARIAN | Update member details |
| PATCH | `/members/{publicId}/status` | ADMIN | Change ACTIVE ↔ BLOCKED |
| DELETE | `/members/{publicId}` | ADMIN | Soft delete (→ BLOCKED) |

### Loan Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/loans` | ADMIN, LIBRARIAN | Issue a loan |
| PATCH | `/loans/{publicId}/return` | ADMIN, LIBRARIAN | Return a loan |
| GET | `/loans` | ADMIN, LIBRARIAN | List loans (paginated + filterable) |
| GET | `/loans/{publicId}` | ADMIN, LIBRARIAN | Get single loan |
| PATCH | `/loans/{publicId}/archive` | ADMIN, LIBRARIAN | Archive a returned loan |
| GET | `/loans/member/{memberPublicId}/active` | ADMIN, LIBRARIAN | Active loans for a member |

**Filter parameters for `GET /loans`:**

| Parameter | Values |
|---|---|
| `status` | `BORROWED`, `RETURNED`, `LATE` |
| `memberPublicId` | e.g. `MEMB-7K9X2P` |
| `bookPublicId` | e.g. `BOOK-K3MP9R` |
| `page` | integer (0-based) |
| `size` | integer |

### Reservation Management

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/reservations` | ADMIN, LIBRARIAN | Create a reservation |
| PATCH | `/reservations/{publicId}/cancel` | ADMIN, LIBRARIAN | Cancel a reservation |
| GET | `/reservations` | ADMIN, LIBRARIAN | List reservations (paginated + filterable) |
| GET | `/reservations/{publicId}` | ADMIN, LIBRARIAN | Get single reservation |

**Filter parameters for `GET /reservations`:**

| Parameter | Values |
|---|---|
| `status` | `PENDING`, `FULFILLED`, `EXPIRED` |
| `memberPublicId` | e.g. `MEMB-7K9X2P` |
| `bookPublicId` | e.g. `BOOK-K3MP9R` |
| `page` | integer (0-based) |
| `size` | integer |

Reservations can only be created when `book.copiesAvailable = 0`. If copies are available, a loan must be issued instead. Members may hold at most 5 active reservations at a time. Archived reservations are excluded from all list and single-record endpoints.

---

## Scheduler Automation

Four background jobs run automatically without any human trigger.

| Scheduler | Schedule | What it does |
|---|---|---|
| **Token Cleanup** | Every hour | Purges expired blacklisted tokens and stale refresh sessions |
| **Loan Overdue** | Daily at midnight | Bulk-updates `BORROWED` loans past their due date to `LATE` |
| **Reservation Fulfillment** | Every 5 minutes | Finds PENDING reservations and fulfills them when a copy becomes available; sets `reservedUntil = now + 48 hrs` |
| **Reservation Expiry** | Every hour | Expires FULFILLED reservations not collected within 48 hours; restores the copy to `copiesAvailable` |

The fulfillment and expiry schedulers are independent. After a copy is restored by expiry, the fulfillment scheduler picks it up within 5 minutes and advances the queue automatically.

---

## Copy Availability Model

`copiesAvailable` is the most sensitive field in the system. It is modified in exactly five places:

| Location | Operation | Condition |
|---|---|---|
| `LoanService.createLoan()` | `− quantity` | Standard loan with no reservation being collected |
| `LoanService.returnLoan()` | `+ quantity` (capped at `copiesTotal`) | Always on return |
| `ReservationScheduler.processReservations()` | `− 1` | One copy set aside per fulfillment |
| `ReservationScheduler.expireReservations()` | `+ 1` (capped) | Copy restored when member does not collect |
| `ReservationService.cancelReservation()` | `+ 1` (capped) | Only when cancelling a FULFILLED reservation |

> **Important (Phase 15):** When a member collects a FULFILLED reservation and the librarian issues a loan, `copiesAvailable` must **not** be decremented again — the scheduler already decremented it at fulfillment time.

---

## Security

### Institution Isolation

The `institution_id` is extracted from the JWT on every request and applied as a hard `WHERE` clause on every query. It is not possible for one institution's users to read or modify another institution's data.

### Token Security

| Concern | Implementation |
|---|---|
| Short-lived access tokens | 5-minute JWT — limits the blast radius of token theft |
| HttpOnly refresh tokens | Not accessible to browser JavaScript |
| Tokens stored as hashes | SHA-256 — raw values never persisted |
| Blacklist on logout | Tokens are rejected immediately, before natural expiry |
| Single session per user | Old refresh session is deleted on every new login |
| Token rotation | Every refresh issues a new refresh token; the old one is invalidated |

### Role Security

| Rule | Where enforced |
|---|---|
| Role never accepted from the client | `createUser()` hardcodes `LIBRARIAN` |
| ADMIN cannot modify another ADMIN | `updateUser()` and `deleteUser()` both enforce this |
| Users cannot delete themselves | `deleteUser()` checks the requesting user's own ID |
| Per-endpoint authorisation | `@PreAuthorize` on every controller method |
| Method-level security enabled | `@EnableMethodSecurity` on `SecurityConfig` |

---

## Response Format

Every API response — success or error — uses this envelope:

```json
{
  "success":   true,
  "message":   "Human-readable description",
  "data":      {},
  "errors":    [],
  "timestamp": "2026-05-13T10:00:00"
}
```

`data` is present on success. `errors` is populated only on validation failures (HTTP 400).

### HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | Success (GET, PATCH, DELETE) |
| 201 | Created (POST) |
| 400 | Bad request / validation failure / business rule violation |
| 401 | Not authenticated (missing or invalid token) |
| 403 | Authenticated but not authorised for this action |
| 404 | Resource not found (or belongs to a different institution) |
| 409 | Conflict (duplicate email, duplicate reservation, etc.) |
| 500 | Internal server error |

---

## Development Status

The backend was built incrementally across 14 phases. All are complete and functional.

| Phase | What Was Built |
|---|---|
| 1–4 | Project setup, domain model, institution registration (3-step flow), email verification |
| 5 | Login, JWT access tokens, HttpOnly refresh tokens, logout, token rotation |
| 6 | Token cleanup scheduler |
| 7 | Global exception handling, standardised `ApiResponse` wrapper |
| 8 | Book management — CRUD with `copiesTotal` / `copiesAvailable` tracking |
| 9 | Author management — CRUD with soft delete |
| 10 | Book–Author link management — many-to-many join |
| 11 | Category management |
| 12 | Member management — CRUD with ACTIVE/BLOCKED status |
| 13 | Loan management — borrow, return, overdue detection, daily scheduler |
| 14 | Reservation management — queue system, fulfillment scheduler, expiry scheduler |

---

## Roadmap

### Phase 15 — Loan–Reservation Integration *(required before production)*

Adds reservation-awareness to loan creation so that available copies are correctly routed to members in the reservation queue, preventing walk-ins from bypassing the queue.

### Phase 16 — Reporting & Dashboard Stats

Read-only aggregate queries on existing tables. Planned metrics: total active loans, most-borrowed books, members with overdue loans, most-reserved books, reservation fulfillment rate, loan volume over time.

### Phase 17 — Frontend (React)

The API is fully defined. The frontend will consume all endpoints documented here.
