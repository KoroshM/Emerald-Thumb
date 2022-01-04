/**
 * Korosh Moosavi
 * Fall 2021
 * Emerald Thumb app
 *
 * The main activity of MainActivity is to prepare the app and set up listeners
 * MainActivity calls Backend functions to auth the user and communicate with the device
 * Then it sets up strings to display, buttons, and listeners
 */

package com.example.emeraldthumb

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.*
import java.io.File
import java.util.*
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy


class MainActivity : AppCompatActivity() {

    // Prepare app content
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Check if user signed in during prior session
        // Sign in user if auth info found, prompt sign in otherwise
        // Set auth button icon to locked/unlocked as needed
        if (UserData.isSignedIn.value!!) {
            fabAuth.setImageResource(R.drawable.ic_baseline_lock)
            introtext.text = R.string.nologin.toString()
            Backend.signOut()
        } else {
            fabAuth.setImageResource(R.drawable.ic_baseline_unlock)
            introtext.text = R.string.loading.toString()
            Backend.signIn(this)
        }

        // Watch user sign-in status
        UserData.isSignedIn.observe(this, Observer<Boolean> { isSignedUp ->
            Log.i(TAG, "isSignedIn changed : $isSignedUp")

            // If user is signed in proceed to main content page, set the strings for the app, and update everything
            if (isSignedUp) {
                fabAuth.setImageResource(R.drawable.ic_baseline_unlock)

                setContentView(R.layout.content_main)
                fabAuth2.setImageResource(R.drawable.ic_baseline_unlock)
                setStrings()
                Picasso.get().load("https://rpiet.s3.us-west-2.amazonaws.com/photo/plantpic.jpg").into(plantpic)
                Backend.update(this)

            // If user is not signed in remain on starting page
            } else {
                setContentView(R.layout.activity_main)
                fabAuth.setImageResource(R.drawable.ic_baseline_lock)

                if (UserData.isSignedIn.value!!) {
                    fabAuth.setImageResource(R.drawable.ic_baseline_lock)
                    Backend.signOut()
                } else {
                    fabAuth.setImageResource(R.drawable.ic_baseline_unlock)
                    Backend.signIn(this)
                }
            }
        })
    }

    // Auth button listener
    // Calls Backend.signIn/signOut based on sign-in status
    fun fabAuthOnClick(v: View) {
        val authButton = v as FloatingActionButton

        if (UserData.isSignedIn.value!!) {
            Backend.signOut()
        } else {
            Backend.signIn(this)
        }
        if (UserData.isSignedIn.value!!) {
            authButton.setImageResource(R.drawable.ic_baseline_lock)
        } else {
            authButton.setImageResource(R.drawable.ic_baseline_unlock)
        }
    }

    // Refresh button listener
    // Send an update request to the device, download the updated data 10s later, and apply updates
    fun fabRefreshOnClick(v: View) {
        // Send update request then download latest readings
        Backend.update(applicationContext)

        // Wait 11s for new data download then apply new data
        Timer().schedule(object: TimerTask() {
            override fun run() {
                val s3Data = File("${applicationContext.filesDir}/ETdata.json")
                if (s3Data.exists()) {
                    update(s3Data)

                } else {
                    return
                }
            }
        }, 11000)
    }

    // Apply updates using new data file
    private fun update(s3Data: File) {
        val jsonString = s3Data.readText()

        try {
            val dataJson = JSONObject(jsonString)
            val pref = getSharedPreferences("MyPref", 0)
            val edit = pref.edit()

            // Moisture level
            val moistlvl = dataJson.getString("moistlvl")
            Log.i("moistlvl: ", moistlvl)
            val moistlvlraw = dataJson.getString("moistlvlraw")
            Log.i("moistlvl: ", moistlvlraw.toString())

            // Temperature
            val templvl = dataJson.getString("templvl")
            Log.i("templvl: ", templvl)
            val templvlraw = dataJson.getString("templvlraw")
            Log.i("templvlraw: ", templvlraw.toString())

            // Water level
            val waterlvl = dataJson.getString("waterlvl")
            Log.i("waterlvl: ", waterlvl)
            val waterlvlraw = dataJson.getString("waterlvlraw")
            Log.i("waterlvlraw: ", waterlvlraw.toString())

            // Update user strings in Shared Preferences
            edit.putString("moistlvl", moistlvl.toString())
            edit.putString("moistlvlraw", moistlvlraw.toString())
            edit.putString("templvl", templvl.toString())
            edit.putString("templvlraw", templvlraw.toString())
            edit.putString("waterlvl", waterlvl.toString())
            edit.putString("waterlvlraw", waterlvlraw.toString())
            edit.apply()

            // Apply new string values
            userplantname.setText(pref.getString("plantName", "Nully the Plant"))
            waterlvlval.setText(pref.getString("waterLvl", "0"))
            humidlvlval.text = pref.getString("moistlvl", "Dry!")
            templvlval.text = pref.getString("templvlraw", "25")

        } catch (je: JSONException) {
            Log.e(TAG,"Error reading device data")
        }
    }

    // Watering button listener
    // Sends a command for the device to water the plant
    fun fabWaterOnClick(v: View) {
        sendCommand("water", 0)
    }

    // Receive the web redirect after authentication
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Backend.handleWebUISignInResponse(requestCode, resultCode, data)
    }

    // Calls backend function with this context to upload the formatted command
    private fun sendCommand(command: String, value: Int) {
        Backend.sendMsg(applicationContext, command, value)
    }

    // Set the strings used in the app to the appropriate values
    // Uses Shared Preferences to store user-dependent strings
    private fun setStrings() {
        val pref = getSharedPreferences("MyPref", 0)
        val edit = pref.edit()

        // Load user strings
        userplantname.setText(pref.getString("plantName", "Nully the Plant"))
        waterlvlval.setText(pref.getString("waterlvlraw", "0"))
        humidlvlval.text = pref.getString("moistlvlraw", "Dry!")
        templvlval.text = pref.getString("templvlraw", "25")

        // Listener for editable text box displaying the plant name at the top of the main content page
        // Stores modified value to Shared Preferences
        userplantname.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                edit.putString("plantName", userplantname.text.toString())
                edit.apply()
            }
        })

        // Listener for editable text box displaying the current water level stored in the device
        // Sends the input value as a command to the device to update its water level
        // Uses setOnFocusChangeListener to avoid sending a command after every character input
        waterlvlval.setOnFocusChangeListener(object: View.OnFocusChangeListener{
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (!hasFocus) {
                    try {
                        val temp = waterlvlval.text.toString().toLong()
                    } catch (nfe: NumberFormatException) {
                        return
                    }
                    val valCheck = waterlvlval.text.toString().toLong()

                    // Ensures the input value is non-negative before sending update message
                    if (valCheck > 0 && valCheck < Int.MAX_VALUE) {
                        Toast.makeText(applicationContext,valCheck.toString(),Toast.LENGTH_LONG).show()
                        edit.putString("waterlvlraw", valCheck.toString())
                        edit.apply()

                        sendCommand("updatewater", valCheck.toInt())
                    } else {
                        Toast.makeText(applicationContext, "Invalid input", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}