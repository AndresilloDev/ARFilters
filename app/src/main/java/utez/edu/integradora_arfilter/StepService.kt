package utez.edu.integradora_arfilter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import utez.edu.integradora_arfilter.models.StepRecord
import java.text.SimpleDateFormat
import java.util.Locale

class StepService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0f

    private val CHANNEL_ID = "StepCounterServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Counter")
            .setContentText("Tracking your steps")
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            totalSteps++
            saveStepsToFirebase()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun saveStepsToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(java.util.Date())

        val dailySteps = totalSteps.toInt()
        val stepRecord = StepRecord(date = currentDate, steps = dailySteps)

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("steps")
            .child(currentDate)
            .setValue(stepRecord)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
