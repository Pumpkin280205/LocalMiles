# LocalMiles — Release Notes

## Version 2.0.0 — Final POE (November 2026)

Everything added or changed since the Part 2 prototype (v1.0).

### New features

**Biometric authentication**
- Fingerprint / face prompt when the app opens with a saved session.
- Turn it on or off in Settings → Security. Turning it on asks you to verify first.
- **Unlock with biometrics** button on the login screen. When biometric login is on, signing out keeps the session on the phone so you can unlock again without Google sign-in.

**Offline mode with automatic sync (drivers)**
- Job status, stop status and proof-of-delivery actions made without internet are saved in a Room database.
- WorkManager sends them to the API automatically when the connection returns, oldest first, with retries.
- A banner on My Jobs and the stop screen shows **Pending**, **Synced** or **Failed** changes.
- Failed (rejected) changes can be **retried** or **dismissed**.
- Changes are kept per account, so another user signing in never sends them.
- Lists show unsynced changes on top of the server data so statuses do not flip back.

**Proof of delivery**
- Take a photo with the camera (shrunk automatically).
- Recipient signs on screen with a finger.
- The phone's real GPS location is sent instead of the stop's address. The app checks the 200 m limit before submitting, so drivers see the problem immediately, even offline.

**Real-time push notifications**
- Firebase Cloud Messaging pushes for job and stop updates, failed deliveries and sync problems.
- Each phone registers its token on sign-in.

**Multi-language support**
- English, Afrikaans (`values-af`) and isiZulu (`values-zu`), chosen in Settings.

**Live driver location and live map**
- Drivers share their position while a job is en route.
- Dispatchers see drivers and vehicles on an OpenStreetMap live map.

### Improvements

- **Unified UI across all roles** (dispatcher, driver, client): one colour palette from the theme, light/dark/system theme option, and a complete text style scale.
- **Settings** for each role: theme, language, biometric login, navigation app and screen-on while driving.
- **Custom app icon** (lime map pin over an orange road on dark teal) with a monochrome version for themed icons; the Android default icon is gone.
- **Status rules and validation** on the server and in the app, so invalid job/stop changes are refused with a clear message.
- API base URL is configurable per build: debug uses your PC's address, release uses the hosted API over HTTPS.
- OpenStreetMap tile settings updated so map tiles load reliably.

### Release and DevOps

- **Signed release build** (version 2.0.0, version code 2). The signing key is private and never committed.
- **GitHub Actions** now runs on every push to any branch: unit tests, debug build, release build, and the API's type-check and tests.
- README rewritten as a technical report; Play Store listing text and privacy policy drafted in `docs/play-store`.

### Tests added

- Offline queue, pending-change overlay, GPS distance rule, biometric preferences, language manager and room DAO tests.

### Known limitations

- Offline support covers driver job/stop status and proof of delivery only, not the whole app.
- A rejected offline change (for example a status that is no longer valid) is shown as Failed and needs a manual retry.
- GPS accuracy indoors can put a driver outside the 200 m limit.
- Not built: route optimisation and in-app driver–dispatcher chat (planned in the original proposal).

---

## Version 1.0 — Part 2 prototype (September 2026)

- Google single sign-on through Firebase, with Dispatcher, Driver and Client roles.
- Dispatcher: create jobs and stops, assign drivers and vehicles, manage fleet, promote users to drivers.
- Driver: sequenced stop workflow, proof of delivery with recipient name and notes.
- Client: shipment tracking with stop-by-stop progress.
- REST API (Express + TypeScript + Supabase) hosted on Render.
- Settings: theme and language preference.
- GitHub Actions running unit tests and a debug build.
