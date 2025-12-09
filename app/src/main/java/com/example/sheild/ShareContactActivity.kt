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



import android.content.Context

import android.widget.Button
import android.widget.EditText

import org.json.JSONArray
import org.json.JSONObject


data class Contact(
    val name: String,
    val phone: String
)

class ShareContactActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQ = 3001

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // mutable list loaded from SharedPreferences
    private val contacts = mutableListOf<Contact>()

    // keep track of the contact user clicked while waiting for permission
    private var pendingContact: Contact? = null

    private lateinit var bottomNavigation: BottomNavigationView

    // SharedPreferences keys
    private val PREF_NAME = "sheild_contacts_pref"
    private val KEY_CONTACTS = "contacts_json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_contacts) // your layout

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()

        // UI for adding contacts
        val nameEt = findViewById<EditText>(R.id.edName)
        val phoneEt = findViewById<EditText>(R.id.edPhone)
        val addBtn = findViewById<Button>(R.id.btnAddContact)
        val clearBtn = findViewById<Button>(R.id.btnClearContacts) // optional clear all

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerViewCallers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(contacts) { contact ->
            pendingContact = contact
            checkPermissionsAndShare(contact)
        }
        recyclerView.adapter = adapter

        // load saved contacts
        loadContactsFromPrefs()

        // Add contact button logic
        addBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter both name and phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // simple phone validation (digits only) - adjust as needed
            val phoneDigits = phone.filter { it.isDigit() }
            if (phoneDigits.length < 7) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newContact = Contact(name, phone)
            contacts.add(0, newContact) // add to top
            adapter.notifyItemInserted(0)
            recyclerView.scrollToPosition(0)
            saveContactsToPrefs()

            // clear inputs
            nameEt.text.clear()
            phoneEt.text.clear()

            Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()
        }

        // Clear all contacts (optional)
        clearBtn.setOnClickListener {
            if (contacts.isNotEmpty()) {
                contacts.clear()
                adapter.notifyDataSetChanged()
                saveContactsToPrefs()
                Toast.makeText(this, "All contacts cleared", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No contacts to clear", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_map -> {
                    val intent = Intent(this, GoogleMapActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_community -> {
                    val intent = Intent(this, ChannelsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_helpline -> {
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

    private fun checkPermissionsAndShare(contact: Contact) {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQ
            )
            Toast.makeText(this, "Please grant location permission to share location", Toast.LENGTH_SHORT).show()
            return
        }

        getCurrentLocationAndSendSms(contact)
    }

    private fun getCurrentLocationAndSendSms(contact: Contact) {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            pendingContact = contact
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQ
            )
            Toast.makeText(this, "Please grant location permission to share location", Toast.LENGTH_SHORT).show()
            return
        }

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

    // permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQ) {
            val granted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (granted && pendingContact != null) {
                getCurrentLocationAndSendSms(pendingContact!!)
            } else {
                Toast.makeText(this, "Location permission denied. Cannot share location.", Toast.LENGTH_LONG).show()
            }
            pendingContact = null
        }
    }

    // ---------- SharedPreferences storage as JSON ----------
    private fun saveContactsToPrefs() {
        val sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val edit = sharedPref.edit()
        val jsonArray = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            jsonArray.put(obj)
        }
        edit.putString(KEY_CONTACTS, jsonArray.toString())
        edit.apply()
    }

    private fun loadContactsFromPrefs() {
        val sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPref.getString(KEY_CONTACTS, null) ?: return
        try {
            val arr = JSONArray(json)
            contacts.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "")
                val phone = obj.optString("phone", "")
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    contacts.add(Contact(name, phone))
                }
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            // if parse fails, just clear prefs
            val edit = sharedPref.edit()
            edit.remove(KEY_CONTACTS)
            edit.apply()
        }
    }
}

