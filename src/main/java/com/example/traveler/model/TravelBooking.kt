package com.example.traveler.model

import java.util.Date

data class TravelBooking(
    val id: Int = 0,
    val userId: Int,
    val destination: String,
    val departureLocation: String,
    val checkInDate: String,
    val checkOutDate: String,
    val numberOfGuests: Int,
    val accommodationType: String,
    val roomType: String,
    val specialRequests: String = "",
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val totalAmount: Double = 0.0,
    val bookingStatus: String = "Pending",
    val createdAt: String
)