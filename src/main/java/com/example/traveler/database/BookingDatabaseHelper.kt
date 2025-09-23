package com.example.traveler.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.traveler.model.TravelBooking

class BookingDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "traveler_bookings.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_BOOKINGS = "bookings"
        
        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_DESTINATION = "destination"
        private const val COLUMN_DEPARTURE_LOCATION = "departure_location"
        private const val COLUMN_CHECK_IN_DATE = "check_in_date"
        private const val COLUMN_CHECK_OUT_DATE = "check_out_date"
        private const val COLUMN_NUMBER_OF_GUESTS = "number_of_guests"
        private const val COLUMN_ACCOMMODATION_TYPE = "accommodation_type"
        private const val COLUMN_ROOM_TYPE = "room_type"
        private const val COLUMN_SPECIAL_REQUESTS = "special_requests"
        private const val COLUMN_CONTACT_NAME = "contact_name"
        private const val COLUMN_CONTACT_EMAIL = "contact_email"
        private const val COLUMN_CONTACT_PHONE = "contact_phone"
        private const val COLUMN_TOTAL_AMOUNT = "total_amount"
        private const val COLUMN_BOOKING_STATUS = "booking_status"
        private const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createBookingsTable = """
            CREATE TABLE $TABLE_BOOKINGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_DESTINATION TEXT NOT NULL,
                $COLUMN_DEPARTURE_LOCATION TEXT NOT NULL,
                $COLUMN_CHECK_IN_DATE TEXT NOT NULL,
                $COLUMN_CHECK_OUT_DATE TEXT NOT NULL,
                $COLUMN_NUMBER_OF_GUESTS INTEGER NOT NULL,
                $COLUMN_ACCOMMODATION_TYPE TEXT NOT NULL,
                $COLUMN_ROOM_TYPE TEXT NOT NULL,
                $COLUMN_SPECIAL_REQUESTS TEXT,
                $COLUMN_CONTACT_NAME TEXT NOT NULL,
                $COLUMN_CONTACT_EMAIL TEXT NOT NULL,
                $COLUMN_CONTACT_PHONE TEXT NOT NULL,
                $COLUMN_TOTAL_AMOUNT REAL DEFAULT 0.0,
                $COLUMN_BOOKING_STATUS TEXT DEFAULT 'Pending',
                $COLUMN_CREATED_AT TEXT NOT NULL
            )
        """.trimIndent()
        
        db?.execSQL(createBookingsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        onCreate(db)
    }

    // Create a new booking
    fun createBooking(booking: TravelBooking): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USER_ID, booking.userId)
            put(COLUMN_DESTINATION, booking.destination)
            put(COLUMN_DEPARTURE_LOCATION, booking.departureLocation)
            put(COLUMN_CHECK_IN_DATE, booking.checkInDate)
            put(COLUMN_CHECK_OUT_DATE, booking.checkOutDate)
            put(COLUMN_NUMBER_OF_GUESTS, booking.numberOfGuests)
            put(COLUMN_ACCOMMODATION_TYPE, booking.accommodationType)
            put(COLUMN_ROOM_TYPE, booking.roomType)
            put(COLUMN_SPECIAL_REQUESTS, booking.specialRequests)
            put(COLUMN_CONTACT_NAME, booking.contactName)
            put(COLUMN_CONTACT_EMAIL, booking.contactEmail)
            put(COLUMN_CONTACT_PHONE, booking.contactPhone)
            put(COLUMN_TOTAL_AMOUNT, booking.totalAmount)
            put(COLUMN_BOOKING_STATUS, booking.bookingStatus)
            put(COLUMN_CREATED_AT, booking.createdAt)
        }

        return try {
            val result = db.insert(TABLE_BOOKINGS, null, contentValues)
            db.close()
            result != -1L
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    // Get all bookings for a user
    fun getUserBookings(userId: Int): List<TravelBooking> {
        val bookings = mutableListOf<TravelBooking>()
        val db = this.readableDatabase
        
        val cursor: Cursor = db.query(
            TABLE_BOOKINGS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_CREATED_AT DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val booking = TravelBooking(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    destination = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESTINATION)),
                    departureLocation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEPARTURE_LOCATION)),
                    checkInDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHECK_IN_DATE)),
                    checkOutDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHECK_OUT_DATE)),
                    numberOfGuests = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMBER_OF_GUESTS)),
                    accommodationType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOMMODATION_TYPE)),
                    roomType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM_TYPE)),
                    specialRequests = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIAL_REQUESTS)) ?: "",
                    contactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME)),
                    contactEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_EMAIL)),
                    contactPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_PHONE)),
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT)),
                    bookingStatus = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOKING_STATUS)),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                )
                bookings.add(booking)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return bookings
    }

    // Get booking by ID
    fun getBookingById(bookingId: Int): TravelBooking? {
        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_BOOKINGS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(bookingId.toString()),
            null,
            null,
            null
        )

        var booking: TravelBooking? = null
        if (cursor.moveToFirst()) {
            booking = TravelBooking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                destination = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESTINATION)),
                departureLocation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEPARTURE_LOCATION)),
                checkInDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHECK_IN_DATE)),
                checkOutDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHECK_OUT_DATE)),
                numberOfGuests = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMBER_OF_GUESTS)),
                accommodationType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOMMODATION_TYPE)),
                roomType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM_TYPE)),
                specialRequests = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIAL_REQUESTS)) ?: "",
                contactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME)),
                contactEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_EMAIL)),
                contactPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_PHONE)),
                totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT)),
                bookingStatus = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOKING_STATUS)),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            )
        }

        cursor.close()
        db.close()
        return booking
    }

    // Update booking status
    fun updateBookingStatus(bookingId: Int, status: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_BOOKING_STATUS, status)
        }

        return try {
            val rowsAffected = db.update(
                TABLE_BOOKINGS,
                contentValues,
                "$COLUMN_ID = ?",
                arrayOf(bookingId.toString())
            )
            db.close()
            rowsAffected > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    // Delete a booking
    fun deleteBooking(bookingId: Int): Boolean {
        val db = this.writableDatabase
        return try {
            val rowsDeleted = db.delete(
                TABLE_BOOKINGS,
                "$COLUMN_ID = ?",
                arrayOf(bookingId.toString())
            )
            db.close()
            rowsDeleted > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    // Get booking count for a user
    fun getUserBookingCount(userId: Int): Int {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_BOOKINGS WHERE $COLUMN_USER_ID = ?",
            arrayOf(userId.toString())
        )

        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }

        cursor.close()
        db.close()
        return count
    }
}