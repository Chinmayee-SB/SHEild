package com.example.sheild

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sheild.ChatActivity.ChatAdapter.Companion.formatTime
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import okhttp3.MediaType.Companion.toMediaTypeOrNull

// Richer Chat model
data class Chat(
    val id: String?,
    val userId: String?,     // from server.user.id
    val sender: String,
    val text: String,
    val timestampMs: Long? = null
)

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private val client = OkHttpClient()
    private val chatList = ArrayList<Chat>()
    private lateinit var adapter: ChatAdapter

    private var chatroomId: String = ""
    private var userId: String = ""

    // track latest timestamp in ms (to be used in subsequent refresh calls)
    private var latestSinceMs: Long = 0L

    // Polling handler
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 4000L
    private val pollRunnable = object : Runnable {
        override fun run() {
            refreshMessages(initial = false)
            pollHandler.postDelayed(this, pollIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        bottomNavigation = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()

        rvMessages = findViewById(R.id.rv_messages)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)

        adapter = ChatAdapter(chatList, getSharedPreferences(Prefs.NAME, MODE_PRIVATE).getString(Prefs.KEY_USER_ID, "") ?: "")
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        // get chatroom id from intent (channelActivity passes it as channel.id)
        val cid = intent?.getIntExtra("channel_id", -1)
        if (cid == null || cid == -1) {
            Toast.makeText(this, "No channel id passed", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        chatroomId = cid.toString()

        // load user id from shared prefs
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        userId = prefs.getString(Prefs.KEY_USER_ID, "") ?: ""
        if (userId.isEmpty()) {
            // user not logged in — send them back to Login
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // initial load: since=0 (server will return all messages)
        refreshMessages(initial = true)

        btnSend.setOnClickListener { sendChat() }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendChat()
                true
            } else false
        }
    }

    override fun onStart() {
        super.onStart()
        // start polling
        pollHandler.postDelayed(pollRunnable, pollIntervalMs)
    }

    override fun onStop() {
        super.onStop()
        // stop polling
        pollHandler.removeCallbacks(pollRunnable)
    }

    /**
     * Refresh messages from server.
     * initial=true -> since=0 to load history
     * initial=false -> use latestSinceMs to fetch only new messages
     */
    private fun refreshMessages(initial: Boolean = false) {
        val httpUrlBuilder = (Prefs.SERVER_BASE_URL + "/refresh").toHttpUrlOrNull()!!.newBuilder()
        httpUrlBuilder.addQueryParameter("chatroomId", chatroomId)
        if (initial) {
            httpUrlBuilder.addQueryParameter("since", "0")
        } else {
            if (latestSinceMs > 0L) {
                httpUrlBuilder.addQueryParameter("since", latestSinceMs.toString())
            } else {
                // if we don't have a since timestamp, request only messages after 0 to be safe
                httpUrlBuilder.addQueryParameter("since", "0")
            }
        }
        val url = httpUrlBuilder.build().toString()

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Failed to refresh: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (!it.isSuccessful || body == null) {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Refresh failed", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    try {
                        val obj = JSONObject(body)
                        val ok = obj.optBoolean("ok", false)
                        if (!ok) {
                            val msg = obj.optString("message", "Refresh error")
                            runOnUiThread { Toast.makeText(this@ChatActivity, msg, Toast.LENGTH_SHORT).show() }
                            return
                        }

                        val messagesJson = obj.optJSONArray("messages") ?: JSONArray()
                        val newChats = ArrayList<Chat>()
                        var highestMs = latestSinceMs

                        for (i in 0 until messagesJson.length()) {
                            val m = messagesJson.getJSONObject(i)
                            val messageText = m.optString("message", "")
                            val userObj = m.optJSONObject("user")
                            val senderName = userObj?.optString("full_name") ?: "Unknown"
                            val senderId = userObj?.optString("id") ?: userObj?.optString("_id") // try both
                            val id = m.optString("_id", null)

                            // parse createdAt to ms
                            var createdAtMs: Long? = null
                            if (m.has("createdAt") && !m.isNull("createdAt")) {
                                val createdAt = m.optString("createdAt")
                                createdAtMs = parseDateToMs(createdAt)
                            }

                            if (createdAtMs != null && createdAtMs > highestMs) highestMs = createdAtMs

                            val chat = Chat(id, senderId, senderName, messageText, createdAtMs)
                            newChats.add(chat)
                        }

                        runOnUiThread {
                            if (initial) {
                                chatList.clear()
                                chatList.addAll(newChats)
                            } else {
                                // append only new messages (server returns messages > since)
                                chatList.addAll(newChats)
                            }
                            adapter.notifyDataSetChanged()
                            if (chatList.isNotEmpty()) rvMessages.scrollToPosition(chatList.size - 1)
                        }

                        if (highestMs > latestSinceMs) latestSinceMs = highestMs
                        else if (initial && latestSinceMs == 0L && chatList.isNotEmpty()) latestSinceMs = System.currentTimeMillis()

                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Parse error: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun sendChat() {
        val text = etMessage.text.toString().trim()
        if (TextUtils.isEmpty(text)) return

        val json = JSONObject()
        json.put("userId", userId)
        json.put("chatroomId", chatroomId)
        json.put("message", text)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(Prefs.SERVER_BASE_URL + "/sendMessage")
            .post(body)
            .build()

        // Optimistic UI: add a temporary "You" message
        val optimistic = Chat(null, userId, "You", text, System.currentTimeMillis())
        chatList.add(optimistic)
        adapter.notifyItemInserted(chatList.size - 1)
        rvMessages.scrollToPosition(chatList.size - 1)
        etMessage.setText("")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Send failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyStr = it.body?.string()
                    if (!it.isSuccessful || bodyStr == null) {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Send failed", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    try {
                        val obj = JSONObject(bodyStr)
                        val ok = obj.optBoolean("ok", false)
                        if (!ok) {
                            val msg = obj.optString("message", "Send error")
                            runOnUiThread { Toast.makeText(this@ChatActivity, msg, Toast.LENGTH_SHORT).show() }
                            return
                        }

                        val saved = obj.optJSONObject("data")
                        if (saved != null) {
                            val messageText = saved.optString("message", text)
                            val userObj = saved.optJSONObject("user")
                            val senderName = userObj?.optString("full_name") ?: "You"
                            val senderId = userObj?.optString("id") ?: userObj?.optString("_id") ?: userId
                            val id = saved.optString("_id", null)
                            val createdAtMs = saved.optString("createdAt")?.let { parseDateToMs(it) }

                            val newChat = Chat(id, senderId, senderName, messageText, createdAtMs)

                            runOnUiThread {
                                // replace last optimistic message if matches
                                if (chatList.isNotEmpty() && chatList.last().timestampMs != null &&
                                    chatList.last().text == text && chatList.last().userId == userId) {
                                    chatList[chatList.size - 1] = newChat
                                    adapter.notifyItemChanged(chatList.size - 1)
                                } else {
                                    chatList.add(newChat)
                                    adapter.notifyItemInserted(chatList.size - 1)
                                }
                                if (createdAtMs != null && createdAtMs > latestSinceMs) latestSinceMs = createdAtMs
                                rvMessages.scrollToPosition(chatList.size - 1)
                            }
                        }
                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Send parse error: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun parseDateToMs(createdAt: String?): Long? {
        if (createdAt == null) return null
        // try number
        try {
            val n = createdAt.toLong()
            if (n > 1000000000000L) return n
        } catch (_: Exception) { /* ignore */ }

        // try common ISO formats
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val d = sdf.parse(createdAt)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_community

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, GoogleMapActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_community -> {
                    startActivity(Intent(this, ChannelsActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_helpline -> {
                    startActivity(Intent(this, HelplineActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // Adapter uses two view types (sent vs received)
    class ChatAdapter(private val items: List<Chat>, private val currentUserId: String) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            private const val TYPE_SENT = 1
            private const val TYPE_RECEIVED = 2

            fun formatTime(ms: Long): String {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                return sdf.format(Date(ms))
            }
        }

        override fun getItemViewType(position: Int): Int {
            val chat = items[position]
            return if (!chat.userId.isNullOrEmpty() && chat.userId == currentUserId)
                TYPE_SENT
            else
                TYPE_RECEIVED
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_SENT) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentVH(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedVH(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val chat = items[position]
            if (holder is SentVH) holder.bind(chat)
            if (holder is ReceivedVH) holder.bind(chat)
        }

        override fun getItemCount(): Int = items.size

        class SentVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val txtMessage: TextView = itemView.findViewById(R.id.tv_text)
            private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

            fun bind(chat: Chat) {
                txtMessage.text = chat.text
                tvTime.text = chat.timestampMs?.let { formatTime(it) } ?: ""
            }
        }

        class ReceivedVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val txtSender: TextView = itemView.findViewById(R.id.tv_sender)
            private val txtMessage: TextView = itemView.findViewById(R.id.tv_text)
            private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

            fun bind(chat: Chat) {
                txtSender.text = chat.sender
                txtMessage.text = chat.text
                tvTime.text = chat.timestampMs?.let { formatTime(it) } ?: ""
            }
        }
    }

}
