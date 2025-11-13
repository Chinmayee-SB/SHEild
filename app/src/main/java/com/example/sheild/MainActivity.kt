package com.example.sheild

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.mainmain)

        // Check location and prompt if disabled
        ensureLocationEnabledIfNeeded()

        setupClickListeners()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Ensure Home is selected when returning to MainActivity
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigation.selectedItemId = R.id.nav_home

        // Re-check location in case user returned from settings
        ensureLocationEnabledIfNeeded()
    }

    private fun setupClickListeners() {
        // Find the "Fake Call" layout by its ID
        val fakeCallLayout = findViewById<LinearLayout>(R.id.fakeCallLayout)
        val profileLayout = findViewById<ImageView>(R.id.profileButton)
        val sos = findViewById<Button>(R.id.sosButton)

        sos.setOnClickListener {
            val intent = Intent(this, SOSActivity::class.java)
            startActivity(intent)
        }

        fakeCallLayout.setOnClickListener {
            val intent = Intent(this, choosecaller::class.java)
            startActivity(intent)
        }

        profileLayout.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Set Home as selected by default
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on Home, do nothing
                    true
                }
                R.id.nav_map -> {
                    // Navigate to Map Activity
                    val intent = Intent(this, GoogleMapActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.nav_community -> {
                    // Navigate to Community Activity
                    val intent = Intent(this, ChannelsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.nav_helpline -> {
                    // Navigate to Community Activity
                    val intent = Intent(this, HelplineActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Check whether location (GPS or Network provider) is enabled.
     * If not enabled, show an AlertDialog that opens the Location settings when user taps "Turn on".
     */
    private fun ensureLocationEnabledIfNeeded() {
        if (!isLocationEnabled()) {
            // Show dialog prompting user to enable location
            showEnableLocationDialog()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Check both GPS and Network providers (covers most devices)
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            false
        }
        val networkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
            false
        }
        return gpsEnabled || networkEnabled
    }

    private fun showEnableLocationDialog() {
        // Build a simple dialog — user can go to settings or cancel
        AlertDialog.Builder(this)
            .setTitle("Location is turned off")
            .setMessage("To share location and use safety features, please enable Location services.")
            .setCancelable(true)
            .setPositiveButton("Turn on") { dialog, _ ->
                // Open location settings
                try {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    // fallback: open general settings if location settings cannot be opened
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
