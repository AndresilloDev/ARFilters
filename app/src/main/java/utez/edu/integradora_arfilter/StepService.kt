// NO IMPLEMENTADO EN EL PROYECTO
// Esta clase es para poder contar los pasos del usuario incluso si la app no está en primer plano, pero no se implementó en el proyecto final aun

package utez.edu.integradora_arfilter

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import utez.edu.integradora_arfilter.models.StepRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate() {
        super.onCreate()

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepSensor == null) {
            Toast.makeText(this, "No step sensor found", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            totalSteps++
            saveStepsToFirebase()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
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
