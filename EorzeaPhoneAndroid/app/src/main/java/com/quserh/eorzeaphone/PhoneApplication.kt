package com.quserh.eorzeaphone

import android.app.Application
import com.quserh.eorzeaphone.ui.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PhoneApplication : Application() {
    val phoneState: PhoneState by lazy {
        PhoneState(this, CoroutineScope(SupervisorJob() + Dispatchers.Main))
    }
}