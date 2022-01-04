/**
 * The UserData class is responsible to hold user data, namely a isSignedIn flag to track current
 *   authentication status and a list of Note objects.
 * These two properties are implemented according to the LiveData publish / subscribe framework.
 *   It allows the Graphical User Interface (GUI) to subscribe to changes and to react accordingly.
 *   To follow best practice, keep the MutableLiveData property private and only expose the readonly
 *   LiveData property. Some additional boilerplate code is required when the data to publish is a
 *   list to make sure observers are notified when individual components in the list are modified.
 * We also added a Note data class, just to hold the data of individual notes. Two distinct
 *   properties are used for ImageName and Image. Image will be taken care of in a subsequent module
 * Implemented the singleton design pattern for the UserData object as it allows referral to it from
 *   anywhere in the application just with UserData.
 */

package com.example.emeraldthumb

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// a singleton to hold user data (this is a ViewModel pattern, without inheriting from ViewModel)
object UserData {

    private const val TAG = "UserData"

    // signed in status
    private val _isSignedIn = MutableLiveData<Boolean>(false)
    var isSignedIn: LiveData<Boolean> = _isSignedIn

    fun setSignedIn(newValue : Boolean) {
        // use postvalue() to make the assignation on the main (UI) thread
        _isSignedIn.postValue(newValue)
    }
}