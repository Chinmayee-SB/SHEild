// ContactsAdapter.kt
package com.example.sheild

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private val items: List<Contact>,
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvCallerName: TextView = view.findViewById(R.id.tvCallerName)
        val card: CardView = view.findViewById(R.id.cardViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_card, parent, false) // ensure filename matches your layout
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val contact = items[position]
        holder.tvCallerName.text = contact.name
        holder.card.setOnClickListener {
            onItemClick(contact)
        }
    }

    override fun getItemCount(): Int = items.size
}
