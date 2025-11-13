package com.example.sheild

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.*
import com.example.sheild.PoliceStation
import com.google.android.material.bottomnavigation.BottomNavigationView


// ------------------------------------------------------------------
// --- RETROFIT INTERFACE: Defines the API endpoint call ---
// ------------------------------------------------------------------
interface GooglePlacesService {
    @GET("maps/api/place/nearbysearch/json")
    fun getNearbyPlaces(
        @Query("location") location: String, // User's lat,lon string
        @Query("radius") radius: Int,       // Search radius in meters
        @Query("type") type: String,        // Must be "police_station"
        @Query("key") key: String          // Your API Key
    ): Call<PlacesApiResponse>
}

// ------------------------------------------------------------------
// --- DATA MODELS: Used by Gson to parse the JSON response ---
// ------------------------------------------------------------------

data class PlacesApiResponse(
    val results: List<PlaceResult>
)

data class PlaceResult(
    val name: String,
    val geometry: Geometry
)

data class Geometry(
    val location: LocationDetail
)

data class LocationDetail(
    val lat: Double,
    val lng: Double // Google API uses 'lng' for longitude
)


// ------------------------------------------------------------------
// --- MAIN ACTIVITY CODE ---
// ------------------------------------------------------------------

class NearbyStationsActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var stationsAdapter: StationsAdapter
    // API Key will be read from the AndroidManifest
    private lateinit var API_KEY: String
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    private lateinit var bottomNavigation: BottomNavigationView

    // Data class to hold processed station details for the RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_stations)

        bottomNavigation = findViewById(R.id.bottomNav)
        setupBottomNavigation()// Requires recyclerViewStations ID

        // Initialize API Key from the manifest
        API_KEY = getApiKeyFromManifest()
        if (API_KEY.isEmpty()) {
            Toast.makeText(this, "Configuration Error: API Key not found in Manifest!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        recyclerView = findViewById(R.id.recyclerViewStations)
        recyclerView.layoutManager = LinearLayoutManager(this)

        checkLocationPermissionAndFetch()
    }

    private fun setupBottomNavigation() {
        // Use the existing bottomNavigation member, not a re-declared local variable
        // Assuming R.id.bottomNavigationView is the ID in your XML, or use R.id.bottomNav if that's correct

        // Set Home as selected, assuming this activity is the 'Home' screen or launched from it
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish() // Close this activity when navigating away
                    true
                }
                R.id.nav_map -> {
                    val intent = Intent(this, GoogleMapActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish() // Close this activity when navigating away
                    true
                }
                R.id.nav_community -> {
                    val intent = Intent(this, ChannelsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish() // Close this activity when navigating away
                    true
                }
                R.id.nav_helpline -> {
                    val intent = Intent(this, HelplineActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish() // Close this activity when navigating away
                    true
                }
                else -> false
            }
        }
    }


    /**
     * Reads the Google Maps API Key securely stored in AndroidManifest.xml metadata.
     */
    private fun getApiKeyFromManifest(): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("API_KEY", "Failed to get API key from manifest: ${e.message}")
            ""
        }
    }

    /**
     * Checks if location permission is granted, otherwise requests it.
     */
    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchUserLocationAndStations()
        }
    }

    /**
     * Handles the result of the permission request.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndStations()
        } else {
            Toast.makeText(this, "Location permission is required to find nearby stations.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Retrieves the user's last known location and starts the API search.
     */
    private fun fetchUserLocationAndStations() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    searchNearbyPoliceStations(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Could not retrieve current location.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Uses Retrofit to call the Google Places Nearby Search API.
     */
    private fun searchNearbyPoliceStations(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GooglePlacesService::class.java)

        service.getNearbyPlaces(
            location = "$lat,$lon",
            radius = 5000, // 5 km radius
            type = "police",
            key = API_KEY
        ).enqueue(object : Callback<PlacesApiResponse> {
            override fun onResponse(call: Call<PlacesApiResponse>, response: Response<PlacesApiResponse>) {
                if (response.isSuccessful) {
                    val placesResponse = response.body()
                    val stationList = mutableListOf<PoliceStation>()

                    placesResponse?.results?.forEach { result ->
                        val stationLat = result.geometry.location.lat
                        val stationLon = result.geometry.location.lng
                        // Calculate straight-line distance since Places API doesn't return it
                        val distance = calculateDistance(lat, lon, stationLat, stationLon)

                        stationList.add(PoliceStation(
                            name = result.name,
                            latitude = stationLat,
                            longitude = stationLon,
                            distance = distance
                        ))
                    }

                    // Sort the list by distance in ascending order
                    stationList.sortBy { it.distance }

                    stationsAdapter = StationsAdapter(stationList) { station ->
                        onStationClick(station)
                    }
                    recyclerView.adapter = stationsAdapter

                } else {
                    Log.e("PlacesAPI", "Response not successful: ${response.code()}")
                    Toast.makeText(this@NearbyStationsActivity, "Failed to fetch stations. Response Code: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlacesApiResponse>, t: Throwable) {
                Log.e("PlacesAPI", "API Call Failed", t)
                Toast.makeText(this@NearbyStationsActivity, "Error contacting server.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Calculates the great-circle distance between two points using the Haversine formula (in KM).
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Launches Google Maps for navigation when a list item is clicked.
     */
    private fun onStationClick(station: PoliceStation) {
        // URI format for starting navigation in Google Maps
        val gmmIntentUri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps") // Force open in Google Maps app

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_SHORT).show()
        }
    }
}