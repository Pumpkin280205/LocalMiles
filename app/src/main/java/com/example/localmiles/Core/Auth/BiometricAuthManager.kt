
package com.example.localmiles.Core.Auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.localmiles.R

class BiometricAuthManager(
    private val activity: FragmentActivity
) {

    companion object {
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
    }

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(activity)

        return biometricManager.canAuthenticate(AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val availability = biometricManager.canAuthenticate(AUTHENTICATORS)

        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false, getAvailabilityMessage(availability))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true, null)
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            .setDescription(activity.getString(R.string.biometric_prompt_description))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setNegativeButtonText(activity.getString(R.string.biometric_cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun getAvailabilityMessage(status: Int): String {
        return when (status) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                activity.getString(R.string.biometric_no_hardware)

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                activity.getString(R.string.biometric_hardware_unavailable)

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                activity.getString(R.string.biometric_none_enrolled)

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                activity.getString(R.string.biometric_security_update)

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                activity.getString(R.string.biometric_unsupported)

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                activity.getString(R.string.biometric_status_unknown)

            else ->
                activity.getString(R.string.biometric_generic_unavailable)
        }
    }
}
