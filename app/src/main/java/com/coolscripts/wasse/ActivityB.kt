package com.coolscripts.wasse

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ActivityB : AppCompatActivity() {
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private var currentUserUid: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_b)
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()

        userList = ArrayList()
        adapter = UserAdapter(this,userList)

        userRecyclerView = findViewById(R.id.userRecyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter

        mDbRef.child("user").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserUid = mAuth.currentUser?.uid
                var currentUserFiliere: String? = null

                for (postSnapshot in snapshot.children) {
                    val currentUser = postSnapshot.getValue(User::class.java)
                    if (currentUser?.uid == currentUserUid) {
                        currentUserFiliere = currentUser?.filiere
                        supportActionBar?.title = currentUser?.filiere
                        break
                    }
                }

                if (currentUserFiliere != null) {
                    userList.clear()

                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(User::class.java)
                        if (currentUser?.filiere == currentUserFiliere && currentUser.uid != currentUserUid) {
                            userList.add(currentUser)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profil -> {

                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("USER_UID", currentUserUid)
                startActivity(intent)
                return true
            }
            R.id.deconnexion -> {

                mAuth.signOut()
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Déconnexion")
                builder.setMessage("Êtes-vous sûr(e) de vouloir vous déconnecter ?")
                builder.setPositiveButton("OUI") { dialog, which ->

                    finish()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                }
                builder.setNegativeButton("NON") { dialog, which ->

                }.show()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



}