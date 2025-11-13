package com.example.sheild // <- change this to your package

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

// Data models


import java.io.Serializable

data class Message(val user: String, val text: String, val color: String) : Serializable

data class Channel(
    val id: Int,
    val name: String,
    val online: Int,
    val total: Int,
    val subtitle: String? = null,
    val messages: List<Message> = emptyList()
) : Serializable

class ChannelsActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure this layout exists (from the previous message)
        setContentView(R.layout.activity_channels)

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()

        // RecyclerView setup
        val recycler = findViewById<RecyclerView>(R.id.recycler_channels)
        recycler.layoutManager = LinearLayoutManager(this)

        // Sample data matching your React example
        val channels = listOf(
            Channel(
                id = 1,
                name = "Women at Work",
                online = 56,
                total = 3429,
                messages = listOf(
                    Message("Jenny", "Yeah, I have been thinking about it for a long time...", "text-orange-600"),
                    Message("Lina", "Hey girls, Wassup!", "text-purple-600")
                )
            ),
            Channel(
                id = 2,
                name = "School Girls",
                online = 38,
                total = 1856,
                messages = listOf(
                    Message("Joanne", "Yeah, I have been thinking about it for a long time...", "text-red-500"),
                    Message("Myle", "Hey girls, Wassup!", "text-blue-600")
                )
            ),
            Channel(
                id = 3,
                name = "Homemakers",
                online = 75,
                total = 2951,
                subtitle = "women online",
                messages = listOf(
                    Message("Sofie", "Yeah, I have been thinking about it for a long time...", "text-pink-600"),
                    Message("Eliza", "Hey girls, Wassup!", "text-teal-600")
                )
            )
        )

        // Attach adapter
        // new: start ChatActivity and pass the channel
        // inside ChannelsActivity, when attaching adapter
        recycler.adapter = ChannelsAdapter(channels) { channel ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("channel_id", channel.id)      // only id
                putExtra("channel_name", channel.name)  // optional: display name in header
            }
            startActivity(intent)
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

    // Adapter implementation
    class ChannelsAdapter(
        private val items: List<Channel>,
        private val onClick: (Channel) -> Unit
    ) : RecyclerView.Adapter<ChannelsAdapter.ChannelVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_channels_adapter, parent, false)
            return ChannelVH(view)
        }

        override fun onBindViewHolder(holder: ChannelVH, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size

        inner class ChannelVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView? = itemView.findViewById(R.id.card_container)
            private val nameTv: TextView = itemView.findViewById(R.id.tv_channel_name)
            private val onlineTv: TextView = itemView.findViewById(R.id.tv_online_count)
            private val totalTv: TextView = itemView.findViewById(R.id.tv_total_info)
            private val msg1: TextView = itemView.findViewById(R.id.tv_msg_1)
            private val msg2: TextView = itemView.findViewById(R.id.tv_msg_2)

            fun bind(channel: Channel, onClick: (Channel) -> Unit) {
                nameTv.text = channel.name
                onlineTv.text = channel.online.toString()
                // If subtitle provided, show like " /total subtitle", else " /total online"
                totalTv.text = "/${channel.total} ${channel.subtitle ?: "online"}"

                // Messages binding (show up to 2 messages)
                if (channel.messages.isNotEmpty()) {
                    val m0 = channel.messages[0]
                    msg1.visibility = View.VISIBLE
                    msg1.text = "${m0.user}: ${m0.text}"
                    msg1.setTextColor(mapColorStringToColor(m0.color))
                } else {
                    msg1.visibility = View.GONE
                }

                if (channel.messages.size > 1) {
                    val m1 = channel.messages[1]
                    msg2.visibility = View.VISIBLE
                    msg2.text = "${m1.user}: ${m1.text}"
                    msg2.setTextColor(mapColorStringToColor(m1.color))
                } else {
                    msg2.visibility = View.GONE
                }

                // Click handling (card or entire item)
                val clickableView = card ?: itemView
                clickableView.setOnClickListener { onClick(channel) }
            }

            // small helper to convert your "text-*" strings to color ints
            private fun mapColorStringToColor(colorKey: String): Int {
                return when (colorKey) {
                    "text-orange-600" -> Color.parseColor("#D97706") // orange-600
                    "text-purple-600" -> Color.parseColor("#7C3AED") // purple-600
                    "text-red-500" -> Color.parseColor("#EF4444") // red-500
                    "text-blue-600" -> Color.parseColor("#2563EB") // blue-600
                    "text-pink-600" -> Color.parseColor("#DB2777") // pink-600
                    "text-teal-600" -> Color.parseColor("#0F766E") // teal-600
                    else -> Color.parseColor("#374151") // default gray-700
                }
            }
        }
    }
}
