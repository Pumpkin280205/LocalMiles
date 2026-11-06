# Activity 1: App Concept and API Proposal — Updated for Part 2

**PROG7314 | Group Worksheet | Maximum 4 students**

> This is the original Part 1 concept worksheet, updated to reflect what was actually implemented for the Part 2 prototype. Sections that changed from the original proposal are marked **(Updated for Part 2)**.

video link - https://youtu.be/W0VLYsKjTDo

unit testing pictures - 

<img width="1920" height="1080" alt="Screenshot 2026-09-22 172719" src="https://github.com/user-attachments/assets/15c6860f-b5a0-40cf-9427-973842dd8905" />
<img width="1920" height="1080" alt="Screenshot 2026-09-22 172743" src="https://github.com/user-attachments/assets/7def991c-c601-410b-a016-cd578b78c2e8" />


## 1. Group and concept

**Group members and student numbers:**

Anjor Misser - ST10452806\
Kiara Naidoo - ST10252256\
Sayali Sookdeo - ST10458649\
Iqra Nazir - ST10448362

**Working app name:** ~~LocalMilesSA~~ **LocalMiles** *(Updated for Part 2 — the built app is named LocalMiles)*

**Problem to be solved:**

Small and medium logistics and courier operators in South Africa largely coordinate deliveries manually, using telephonic dispatch, WhatsApp groups, and paper waybills. This causes missed SLAs, disputed proof of delivery, duplicated or inefficient routes that waste fuel, poor real-time visibility for both dispatchers and clients, and reconciliation delays when drivers operate in areas with unreliable network coverage. There is no affordable, purpose-built tool that unifies dispatching, live fleet visibility, offline-capable proof of delivery and client tracking for smaller operators who cannot afford enterprise fleet-management platforms.

**Target users:**

- Depot dispatchers/fleet managers who assign and monitor delivery jobs.
- Delivery drivers who execute multi-stop routes, often in low-connectivity areas.
- Business clients (retailers and manufacturers) who outsource deliveries and need shipment visibility.

## 2. Proposed mobile solution

**Core solution — as implemented in Part 2:**

LocalMiles is a single Android app with three role-based views — Dispatcher (admin), Driver, and Client — sharing one login and one backend. A dispatcher creates a delivery job and adds its delivery stops, in sequence, from the Jobs screen. The dispatcher then progresses the job through its status lifecycle (Pending → Assigned → EnRoute) and it appears on the assigned driver's job list. The driver opens the job, starts it, and works through its stops strictly in the dispatcher's sequence order — marking each stop Arrived, then capturing Proof of Delivery (photo/signature-ready, recipient name, GPS position, notes) before the next stop becomes actionable. Once every stop on a job is completed, the job itself auto-completes, and the linked client sees the updated status and stop-by-stop progress on their own Track Shipment screen.

