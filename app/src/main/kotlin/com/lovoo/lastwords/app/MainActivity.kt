package com.lovoo.lastwords.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.lovoo.lastwords.LastWords
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var level = Const.highestLevel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        level = intent.getIntExtra("level", level)

        if (level > Const.highestLevel) {
            // each app start should start by 1 if not then the process was still running
            Const.highestLevel = level
        }

        level_text.text = "#$level"

        start_activity_button.setOnClickListener {
            Intent(this@MainActivity, MainActivity::class.java).let {
                it.putExtra("level", level + 1)
                startActivity(it)
            }
        }

        finish_app_button.setOnClickListener {
            LastWords.finishApp(500)
        }
    }
}
