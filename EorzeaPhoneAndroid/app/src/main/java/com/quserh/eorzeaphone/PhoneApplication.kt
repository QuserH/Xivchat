package com.quserh.eorzeaphone

import android.app.Application
import com.quserh.eorzeaphone.ui.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 进程被系统回收后（如用户清理后台）由 START_STICKY 重启时，无界面也要立刻恢复连接。
        phoneState
    }
    val phoneState: PhoneState by lazy {
        PhoneState(this, CoroutineScope(SupervisorJob() + Dispatchers.Main))
    }
}