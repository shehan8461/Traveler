package com.example.traveler

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.traveler.adapter.BookingAdapter
import com.example.traveler.database.BookingDatabaseHelper
import com.example.traveler.database.DatabaseHelper
import com.example.traveler.databinding.ActivityBookingListBinding
import com.example.traveler.utils.SessionManager

class BookingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var bookingDatabaseHelper: BookingDatabaseHelper
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var sharedPreferences: SharedPreferences

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
        loadBookings()
        setupBackButton()
    }

    private fun setupUI() {
        supportActionBar?.title = "My Bookings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.recyclerViewBookings.layoutManager = LinearLayoutManager(this)
        
        binding.fabNewBooking.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }

        binding.btnCreateFirstBooking.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }
    }

    private fun loadBookings() {
        val username = sessionManager.getUsername()
        val user = databaseHelper.getUser(username)
        
        user?.let {
            val bookings = bookingDatabaseHelper.getUserBookings(it.id)
            
            if (bookings.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.recyclerViewBookings.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.recyclerViewBookings.visibility = android.view.View.VISIBLE
                
                bookingAdapter = BookingAdapter(bookings) { booking ->
                    // Handle booking item click (could open details or edit)
                }
                binding.recyclerViewBookings.adapter = bookingAdapter
            }
        }
    }

    private fun setupBackButton() {
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
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