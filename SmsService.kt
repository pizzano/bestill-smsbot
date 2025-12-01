package com.bestill.smsbot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class SmsService : Service() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "smsbot_channel",
                "SMS Bot",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, "smsbot_channel")
            .setContentTitle("Bestill SMS Bot")
            .setContentText("SMS-meldinger sendes automatisk…")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .build()

        startForeground(1, notification)

        startFirebaseListener()
    }

    private fun startFirebaseListener() {
        val ref = FirebaseDatabase.getInstance().getReference("smsQueue")

        ref.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val sent = snapshot.child("sent").getValue(Boolean::class.java) ?: false
                if (sent) return

                val to = snapshot.child("to").value.toString()
                val text = snapshot.child("text").value.toString()

                try {
                    val sms = SmsManager.getDefault()
                    sms.sendTextMessage(to, null, text, null, null)

                    snapshot.ref.child("sent").setValue(true)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, prev: String?) {}
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
