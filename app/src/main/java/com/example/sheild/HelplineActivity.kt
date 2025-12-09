package com.example.sheild

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sheild.databinding.ActivityHelplineBinding // Replace with your actual binding class
import com.google.android.material.bottomnavigation.BottomNavigationView

// 1. Data Class for Helpline Info
data class Helpline(val name: String, val number: String)

// 2. Interface for Click Listener
interface OnHelplineClickListener {
    fun onCallClick(phoneNumber: String)
}

// 4. HelplineAdapter.kt
// You will need a layout resource 'item_helpline.xml' for the individual list item

class HelplineAdapter(
    private val helplines: List<Helpline>,
    private val clickListener: OnHelplineClickListener
) : RecyclerView.Adapter<HelplineAdapter.HelplineViewHolder>() {

    class HelplineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvHelplineName) // Replace with your actual ID
        val tvNumber: TextView = itemView.findViewById(R.id.tvHelplineNumber) // Replace with your actual ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelplineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_helpline, parent, false)
        return HelplineViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelplineViewHolder, position: Int) {
        val helpline = helplines[position]
        holder.tvName.text = helpline.name
        holder.tvNumber.text = helpline.number

        // Set the click listener on the entire item view
        holder.itemView.setOnClickListener {
            // Trigger the call action via the interface method
            clickListener.onCallClick(helpline.number)
        }
    }

    override fun getItemCount(): Int = helplines.size
}
class HelplineActivity : AppCompatActivity(), OnHelplineClickListener {

    private lateinit var binding: ActivityHelplineBinding
    private val CALL_PERMISSION_REQUEST_CODE = 101

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelplineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()
        // Sample data for the helpline list
        val helplines = listOf(
            Helpline("Emergency Services", "911"), // Use 112, 999, etc. based on location
            Helpline("Women's Safety Line", "181"),
            Helpline("Child Protection", "1098"),
            Helpline("Cyber Crime Help", "1930")
        )

        // Setup RecyclerView
        binding.recyclerViewHelplines.apply {
            layoutManager = LinearLayoutManager(this@HelplineActivity)
            adapter = HelplineAdapter(helplines, this@HelplineActivity) // Pass 'this' as the click listener
        }

        // Setup Bottom Navigation (Optional: to handle navigation clicks)
        // binding.bottomNavigationView.setOnItemSelectedListener { item ->
        //     when (item.itemId) {
        //         R.id.nav_home -> { /* Navigate to Home */ true }
        //         // ... handle other nav items
        //         else -> false
        //     }
        // }
    }

    // Implementation of the OnHelplineClickListener interface
    override fun onCallClick(phoneNumber: String) {
        checkAndRequestCallPermission(phoneNumber)
    }

    private fun checkAndRequestCallPermission(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST_CODE
            )
            // Store the number temporarily if permission is needed
            // This is a simplified approach; for production, manage state carefully
            // In this example, we'll re-try calling in onRequestPermissionsResult
        } else {
            // Permission has already been granted, initiate the call
            initiateCall(phoneNumber)
        }
    }

    private fun setupBottomNavigation() {
        // Correctly set the selected item for the current screen
        bottomNavigation.selectedItemId = R.id.nav_helpline

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_map -> { // <-- NEW HELPLINE LOGIC
                    val intent = Intent(this, GoogleMapActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                } // This is the current activity, so no action/just return true
                R.id.nav_community -> {
                    // Assuming you want to navigate to the Community feature
                    // Replace 'CommunityActivity' with the correct activity class if needed
                    val intent = Intent(this, ChannelsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_helpline -> true
                else -> false
            }
        }
    }

    private fun initiateCall(phoneNumber: String) {
        // Use Intent.ACTION_CALL to make the call directly
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else { Toast.makeText(this, "No phone application found to handle the call.", Toast.LENGTH_SHORT).show()
        }
    }
    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. You would need to re-initiate the call logic here.
                // A common practice is to store the number being called before requesting permission.
                // For simplicity, we'll ask the user to tap again.
                Toast.makeText(this, "Permission granted. Please tap the helpline again to call.", Toast.LENGTH_LONG).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Call permission is required to make a call.", Toast.LENGTH_LONG).show()
            }
        }
    }
}