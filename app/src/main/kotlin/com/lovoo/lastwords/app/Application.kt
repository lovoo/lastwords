package com.lovoo.lastwords.app

import android.app.Application
import android.widget.Toast
import com.lovoo.lastwords.LastWords

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        LastWords.init(this)

        LastWords.register(object : LastWords.Listener {
            override fun onAppFinished() {
                Toast.makeText(this@Application, "App finished", Toast.LENGTH_SHORT).show()
            }
        })
    }
}