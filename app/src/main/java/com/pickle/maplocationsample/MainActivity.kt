package com.pickle.maplocationsample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import timber.log.Timber

private const val REQUEST_PERMISSION_LOCATION_START_UPDATE = 101
private const val REQUEST_CHECK_FOR_SETTINGS = 200

private const val MAP_DEFAULT_ZOOM = 8f

/**
 * For more informations go to https://developer.android.com/training/location
 * What you need to do to get the location
 * - Adding Google services for location (check the doc for dependency)
 * - Add permissions to the AndroidManifest.xml
 * - Check and ask (onRequestPermissionsResult) location permissions on runtime
 *     (Note that Android version will ask different types of permissions)
 * - Check if location is enabled on the phone, handle if it's not the case
 * - Create your location request (With time interval, precisions etc.)
 * - Start location update from fused client
 * - Handle the result.
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var locationLiveData: LocationLiveData

    private var userMarker : Marker? = null

    private var firstLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * We set map options according to the documentation here
         * https://developers.google.com/maps/documentation/android-sdk/map#programmatically
         * We put any settings we want for our map
         */
        val mapOptions = GoogleMapOptions()
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .zoomControlsEnabled(true)
            .zoomGesturesEnabled(true)

        //See https://developers.google.com/maps/documentation/android-sdk/map
        //For more detail about how to use a map Fragment
        val mapFragment = SupportMapFragment.newInstance(mapOptions)
        mapFragment.getMapAsync(this)

        //Now we replace the framelayout set in our XML by the actual map Fragment
        supportFragmentManager
            .beginTransaction().replace(R.id.mapFrame, mapFragment)
            .commit()

        locationLiveData = LocationLiveData(this)
        locationLiveData.observe(this, Observer { handleLocationData(it!!) })

    }

    /**
     * Callback set to this activity by `getMapAsync` of the SupportMapFragment furnished by Google
     * On map Ready is called when Google map is ready to display the map inside the fragment
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            map = it
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_theme))
        }
    }

    /**
     * Override of Activity function, will be useful when you start an activity to expect a result from it.
     * You must attribute a request code when call the external activity. This activity will respond with the provided request code,
     * so you can act following the request code received.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_FOR_SETTINGS -> locationLiveData.startRequestLocation()
        }
    }


    /**
     * This override the request permission result.
     * So we gather the user response of what the system has prompted about our permission request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //Not so cool, should be refactored be able to handle any size of permissions array
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return
        }

        //We can manage multiple different permissions. We need to set up what to do when a permission is granted.
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION_START_UPDATE -> locationLiveData.startRequestLocation()
        }
    }

    private fun handleLocationData(locationData: LocationData) {
        if (handleLocationException(locationData.exception)) {
            return
        }

        //If we get the location
        locationData.location?.let { it ->
            val latLng = LatLng(it.latitude, it.longitude)

            map.moveCamera(CameraUpdateFactory
                .newLatLngZoom(latLng, MAP_DEFAULT_ZOOM))
            firstLocation = false
        }
    }

    /**
     * Will handle all exceptions coming from our LocationLiveData
     */
    private fun handleLocationException(exception: Exception?): Boolean {
        exception ?: return false
        Timber.e(exception, "HandleLocationException()")
        when (exception) {
            is SecurityException -> checkLocationPermission(
                REQUEST_PERMISSION_LOCATION_START_UPDATE
            )
            is ResolvableApiException -> exception.startResolutionForResult(
                this,
                REQUEST_CHECK_FOR_SETTINGS
            )
        }
        return true
    }


    /**
     * Ask for permissions when needed location
     * As noted that in Android 10, you need to also ask for ACCESS_BACKGROUND_LOCATION
     * See https://www.youtube.com/watch?v=L7zwfTwrDEs
     */
    private fun checkLocationPermission(requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
            return false
        }
        return true
    }


}