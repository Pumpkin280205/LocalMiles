package com.example.localmiles.Core.Auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.localmiles.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/** User dismissed the Google sheet — not an error, just abort quietly. */
class SignInCancelled : Exception()

/** No usable Google account, Play Services problem, or Firebase rejected the credential. */
class SignInUnavailable(message: String, cause: Throwable?) : Exception(message, cause)

/**
 * Runs the login handshake the backend expects:
 *
 *   1. Credential Manager  ->  Google ID token
 *   2. FirebaseAuth.signInWithCredential(...)  ->  Firebase user
 *   3. user.getIdToken(true)  ->  Firebase ID token
 *
 * The Firebase ID token (step 3) is what POST /api/auth/login verifies via
 * firebase-admin. The raw Google token from step 1 is only an intermediate.
 */
class GoogleAuthClient(
    private val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    /**
     * Shows the "Sign in with Google" sheet and returns a fresh Firebase ID token.
     * [activityContext] MUST be an Activity context — Credential Manager renders UI.
     */
    suspend fun signInAndGetFirebaseIdToken(activityContext: Context): String {
        require(webClientId.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID is empty — set it in local.properties and re-sync Gradle."
        }

        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
                .credential
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelled()
        } catch (e: NoCredentialException) {
            throw SignInUnavailable("No Google account available on this device", e)
        } catch (e: GetCredentialException) {
            throw SignInUnavailable(e.message ?: "Google sign-in failed", e)
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw SignInUnavailable("Unexpected credential type: ${credential.type}", null)
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

        val firebaseCred = GoogleAuthProvider.getCredential(googleIdToken, null)
        val user = try {
            firebaseAuth.signInWithCredential(firebaseCred).await().user
        } catch (e: Exception) {
            throw SignInUnavailable(e.message ?: "Firebase sign-in failed", e)
        } ?: throw SignInUnavailable("Firebase returned no user", null)

        return user.getIdToken(true).await().token
            ?: throw SignInUnavailable("Firebase ID token was null", null)
    }

    /**
     * Silent refresh for an already-cached Firebase user (no UI). Returns null if
     * there is no signed-in Firebase user — caller should then show LoginScreen.
     */
    suspend fun refreshFirebaseIdTokenOrNull(): String? =
        firebaseAuth.currentUser?.getIdToken(true)?.await()?.token

    fun signOut() = firebaseAuth.signOut()
}
