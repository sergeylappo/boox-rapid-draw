package com.sergeylappo.booxrapiddraw

import android.app.Application
import android.os.Build
import com.onyx.android.sdk.rx.RxManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class BooxRapidDraw : Application() {

    override fun onCreate() {
        super.onCreate()
        RxManager.Builder.initAppContext(this)
        checkHiddenApiBypass()
    }

    private fun checkHiddenApiBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }
}
