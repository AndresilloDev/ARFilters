package utez.edu.integradora_arfilter

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import utez.edu.integradora_arfilter.R
import utez.edu.integradora_arfilter.adapters.StepHistoryAdapter
import utez.edu.integradora_arfilter.models.StepHistory
import utez.edu.integradora_arfilter.models.StepRecord

class StepHistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_history)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewStepHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        fetchStepHistory()
    }

    private fun fetchStepHistory() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        database.child("users").child(userId).child("steps")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stepHistoryList = mutableListOf<StepHistory>()

                    for (childSnapshot in snapshot.children) {
                        val stepRecord = childSnapshot.getValue(StepRecord::class.java)
                        stepRecord?.let {
                            stepHistoryList.add(
                                StepHistory(
                                    date = it.date,
                                    steps = it.steps
                                )
                            )
                        }
                    }

                    // Sort by date in descending order
                    stepHistoryList.sortByDescending { it.date }

                    val adapter = StepHistoryAdapter(stepHistoryList)
                    recyclerView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@StepHistoryActivity,
                        "Error al cargar el historial",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}