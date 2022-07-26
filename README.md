# Overview

This module contains example of implementation [Keyri](https://keyri.com) with AWS Cognito
Authentication.

## Contents

* [Requirements](#Requirements)
* [Permissions](#Permissions)
* [Keyri Integration](#Keyri-Integration)
* [AWS Cognito Authentication Integration](#AWS-Cognito-Authentication-Integration)
* [Authentication](#Authentication)

## Requirements

* Android API level 23 or higher
* AndroidX compatibility
* Kotlin coroutines compatibility

Note: Your app does not have to be written in kotlin to integrate this SDK, but must be able to
depend on kotlin functionality.

## Permissions

Open your app's `AndroidManifest.xml` file and add the following permission:

```xml

<uses-permission android:name="android.permission.INTERNET" />
```

## Keyri Integration

* Add the JitPack repository to your root build.gradle file:

```groovy
allprojects {
    repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
}
```

* Add SDK dependency to your build.gradle file and sync project:

```kotlin
dependencies {
    // ...
    implementation("com.github.Keyri-Co.keyri-android-whitelabel-sdk:keyrisdk:$latestKeyriVersion")
    implementation("com.github.Keyri-Co.keyri-android-whitelabel-sdk:scanner:$latestKeyriVersion")
}
```

## AWS Cognito Authentication Integration

* Add Cognito AWS service to your
  project: [Getting started with Amazon Cognito](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-getting-started.html)
  .

* Create and
  configure [Amazon Cognito User Pool](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools.html?icmpid=docs_cognito_console_help_panel)
  .

* Add the `mavenLocal()` repository to your root `build.gradle` file:

```groovy
allprojects {
    repositories {
        // ...
        mavenLocal()
    }
}
```

* Add AWS Cognito dependencies to your `build.gradle` file and sync project:

```groovy
  // AWS Cognito
implementation 'com.amazonaws:aws-android-sdk-cognitoauth:2.49.0'
implementation 'com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.49.0'
implementation 'com.amazonaws:aws-android-sdk-cognitoidentityprovider-asf:2.49.0'
```

* Check this
  guide [Using Android SDK with Amazon Cognito Your User Pools](https://aws.amazon.com/blogs/mobile/using-android-sdk-with-amazon-cognito-your-user-pools/)
  of using Cognito SDK.

* Or you can
  use [Amplify Auth](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android/) to
  interact with Cognito.

## Authentication

* Sign in:

```kotlin
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

        override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation) =
            Unit

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
```

* Sign up:

```kotlin
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
```

* Create payload:

```kotlin
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
```

* Authenticate with Keyri. In the next showing `AuthWithScannerActivity` with providing
  `publicUserId` and `payload`.

```kotlin
private val easyKeyriAuthLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Process authentication result
    }

private fun keyriAuth(publicUserId: String?, payload: String) {
    val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
        putExtra(AuthWithScannerActivity.APP_KEY, BuildConfig.APP_KEY)
        putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, publicUserId)
        putExtra(AuthWithScannerActivity.PAYLOAD, payload)
    }

    easyKeyriAuthLauncher.launch(intent)
}
```
