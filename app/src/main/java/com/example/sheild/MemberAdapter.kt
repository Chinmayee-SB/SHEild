package com.example.sheild

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

data class Member(
    val id: String,
    val name: String,
    val address: String
)
class MembersAdapter(private val onClick: (Member) -> Unit) :
    ListAdapter<Member, MembersAdapter.VH>(MemberDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View, val onClick: (Member) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.textName)
        private val addressTv: TextView = itemView.findViewById(R.id.textAddress)

        fun bind(item: Member) {
            nameTv.text = item.name
            addressTv.text = item.address
            itemView.setOnClickListener { onClick(item) }
        }
    }

    class MemberDiff : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean =
            oldItem == newItem
    }
}
