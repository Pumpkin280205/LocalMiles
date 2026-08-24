> # Instruction: Copy this template to your own repo and edit together with your group.

# Activity 1: App Concept and API Proposal

**PROG7314 | Group Worksheet | Maximum 4 students**

## 1. Group and concept

**Group members and student numbers:**  

Anjor Misser - ST10452806\
Kiara Naidoo - ST10252256\
Sayali Sookdeo - ST10458649\
Iqra Nazir - ST10448362


**Working app name:**  
LocalMilesSA

**Problem to be solved:**

Small and medium logistics and courier operators in South Africa largely coordinate deliveries manually, using telephonic dispatch, WhatsApp groups, and paper waybills. This causes missed SLAs, disputed proof of delivery, duplicated or inefficient routes that waste fuel, poor real-time visibility for both dispatchers and clients, and reconciliation delays when drivers operate in areas with unreliable network coverage. There is no affordable, purpose-built tool that unifies dispatching, live fleet visibility, offline-capable proof of delivery and client tracking for smaller operators who cannot afford enterprise fleet-management platforms.  

**Target users:**

●	Depot dispatchers/fleet managers who assign and monitor delivery jobs.\
●	Delivery drivers who execute multi-stop routes, often in low-connectivity areas.\
●	Business clients (retailers and manufacturers) who outsource deliveries and need shipment visibility.

## 2. Proposed mobile solution

**Core solution:** What will the app do, and what is the main user journey?

FleetPulse is a single Android app with role-based views. A dispatcher creates a delivery job containing multiple stops; the system suggests an optimised stop sequence across the depot's available drivers and vehicles. The assigned driver receives the job as a push notification, opens the route in-app, and works through each stop, navigating, capturing proof of delivery (photo, signature and geotagged timestamp), and logging exceptions such as a failed delivery. All stop-level actions are queued locally and sync automatically once connectivity returns. Dispatchers see fleet position and job status update in real time on a live map, and clients linked to a job receive live tracking and delivery confirmation without needing to phone the depot.

**Why Android?** Which mobile capabilities make a native app appropriate?

●	Background location and geofencing for live driver tracking and automatic stop arrival/departure detection.\
●	Native camera and BiometricPrompt APIs for proof-of-delivery capture and secure driver authentication.\
●	Local persistence (Room database) to queue stop completions, photos and signatures while offline, common on rural or depot-yard routes with poor signal.\
●	Firebase Cloud Messaging for low-latency push notifications to drivers, dispatchers and clients.\
●	Compatibility with low-cost and ruggedised Android devices, which dominate the SA courier and field-driver market, keeping the solution affordable for SMEs.  

**Positive impact:** How will the app benefit users, a community, an organisation, or society?

●	Reduces delivery disputes through an auditable, timestamped proof-of-delivery trail.\
●	Improves SLA compliance and customer trust through real-time tracking and exception alerts.\
●	Cuts fuel and time waste through route optimisation, lowering operating costs for small operators.\
●	Formalises and digitises informal SME logistics operations, supporting record-keeping for tax, insurance and client reporting.\
●	Makes enterprise-grade dispatch tooling accessible and affordable to small South African logistics businesses, supporting local job creation and business growth.

## 3. Custom REST API

Explain what the API stores or processes and why it is central to the app.

The API stores and manages depots, vehicles, drivers, clients, delivery jobs and stops, proof-of-delivery records, and offline sync logs. It is the single source of truth that reconciles actions captured offline by drivers with the real-time views seen by dispatchers and clients, and it enforces the app's core business logic.

**API responsibilities and business logic:**  

●	Authenticating and authorising users via SSO and issuing role-scoped tokens (driver, dispatcher, client).\
●	Sequencing multi-stop routes for route-optimisation suggestions.\
●	Detecting and flagging SLA breaches (e.g. a stop overdue against its delivery window) and triggering notifications.\
●	Resolving conflicts when a driver's offline-queued updates are synced (e.g. a stop marked complete while dispatcher had already reassigned it).\
●	Aggregating fleet and driver performance data for dispatcher reporting dashboards.


