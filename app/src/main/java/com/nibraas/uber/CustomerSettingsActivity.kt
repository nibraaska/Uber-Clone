package com.nibraas.uber

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
    private lateinit var stringProfileImage: String

    private lateinit var profileImage: ImageView
    private var resultUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_settings)

        name = findViewById(R.id.name)
        phone = findViewById(R.id.phoneNumber)
        backBtn = findViewById(R.id.back)
        confirmBtn = findViewById(R.id.confirm)
        profileImage = findViewById(R.id.profileImage)

        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child("Customers")
            .child(userID)

        getUserInfo()

        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

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
                    if (map["profileImage"]!=null){
                        stringProfileImage = map["profileImage"].toString()
                        Glide.with(application).load(stringProfileImage).into(profileImage)
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


        when {
            resultUri != null -> {
                val filePath = FirebaseStorage.getInstance().getReference("profile_images").child(userID)
                val bitmap = MediaStore.Images.Media.getBitmap(application.contentResolver, resultUri)
                val boas = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas)

                val data = boas.toByteArray()
                val uploadTask = filePath.putBytes(data)


                uploadTask.addOnSuccessListener {
                    filePath.downloadUrl.addOnSuccessListener {
                        val newImage: HashMap<String, Any> = HashMap()
                        newImage["profileImage"] = it.toString()
                        databaseReference.updateChildren(newImage)
                        finish()
                    }
                }

                uploadTask.addOnFailureListener {
                    finish()
                }
            }
            else -> finish()
        }

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            resultUri = uri!!
            profileImage.setImageURI(resultUri)
        }
    }
}
