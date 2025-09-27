package com.example.traveler

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.traveler.database.BookingDatabaseHelper
import com.example.traveler.database.DatabaseHelper
import com.example.traveler.databinding.ActivityEditBookingBinding
import com.example.traveler.model.TravelBooking
import com.example.traveler.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class EditBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBookingBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var bookingDatabaseHelper: BookingDatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentBooking: TravelBooking? = null
    private var bookingId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        databaseHelper = DatabaseHelper(this)
        bookingDatabaseHelper = BookingDatabaseHelper(this)
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Get booking ID from intent
        bookingId = intent.getIntExtra("BOOKING_ID", -1)
        if (bookingId == -1) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupDropdowns()
        setupDatePickers()
        setupClickListeners()
        loadBookingData()
    }

    private fun setupUI() {
        supportActionBar?.title = "Edit Booking"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupDropdowns() {
        // Accommodation types - Updated to include historical places and attractions
        val accommodationTypes = arrayOf(
            "Ancient Temples", "Historical Castles", "Archaeological Sites", "Museums", 
            "Heritage Hotels", "Colonial Buildings", "Palaces", "Monasteries", 
            "Ancient Ruins", "Cultural Centers", "Historical Monuments", "Art Galleries",
            "Traditional Guesthouses", "Heritage Resorts", "Historic Inns"
        )
        val accommodationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accommodationTypes)
        binding.etAccommodationType.setAdapter(accommodationAdapter)

        // Room/Experience types - Updated for historical and cultural experiences
        val roomTypes = arrayOf(
            "Guided Tour", "Self-Guided Visit", "Private Tour", "Group Tour", 
            "Audio Guide Tour", "Heritage Suite", "Historical Room", "Cultural Experience", 
            "Educational Program", "Workshop Experience", "VIP Access", "Standard Visit",
            "Premium Experience", "Family Package", "Student Tour"
        )
        val roomAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roomTypes)
        binding.etRoomType.setAdapter(roomAdapter)
    }

    private fun setupDatePickers() {
        binding.etCheckInDate.setOnClickListener {
            showDatePicker { selectedDate ->
                binding.etCheckInDate.setText(selectedDate)
                // Clear check-out date if it's before check-in
                val checkOutDate = binding.etCheckOutDate.text.toString()
                if (checkOutDate.isNotEmpty() && isDateBefore(selectedDate, checkOutDate)) {
                    binding.etCheckOutDate.setText("")
                }
            }
        }

        binding.etCheckOutDate.setOnClickListener {
            val checkInDate = binding.etCheckInDate.text.toString()
            if (checkInDate.isEmpty()) {
                Toast.makeText(this, "Please select check-in date first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            showDatePicker(minDate = getDateFromString(checkInDate)) { selectedDate ->
                binding.etCheckOutDate.setText(selectedDate)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnUpdateBooking.setOnClickListener {
            handleBookingUpdate()
        }

        binding.btnCancel.setOnClickListener {
            showCancelDialog()
        }
    }

    private fun loadBookingData() {
        currentBooking = bookingDatabaseHelper.getBookingById(bookingId)
        currentBooking?.let { booking ->
            // Populate fields with existing data
            binding.etDestination.setText(booking.destination)
            binding.etDepartureLocation.setText(booking.departureLocation)
            binding.etCheckInDate.setText(booking.checkInDate)
            binding.etCheckOutDate.setText(booking.checkOutDate)
            binding.etNumberOfGuests.setText(booking.numberOfGuests.toString())
            binding.etAccommodationType.setText(booking.accommodationType)
            binding.etRoomType.setText(booking.roomType)
            binding.etContactName.setText(booking.contactName)
            binding.etContactEmail.setText(booking.contactEmail)
            binding.etContactPhone.setText(booking.contactPhone)
            binding.etSpecialRequests.setText(booking.specialRequests)
        } ?: run {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleBookingUpdate() {
        val destination = binding.etDestination.text.toString().trim()
        val departureLocation = binding.etDepartureLocation.text.toString().trim()
        val checkInDate = binding.etCheckInDate.text.toString().trim()
        val checkOutDate = binding.etCheckOutDate.text.toString().trim()
        val numberOfGuests = binding.etNumberOfGuests.text.toString().trim()
        val accommodationType = binding.etAccommodationType.text.toString().trim()
        val roomType = binding.etRoomType.text.toString().trim()
        val contactName = binding.etContactName.text.toString().trim()
        val contactEmail = binding.etContactEmail.text.toString().trim()
        val contactPhone = binding.etContactPhone.text.toString().trim()
        val specialRequests = binding.etSpecialRequests.text.toString().trim()

        // Reset errors
        clearErrors()

        // Validate inputs
        if (!validateInputs(
                destination, departureLocation, checkInDate, checkOutDate,
                numberOfGuests, accommodationType, roomType, contactName,
                contactEmail, contactPhone
            )) {
            return
        }

        // Create updated booking object
        val updatedBooking = currentBooking!!.copy(
            destination = destination,
            departureLocation = departureLocation,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            numberOfGuests = numberOfGuests.toInt(),
            accommodationType = accommodationType,
            roomType = roomType,
            specialRequests = specialRequests,
            contactName = contactName,
            contactEmail = contactEmail,
            contactPhone = contactPhone
        )

        // Update booking in database
        if (bookingDatabaseHelper.updateBooking(updatedBooking)) {
            Toast.makeText(this, "Booking updated successfully!", Toast.LENGTH_SHORT).show()
            
            // Return to booking list with result
            val resultIntent = Intent().apply {
                putExtra("UPDATED_BOOKING_ID", bookingId)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Failed to update booking. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(
        destination: String, departureLocation: String, checkInDate: String,
        checkOutDate: String, numberOfGuests: String, accommodationType: String,
        roomType: String, contactName: String, contactEmail: String, contactPhone: String
    ): Boolean {
        var isValid = true

        if (destination.isEmpty()) {
            binding.tilDestination.error = "Destination is required"
            isValid = false
        }

        if (departureLocation.isEmpty()) {
            binding.tilDepartureLocation.error = "Departure location is required"
            isValid = false
        }

        if (checkInDate.isEmpty()) {
            binding.tilCheckInDate.error = "Check-in date is required"
            isValid = false
        }

        if (checkOutDate.isEmpty()) {
            binding.tilCheckOutDate.error = "Check-out date is required"
            isValid = false
        }

        if (numberOfGuests.isEmpty()) {
            binding.tilNumberOfGuests.error = "Number of guests is required"
            isValid = false
        } else {
            try {
                val guests = numberOfGuests.toInt()
                if (guests <= 0 || guests > 20) {
                    binding.tilNumberOfGuests.error = "Number of guests must be between 1 and 20"
                    isValid = false
                }
            } catch (e: NumberFormatException) {
                binding.tilNumberOfGuests.error = "Please enter a valid number"
                isValid = false
            }
        }

        if (accommodationType.isEmpty()) {
            binding.tilAccommodationType.error = "Accommodation type is required"
            isValid = false
        }

        if (roomType.isEmpty()) {
            binding.tilRoomType.error = "Room type is required"
            isValid = false
        }

        if (contactName.isEmpty()) {
            binding.tilContactName.error = "Contact name is required"
            isValid = false
        }

        if (contactEmail.isEmpty()) {
            binding.tilContactEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) {
            binding.tilContactEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (contactPhone.isEmpty()) {
            binding.tilContactPhone.error = "Phone number is required"
            isValid = false
        } else if (contactPhone.length < 10) {
            binding.tilContactPhone.error = "Please enter a valid phone number"
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        binding.tilDestination.error = null
        binding.tilDepartureLocation.error = null
        binding.tilCheckInDate.error = null
        binding.tilCheckOutDate.error = null
        binding.tilNumberOfGuests.error = null
        binding.tilAccommodationType.error = null
        binding.tilRoomType.error = null
        binding.tilContactName.error = null
        binding.tilContactEmail.error = null
        binding.tilContactPhone.error = null
    }

    private fun showDatePicker(minDate: Date? = null, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        minDate?.let { calendar.time = it }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)
                onDateSelected(dateFormat.format(selectedCalendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = minDate?.time ?: System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun isDateBefore(date1: String, date2: String): Boolean {
        return try {
            val d1 = dateFormat.parse(date1)
            val d2 = dateFormat.parse(date2)
            d1?.before(d2) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getDateFromString(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Edit")
            .setMessage("Are you sure you want to cancel editing? All changes will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        showCancelDialog()
        return true
    }

    override fun onBackPressed() {
        showCancelDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.booking_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? Your changes will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        // Clear shared preferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Navigate to login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}