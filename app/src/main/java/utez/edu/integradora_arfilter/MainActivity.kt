package utez.edu.integradora_arfilter

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import utez.edu.integradora_arfilter.models.StepRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private lateinit var stepsTextView: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepsTextView = findViewById(R.id.stepsTextView)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepSensor == null) {
            Toast.makeText(this, "No step sensor found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this, StepHistoryActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (stepSensor == null) {
            Toast.makeText(this, "Step sensor not available on this device", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            loadPreviousSteps()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        saveStepsToFirebase()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                totalSteps++
                updateStepsUI()
            }
        }
    }

    private fun updateStepsUI() {
        val displaySteps = totalSteps.toInt()
        stepsTextView.text = displaySteps.toString()
        android.util.Log.d("StepCounter", "Total Steps: $displaySteps")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun loadPreviousSteps() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        database.child("users").child(userId).child("steps").child(currentDate)
            .get().addOnSuccessListener { snapshot ->
                val savedSteps = snapshot.child("steps").getValue(Int::class.java) ?: 0

                totalSteps = savedSteps.toFloat()
                updateStepsUI()
            }.addOnFailureListener {
                totalSteps = 0f
                updateStepsUI()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveStepsToFirebase()
    }

    private fun saveStepsToFirebase() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        val dailySteps = totalSteps.toInt()

        val stepRecord = StepRecord(date = currentDate, steps = dailySteps)

        database.child("users").child(userId).child("steps").child(currentDate).setValue(stepRecord)
            .addOnCompleteListener {
                android.util.Log.d("StepCounter", "Steps saved to Firebase Database")
            }
    }
}