/**
 * Korosh Moosavi
 * Fall 2021
 * Emerald Thumb app
 *
 * The app launches on this method, which just calls the initialize function in Backend.kt
 */

package com.example.emeraldthumb

import android.app.Application

class EmeraldThumb : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Amplify when application is starting
        Backend.initialize(applicationContext)
    }
}