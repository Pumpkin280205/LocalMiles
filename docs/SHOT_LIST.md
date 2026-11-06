# Screenshot shot list

Use a **physical phone**, English language unless stated, light theme for most shots (use dark theme for one or two
so the listing shows both), battery full, no other notifications in the status bar, and clean demo data
(real-looking names and addresses, no "test test").

Save raw screenshots with exactly these names. `docs/tools/frame_screenshots.py` turns them into Play Store images in the
app colours, and the README gallery uses the same names (`docs/screenshots/`).

| File name | Screen | Role | Tip |
|---|---|---|---|
| `01-login.png` | Login screen with **Sign in with Google** and **Unlock with biometrics** | any (signed out, biometric on) | Turn biometric login on, then sign out so both buttons show |
| `02-biometric.png` | The fingerprint / face prompt on app launch | any | Close the app from recents and reopen it |
| `03-settings.png` | Settings with the Security section visible | any | Scroll so theme, language and biometric toggle are visible |
| `04-driver-stops.png` | Driver job with the stop list and "Stop X of N" | driver | Use a job with 3+ stops, one done |
| `05-offline-banner.png` | The orange "saved offline, waiting to sync" banner | driver | Airplane mode on, mark a stop Arrived |
| `06-pod.png` | Proof of delivery screen with photo taken and signature drawn | driver | Fill in a recipient name |
| `07-live-map.png` | Dispatcher live map with driver markers | dispatcher | Have a driver sharing location |
| `08-fleet.png` | Fleet management list | dispatcher | A few vehicles, mix of active/inactive |
| `09-client-tracking.png` | Client track shipment with stop progress | client | A job part-way through |
| `10-notification.png` | A push notification in the notification shade | any | Trigger a status change from the dispatcher |
| `11-languages.png` | Any screen in isiZulu (or Afrikaans) | any | Change language in Settings |
| `12-dispatcher-dashboard.png` | Dispatcher dashboard | dispatcher | |

**For the Play Store:** upload at least 2 (aim for 4 to 8). Best ones: 04, 05, 06, 07, 09, 01.
**For the README:** use 01 to 09.
**For the video and evidence:** also take screenshots of the Firebase console (Authentication users), the Supabase tables
(jobs, stops, pod, offline sync actions), the Render deployment, a green GitHub Actions run and the commit graph.
