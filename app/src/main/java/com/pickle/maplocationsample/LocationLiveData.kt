package com.pickle.maplocationsample


import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LiveData
import com.google.android.gms.location.*
import timber.log.Timber
import java.lang.Exception

data class LocationData(val location: Location? = null, val exception: Exception? = null)
//This refactor the huge main activity

class LocationLiveData(context: Context): LiveData<LocationData>() {
    //We use the appContext to avoid strong link with the activity or fragment.
    //Cause location can be used across multiple part of the app.
    private val appContext = context.applicationContext

    //Usage of Google Play services
    //Fused mean that this service will combine (fusion) multiple way to localize this device (wifi, cellular, gps, etc.)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    /**
     * See apply in Kotlin documentation to learn more about this syntax.
     * https://kotlinlang.org/docs/reference/scope-functions.html#apply
     */
    private val locationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Callback provided to the request location function.
     * This let the system answer to your code with the location it found for the device.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            locationResult.locations.forEach { location ->
                value =
                    LocationData(location = location)
            }
        }
    }

    //User to track the first LiveData subscriber
    // To send the last known location immediately
    private var firstSubscriber = true

    override fun onActive() {
        super.onActive()
        if(firstSubscriber){
            requestLastLocation()
            requestLocation()
            firstSubscriber = false
        }

    }

    override fun onInactive() {
        super.onInactive()
        //Be careful, cause only one client unsubscribe from the live data, it will stop updating for everyone :o
        fusedLocationClient.removeLocationUpdates(locationCallback)
        firstSubscriber = true
    }


    /**
     * Create a request to ask the system to refresh the location of the device
     * Also check if the location is enabled
     */
    fun startRequestLocation(){
        /**
         * Here we only check if the User has enabled the location settings on the device.
         */
        val task = LocationServices
            .getSettingsClient(appContext)
            .checkLocationSettings(
                LocationSettingsRequest.Builder().apply {
                    addLocationRequest(locationRequest)
                }.build()
            )

        /**
         * According to the location settings request responses we trigger some actions.
         * Here if it's success, it means that user has enabled Location service, so we are good
         */
        task.addOnSuccessListener { locationSettingsResponse ->
            Timber.i("Location settings satisfied. Init location request here")
            requestLocation()
        }

        /**
         * Here we handle when user has disabled the Location Services, so we trigger (if the exception type allow it)
         * the Setting interface to launch to let user change its settings.
         */
        task.addOnFailureListener {
            Timber.e(it, "Failed to modify location settings")
            value = LocationData(exception = it)
        }
    }

    /**
     * Now we start getting the actual location ;)
     */
    private fun requestLocation() {
        //We get the location
        // See https://developer.android.com/training/location/receive-location-updates for more information
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (exception: SecurityException){
            value =
                LocationData(exception = exception)
        }
    }

    /**
     * Getting the last known location from the operating system.
     */
    private fun requestLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                value =
                    LocationData(location = location)
            }
            fusedLocationClient.lastLocation.addOnFailureListener{ exception ->
                value =
                    LocationData(exception = exception)
            }
        } catch (e: SecurityException){
            value = LocationData(exception = e)
        }
    }

}