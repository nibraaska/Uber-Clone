package com.nibraas.uber

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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

    private lateinit var logoutBtn: Button


    private val LOCATION_REQUEST_CODE = 1
    lateinit var mapFragment: SupportMapFragment

    private var customerID: String? = null

    private var pickUpMarker: Marker? = null
    private lateinit var assignedCustomerPickupLocationRef: DatabaseReference
    private var assignedCustomerPickupLocationRefListener: ValueEventListener? = null

    private var notLoggingOut = false

    val userID = FirebaseAuth.getInstance().currentUser?.uid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        logoutBtn = findViewById(R.id.logout)
        logoutBtn.setOnClickListener {
            notLoggingOut = true
            disconnectDriver()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)
        }else {
            mapFragment.getMapAsync(this)
        }

        getAssignedCustomer()
    }

    private fun getAssignedCustomer() {
        val driverID = FirebaseAuth.getInstance().currentUser?.uid
        val assignedCustomerRef = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child("Drivers")
            .child(driverID!!)
            .child("customerRideID")

        assignedCustomerRef.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                customerID = p0.value.toString()
                getAssignedCustomerPickUpLocation()
            }
        })
    }

    private fun getAssignedCustomerPickUpLocation() {
        assignedCustomerPickupLocationRef =
            customerID?.let {
                FirebaseDatabase.getInstance().reference
                    .child("customerRequests")
                    .child(it)
                    .child("l")
            }!!

        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists() && customerID!=null) {
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
                    pickUpMarker = mMap.addMarker(MarkerOptions().position(driverLatLang).title("Pickup Location").icon(bitmapDescriptorFromVector(this@DriverMapActivity, R.mipmap.ic_pickup)))
                } else {
                    customerID = null
                    pickUpMarker?.remove()
                    if (assignedCustomerPickupLocationRefListener != null) {
                        assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener!!)
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

        val refAvailable = FirebaseDatabase.getInstance().reference.child("DriversAvailable")
        val refWorking = FirebaseDatabase.getInstance().reference.child("DriversWorking")

        val geoFireAvailable = GeoFire(refAvailable)
        val geoFireWorking = GeoFire(refWorking)

        Log.d("here", customerID.toString())
        when(customerID.toString()){
            "null" -> {
                geoFireWorking.removeLocation(userID) { _, _ -> }
                geoFireAvailable.setLocation(userID, GeoLocation(lastLocation.latitude, lastLocation.longitude)) { key, value ->  Log.d("here", "Available $key, $value")}
            }
            else -> {
                geoFireAvailable.removeLocation(userID) { _, _ -> }
                geoFireWorking.setLocation(userID, GeoLocation(lastLocation.latitude, lastLocation.longitude)) { key, value ->  Log.d("here", "Working $key, $value")}
            }
        }
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

    private fun disconnectDriver(){
        val firebaseReference = FirebaseDatabase.getInstance().reference.child("driversAvailable")

        val geoFire = GeoFire(firebaseReference)
        geoFire.removeLocation(userID) {_, _ -> }
    }
    override fun onStop() {
        super.onStop()
        if (!notLoggingOut){
            disconnectDriver()
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
