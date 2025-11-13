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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.*
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

private fun createBitmapDescriptorFromText(context: Context, title: String, subtitle: String, isUser: Boolean = false): BitmapDescriptor {
    val inflater = LayoutInflater.from(context)
    val markerView: View = inflater.inflate(R.layout.view_marker, null)

    val titleTv = markerView.findViewById<TextView>(R.id.markerTitle)
    val subtitleTv = markerView.findViewById<TextView>(R.id.markerSubtitle)

    titleTv.text = title
    subtitleTv.text = subtitle

    // Optional: if user marker, adjust background tint programmatically
    if (isUser) {
        // set a different background if you made marker_background_user drawable
        markerView.setBackgroundResource(R.drawable.marker_background_user)
        // optionally change text color: titleTv.setTextColor(...)
    }

    // Measure & layout the view (required before drawing)
    val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    markerView.measure(spec, spec)
    markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

    val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    markerView.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}


class GoogleMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private lateinit var API_KEY: String
    private lateinit var bottomNavigation: BottomNavigationView

    // --- Retrofit interface (Places Nearby Search) ---
    interface GooglePlacesService {
        @GET("maps/api/place/nearbysearch/json")
        fun getNearbyPlaces(
            @Query("location") location: String,
            @Query("radius") radius: Int,
            @Query("type") type: String,
            @Query("key") key: String
        ): Call<PlacesApiResponse>
    }

    data class PlacesApiResponse(val results: List<PlaceResult>?)
    data class PlaceResult(val name: String?, val geometry: Geometry?)
    data class Geometry(val location: LocationDetail?)
    data class LocationDetail(val lat: Double, val lng: Double)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_map)

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()

        val fabRight = findViewById<FloatingActionButton>(R.id.fabRight)

        fabRight.setOnClickListener {
            val intent = Intent(this, NearbyStationsActivity::class.java)
            startActivity(intent)
        }

        val fabLeft = findViewById<FloatingActionButton>(R.id.fabLeft)

        fabLeft.setOnClickListener {
            val intent = Intent(this, ShareContactActivity::class.java)
            startActivity(intent)
        }





        // read API key from manifest metadata (same utility used earlier)
        API_KEY = getApiKeyFromManifest()
        if (API_KEY.isEmpty()) {
            Toast.makeText(this, "API key missing in manifest", Toast.LENGTH_LONG).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun setupBottomNavigation() {
        // Correctly set the selected item for the current screen
        bottomNavigation.selectedItemId = R.id.nav_map

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_map -> true // This is the current activity, so no action/just return true
                R.id.nav_community -> {
                    // Navigate to Community Activity
                    val intent = Intent(this, ChannelsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.nav_helpline -> { // <-- NEW HELPLINE LOGIC
                    val intent = Intent(this, HelplineActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                else -> false
            }
        }
    }


    private fun getApiKeyFromManifest(): String {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            Log.e("API_KEY", "manifest read failed: ${e.message}")
            ""
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        mMap.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is PoliceStation) {
                openNavigationTo(tag.latitude, tag.longitude)
                true
            } else {
                false
            }
        }

        checkLocationPermissionAndShow()
    }

    private fun checkLocationPermissionAndShow() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            showCurrentLocationAndLoadStations()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showCurrentLocationAndLoadStations()
        } else {
            Toast.makeText(this, "Location permission required to show nearby stations.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showCurrentLocationAndLoadStations() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))

                    // Add a distinct user marker using custom blue background (marker_background_user)
                    val userDesc = createBitmapDescriptorFromText(this, "You", "", isUser = true)
                    val userMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(userLatLng)
                            .title("You")
                            .icon(userDesc)
                    )
                    // userMarker?.tag = ... (optional)

                    // Fetch nearby police stations and add markers
                    fetchNearbyPolice(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Unable to obtain location.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchNearbyPolice(lat: Double, lng: Double, radius: Int = 5000) {
        if (API_KEY.isEmpty()) {
            Toast.makeText(this, "Missing API Key", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Searching nearby police...", Toast.LENGTH_SHORT).show()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GooglePlacesService::class.java)
        service.getNearbyPlaces(
            location = "$lat,$lng",
            radius = radius,
            type = "police",
            key = API_KEY
        ).enqueue(object : Callback<PlacesApiResponse> {
            override fun onResponse(call: Call<PlacesApiResponse>, response: Response<PlacesApiResponse>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@GoogleMapActivity, "Places API error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val results = response.body()?.results ?: emptyList()
                if (results.isEmpty()) {
                    Toast.makeText(this@GoogleMapActivity, "No nearby police stations found.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Add markers for each result
                for (r in results) {
                    val name = r.name ?: "Police Station"
                    val latL = r.geometry?.location?.lat ?: continue
                    val lngL = r.geometry.location.lng

                    // inside onResponse where you loop results
                    val distanceKm = calculateDistanceKm(lat, lng, latL, lngL) // your helper
                    val distanceText = if (distanceKm < 1.0) {
                        "${(distanceKm * 1000).toInt()} m"
                    } else {
                        String.format("%.2f km", distanceKm)
                    }

// Create a marker showing name + distance
                    val caption = name
                    val subtitle = distanceText
                    val bd = createBitmapDescriptorFromText(this@GoogleMapActivity, caption, subtitle, isUser = false)

                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(latL, lngL))
                            .title(name) // still useful for accessibility/tooltip
                            .icon(bd)
                    )
// attach data
                    marker?.tag = PoliceStation(name, latL, lngL, distanceKm)

                }

                // Optionally, adjust camera bounds to include markers (not implemented here)
            }

            override fun onFailure(call: Call<PlacesApiResponse>, t: Throwable) {
                Toast.makeText(this@GoogleMapActivity, "Places API call failed", Toast.LENGTH_SHORT).show()
                Log.e("PlacesAPI", "failure", t)
            }
        })
    }

    private fun openNavigationTo(lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // fallback to generic geo URI if Google Maps not installed
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        }
    }

    // Haversine distance (km) — optional helper
    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }
}
