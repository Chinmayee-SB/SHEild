package com.example.sheild

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import com.google.android.material.bottomnavigation.BottomNavigationView

data class Contact(
    val name: String,
    val phone: String
)

class ShareContactActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQ = 3001
    private lateinit var recyclerView: RecyclerView
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // hardcoded contacts for now (name + phone)
    private val contacts = listOf(
        Contact("Mom", "9916482942"),
        Contact("Chinmayee", "7022195965"),

    )

    // keep track of the contact user clicked while waiting for permission
    private var pendingContact: Contact? = null

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_contacts) // your first xml filename; update if different

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()
        recyclerView = findViewById(R.id.recyclerViewCallers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ContactsAdapter(contacts) { contact ->
            // On click -> attempt to fetch location and send SMS via SMS app
            pendingContact = contact
            checkPermissionsAndShare(contact)
        }
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

    private fun checkPermissionsAndShare(contact: Contact) {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            // request both; when granted, we'll resume in onRequestPermissionsResult
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQ
            )
            Toast.makeText(this, "Please grant location permission to share location", Toast.LENGTH_SHORT).show()
            return
        }

        // permissions ok -> get location and send
        getCurrentLocationAndSendSms(contact)
    }


    private fun getCurrentLocationAndSendSms(contact: Contact) {
        // Defensive permission check right before calling location API
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            // permissions are not present — request and return
            pendingContact = contact
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQ
            )
            Toast.makeText(this, "Please grant location permission to share location", Toast.LENGTH_SHORT).show()
            return
        }

        // Use getCurrentLocation for a fresh fix. Provide a cancellation token.
        val cts = CancellationTokenSource()

        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val mapsLink = "https://maps.google.com/?q=$lat,$lon"

                    // optional: human readable address (best-effort)
                    val address = getAddressSafe(lat, lon)

                    val message = buildString {
                        append("Hi, I'm sharing my current location as a precaution.\n")
                        append("Location: $mapsLink\n")
                        append("Lat: $lat, Lon: $lon\n")
                        if (!address.isNullOrEmpty()) {
                            append("Approx address: $address")
                        }
                    }

                    openSmsAppWithMessage(contact.phone, message)
                } else {
                    Toast.makeText(this, "Unable to get current location. Try again", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { ex ->
                Toast.makeText(this, "Failed to get location: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        } catch (se: SecurityException) {
            // This should not happen because we checked permission above, but handle defensively
            Toast.makeText(this, "Location permission missing (security). Please grant permission.", Toast.LENGTH_LONG).show()
            pendingContact = contact
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQ
            )
        }
    }


    private fun openSmsAppWithMessage(phone: String, message: String) {
        try {
            val uri = Uri.parse("smsto:$phone")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra("sms_body", message)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No SMS app found", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAddressSafe(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val list = geocoder.getFromLocation(lat, lon, 1)
            if (!list.isNullOrEmpty()) {
                val addr = list[0]
                val parts = arrayListOf<String>()
                addr.thoroughfare?.let { parts.add(it) }
                addr.subLocality?.let { parts.add(it) }
                addr.locality?.let { parts.add(it) }
                addr.adminArea?.let { parts.add(it) }
                addr.countryName?.let { parts.add(it) }
                parts.joinToString(", ")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQ) {
            val granted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (granted && pendingContact != null) {
                // resume sharing for pending contact (which the user tapped)
                getCurrentLocationAndSendSms(pendingContact!!)
            } else {
                Toast.makeText(this, "Location permission denied. Cannot share location.", Toast.LENGTH_LONG).show()
            }
            pendingContact = null
        }
    }
}
