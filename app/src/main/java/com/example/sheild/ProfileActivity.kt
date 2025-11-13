package com.example.sheild

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.ImageButton

class ProfileActivity : AppCompatActivity() {

    // --- START: Added for Image Logic ---
    private lateinit var profileImageView: ImageView
    private var imageUri: Uri? = null

    // Launcher for selecting an image from the gallery
    private val imagePickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // Display the selected image
                profileImageView.setImageURI(it)
                imageUri = it

                // Save the URI and grant persistence permission
                persistUriAndSave(it)
            }
        }

    companion object {
        private const val PROFILE_IMAGE_URI_KEY = "profile_image_uri"
        private const val SHARED_PREFS_NAME = "addProfile"
    }
    // --- END: Added for Image Logic ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile)

        // --- START: Added for Image Logic ---
        profileImageView = findViewById(R.id.ivProfileImage)
        val editImageButton = findViewById<ImageButton>(R.id.btnEditProfile)

        // Set click listener on the image itself to trigger image selection
        profileImageView.setOnClickListener {
            imagePickerLauncher.launch("image/*") // Launch the image picker
        }

        // Set click listener on the edit button to also trigger image selection
        editImageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*") // Launch the image picker
        }
        // --- END: Added for Image Logic ---

        load()

        val name = findViewById<EditText>(R.id.etName)
        // Note: etEmail is used for 'password' in this context, based on the provided code
        val password = findViewById<EditText>(R.id.etEmail)
        val logout = findViewById<Button>(R.id.btnLogOut)

        val save = findViewById<Button>(R.id.btnSaveProfile)

        val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        var edit = sharedPref.edit()

        save.setOnClickListener {
            edit.putString("name", name.text.toString())
            edit.putString("password", password.text.toString())
            // Save the image URI if it exists
            imageUri?.let { uri ->
                edit.putString(PROFILE_IMAGE_URI_KEY, uri.toString())
            }
            edit.apply()
            Toast.makeText(this, "Data Saved",Toast.LENGTH_LONG).show()
        }

        logout.setOnClickListener {
            performLogout()
        }

        setupBottomNavigation()
    }

    // --- START: Added Helper Functions for Image Logic ---

    private fun persistUriAndSave(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            // Persist the read permission for the URI so it can be accessed later, even after reboot
            contentResolver.takePersistableUriPermission(uri, flags)

            // Also save the URI to SharedPreferences immediately for the current session
            val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putString(PROFILE_IMAGE_URI_KEY, uri.toString())
                apply()
            }
            Toast.makeText(this, "Profile picture saved.", Toast.LENGTH_SHORT).show()

        } catch (e: SecurityException) {
            Toast.makeText(this, "Could not grant persistent permission for the image.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    // --- END: Added Helper Functions for Image Logic ---

    private fun load(){
        val n = findViewById<EditText>(R.id.etName)
        val p = findViewById<EditText>(R.id.etEmail)
        val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        val name = sharedPref.getString("name", "")
        val password = sharedPref.getString("password", "")

        // --- START: Added Logic to Load Image URI ---
        val savedUriString = sharedPref.getString(PROFILE_IMAGE_URI_KEY, null)
        if (savedUriString != null) {
            try {
                imageUri = Uri.parse(savedUriString)
                profileImageView.setImageURI(imageUri)
            } catch (e: Exception) {
                // Handle invalid or inaccessible URI (e.g., file deleted)
                e.printStackTrace()
                Toast.makeText(this, "Could not load profile image.", Toast.LENGTH_SHORT).show()
            }
        }
        // --- END: Added Logic to Load Image URI ---

        n.text = Editable.Factory.getInstance().newEditable(name)
        p.text = Editable.Factory.getInstance().newEditable(password)

        Toast.makeText(this,name+" "+password,Toast.LENGTH_LONG).show()
    }

    private fun performLogout() {
        // Clear stored profile info (and any other session data you want)
        // **Recommendation: Add SharedPreferences clearing logic here**

        // Navigate to login screen if you have one. Replace LoginActivity::class.java if different.
        val loginIntent = try {
            Intent(this, LoginActivity::class.java)
        } catch (e: Throwable) {
            // fallback to MainActivity if LoginActivity doesn't exist
            Intent(this, MainActivity::class.java)
        }
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
        finish()
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
                    val intent = Intent(this, CommunityActivity::class.java)
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
}