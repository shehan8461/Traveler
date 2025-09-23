package com.example.traveler

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.traveler.database.BookingDatabaseHelper
import com.example.traveler.database.DatabaseHelper
import com.example.traveler.databinding.ActivityBookingBinding
import com.example.traveler.model.TravelBooking
import com.example.traveler.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var bookingDatabaseHelper: BookingDatabaseHelper
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        databaseHelper = DatabaseHelper(this)
        bookingDatabaseHelper = BookingDatabaseHelper(this)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupDropdowns()
        setupDatePickers()
        setupClickListeners()
        prefillUserData()
        setupBackButton()
    }

    private fun setupUI() {
        supportActionBar?.title = "Book Your Travel"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupDropdowns() {
        // Accommodation types
        val accommodationTypes = arrayOf(
            "Hotel", "Resort", "Apartment", "Villa", "Hostel", "Guest House", "Bed & Breakfast"
        )
        val accommodationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accommodationTypes)
        binding.etAccommodationType.setAdapter(accommodationAdapter)

        // Room types
        val roomTypes = arrayOf(
            "Single Room", "Double Room", "Twin Room", "Triple Room", "Family Room", 
            "Suite", "Deluxe Room", "Standard Room", "Premium Room"
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
        binding.btnSubmitBooking.setOnClickListener {
            handleBookingSubmission()
        }

        binding.btnCancel.setOnClickListener {
            showCancelDialog()
        }
    }

    private fun setupBackButton() {
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            showCancelDialog()
        }
    }

    private fun prefillUserData() {
        val username = sessionManager.getUsername()
        val user = databaseHelper.getUser(username)
        
        user?.let {
            binding.etContactEmail.setText(it.email)
            // You might want to store and retrieve full name separately
            binding.etContactName.setText(username)
        }
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

    private fun handleBookingSubmission() {
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

        // Get current user
        val username = sessionManager.getUsername()
        val user = databaseHelper.getUser(username)
        if (user == null) {
            Toast.makeText(this, "User session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Create booking object
        val booking = TravelBooking(
            userId = user.id,
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
            contactPhone = contactPhone,
            createdAt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        )

        // Save booking
        if (bookingDatabaseHelper.createBooking(booking)) {
            showSuccessDialog()
        } else {
            Toast.makeText(this, "Failed to submit booking. Please try again.", Toast.LENGTH_SHORT).show()
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

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Booking Submitted!")
            .setMessage("Your travel booking has been submitted successfully. You will receive a confirmation email shortly.")
            .setPositiveButton("Continue") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking? All entered data will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
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
}