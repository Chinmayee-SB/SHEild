//package com.example.yourapp
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.sheild.R
//
//class CommunityAdapter(
//    private val members: List<CommunityMember>,
//    private val onItemClick: (CommunityMember) -> Unit
//) : RecyclerView.Adapter<CommunityAdapter.MemberViewHolder>() {
//
//    inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val nameTextView: TextView = view.findViewById(R.id.textViewName)
//        val addressTextView: TextView = view.findViewById(R.id.textViewAddress)
//
//        fun bind(member: CommunityMember) {
//            nameTextView.text = member.name
//            addressTextView.text = member.address
//
//            itemView.setOnClickListener {
//                onItemClick(member)
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_community_member, parent, false)
//        return MemberViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
//        holder.bind(members[position])
//    }
//
//    override fun getItemCount(): Int = members.size
//}