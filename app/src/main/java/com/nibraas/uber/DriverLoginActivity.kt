package com.nibraas.uber

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.log

class DriverLoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)

        auth = FirebaseAuth.getInstance()
        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            if (it.currentUser != null)
                startActivity(Intent(this, DriverMapActivity::class.java))
        }

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)

        loginBtn = findViewById(R.id.login)
        registerBtn = findViewById(R.id.register)

        registerBtn.setOnClickListener {
            val emailString = email.text.toString()
            val passwordString = password.text.toString()

            auth.createUserWithEmailAndPassword(emailString, passwordString)
                .addOnCompleteListener {
                    if (!it.isSuccessful){
                        Toast.makeText(this, it.result.toString(), Toast.LENGTH_SHORT).show()
                    } else {
                        val userId = auth.currentUser!!.uid
                        FirebaseDatabase.getInstance().reference
                            .child("Users")
                            .child("Drivers")
                            .child(userId)
                            .setValue(true)
                    }
                }
        }

        loginBtn.setOnClickListener {
            val emailString = email.text.toString()
            val passwordString = password.text.toString()

            auth.signInWithEmailAndPassword(emailString, passwordString)
                .addOnCompleteListener {
                    if (!it.isSuccessful){
                        Toast.makeText(this, "Error signin", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(firebaseAuthListener)
    }
}
