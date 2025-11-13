package com.example.sheild

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying the list of Police Stations in a RecyclerView.
 * It calculates and displays the distance for each station.
 */
class StationsAdapter(
    // List of stations to display, including calculated distance
    private val stations: List<PoliceStation>,
    private val onItemClick: (PoliceStation) -> Unit
) : RecyclerView.Adapter<StationsAdapter.StationViewHolder>() {

    /**
     * ViewHolder holds the view references (TextViews) for a single list item.
     * IDs are mapped to the elements defined in item_police_station.xml.
     */
    class StationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // CORRECTED: Mapped to R.id.stationName from item_police_station.xml
        val nameTextView: TextView = view.findViewById(R.id.textName)
        // CORRECTED: Mapped to R.id.stationDistance from item_police_station.xml
        val distanceTextView: TextView = view.findViewById(R.id.textDistance)
    }

    /**
     * Creates and inflates the view holder, using the custom item layout.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            // CORRECTED: Reference item_police_station.xml as the item layout
            .inflate(R.layout.activity_stations_adapter, parent, false)
        return StationViewHolder(view)
    }

    /**
     * Binds the data from the PoliceStation object to the views in the ViewHolder.
     */
    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]

        holder.nameTextView.text = station.name
        // Formats the distance to two decimal places, e.g., "1.52 km away"
        holder.distanceTextView.text = "${String.format("%.2f", station.distance)} km away"

        holder.itemView.setOnClickListener {
            onItemClick(station)
        }
    }

    /**
     * Returns the total number of items in the list.
     */
    override fun getItemCount() = stations.size
}