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
import androidx.core.view.isVisible
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import com.keyri.examplecognito.R
import com.keyri.examplecognito.databinding.ActivityRegisterBinding
import com.keyrico.keyrisdk.Keyri
import org.json.JSONObject
import kotlin.random.Random

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        initUI()
    }

    private fun initUI() {
        with(binding) {
            bRegister.setOnClickListener {
                val email = etEmail.getNotEmptyText()
                val password = etPassword.getNotEmptyText()
                val givenName = etGivenName.getNotEmptyText()
                val familyName = etFamilyName.getNotEmptyText()

                if (email != null && password != null && givenName != null && familyName != null) {
                    cognitoSignUp(email, password, givenName, familyName)
                }
            }
        }
    }

    private fun cognitoSignUp(
        email: String,
        password: String,
        givenName: String,
        familyName: String
    ) {
        val signupCallback: SignUpHandler = object : SignUpHandler {
            override fun onSuccess(user: CognitoUser, signUpResult: SignUpResult) {
                if (signUpResult.userConfirmed) {
                    getSession(user, password)
                } else {
                    with(binding) {
                        llConfirmation.isVisible = true
                        llRegisterContent.isVisible = false

                        bConfirm.setOnClickListener {
                            etCode.text?.takeIf { it.length == 6 }?.let {
                                confirm(user, password, it.toString())
                            }
                        }
                    }
                }
            }

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

        val userAttributes = CognitoUserAttributes().apply {
            addAttribute("email", email)
            addAttribute("given_name", givenName)
            addAttribute("family_name", familyName)
        }

        userPool.signUpInBackground(email, password, userAttributes, null, signupCallback)
    }

    private fun confirm(user: CognitoUser, password: String, confirmationCode: String) {
        val confirmationCallback: GenericHandler = object : GenericHandler {
            override fun onSuccess() = getSession(user, password)

            override fun onFailure(exception: Exception) {
                val message = requireNotNull(exception.message)

                copyMessageToClipboard(message)
                showMessage(message)
            }
        }

        val forcedAliasCreation = false

        user.confirmSignUpInBackground(confirmationCode, forcedAliasCreation, confirmationCallback)
    }

    private fun getSession(cognitoUser: CognitoUser, password: String) {
        val authenticationHandler = object : AuthenticationHandler {

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

            override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation) =
                Unit

            override fun authenticationChallenge(continuation: ChallengeContinuation?) = Unit

            override fun onFailure(exception: Exception) {
                val message = requireNotNull(exception.message)

                copyMessageToClipboard(message)
                showMessage(message)
            }
        }

        cognitoUser.getSessionInBackground(authenticationHandler)
    }

    private fun createPayloadAndSendResult(userSession: CognitoUserSession) {
        val keyri = Keyri()

        val associationKey = keyri.getAssociationKey(userSession.username)
        val timestampNonce = "${System.currentTimeMillis()}_${Random.nextInt()}"
        val signature = keyri.getUserSignature(userSession.username, timestampNonce)

        val payload = JSONObject().apply {
            put("username", userSession.username)
            put("timestamp_nonce", timestampNonce)
            put("userSignature", signature)
            put("associationKey", associationKey)
        }.toString()

        val intent = Intent().apply {
            putExtra(MainActivity.KEY_EMAIL, userSession.username)
            putExtra(MainActivity.KEY_PAYLOAD, payload)
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