*(Updated for Part 2 — route optimisation, live push-notification-driven job assignment, and in-app driver–dispatcher chat are not yet built; the core dispatch → drive → track loop above is what's implemented and demonstrated.)*

**Why Android?**

- Native Camera and Location APIs for proof-of-delivery capture (photo + GPS-verified stop location — the backend rejects a POD submitted more than 200m from its registered stop).
- Local persistence (Room database) used for the driver-side offline sync groundwork (`OfflineQueueManager`, `SyncWorker`) — full offline-mode with automatic conflict-resolved sync is targeted for the final POE, not required for Part 2.
- Jetpack Compose throughout, for a single consistent, role-adaptive UI without a legacy XML/Compose split.
- Firebase Authentication via Google Sign-In (Credential Manager) for SSO, shared across all three roles from one account system.

**Positive impact:** unchanged from the original proposal — reduces delivery disputes through an auditable, sequenced delivery record; improves visibility for clients without needing to phone the depot; and gives small operators access to structured dispatch tooling without enterprise pricing.

## 3. Custom REST API *(Updated for Part 2)*

The API stores and manages depots, vehicles, drivers, clients, users, delivery jobs, delivery stops, and Proof of Delivery records. It is the single source of truth reconciling dispatcher, driver and client views, and it enforces the app's real business rules server-side.

**API responsibilities and business logic, as implemented:**

- Authenticating users via Firebase-verified SSO and issuing a scoped session JWT (`admin` / `driver` / `client` roles).
- Enforcing valid job and stop status transitions server-side (e.g. a job can only reach `Completed` from `EnRoute`; a stop only from `Arrived`) — rejecting invalid jumps with a 409.
- Enforcing a GPS-proximity check on Proof of Delivery submission: a driver's submitted location must be within 200m of the stop's registered coordinates, or the submission is rejected.
- Linking every self-registered client user to a `Client` record automatically on first login, so a dispatcher can immediately assign jobs to them.
- Scoping job visibility by role — a driver or client can only see their own jobs via `GET /api/jobs?mine=true`.

**Endpoints actually implemented (subset most relevant to Part 2):**

| # | Method and route | Purpose |
|---|---|---|
| 1 | `POST /api/auth/login` | Authenticate a user via Firebase-verified SSO and return a session JWT. |
| 2 | `GET /api/jobs?mine=true` | Retrieve the delivery jobs assigned to (or owned by) the current driver/client. |
| 3 | `POST /api/jobs` | Create a new delivery job (dispatcher only). |
| 4 | `PATCH /api/jobs/{id}/status` | Transition a job's status (e.g. Assigned → EnRoute → Completed), validated server-side. |
| 5 | `POST /api/jobs/{jobId}/stops` | Add a delivery stop to a job, in sequence (dispatcher only). |
| 6 | `PATCH /api/stops/{id}/status` | Transition a stop's status (Pending → Arrived → Completed), validated server-side. |
| 7 | `POST /api/stops/{stopId}/pod` | Submit Proof of Delivery for a stop, enforcing the GPS-proximity check. |
| 8 | `GET /api/clients` | List Client records, used to populate the dispatcher's client picker on job creation. |

*(Updated for Part 2 — route-suggestion, SLA-breach detection, offline-batch conflict resolution, and fleet-performance aggregation endpoints from the original proposal are not yet built; the endpoints above are what's live and tested.)*

## 4. POE feature fit *(Updated for Part 2)*

| Requirement | Status as of Part 2 |
|---|---|
| Single Sign-On | **Implemented.** Google Sign-In via Android Credential Manager, exchanged for a Firebase ID token, verified by the backend, issuing a role-scoped session JWT. |
| Settings — at least three | **Partially implemented.** Each role has its own settings screen; currently covers theme (light/dark/system) and a language preference toggle (English/isiZulu — stored as a preference, full string-resource localisation not yet wired up). |
| Biometric authentication | **Not yet implemented.** Reserved for the final POE per the assignment brief. |
| Offline action and synchronisation | **Groundwork in place, not yet complete.** Room database and a driver-side `OfflineQueueManager`/`SyncWorker` exist; full queue-and-sync-on-reconnect behaviour is targeted for the final POE. |
| Real-time notification | **Not yet implemented.** Reserved for the final POE (Firebase Cloud Messaging). |
| Two South African languages | **Not yet implemented.** A language selector exists in Settings, but only English string resources currently exist — full isiZulu localisation is planned for the final POE. |

## 5. User-defined features implemented in Part 2

| # | Feature | Purpose and user value |
|---|---|---|
| 1 | Fleet management | Dispatcher can add vehicles, edit their details, and toggle active/inactive status, directly affecting what's assignable to a job. |
| 2 | Driver promotion workflow | A dispatcher can promote any client-role user to a driver in one flow, which creates the linked driver record and flips their role atomically. |
| 3 | Sequenced delivery-stop workflow | Stops must be actioned strictly in the dispatcher's sequence order — a driver can't jump ahead to a later stop, with a live "Stop X of N" progress counter. |
| 4 | Client self-service tracking | Clients see their own shipment's live status and a stop-by-stop progress view against the dispatcher's registered stops. |
| 5 | Delivery history and live map | Dispatcher can review completed/cancelled jobs and see driver/vehicle positions on a live map. |

*(Updated for Part 2 — the original proposal's route-optimisation-suggestion and in-app driver–dispatcher chat features are not yet built; the five features above are what's implemented and demonstrable in the current prototype.)*

## 6. Comparable Android apps

Unchanged from the Part 1 research — see the full comparative analysis in the Research Report (Route4Me Route Planner, Onfleet Driver, Track-POD Delivery Driver App).

## 7. Feasibility — actual technology used *(Updated for Part 2)*

- **Android stack:** Kotlin, Jetpack Compose, Retrofit2 + OkHttp + Gson, Room, WorkManager, Firebase Authentication, Google Maps Compose, Hilt.
- **API technology:** ~~ASP.NET Core Web API (C#)~~ **Express 5 + TypeScript**, following a controller-based structure with role-based middleware.
- **Database:** ~~SQL Server via Entity Framework Core~~ **Supabase (Postgres)**, accessed via the Supabase JS client.
- **Hosting:** ~~Azure App Service / Azure SQL~~ **Render** (API), **Supabase** (database).
- **Authentication:** ~~OAuth2/OpenID Connect SSO (Google/Microsoft Azure AD) + JWT~~ **Firebase Authentication (Google Sign-In) + a custom session JWT** issued by the backend after Firebase ID-token verification.
- **Key SDKs actually used:** Firebase Auth, Google Maps Compose, Retrofit/OkHttp, MockK + kotlinx-coroutines-test (unit testing), GitHub Actions (CI).

*(Updated for Part 2 — the stack changed from the originally proposed ASP.NET Core/SQL Server/Azure combination to a Node.js/TypeScript/Supabase/Render stack, decided during early Part 2 build-out.)*

**Scope check — what was actually deferred:** matching the original scope-check decision, route optimisation and the driver–dispatcher chat feature were the first things deferred, along with the four items explicitly marked "(POE only)" in the assignment brief: biometric authentication, full offline sync, push notifications, and multi-language support. The core dispatcher → driver → client job/stop/POD loop was kept intact and fully working, per the original priority.

### Lecturer decision

- [ ] Approved
- [ ] Approved with changes
- [ ] Revise and resubmit

**Conditions or comments:**
