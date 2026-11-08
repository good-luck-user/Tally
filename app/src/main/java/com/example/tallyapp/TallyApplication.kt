package com.example.tallyapp

import android.app.Application
import com.example.tallyapp.logging.TallyLog

class TallyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TallyLog.init(this)
    }
}
