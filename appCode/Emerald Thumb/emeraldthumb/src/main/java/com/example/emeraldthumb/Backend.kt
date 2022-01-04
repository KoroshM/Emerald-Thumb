/**
 * Korosh Moosavi
 * Fall 2021
 * Emerald Thumb app
 *
 * The main activity of Backend is to handle cloud communication functions
 * Backend performs the Amplify/Cognito auth functions and the Store upload/download functions
 */

package com.example.emeraldthumb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import java.io.*
import java.util.*

object Backend {

    private const val TAG = "Backend"

    // Initialize Amplify
    // Call Amplify functions to perform Cognito auth
    fun initialize(applicationContext: Context) : Backend {
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin(AWSS3StoragePlugin())

            val config = AmplifyConfiguration.builder(applicationContext).devMenuEnabled(true).build()
            Amplify.configure(config, applicationContext)

            Log.i(TAG, "Initialized Amplify")

        } catch (e: AmplifyException) {
            Log.e(TAG, "Could not initialize Amplify", e)
        }

        Log.i(TAG, "Registering hub event")
        // Listen for auth event
        Amplify.Hub.subscribe(HubChannel.AUTH) { hubEvent: HubEvent<*> ->
            when (hubEvent.name) {
                InitializationStatus.SUCCEEDED.toString() -> {
                    Log.i(TAG, "Amplify successfully initialized")
                }

                InitializationStatus.FAILED.toString() -> {
                    Log.i(TAG, "Amplify initialization failed")
                }
                else -> {
                    when (AuthChannelEventName.valueOf(hubEvent.name)) {
                        AuthChannelEventName.SIGNED_IN -> {
                            updateUserData(true)
                            Log.i(TAG, "HUB : SIGNED_IN")
                        }

                        AuthChannelEventName.SIGNED_OUT -> {
                            updateUserData(false)
                            Log.i(TAG, "HUB : SIGNED_OUT")
                        }
                        else -> Log.i(TAG, """HUB EVENT:${hubEvent.name}""")
                    }
                }
            }
        }

        Log.i(TAG, "Retrieving session status")
        // Check if user is already authenticated
        Amplify.Auth.fetchAuthSession(
            { result ->
                Log.i(TAG, result.toString())
                val cognitoAuthSession = result as AWSCognitoAuthSession

                // Update UI
                this.updateUserData(cognitoAuthSession.isSignedIn)

                when (cognitoAuthSession.identityId.type) {
                    AuthSessionResult.Type.SUCCESS ->  Log.i(TAG, "IdentityId: " + cognitoAuthSession.identityId.value)
                    AuthSessionResult.Type.FAILURE -> Log.i(TAG, "IdentityId not present because: " + cognitoAuthSession.identityId.error.toString())
                }
            },
            { error -> Log.i(TAG, error.toString()) }
        )

        return this
    }

    // Set user sign-in status
    private fun updateUserData(withSignedInStatus : Boolean) {
        UserData.setSignedIn(withSignedInStatus)
    }

    // Sign user out
    fun signOut() {
        Log.i(TAG, "Initiate Signout Sequence")
        Amplify.Auth.signOut(
            { Log.i(TAG, "Signed out!") },
            { error -> Log.e(TAG, error.toString()) }
        )
    }

    // Sign user in with WebUI
    fun signIn(callingActivity: Activity) {
        Log.i(TAG, "Initiate Signin Sequence")
        Amplify.Auth.signInWithWebUI(
            callingActivity,
            { result: AuthSignInResult ->  Log.i(TAG, result.toString()) },
            { error: AuthException -> Log.e(TAG, error.toString()) }
        )
    }

    // Pass the data from web redirect to Amplify
    fun handleWebUISignInResponse(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "received requestCode : $requestCode and resultCode : $resultCode")
        if (requestCode == AWSCognitoAuthPlugin.WEB_UI_SIGN_IN_ACTIVITY_CODE) {
            Amplify.Auth.handleWebUISignInResponse(data)
        }
    }

    // Send an update request to the device then download the update after 10s
    fun update(applicationContext: Context) {
        sendMsg(applicationContext,"updatestats",0)

        // Give 10 seconds for update command to be received and executed
        // Measured round-trip time is ~5s
        Timer().schedule(object: TimerTask() {
            override fun run() {
                val s3Data = File("${applicationContext.filesDir}/ETdata.json")
                Amplify.Storage.downloadFile("data/ETdata.json",
                    s3Data,
                    { Log.i("MyAmplifyApp", "Successfully downloaded: ${it.file.name}")},
                    { Log.e("MyAmplifyApp",  "Download Failure", it)}
                )

                val s3Pic = File("${applicationContext.filesDir}/plantpic.jpg")  // Save local copy for offline access until next update
                Amplify.Storage.downloadFile("photo/plantpic.jpg",
                    s3Pic,
                    { Log.i("MyAmplifyApp", "Successfully downloaded: ${it.file.name}")},
                    { Log.e("MyAmplifyApp",  "Download Failure", it)}
                )
            }
        }, 10000)
    }

    // Method to simplify sending MQTT commands in Kotlin
    // Cloud lambda will republish the contents of this JSON to the 'rpi/input' topic
    fun sendMsg(applicationContext: Context, command: String, value: Int) {
        val file = File("${applicationContext.filesDir}/ETcommand.json")
        file.writeText("{\"command\":\"$command\",\"value\":$value}")

        Amplify.Storage.uploadFile("app/ETcommand.json", file,
            { Log.i("MyAmplifyApp", "Successfully uploaded: ${it.key}")},
            { Log.e("MyAmplifyApp", "Command upload failed", it)}
        )
    }
}