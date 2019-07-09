package com.nibraas.uber

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CustomerMapActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    private lateinit var mMap: GoogleMap
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var lastLocation: Location
    private lateinit var locationRequest: LocationRequest

    private lateinit var logoutBtn: Button
    private lateinit var callUberBtn: Button
    private lateinit var settingsBtn: Button

    private lateinit var pickUpLocation: LatLng

    private var driverFound = false
    private lateinit var driverID: String


    private val LOCATION_REQUEST_CODE = 1
    private var radius: Double = 1.0
    lateinit var mapFragment: SupportMapFragment

    var driverMarker: Marker? = null
    private var pickupMarker: Marker? = null

    private var requestBol = false

    private lateinit var geoQuery: GeoQuery
    private lateinit var driverLocationRef: DatabaseReference
    private lateinit var driverLocationRefListener: ValueEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        logoutBtn = findViewById(R.id.logout)
        callUberBtn = findViewById(R.id.request)
        settingsBtn = findViewById(R.id.setting)

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, CustomerSettingsActivity::class.java))
        }

        callUberBtn.setOnClickListener {
            when (requestBol){
                false -> {

                    requestBol = true

                    val userID = FirebaseAuth.getInstance().currentUser?.uid
                    val databaseReference = FirebaseDatabase.getInstance().reference.child("customerRequests")

                    GeoFire(databaseReference).setLocation(userID,
                        GeoLocation(lastLocation.latitude, lastLocation.longitude)) { _, _ -> }

                    pickUpLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    pickupMarker = mMap.addMarker(MarkerOptions().position(pickUpLocation).title("Pick up here").icon(bitmapDescriptorFromVector(this, R.mipmap.ic_pickup)))

                    callUberBtn.text = "Getting your driver"

                    getClosestDriver()
                }
                true -> {
                    requestBol = false

                    geoQuery.removeAllListeners()
                    driverLocationRef.removeEventListener(driverLocationRefListener)

                    val userID = FirebaseAuth.getInstance().currentUser?.uid
                    val databaseReference = FirebaseDatabase.getInstance().reference.child("customerRequests")
                    GeoFire(databaseReference).removeLocation(userID) {_, _ -> }

                    if (driverID.isNotEmpty()){
                        val databaseRef = FirebaseDatabase.getInstance().reference
                            .child("Users")
                            .child("Drivers")
                            .child(driverID)
                        databaseRef.setValue(true)
                        driverID = ""
                    }
                    driverFound = false
                    radius = 1.0

                    pickupMarker?.remove()
                    driverMarker?.remove()

                    callUberBtn.text = "Call Uber"
                }
            }

        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)
        }else {
            mapFragment.getMapAsync(this)
        }
    }

    private fun getClosestDriver() {
        val driverLocations = FirebaseDatabase.getInstance().reference.child("DriversAvailable")
        val geoFire = GeoFire(driverLocations)
        geoQuery = geoFire.queryAtLocation(GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius)
        geoQuery.removeAllListeners()

        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener{
            override fun onGeoQueryReady() {
                if (!driverFound){
                    radius++
                    Log.d("here", "here")
                    getClosestDriver()
                }
            }

            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                if (!driverFound && requestBol) {
                    driverFound = true
                    driverID = key!!



                    val databaseRef = FirebaseDatabase.getInstance().reference
                        .child("Users")
                        .child("Drivers")
                        .child(driverID)
                    val customerID = FirebaseAuth.getInstance().currentUser?.uid

                    databaseRef.child("customerRideID").setValue(customerID)

                    callUberBtn.text = "Looking for Uber location"
                    getDriverLocation()

                }
            }

            override fun onKeyMoved(key: String?, location: GeoLocation?) {}

            override fun onKeyExited(key: String?) {}

            override fun onGeoQueryError(error: DatabaseError?) {}

        })
    }

    private fun getDriverLocation() {
        callUberBtn.text = "Driver Found"
        driverLocationRef = FirebaseDatabase.getInstance().reference
            .child("DriversWorking")
            .child(driverID)
            .child("l")
        driverLocationRefListener = driverLocationRef.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists() && requestBol){
                    val map = p0.value as(List<*>)
                    var locationLat = 0.0
                    var locationLang = 0.0

                    if (map[0] != null){
                        locationLat = map[0].toString().toDouble()
                    }

                    if (map[1] != null){
                        locationLang = map[1].toString().toDouble()
                    }
                    val driverLatLang = LatLng(locationLat, locationLang)
                    driverMarker = if (driverMarker == null){
                        mMap.addMarker(MarkerOptions().position(driverLatLang).title("Driver Location").icon(bitmapDescriptorFromVector(this@CustomerMapActivity, R.mipmap.ic_car)))
                    } else {
                        driverMarker?.remove()
                        mMap.addMarker(MarkerOptions().position(driverLatLang).title("Driver Location").icon(bitmapDescriptorFromVector(this@CustomerMapActivity, R.mipmap.ic_car)))
                    }


                    val loc1 = Location("")
                    loc1.longitude = pickUpLocation.longitude
                    loc1.latitude = pickUpLocation.latitude

                    val loc2 = Location("")
                    loc2.longitude = driverLatLang.longitude
                    loc2.latitude = driverLatLang.latitude

                    val distance = loc1.distanceTo(loc2)

                    if (distance < 100){
                        callUberBtn.text = "Driver is here"
                    } else {
                        callUberBtn.text = distance.toString()
                    }
                }
            }

        })
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

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            this.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}