List at least five proposed endpoints:

|**#**|**Method and route**|**Purpose**|
|---|---|---|
|1|POST /api/auth/login|Authentcate a user (driver, dispatcher or client) via SSO and<br>return a JWT and refresh token.|
|2|GET /api/drivers/{id}/jobs|Retrieve the delivery jobs and stop sequence currently<br>assigned to a driver.|
|3|POST<br>/api/jobs/{jobId}/stops/{stopId}/pod|Submit proof of delivery (photo, signature, GPS coordinates,<br>tmestamp) for a completed stop, including offline-queued<br>submissions.|
|4|POST /api/jobs|Create a new delivery job with multple stops and trigger<br>route-optmisaton logic.|
|5|GET /api/depots/{id}/feet-status|Return live vehicle positons and job statuses for all drivers at<br>a depot, for the dispatcher map view.|
|6|GET<br>/api/clients/{id}/shipments/{jobId}/tr<br>acking|Provide client-facing live tracking data and delivery<br>confrmaton for a shipment.|
|7|POST /api/sync/batch|Accept a batch of offline-queued driver actons and resolve<br>conficts against server state.|


## 4. POE feature fit

|**Requirement**|**How it will ft the proposed app**|
|---|---|
|Single Sign-On|Drivers and dispatchers log in with a company Google or Microsof (Azure<br>AD) account via OAuth2/OpenID Connect, giving one identty across the<br>dispatcher web console and the Android driver app without separate<br>passwords per depot.|
|Settings — identfy at least three|(1) Language selecton (English / isiZulu); (2) notfcaton preferences, e.g.<br>SLA-breach alert thresholds; (3) offline sync frequency / data-saver mode<br>for low-connectvity areas; (4) preferred map/navigaton provider (Google<br>Maps or Waze).|
|Biometric authentcaton|Fingerprint or face unlock (Android BiometricPrompt) is required to open<br>the driver app each shif and to confrm submission of proof of delivery,<br>preventng a driver's device from being used by someone else and<br>reducing delivery fraud.|
|Offline acton and synchronisaton|The driver app uses a local Room database to queue stop completons,<br>POD photos/signatures and GPS breadcrumbs when there is no signal,<br>typical of rural depots or basement loading bays. Queued actons are<br>uploaded through the batch sync endpoint and reconciled by server-side<br>confict-resoluton logic once connectvity returns.|
|Real-tme notificaton|Firebase Cloud Messaging pushes: new job assignments to drivers, SLA-<br>breach and excepton alerts to dispatchers, and 'out for delivery'/<br>'delivered' updates to clients.|
|Two South African languages|The interface is localised in English and isiZulu, covering navigaton menus,<br>job instructons and notificaton text, selectable in Settings or defaultng to<br>the device language.|


## 5. Five user-defined features


|**#**|**Feature**|**Purpose and user value**|
|---|---|---|
|1|Route optmisaton<br>suggeston|Automatcally re-sequences a driver's mult-stop route to minimise<br>distance and tme, reducing fuel cost and enabling more deliveries per<br>shif.|
|2|Excepton/incident reportng|Lets a driver log a failed or problem delivery (client absent, damaged<br>goods, access denied) with a photo and note, giving dispatchers<br>evidence to resolve client disputes quickly.|
|3|In-app driver–dispatcher|Provides quick, logged text communicaton between driver and|||chat|dispatcher for real-tme issue resoluton, avoiding costly or missed<br>phone calls while driving.|
|4|Client self-service tracking|Gives business clients a live map and ETA for their shipment plus<br>delivery confrmaton, cutng 'where is my order' calls to the depot.|
|5|Depot performance<br>dashboard|Shows dispatchers KPIs such as on-time delivery rate, per-driver<br>performance and total distance/fuel used, supportng data-driven<br>operatonal decisions.|



