package com.urim.displaycheck

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        val textView = findViewById<TextView>(R.id.display)
        ("Client Display : densityDpi ${dm.densityDpi},\n\n$dm").also { textView.text = it }
    }


}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density) as Int
}

fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density) as Int
}