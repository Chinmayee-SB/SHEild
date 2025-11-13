package com.example.sheild

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class CommunityActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        // Bottom nav
        bottomNavigation = findViewById(R.id.bottomNav)
        setupBottomNavigation()

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerViewCommunity)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MembersAdapter { member ->
            // click handler: open details, or show toast / start activity
            // Example: start a detail activity (create if needed)
            // val intent = Intent(this, MemberDetailActivity::class.java)
            // intent.putExtra("member_id", member.id)
            // startActivity(intent)
        }
        recyclerView.adapter = adapter

        // populate with sample data (replace with real data source later)
        val sample = generateSampleMembers()
        adapter.submitList(sample)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_community

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
                R.id.nav_community -> true
                else -> false
            }
        }
    }

    private fun generateSampleMembers(): List<Member> {
        return listOf(
            Member("1", "Jennifer Lydia", "Wild West Street, New York"),
            Member("2", "Maddie", "Bell Button, Madison"),
            Member("3", "Lawrence", "Caramel Street, Pensachueia"),
            Member("4", "Medona S", "Baker's Street, London"),
            Member("5", "Christina", "Schneider's Lane, New York"),
            Member("6", "Emilia Chan", "Rivalry Street, New York")
        )
    }
}
