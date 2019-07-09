package com.nibraas.uber

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CustomerSettingsActivity : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var phone: EditText
    private lateinit var backBtn: Button
    private lateinit var confirmBtn: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private lateinit var userID: String
    private lateinit var stringName: String
    private lateinit var stringPhone: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_settings)

        name = findViewById(R.id.name)
        phone = findViewById(R.id.phoneNumber)
        backBtn = findViewById(R.id.back)
        confirmBtn = findViewById(R.id.confirm)

        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child("Customers")
            .child(userID)

        getUserInfo()

        confirmBtn.setOnClickListener {
            saveUserInformation()
        }

        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun getUserInfo(){
        databaseReference.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists() && p0.childrenCount > 0){
                    val map = p0.value as Map<*, *>
                    if (map["name"]!=null){
                        stringName = map["name"].toString()
                        name.setText(stringName)
                    }
                    if (map["phone"]!=null){
                        stringPhone = map["phone"].toString()
                        phone.setText(stringPhone)
                    }
                }
            }

        })
    }

    private fun saveUserInformation() {

        stringName = name.text.toString()
        stringPhone = phone.text.toString()

        val userInfoMap: HashMap<String, Any> = HashMap()
        userInfoMap["name"] = stringName
        userInfoMap["phone"] = stringPhone
        databaseReference.updateChildren(userInfoMap)

        finish()
    }
}
