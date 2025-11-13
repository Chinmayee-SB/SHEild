package com.example.sheild


import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

// Renamed from Message to Chat
data class Chat(val sender: String, val text: String)

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button


    private lateinit var adapter: ChatAdapter
    private lateinit var bottomNavigation: BottomNavigationView
    private val chatList = mutableListOf<Chat>() // renamed list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()

        rvMessages = findViewById(R.id.rv_messages)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById<Button>(R.id.btn_send)



        // Sample chats based on your UI
        chatList.addAll(
            listOf(
                Chat("Nina", "Hello. Have you just arrived at the camp? I'm Nina. My name is Nelly. Nice to meet you."),
                Chat("Nelly", "Oh, OK... Where are you from?"),
                Chat("Nina", "I'm from Greece, but I've lived in this area for a long time. You?"),
                Chat("Nelly", "I'm from the USA, I'm here on holidays."),
                Chat("Nina", "Are you from a big family?"),
                Chat("Nelly", "No, there are just five of us- me, my sister, my brother and my parents. What about you?"),
                Chat("Nina", "I've got two sisters."),
                Chat("Nelly", "Oh, that's nice. What do you usually do in your free time? Do you have any hobbies?"),
                Chat("Nina", "I love hanging out with my friends or stay at home and read a good book. I don't have a lot of hobbies. I enjoy playing volleyball... What about you?"),
                Chat("Nelly", "I love it, too...What's your favourite subject?"),
                Chat("Nina", "I like Biology. I love learning about the Environment."),
                Chat("Nelly", "Me too. I think we are going to be great friends!"),
                Chat("Nina", "So do I!")
            )
        )

        adapter = ChatAdapter(chatList)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        rvMessages.scrollToPosition(chatList.size - 1)

        btnSend.setOnClickListener { sendChat() }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendChat()
                true
            } else false
        }




    }

    private fun setupBottomNavigation() {
        // Correctly set the selected item for the current screen
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
                R.id.nav_helpline -> {
                    // Assuming you want to navigate to the Community feature
                    // Replace 'CommunityActivity' with the correct activity class if needed
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

    private fun sendChat() {
        val text = etMessage.text.toString().trim()
        if (!TextUtils.isEmpty(text)) {
            val newChat = Chat("You", text)
            chatList.add(newChat)
            adapter.notifyItemInserted(chatList.size - 1)
            rvMessages.scrollToPosition(chatList.size - 1)
            etMessage.setText("")
        }
    }

    class ChatAdapter(private val items: List<Chat>) :
        RecyclerView.Adapter<ChatAdapter.ChatVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return ChatVH(view)
        }

        override fun onBindViewHolder(holder: ChatVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class ChatVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val senderTv: TextView = itemView.findViewById(R.id.tv_sender)
            private val textTv: TextView = itemView.findViewById(R.id.tv_text)

            fun bind(chat: Chat) {
                senderTv.text = chat.sender
                textTv.text = chat.text
            }
        }
    }
}
