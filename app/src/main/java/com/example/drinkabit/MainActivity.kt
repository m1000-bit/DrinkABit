package com.example.drinkabit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.LottieAnimationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var cntBevute = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var animBicchiere: LottieAnimationView
    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // fullscren, nascon
        if (Build.VERSION.SDK_INT >= 30) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val ctrl = WindowInsetsControllerCompat(window, window.decorView)
            ctrl.hide(WindowInsetsCompat.Type.statusBars())
            ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        animBicchiere = findViewById(R.id.bicchiere)
        tvInfo = findViewById(R.id.testo)

        // carico il conteggio salvato??
        loadBevute()

        findViewById<TextView>(R.id.bottoneBevi).setOnClickListener {
            if (cntBevute < 8) {
                cntBevute++
                // 8 bicchieri = 2L
                animBicchiere.progress = cntBevute.toFloat() / 8f
                saveData()
                updateLabel()
            }
        }

        // reset a mezzanotte
        scheduleDailyReset()
        setupAlarm()
    }

    override fun onResume() {
        super.onResume()
        loadBevute()
    }

    private fun loadBevute() {
        val prefs = getSharedPreferences("drinkabit", MODE_PRIVATE)
        val savedDate = prefs.getString("data", "") ?: ""
        val today = SimpleDateFormat("yyyyMMdd", Locale.ITALIAN).format(Date())

        cntBevute = if (savedDate == today) prefs.getInt("bevute", 0) else 0
        // safety
        if (cntBevute > 8) cntBevute = 8

        animBicchiere.progress = cntBevute.toFloat() / 8f
        updateLabel()
    }

    private fun updateLabel() {
        // calcolo i litri
        val ml = cntBevute * 250
        val litri = ml / 1000
        val resto = ml % 1000

        val txt = if (resto == 0) {
            "$litri litri su 2"
        } else if (resto % 100 == 0) {

            "$litri,${resto / 100} litri su 2"
        } else {

            "$litri,$resto litri su 2"
        }
        tvInfo.text = "Hai bevuto $txt"
    }

    private fun saveData() {
        getSharedPreferences("drinkabit", MODE_PRIVATE).edit()
            .putInt("bevute", cntBevute)
            .putString("data", SimpleDateFormat("yyyyMMdd", Locale.ITALIAN).format(Date()))
            .apply()
    }

    // resetto
    private fun doReset() {
        cntBevute = 0
        animBicchiere.progress = 0f
        saveData()
        updateLabel()
    }

    // metodo "soft" reset
    private fun scheduleDailyReset() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 1) // un secondo dopo mezzanotte, così l'alarme parte prima
        cal.set(Calendar.MILLISECOND, 0)

        val delay = cal.timeInMillis - System.currentTimeMillis()
        handler.postDelayed({
            doReset()
            scheduleDailyReset() // ricomincia per il giorno dopo
        }, delay)
    }

    // metodo "hard" reset
    private fun setupAlarm() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val am = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, ResettaRicevitore::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // su alcuni device serve il permesso S_A
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (se: SecurityException) {

            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
// speriamo che funzioni :)