package com.nibraas.uber

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var driverBtn: Button
    private lateinit var customerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        driverBtn = findViewById(R.id.driver)
        customerBtn = findViewById(R.id.customer)

        driverBtn.setOnClickListener {
            startActivity(Intent(this, DriverLoginActivity::class.java))
        }

        customerBtn.setOnClickListener {
            startActivity(Intent(this, CustomerLoginActivity::class.java))
        }
    }
}
