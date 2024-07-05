package com.coolscripts.wasse

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SignUp : AppCompatActivity() {

    lateinit var edtPrenom: EditText
    lateinit var edtNom: EditText
    lateinit var edtNumApogee: EditText
    lateinit var edtEmail: EditText
    lateinit var edtPassword: EditText
    lateinit var edtPasswordConfirm: EditText
    lateinit var spinnerFiliere: Spinner
    lateinit var btnLogin: TextView
    lateinit var btnSignup: Button
    lateinit var mAuth: FirebaseAuth
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var imageViewProfile: ImageView
    private lateinit var storageRef: StorageReference
    private var imageUri: Uri? = null
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        supportActionBar?.hide()
        mAuth = FirebaseAuth.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        edtPrenom = findViewById(R.id.editTextPrenom)
        edtNom = findViewById(R.id.editTextNom)
        edtNumApogee = findViewById(R.id.editTextNumApogee)
        edtEmail = findViewById(R.id.editTextEmail)
        edtPassword = findViewById(R.id.editTextPassword)
        edtPasswordConfirm = findViewById(R.id.editTextPasswordConfirm)
        spinnerFiliere = findViewById(R.id.spinnerFiliere)
        btnSignup = findViewById(R.id.SignUp)
        btnLogin = findViewById(R.id.LoginButton)
        imageViewProfile = findViewById(R.id.ImagedeProfile)

        imageViewProfile.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Choisir une photo de profil"),
                PICK_IMAGE_REQUEST
            )
        }

        btnLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        btnSignup.setOnClickListener {
            val prenom = edtPrenom.text.toString()
            val nom = edtNom.text.toString()
            val numApogee = edtNumApogee.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()
            val passwordConfirm = edtPasswordConfirm.text.toString()
            val filiere = spinnerFiliere.selectedItem.toString()

            if (prenom.isEmpty() || nom.isEmpty() || numApogee.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
            } else if (password != passwordConfirm) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
            } else {
                checkNumApogeeAndSignUp(prenom, nom, numApogee, email, password, filiere)
            }
        }

        setupSpinner()
    }

    private fun setupSpinner() {
        val filieres = arrayOf("RSSP", "GI", "SEECS", "GCDSTE", "GIL")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filieres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiliere.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            imageViewProfile.setImageURI(imageUri)
        }
    }

    private fun checkNumApogeeAndSignUp(prenom: String, nom: String, numApogee: String, email: String, password: String, filiere: String) {
        val numApogeeRef = FirebaseDatabase.getInstance().getReference("n_apogee")
        numApogeeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(numApogee)) {
                    signUp(prenom, nom, numApogee, email, password, filiere)
                } else {
                    Toast.makeText(this@SignUp, "Numéro Apogée n'existe pas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignUp, "Erreur de vérification", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun signUp(prenom: String, nom: String, numApogee: String, email: String, password: String, filiere: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inscription réussie!", Toast.LENGTH_SHORT).show()
                    val user = mAuth.currentUser
                    val userId = user?.uid
                    if (userId != null && imageUri != null) {
                        saveImageToStorage(userId, imageUri!!)
                    }
                    if (userId != null) {
                        addUserToDatabase(prenom, nom, numApogee, email, userId, filiere)
                    }
                    addNumApogeeToDatabase(numApogee)
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Cet email est déjà utilisé. Veuillez utiliser un autre email.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Échec de l'inscription. Veuillez réessayer.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun saveImageToStorage(userId: String, imageUri: Uri) {
        val imagesRef = storageRef.child("images/$userId/profile.jpg")
        imagesRef.putFile(imageUri)
            .addOnSuccessListener {
                Toast.makeText(this, "Image enregistrée avec succès sur Firebase Storage", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erreur lors de l'enregistrement de l'image sur Firebase Storage: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addUserToDatabase(prenom: String, nom: String, numApogee: String, email: String, uid: String, filiere: String) {
        mDbRef = FirebaseDatabase.getInstance().getReference()
        val user = User(prenom, nom, numApogee, email, uid, filiere, System.currentTimeMillis())
        mDbRef.child("user").child(uid).setValue(user)
    }

    private fun addNumApogeeToDatabase(numApogee: String) {
        val numApogeeRef = FirebaseDatabase.getInstance().getReference("n_apogee")
        numApogeeRef.child(numApogee).setValue(true)
    }
}
