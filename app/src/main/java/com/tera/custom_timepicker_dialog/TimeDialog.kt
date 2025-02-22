package com.tera.custom_timepicker_dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import java.util.Calendar

typealias OnChangeListener = (hour: Int, min: Int, sec: Int) -> Unit

class TimeDialog(context: Context) {

    companion object {
        const val MAX_HOUR = 100
    }

    private var mListener: OnChangeListener? = null
    private var mDialog: AlertDialog? = null

    private var mHour = 0
    private var mMin = 0
    private var mSec = 0
    private var density = 0f // Логическая плотность дисплея

    private var bnOk: MaterialButton
    private var bnCansel: MaterialButton
    private var pickerH: NumberPickerCustom
    private var pickerM: NumberPickerCustom
    private var pickerS: NumberPickerCustom


    init {
        val view = LayoutInflater.from(context).inflate(R.layout.time_dialog_layout, null)

        pickerH = view.findViewById(R.id.pickerH)
        pickerM = view.findViewById(R.id.pickerM)
        pickerS = view.findViewById(R.id.pickerS)

        bnOk = view.findViewById(R.id.bnOk)
        bnCansel = view.findViewById(R.id.bnCansel)

        pickerH.hintText = context.getString(R.string.h)
        pickerM.hintText = context.getString(R.string.m)
        pickerS.hintText = context.getString(R.string.s)
        density = context.resources.displayMetrics.density

        mDialog = AlertDialog.Builder(context).create()

        bnOk.setOnClickListener {
            mListener?.invoke(mHour, mMin, mSec)
            mDialog?.dismiss()
        }
        bnCansel.setOnClickListener {
            mDialog?.dismiss()
        }

        setParams()
        initListener()
        setTime()

        mDialog?.setCancelable(false)
        mDialog?.setView(view)
        mDialog?.show()
    }

    // Слушатели
    private fun initListener() {
        pickerH.setOnChangeListener {
            mHour = it
            if (mListener != null)
                mListener?.invoke(mHour, mMin, mSec)
        }
        pickerM.setOnChangeListener {
            mMin = it
            if (mListener != null)
                mListener?.invoke(mHour, mMin, mSec)
        }
        pickerS.setOnChangeListener {
            mSec = it
            if (mListener != null)
                mListener?.invoke(mHour, mMin, mSec)
        }
    }

    private fun setParams() {
        var array = getArray(MAX_HOUR)
        pickerH.displayedValues = array
        array = getArray(59)
        pickerM.displayedValues = array
        pickerS.displayedValues = array
    }

    // Получить массив строк
    private fun getArray(max: Int): Array<String> {
        val arrayInt = (0..max).toList().toIntArray()
        val arrayStr = arrayInt.map { it.toString() }.toTypedArray()
        for (i in 0..9) {
            arrayStr[i] = "0$i"
        }
        return arrayStr
    }

    // Установить время
    private fun setTime() {
        val calendar = Calendar.getInstance()
        pickerH.value = calendar[Calendar.HOUR_OF_DAY]
        pickerM.value = calendar[Calendar.MINUTE]
        pickerS.value = calendar[Calendar.SECOND]
    }

    fun setOnChangeListener(listener: OnChangeListener) {
        mListener = listener
    }

    var hour: Int = 0
        set(value) {
            field = value
            mHour = value
            pickerH.value = value
        }

    var min: Int = 0
        set(value) {
            field = value
            mMin = value
            pickerM.value = value
        }

    var sec: Int = 0
        set(value) {
            field = value
            mSec = value
            pickerS.value = value
        }

    var fadingExtent: Int = 0
        set(value) {
            field = value
            val ext = value / 10f
            pickerH.fadingExtent = ext
            pickerM.fadingExtent = ext
            pickerS.fadingExtent = ext
        }

    var fontFamily: Typeface? = Typeface.DEFAULT
        set(value) {
            field = value
           pickerH.fontFamily = value
           pickerM.fontFamily = value
           pickerS.fontFamily = value
        }

    var hintTextHour: String = ""
        set(value) {
            field = value
            pickerH.hintText = value
        }

    var hintTextMin: String = ""
        set(value) {
            field = value
            pickerM.hintText = value
        }

    var hintTextSec: String = ""
        set(value) {
            field = value
            pickerS.hintText = value
        }

    var hintColor: Int = 0
        set(value) {
            field = value
            pickerH.hintColor = value
            pickerM.hintColor = value
            pickerS.hintColor = value
        }

    var hintSize: Int = 0
        set(value) {
            field = value
            val size = value * density
            pickerH.hintSize = size
            pickerM.hintSize = size
            pickerS.hintSize = size
        }

    var intervalLongPress: Int = 0
        set(value) {
            field = value
            pickerH.intervalLongPress = value
            pickerM.intervalLongPress = value
            pickerS.intervalLongPress = value
        }

    var maxHours: Int = 100
        set(value) {
            field = value
            val array = getArray(value)
            pickerH.displayedValues = array
            pickerH.value = mHour
        }

    var showHint: Boolean = true
        set(value) {
            field = value
            pickerH.showHint = value
            pickerM.showHint = value
            pickerS.showHint = value
        }

    var showRows5: Boolean = true
        set(value) {
            field = value
            pickerH.showRows5 = value
            pickerM.showRows5 = value
            pickerS.showRows5 = value
        }

    var showSec: Boolean = true
        set(value) {
            field = value
            if (value) pickerS.isVisible = true
            else pickerS.isVisible = false
        }

    var textColor: Int = 0
        set(value) {
            field = value
            pickerH.textColor = value
            pickerM.textColor = value
            pickerS.textColor = value
        }

    var textColorSel: Int = 0
        set(value) {
            field = value
            pickerH.textColorSel = value
            pickerM.textColorSel = value
            pickerS.textColorSel = value
        }

    var textSize: Int = 0
        set(value) {
            field = value
            val size = value * density
            pickerH.textSize = size
            pickerM.textSize = size
            pickerS.textSize = size
        }

    var textSizeSel: Int = 0
        set(value) {
            field = value
            val size = value * density
            pickerH.textSizeSel = size
            pickerM.textSizeSel = size
            pickerS.textSizeSel = size
        }

    var buttonCancelColor: Int = 0
        set(value) {
            field = value
            bnCansel.setBackgroundColor(value)
        }

    var buttonOkColor: Int = 0
        set(value) {
            field = value
            bnOk.setBackgroundColor(value)
        }

    var textCancelColor: Int = 0
        set(value) {
            field = value
            bnCansel.setTextColor(value)
        }

    var textOkColor: Int = 0
        set(value) {
            field = value
            bnOk.setTextColor(value)
        }

}