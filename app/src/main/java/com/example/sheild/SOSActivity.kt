package com.example.sheild

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import androidx.core.content.ContextCompat

import android.telephony.SmsManager

import android.widget.Toast

import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Context
import android.location.LocationManager
import android.app.AlertDialog // Use the standard Android AlertDialog

import android.animation.AnimatorSet
import android.animation.ObjectAnimator

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator


class SOSActivity : AppCompatActivity() {


    // Permission codes for different requests
    private val SMS_PERMISSION_CODE = 101
    private val LOCATION_PERMISSION_CODE = 102

    private lateinit var wave1: View
    private lateinit var wave2: View
    private lateinit var wave3: View
    private lateinit var wave4: View
    private lateinit var sosButton: View

    // Client for getting the user's location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Emergency Contact details
    private val SHARED_PREFS_NAME = "addProfile"
    private val SOS_PHONE_KEY = "sos_phone"

    private val SOS_BASE_MESSAGE = "Good Morning , SOS Works. My location is:"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        wave1 = findViewById(R.id.wave1)
        wave2 = findViewById(R.id.wave2)
        wave3 = findViewById(R.id.wave3)
        wave4 = findViewById(R.id.wave4)

            // Start the location-gathering and SMS process
            checkLocationServiceStatusAndProceed()
            startWaveAnimation()

    }

    private fun startWaveAnimation() {
        // Create wave animations with staggered timing
        val wave1Anim = createWaveAnimation(wave1, 0L)
        val wave2Anim = createWaveAnimation(wave2, 300L)
        val wave3Anim = createWaveAnimation(wave3, 600L)
        val wave4Anim = createWaveAnimation(wave4, 900L)

        // Start all animations together
        wave1Anim.start()
        wave2Anim.start()
        wave3Anim.start()
        wave4Anim.start()
    }

    private fun createWaveAnimation(view: View, delay: Long): AnimatorSet {
        // Scale animation
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1.5f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.5f)

        // Fade animation
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.8f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 2000
        animatorSet.startDelay = delay
        animatorSet.interpolator = AccelerateDecelerateInterpolator()

        // Repeat the animation
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.scaleX = 0.5f
                view.scaleY = 0.5f
                view.alpha = 0f
                animatorSet.start()
            }
        })

        return animatorSet
    }

    private fun getEmergencyPhone(): String? {
        val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        // if not set, return null so caller can handle fallback
        return sharedPref.getString(SOS_PHONE_KEY, null)
    }


    // Add this function inside your SOSActivity class
    private fun checkLocationServiceStatusAndProceed() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val phone = getEmergencyPhone()
        if (phone == null) {
            Toast.makeText(this, "No SOS contact set. Go to Profile → Edit SOS Contact.", Toast.LENGTH_LONG).show()
        }

        if (isGpsEnabled || isNetworkEnabled) {
            // Location services are ON, proceed with permission check and sending SMS
            checkPermissionsAndSendSOS()
        } else {
            // Location services are OFF, prompt the user to enable them
            showLocationSettingsDialog()
        }
    }

    // Add this function inside your SOSActivity class
    private fun showLocationSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Please enable GPS/Location services to include your current location in the SOS message.")
            .setPositiveButton("Go to Settings") { dialog, which ->
                // Launch the system location settings screen
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // User cancelled, but we can still try to send a message without location
                Toast.makeText(this, "Location services disabled. Sending SOS without location.", Toast.LENGTH_LONG).show()
                // fallback if not set
                sendSmsMessage(getEmergencyPhone(), "$SOS_BASE_MESSAGE Location unavailable.")
                dialog.dismiss()
            }
            .show()
    }
    // --- Main Logic Function ---
    private fun checkPermissionsAndSendSOS() {
        // 1. Check SMS permission
        val smsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        // 2. Check Location permission
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        val permissionsToRequest = mutableListOf<String>()

        if (smsPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Request missing permissions
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_CODE // Using one code for both, but will handle separately in result
            )
        } else {
            // All permissions are granted, proceed to get location
            getLastLocationAndSendSMS()
        }
    }

    // --- Location Retrieval ---
    private fun getLastLocationAndSendSMS() {
        // Double-check location permission just before accessing location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // Location successfully retrieved
                        val latitude = location.latitude
                        val longitude = location.longitude

                        // Create the Google Maps URL
                        val mapLink = "https://maps.google.com/?q=$latitude,$longitude"
                        val fullMessage = "$SOS_BASE_MESSAGE $mapLink"

                        // Send the SMS with the location link
                        // building fullMessage earlier:

                        sendSmsMessage(getEmergencyPhone(), fullMessage)


                    } else {
                        // Location is null, send a message without coordinates
                        Toast.makeText(this, "Could not get location. Sending SOS without location.", Toast.LENGTH_LONG).show()
                        sendSmsMessage(getEmergencyPhone(), "$SOS_BASE_MESSAGE Location unavailable.")
                    }
                }
                .addOnFailureListener { e ->
                    // Failed to get location (e.g., GPS is off)
                    Toast.makeText(this, "Location error: ${e.message}. Sending SOS without location.", Toast.LENGTH_LONG).show()
                    sendSmsMessage(getEmergencyPhone()  , "$SOS_BASE_MESSAGE Location unavailable.")
                }
        }
    }

    // --- SMS Sending (Modified from your original code) ---
    private fun sendSmsMessage(phoneNumber: String?, message: String) {
        // This function assumes SMS permission has already been checked by checkPermissionsAndSendSOS()
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            Toast.makeText(this, "SOS Sent to $phoneNumber!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "SMS Failed to Send: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


    // --- Permission Result Handler ---
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                // All permissions granted, try sending SMS with location again
                getLastLocationAndSendSMS()
            } else {
                Toast.makeText(this, "Required permissions denied. SOS will not be fully functional.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
