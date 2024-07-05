package com.coolscripts.wasse

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var apogeeEditText: EditText
    private lateinit var filiereEditText: EditText
    private lateinit var updateButton: Button
    private lateinit var profileImage: ImageView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var storageRef: StorageReference
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.title ="Profile"
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()
        storageRef = FirebaseStorage.getInstance().reference

        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        apogeeEditText = findViewById(R.id.apogeeEditText)
        filiereEditText = findViewById(R.id.filiereEditText)
        updateButton = findViewById(R.id.updateButton)
        profileImage = findViewById(R.id.profileImage)

        loadUserProfile()

        profileImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Choisir une photo de profil"), PICK_IMAGE_REQUEST)
        }

        updateButton.setOnClickListener {
            val uid = mAuth.currentUser?.uid
            if (uid != null) {
                updateProfile(uid)
            }
        }
    }

    private fun loadUserProfile() {
        val uid = mAuth.currentUser?.uid
        if (uid != null) {
            mDbRef.child("user").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        firstNameEditText.setText(user.prenom)
                        lastNameEditText.setText(user.nom)
                        apogeeEditText.setText(user.numApogee)
                        filiereEditText.setText(user.filiere)

                        // Charger l'ancienne image de profil si elle existe dans le stockage Firebase
                        val profileImageRef = storageRef.child("images/$uid/profile.jpg")
                        profileImageRef.downloadUrl.addOnSuccessListener { uri ->

                            Picasso.get().load(uri).into(profileImage)
                        }.addOnFailureListener {

                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Gerer l'annulation
                }
            })
        }
    }

    private fun updateProfile(uid: String) {
        val firstName = firstNameEditText.text.toString()
        val lastName = lastNameEditText.text.toString()
        val filiere = filiereEditText.text.toString()

        val userMap = mapOf(
            "prenom" to firstName,
            "nom" to lastName,
            "filiere" to filiere
        )

        mDbRef.child("user").child(uid).updateChildren(userMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (imageUri != null) {
                    val fileReference = storageRef.child("images/$uid/profile.jpg")
                    fileReference.putFile(imageUri!!)
                        .addOnSuccessListener { taskSnapshot ->
                            Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Erreur lors de l'enregistrement de l'image sur Firebase Storage: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Échec de la mise à jour du profil. Veuillez réessayer.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            profileImage.setImageURI(imageUri)
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}
