package com.systemallica.valenbisi


import com.squareup.leakcanary.LeakCanary

class Application : androidx.multidex.MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }

}
