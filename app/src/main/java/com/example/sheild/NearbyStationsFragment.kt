package com.example.sheild

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.*
import com.example.sheild.PoliceStation
// Reuse your GooglePlacesService and data models from the Activity file
// (If they are in the same package file, no duplication needed. Otherwise copy them here.)

class NearbyStationsFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var stationsAdapter: StationsAdapter
    private lateinit var textEmpty: TextView
    private lateinit var API_KEY: String

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_nearby_stations_fragment, container, false)

        recyclerView = root.findViewById(R.id.recyclerViewStations)
        textEmpty = root.findViewById(R.id.textEmpty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize fused location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Load API key from manifest metadata (same method used previously)
        API_KEY = getApiKeyFromManifest()
        if (API_KEY.isEmpty()) {
            Toast.makeText(requireContext(), "API Key not found in Manifest", Toast.LENGTH_LONG).show()
            textEmpty.text = "Configuration error"
            return root
        }

        checkLocationPermissionAndFetch()
        return root
    }

    private fun getApiKeyFromManifest(): String {
        return try {
            val appInfo = requireActivity().packageManager.getApplicationInfo(requireActivity().packageName, PackageManager.GET_META_DATA)
            appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("API_KEY", "Failed to get API key from manifest: ${e.message}")
            ""
        }
    }

    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchUserLocationAndStations()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationAndStations()
            } else {
                Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_LONG).show()
                textEmpty.text = "Location permission required"
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun fetchUserLocationAndStations() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            textEmpty.text = "Location permission required"
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    searchNearbyPoliceStations(location.latitude, location.longitude)
                } else {
                    textEmpty.text = "Could not retrieve location"
                }
            }
            .addOnFailureListener {
                textEmpty.text = "Failed to get location"
            }
    }

    // Retrofit interface and models used by your Activity — reuse or place here if needed
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

    private fun searchNearbyPoliceStations(lat: Double, lon: Double) {
        textEmpty.visibility = View.VISIBLE
        textEmpty.text = "Finding nearby police..."

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GooglePlacesService::class.java)
        service.getNearbyPlaces(
            location = "$lat,$lon",
            radius = 5000,
            type = "police",
            key = API_KEY
        ).enqueue(object : Callback<PlacesApiResponse> {
            override fun onResponse(call: Call<PlacesApiResponse>, response: Response<PlacesApiResponse>) {
                if (response.isSuccessful) {
                    val placesResponse = response.body()
                    val stationList = mutableListOf<PoliceStation>()

                    placesResponse?.results?.forEach { result ->
                        val name = result.name ?: "Unknown"
                        val stationLat = result.geometry?.location?.lat ?: return@forEach
                        val stationLon = result.geometry.location.lng
                        val distance = calculateDistance(lat, lon, stationLat, stationLon)
                        stationList.add(PoliceStation(name, stationLat, stationLon, distance))
                    }

                    if (stationList.isEmpty()) {
                        textEmpty.text = "No stations found"
                        textEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        stationList.sortBy { it.distance }
                        stationsAdapter = StationsAdapter(stationList) { station -> onStationClick(station) }
                        recyclerView.adapter = stationsAdapter
                        textEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                } else {
                    textEmpty.text = "Failed to fetch stations (${response.code()})"
                }
            }

            override fun onFailure(call: Call<PlacesApiResponse>, t: Throwable) {
                textEmpty.text = "Error contacting server"
                Log.e("PlacesAPI", "API Call Failed", t)
            }
        })
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun onStationClick(station: PoliceStation) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(requireContext(), "Google Maps app is not installed.", Toast.LENGTH_SHORT).show()
        }
    }
}
