package utez.edu.integradora_arfilter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private lateinit var stepsTextView: TextView
    private lateinit var kilometerTextView: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    companion object {
        private const val ACTIVITY_RECOGNITION_PERMISSION_CODE = 100
        private const val STEP_LENGTH_METERS = 0.78 // Longitud promedio de un paso en metros
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepsTextView = findViewById(R.id.stepsTextView)
        kilometerTextView = findViewById(R.id.kilometerTextView)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        requestActivityRecognitionPermission()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepSensor == null) {
            Toast.makeText(this, "No se encontró un sensor de pasos", Toast.LENGTH_SHORT).show()
        }

        startStepService()
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_PERMISSION_CODE
                )
            }
        }
    }

    private fun startStepService() {
        val intent = Intent(this, StepService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de reconocimiento de actividad otorgado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
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
            Toast.makeText(this, "El sensor de pasos no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            loadPreviousSteps()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            totalSteps++
            updateStepsUI()
        }
    }

    private fun updateStepsUI() {
        val displaySteps = totalSteps.toInt()
        val kilometers = (totalSteps * STEP_LENGTH_METERS) / 1000

        stepsTextView.text = "$displaySteps Pasos"
        kilometerTextView.text = String.format("%.2f Km", kilometers)
    }

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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
