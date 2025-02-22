package com.tera.custom_timepicker_dialog

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tera.custom_timepicker_dialog.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object{
        const val HOUR = "hour"
        const val MIN = "min"
        const val SEC = "sec"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sp: SharedPreferences
    private var myHour = 0
    private var myMin = 0
    private var mySec = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sp = getSharedPreferences("settings", Context.MODE_PRIVATE)
        myHour = sp.getInt(HOUR, 0)
        myMin = sp.getInt(MIN, 0)
        mySec = sp.getInt(SEC, 0)

        setText()
        binding.bnOpen.setOnClickListener {
            openDialog()
        }

    }

    private fun setText() = with(binding){
        var str = myHour.toString()
        tvHour.text = str
        str = myMin.toString()
        tvMin.text = str
        str = mySec.toString()
        tvSec.text = str
    }

    private fun openDialog(){
        val font = resources.getFont(R.font.led_bold)
        val dialog = TimeDialog(this)
        dialog.hour = myHour
        dialog.min = myMin
        dialog.sec = mySec
        dialog.maxHours = 23
        dialog.textColorSel = Color.BLUE
        dialog.fontFamily = font
        dialog.setOnChangeListener { hour, min, sec ->
            myHour = hour
            myMin = min
            mySec = sec
            setText()
        }
    }

    override fun onStop() {
        super.onStop()
        val editor = sp.edit()
        editor.putInt(HOUR, myHour)
        editor.putInt(MIN, myMin)
        editor.putInt(SEC, mySec)
        editor.apply()
    }
}