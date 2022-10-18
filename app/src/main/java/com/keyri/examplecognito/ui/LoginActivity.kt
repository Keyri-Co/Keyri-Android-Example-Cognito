package com.keyri.examplecognito.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.regions.Regions
import com.keyri.examplecognito.R
import com.keyri.examplecognito.databinding.ActivityLoginBinding
import com.keyri.examplecognito.ui.MainActivity.Companion.KEY_EMAIL
import com.keyri.examplecognito.ui.MainActivity.Companion.KEY_PAYLOAD
import com.keyrico.keyrisdk.Keyri
import org.json.JSONObject
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        with(binding) {
            bLogin.setOnClickListener {
                val email = etEmail.getNotEmptyText()
                val password = etPassword.getNotEmptyText()

                if (email != null && password != null) {
                    signIn(email, password)
                }
            }
        }
    }

    private fun signIn(email: String, password: String) {
        val authenticationHandler: AuthenticationHandler = object : AuthenticationHandler {

            override fun onSuccess(userSession: CognitoUserSession, newDevice: CognitoDevice?) {
                createPayloadAndSendResult(userSession)
            }

            override fun getAuthenticationDetails(
                authenticationContinuation: AuthenticationContinuation,
                userId: String
            ) {
                val authenticationDetails = AuthenticationDetails(userId, password, null)

                authenticationContinuation.setAuthenticationDetails(authenticationDetails)
                authenticationContinuation.continueTask()
            }

            override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation) = Unit

            override fun authenticationChallenge(continuation: ChallengeContinuation?) = Unit

            override fun onFailure(exception: Exception) {
                val message = requireNotNull(exception.message)

                copyMessageToClipboard(message)
                showMessage(message)
            }
        }

        val userPoolID = getString(R.string.user_pool_id)
        val clientId = getString(R.string.client_id)
        val clientSecret = getString(R.string.client_secret)
        val region = Regions.US_EAST_1

        val userPool = CognitoUserPool(this, userPoolID, clientId, clientSecret, region)
        val authDetails = AuthenticationDetails(email, password, null)
        val user = userPool.user

        user.initiateUserAuthentication(authDetails, authenticationHandler, true).run()
    }

    private fun createPayloadAndSendResult(userSession: CognitoUserSession) {
        val keyri = Keyri(this)

        val associationKey = keyri.getAssociationKey(userSession.username)
        val timestampNonce = "${System.currentTimeMillis()}_${Random.nextInt()}"
        val signature = keyri.generateUserSignature(userSession.username, timestampNonce)

        val payload = JSONObject().apply {
            put("username", userSession.username)
            put("timestamp_nonce", timestampNonce)
            put("userSignature", signature)
            put("associationKey", associationKey)
        }.toString()

        val intent = Intent().apply {
            putExtra(KEY_EMAIL, userSession.username)
            putExtra(KEY_PAYLOAD, payload)
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    private fun EditText.getNotEmptyText(): String? {
        return text?.takeIf { it.isNotEmpty() }?.toString()
    }

    private fun showMessage(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun copyMessageToClipboard(message: String) {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("Keyri Cognito example", message)

        clipboard.setPrimaryClip(clip)
    }
}
