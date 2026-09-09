# Comments added / changed by Claude — backup copy

Saved so you can strip comments from the source files and still keep the explanations.
Only three files received comments from this session. `RetrofitClient.kt`,
`app/build.gradle.kts`, `build.gradle.kts` (root) and `libs.versions.toml` were
changed too, but no comments were added to them.

---

## 1. `app/src/main/java/com/example/localmiles/Core/Network/ApiService.kt`  (new file)

### Interface KDoc (above `interface ApiService`)
```kotlin
/**
 * The typed contract for AJ's Express API.
 *
 * Retrofit generates the implementation of this interface at runtime (that is what
 * `retrofit.create(ApiService::class.java)` does in [RetrofitClient]). Each function
 * maps to one HTTP endpoint; the annotations describe the request and the return
 * type describes the response body after Gson has parsed the JSON.
 *
 * `suspend` means the call runs on a background thread and suspends the calling
 * coroutine until the response arrives — call these from a `viewModelScope.launch { }`.
 */
```

### Per-endpoint comments (inside the interface)
```kotlin
//POST {BASE_URL}auth/login — exchanges credentials for a bearer token.
//GET {BASE_URL}auth/me — returns the profile for the currently authenticated user.
//GET {BASE_URL}jobs — every job visible to the caller's role.
//GET {BASE_URL}jobs/{id} — a single job by id.
```

### Block comment (above the request/response data classes)
```kotlin
// ---------------------------------------------------------------------------
// Request / response models.
// These live here for now so the file compiles on its own; move them into the
// Dto package as they grow. Field names must match the JSON keys the server
// sends — use @SerializedName when they differ (e.g. snake_case from Express).
// ---------------------------------------------------------------------------
```

### Field comment (on `UserResponse.role`)
```kotlin
/** "dispatcher" | "driver" | "client" */
```

---

## 2. `app/src/main/java/com/example/localmiles/Core/Utils/SessionManager.kt`  (new file)

### Class KDoc (above `class SessionManager`)
```kotlin
/**
 * The single source of truth for "who is logged in right now".
 *
 * Wraps a small [SharedPreferences] file so the auth token and basic user info
 * survive process death and app restarts. Create one instance per app (see
 * LocalMilesApp / Hilt) and pass it wherever it is needed —
 * [com.example.localmiles.Core.Network.RetrofitClient] reads [getToken] on every
 * outgoing request to attach the `Authorization` header.
 *
 * Note: SharedPreferences is plain text on disk. Fine to start with; swap the
 * `prefs` initializer for EncryptedSharedPreferences (androidx.security:security-crypto)
 * before shipping if the token is long-lived.
 */
```

### Per-method comments
```kotlin
//Persist the bearer token returned by `auth/login`.                                  -> saveToken()
//The current token, or null if the user has never logged in / has logged out.        -> getToken()
//Cache the identity so the UI can branch on role without a network call.             -> saveUser()
//"dispatcher" | "driver" | "client" — used for the post-login navigation split.      -> getRole()
//Wipe everything on logout.                                                          -> clear()
```

---

## 3. `gradle.properties`

### Above `android.disallowKotlinSourceSets=false`
```properties
# AGP 9 uses built-in Kotlin. KSP still registers its generated dirs via the
# kotlin.sourceSets DSL, which built-in Kotlin rejects by default. Allow it.
```
Keep this one — it explains a non-obvious build flag that will otherwise look
mysterious to anyone reading `gradle.properties` later.

---

## Non-comment changes (for your notes — no comments involved)

| File | Change |
|---|---|
| `RetrofitClient.kt` | fixed 3 imports: removed bogus `com.google.firebase.appdistribution.gradle.ApiService`; `com.example.localmiles.app.BuildConfig` -> `com.example.localmiles.BuildConfig`; `com.example.localmiles.app.core.Utils.SessionManager` -> `com.example.localmiles.Core.Utils.SessionManager` |
| `app/build.gradle.kts` | added `alias(libs.plugins.ksp)` to `plugins {}`; added `buildConfig = true` to `buildFeatures {}`; removed `implementation(libs.firebase.appdistribution.gradle)` |
| `build.gradle.kts` (root) | added `alias(libs.plugins.ksp) apply false` |
| `libs.versions.toml` | added `ksp` version (`2.2.10-2.0.2`) + `ksp` plugin entry; removed `firebaseAppdistributionGradle` version and `firebase-appdistribution-gradle` library entries |
| `gradle.properties` | added `android.disallowKotlinSourceSets=false` |
