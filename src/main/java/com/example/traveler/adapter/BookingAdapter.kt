package com.example.traveler.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.traveler.R
import com.example.traveler.model.TravelBooking

class BookingAdapter(
    private val bookings: List<TravelBooking>,
    private val onItemClick: (TravelBooking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
        holder.itemView.setOnClickListener { onItemClick(booking) }
    }

    override fun getItemCount(): Int = bookings.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        private val tvDates: TextView = itemView.findViewById(R.id.tvDates)
        private val tvGuests: TextView = itemView.findViewById(R.id.tvGuests)
        private val tvAccommodation: TextView = itemView.findViewById(R.id.tvAccommodation)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvBookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)

        fun bind(booking: TravelBooking) {
            tvDestination.text = "${booking.departureLocation} → ${booking.destination}"
            tvDates.text = "${booking.checkInDate} - ${booking.checkOutDate}"
            tvGuests.text = "${booking.numberOfGuests} guest${if (booking.numberOfGuests > 1) "s" else ""}"
            tvAccommodation.text = "${booking.accommodationType} - ${booking.roomType}"
            tvStatus.text = booking.bookingStatus
            tvBookingDate.text = "Booked on ${booking.createdAt}"

            // Set status color
            when (booking.bookingStatus.lowercase()) {
                "confirmed" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "pending" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                "cancelled" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                else -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            }
        }
    }
}