## 6. Comparable Android apps

This is not the formal research report. Three suitable apps have been identified that could later be compared with the FleetPulse concept. 

|**App**|**Google Play Store link**|**Why it is comparable**|
|---|---|---|
|1. Route4Me Route<br>Planner|[https://play.google.com/store/apps/<br>details?<br>id=com.route4me.routeoptmizer](https://play.google.com/store/apps/details?id=com.route4me.routeoptimizer&pcampaignid=web_share)|A widely-used mult-stop route<br>optmisaton and navigaton app for<br>professional drivers and couriers;<br>comparable to FleetPulse's route-<br>optmisaton and driver-navigaton feature<br>but built for individual/enterprise route<br>planning rather than integrated<br>dispatcher-driver-client coordinaton.|
|2. Onfleet Driver|[https://play.google.com/store/apps/<br>details?id=com.onfleet.driver.app](https://play.google.com/store/apps/details?id=com.onfleet.driver.app&pcampaignid=web_share)|A last-mile delivery driver companion app<br>with live navigaton, proof-of-delivery<br>capture and dispatcher/customer<br>communicaton; closely comparable in<br>scope, though it is a driver-only client<br>paired with a separate web dispatch<br>platorm rather than one combined mobile<br>app.|
|3. Track-POD Delivery<br>Driver App|[https://play.google.com/store/apps/<br>details?id=com.pt.ms](https://play.google.com/store/apps/details?id=com.pt.ms&pcampaignid=web_share)|An electronic proof-of-delivery app that<br>works ofine and syncs on reconnecton,<br>with route lists, signature/photo capture<br>and vehicle checks; comparable to<br>FleetPulse's ofine sync and POD features,<br>aimed at general logistcs and courier<br>operators.|


## 7. Feasibility and approval

**Proposed technology:** Android stack, API technology, database, hosting, authentication and key SDKs.  

- Android stack: Kotlin, Jetpack Compose, Room (offline storage), WorkManager (background sync), BiometricPrompt, Google Maps SDK, Firebase Cloud Messaging. 

- API technology: ASP.NET Core Web API (C#) using the Repository Pattern and Dependency Injection, following the same architecture applied in the team's GLMS POE work. 

- Database: SQL Server (relational data — jobs, stops, users, PODs) via Entity Framework Core. 

- Hosting: Azure App Service for the API and Azure Blob Storage for POD photos/signatures; Azure SQL Database for the data tier. 

- Authentication: OAuth2/OpenID Connect SSO (Google/Microsoft Azure AD) plus JWT bearer tokens for API access; biometric unlock on-device. 

- Key SDKs: Firebase (push notifications, crash reporting), Google Maps/Directions API (navigation and route optimisation), Azure Blob Storage SDK (media upload). 


**Three main risks and how they will be reduced:**  

1. Data conflicts from offline sync (two updates to the same stop) could corrupt job status. Mitigated by designing clear server-side conflict-resolution rules (last-confirmed-state wins, with an audit log) and testing sync scenarios early. 

2. Route-optimisation logic is complex and could delay delivery. Mitigated by starting with a simple nearest-neighbour or third-party Directions API optimisation call for the MVP, and only building a custom algorithm if time allows. 

3. Limited real-world testing devices/connectivity conditions could hide offline-sync bugs. Mitigated by using Android emulator network-throttling and airplane-mode testing throughout development, not just at the end. 

**Scope check:** What will be removed first if the project becomes too large?  

The client self-service tracking view and the depot performance dashboard (features 4 and 5) would be removed or reduced to a minimal read-only screen first, since the core dispatcher–driver job/POD/offline-sync loop must remain intact to satisfy the POE's mandatory requirements. Routeoptimisation would be simplified to a basic optimisation API call rather than a custom algorithm if needed. 


### Lecturer decision

- [ ] Approved
- [ ] Approved with changes
- [ ] Revise and resubmit

**Conditions or comments:**  
