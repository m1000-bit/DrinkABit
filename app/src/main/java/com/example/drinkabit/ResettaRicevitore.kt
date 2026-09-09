package com.example.drinkabit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// mezzanotte dovrebbe resettarsi...
class ResettaRicevitore : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val prefs = ctx.getSharedPreferences("drinkabit", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("bevute", 0)
            .putString("data", SimpleDateFormat("yyyyMMdd", Locale.ITALIAN).format(Date()))
            .apply()
    }
}
