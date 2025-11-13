package com.example.sheild

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

// 1. Data Class for an item in the list
data class CallerItem(
    val name: String,
     // Flag to distinguish the special item
)
class choosecaller : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var callerAdapter: CallerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_choose_caller)

        // After setContentView(R.layout.activity_choose_caller)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, GoogleMapActivity::class.java))
                    true
                }
                R.id.nav_community -> {
                    startActivity(Intent(this, CommunityActivity::class.java))
                    true
                }
                else -> false
            }
        }


        recyclerView = findViewById(R.id.recyclerViewCallers) // You'll need to add this ID to your XML ScrollView content
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2. Flexible list of CallerItems
        val callerItems = listOf(
            CallerItem("Mom"),
            CallerItem("Dad"),
            CallerItem("Friend"),
            CallerItem("Sister"),
            // This is the non-caller option, distinguished by the flag

        )

        callerAdapter = CallerAdapter(callerItems) { item ->
            // ⭐️ KEY CHANGE: Intent and Data Passing ⭐️

                // Logic for a fake call contact: Start IncomingCallActivity and pass the name
                Toast.makeText(this, "Starting fake call for ${item.name}...", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, IncomingCallActivity::class.java)
                // Pass the chosen caller name
                intent.putExtra("callerName", item.name)
                startActivity(intent)

        }
        recyclerView.adapter = callerAdapter

        // The Bottom Navigation Bar implementation would typically be in a separate common fragment/activity
        // or handled by a BottomNavigationView component, but for this context, we'll focus on the content.
    }

    // Helper function for demonstration purposes
    private fun showToast(message: String) {
        // In a real app, you'd use Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        println(message) // For console output in a non-GUI environment
    }
}

// 3. RecyclerView Adapter to display the list flexibly
class CallerAdapter(
    private val items: List<CallerItem>,
    private val onItemClicked: (CallerItem) -> Unit
) : RecyclerView.Adapter<CallerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardViewItem) // Add this ID to the item layout
        val nameTextView: TextView = view.findViewById(R.id.tvCallerName) // Add this ID to the item layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Replace this with your actual single card layout XML file (e.g., caller_item.xml)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.caller_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameTextView.text = item.name

        // Set up the click listener
        holder.cardView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount() = items.size
}

/*
NOTE: To make this code run, you would need to:
1. Save this file as ChooseCallerActivity.kt
2. Create an XML layout file for the Activity (e.g., activity_choose_caller.xml)
   which is a modified version of your provided XML, specifically replacing
   the repeated CardViews with a single **RecyclerView**.
3. Create a separate XML layout file for a single list item (e.g., caller_item.xml)
   that contains the CardView structure for the caller name and chevron.
4. Add the required IDs (like `recyclerViewCallers`, `cardViewItem`, `tvCallerName`) to your XML files.
*/



