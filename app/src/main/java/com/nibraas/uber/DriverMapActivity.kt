package com.nibraas.uber

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DriverMapActivity : AppCompatActivity(),
                            OnMapReadyCallback,
                            GoogleApiClient.ConnectionCallbacks,
                            GoogleApiClient.OnConnectionFailedListener,
                            LocationListener{
    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    private lateinit var mMap: GoogleMap
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var lastLocation: Location
    private lateinit var locationRequest: LocationRequest


    private val LOCATION_REQUEST_CODE = 1
    lateinit var mapFragment: SupportMapFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)
        }else {
            mapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)
        }
        buildGoogleApiClient()
        mMap.isMyLocationEnabled = true
    }

    private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient
            .Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        googleApiClient.connect()
    }

    override fun onConnected(p0: Bundle?) {
        locationRequest = LocationRequest()
            .setInterval(1000)
            .setFastestInterval(1000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location

        val latLng = LatLng(location.latitude, location.longitude)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11F))


        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val firebaseReference = FirebaseDatabase.getInstance().reference.child("driversAvailable")

        val geoFire = GeoFire(firebaseReference)
        geoFire.setLocation(userID, GeoLocation(lastLocation.latitude, lastLocation.longitude)) { _, _ -> }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            LOCATION_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this)
                } else {
                    Toast.makeText(this, "Please provide the permission for map", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val firebaseReference = FirebaseDatabase.getInstance().reference.child("driversAvailable")

        val geoFire = GeoFire(firebaseReference)
        geoFire.removeLocation(userID)
    }
}
