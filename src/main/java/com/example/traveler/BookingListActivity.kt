package com.example.traveler

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.traveler.adapter.BookingAdapter
import com.example.traveler.database.BookingDatabaseHelper
import com.example.traveler.database.DatabaseHelper
import com.example.traveler.databinding.ActivityBookingListBinding
import com.example.traveler.model.TravelBooking
import com.example.traveler.utils.SessionManager

class BookingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var bookingDatabaseHelper: BookingDatabaseHelper
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var bookingsList = mutableListOf<TravelBooking>()
    
    companion object {
        private const val EDIT_BOOKING_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingListBinding.inflate(layoutInflater)
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

        setupUI()
        setupRecyclerView()
        loadBookings()
        setupBackButton()
    }

    private fun setupUI() {
        supportActionBar?.title = "My Bookings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.fabNewBooking.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }

        binding.btnCreateFirstBooking.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            bookings = bookingsList,
            onItemClick = { booking ->
                // Handle booking item click (could show details or other action)
                // For now, we'll do nothing as edit/delete buttons handle the actions
            },
            onEditClick = { booking ->
                handleEditBooking(booking)
            },
            onDeleteClick = { booking ->
                handleDeleteBooking(booking)
            }
        )
        
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(this@BookingListActivity)
            adapter = bookingAdapter
            addItemDecoration(DividerItemDecoration(this@BookingListActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun loadBookings() {
        val username = sessionManager.getUsername()
        val user = databaseHelper.getUser(username)
        
        user?.let {
            bookingsList.clear()
            bookingsList.addAll(bookingDatabaseHelper.getUserBookings(it.id))
            
            if (bookingsList.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.btnCreateFirstBooking.visibility = android.view.View.VISIBLE
                binding.recyclerViewBookings.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.btnCreateFirstBooking.visibility = android.view.View.GONE
                binding.recyclerViewBookings.visibility = android.view.View.VISIBLE
                bookingAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleEditBooking(booking: TravelBooking) {
        val intent = Intent(this, EditBookingActivity::class.java)
        intent.putExtra("BOOKING_ID", booking.id)
        startActivityForResult(intent, EDIT_BOOKING_REQUEST)
    }

    private fun handleDeleteBooking(booking: TravelBooking) {
        AlertDialog.Builder(this)
            .setTitle("Delete Booking")
            .setMessage("Are you sure you want to delete this booking to ${booking.destination}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteBooking(booking)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBooking(booking: TravelBooking) {
        if (bookingDatabaseHelper.deleteBooking(booking.id)) {
            Toast.makeText(this, "Booking deleted successfully", Toast.LENGTH_SHORT).show()
            loadBookings() // Refresh the list
        } else {
            Toast.makeText(this, "Failed to delete booking", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackButton() {
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_BOOKING_REQUEST && resultCode == Activity.RESULT_OK) {
            // Refresh the booking list after edit
            loadBookings()
            val updatedBookingId = data?.getIntExtra("UPDATED_BOOKING_ID", -1)
            if (updatedBookingId != -1) {
                Toast.makeText(this, "Booking updated successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh bookings when returning from other activities
        loadBookings()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
            .setMessage("Are you sure you want to logout?")
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