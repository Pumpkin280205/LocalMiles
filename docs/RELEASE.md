# Releasing LocalMiles

## 1. The signing key

The release build is signed with a private key. **Never commit it.** It lives outside the repo
(for example `Desktop\LocalMiles-Release-Keys\localmiles-release.jks`) and is described by a
git-ignored file in the project root called `keystore.properties`:

```properties
storeFile=C:/path/to/localmiles-release.jks
storePassword=...
keyAlias=localmiles
keyPassword=...
```

Back the `.jks` file and its passwords up somewhere safe. If the key is lost the app cannot be updated on
the Play Store.

Create a new key (only if you do not already have one):

```
keytool -genkeypair -v -keystore localmiles-release.jks -alias localmiles -keyalg RSA -keysize 2048 -validity 9125
```

## 2. Register the release key with Firebase (needed for Google sign-in)

The release app is signed with a different key from the debug app, so Google sign-in fails in it until the
release fingerprint is added:

1. Get the SHA-1: `keytool -list -v -keystore localmiles-release.jks -alias localmiles`
2. Firebase console → Project settings → your Android app → **Add fingerprint** → paste the SHA-1 (and SHA-256).
3. If you publish through Play App Signing, also add the SHA-1/SHA-256 shown in Play Console → App integrity.
4. Download the new `google-services.json` and replace `app/google-services.json`.

## 3. Build

```
./gradlew assembleRelease      # signed APK  -> app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease        # Play Store bundle -> app/build/outputs/bundle/release/app-release.aab
```

Check the APK signature: `apksigner verify --print-certs app-release.apk`

Version numbers are in `app/build.gradle.kts` (`versionCode` must go up on every upload).

## 4. GitHub Actions secrets (optional: signed APK from CI)

Without secrets the CI release build is unsigned, which is fine for proving it compiles. To have CI sign it, add these
repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `LOCALMILES_KEYSTORE_BASE64` | the `.jks` file as base64 (`base64 -w0 localmiles-release.jks`) |
| `LOCALMILES_KEYSTORE_PASSWORD` | store password |
| `LOCALMILES_KEY_ALIAS` | `localmiles` |
| `LOCALMILES_KEY_PASSWORD` | key password |

## 5. Tag the final submission

```
git tag -a Final-POE -m "Final POE submission (v2.0.0)"
git push origin Final-POE
```

## 6. Play Store checklist

- [ ] Upload the `.aab` (Play Console → Testing or Production → Create release)
- [ ] App icon 512×512 and feature graphic 1024×500 ([docs/branding](branding))
- [ ] At least 2 phone screenshots (aim for 4 to 8), see the screenshot tool in the handoff pack
- [ ] Listing text from [play-store/listing.md](play-store/listing.md)
- [ ] Privacy policy published at a public URL ([play-store/PRIVACY_POLICY.md](play-store/PRIVACY_POLICY.md)) and linked in Play Console
- [ ] Data safety form completed (answers in the listing file)
- [ ] Content rating questionnaire, target audience, and app access (give reviewers a demo login)
