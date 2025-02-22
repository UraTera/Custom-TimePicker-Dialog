package com.tera.custom_timepicker_dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.floor

typealias OnValueChangeListener = (value: Int) -> Unit

class NumberPickerCustom(
    context: Context,
    attrs: AttributeSet?,
    defStyleRes: Int

) : View(context, attrs, defStyleRes) {

    constructor(context: Context, attributesSet: AttributeSet?) :
            this(context, attributesSet, 0)

    constructor(context: Context) : this(context, null)

    companion object {

        const val VIEW_WIDTH = 165 // Ширина элемента
        const val ITEM_HEIGHT = 158 // Высота строки

        const val TEXT_COLOR = -12763843
        const val TEXT_COLOR_SEL = Color.BLACK
        const val TEXT_SIZE = 52     // 20sp
        const val TEXT_SIZE_SEL = 63 // 24sp
        const val HINT_SIZE = 42     // 16sp

        const val HINT_OFFSET = 15f // Отступ подсказки
        const val DIVIDER_COLOR = Color.BLACK
        const val DIVIDER_HEIGHT = 5
        const val DIVIDER_OFFSET = 20
        const val SHOW_ROW_5 = true // Показать 5 строк
        const val INTERVAL_LONG_PRESS = 200 // Интервал обновления при длительном нажатии


        // Интервал времени для прокрутки расстояния высоты одного элемента
        private const val HANDLER_INTERVAL_REFRESH = 32 // millisecond

        // Длительность прокрутки элемента
        private const val INTERVAL_REVISE = 300

        // Длительность прокрутки при нажатии
        private const val DURATION_PRESS = 300
    }

    private val mPaintDivider = Paint()
    private val mPaintText = TextPaint()
    private val mPaintHint = Paint()

    private var mScroller = Scroller(context)
    private val mVelocityTracker: VelocityTracker = VelocityTracker.obtain()
    private val mMinVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    // Слушатель значения
    private var mListener: OnValueChangeListener? = null

    private var mMinShowIndex = -1
    private var mMaxShowIndex = -1
    private var mPrevPickedIndex = 0
    private var mScaledTouchSlop = 8

    private var mFriction = 1f
    private var mTextOffset = 0f
    private var mTextOffsetSel = 0f

    // true для установки текущей позиции, false для установки позиции на 0 (false)
    private var mCurrentItemIndexEffect = false

    // true, если NumberPicker инициализирован
    private var mHasInit = false

    // Ответ на изменение в главной теме
    private var mRespondChangeInMainThread = true
    private var mHandlerThread: HandlerThread? = null
    private var mHandlerInNewThread: Handler? = null
    private var mHandlerInMainThread: Handler? = null

    // Индекс содержимого первого показанного элемента
    private var mCurrDrawFirstItemIndex = 0

    // Y первого показанного элемента
    private var mCurrDrawFirstItemY = 0

    // Глобальный Y, соответствующий скроллеру
    private var mCurrDrawGlobalY = 0
    private var mInScrollingPickedOldValue = 0
    private var mInScrollingPickedNewValue = 0

    // Y последнего нажатия.
    private var mLastDownEventY = 0f

    // Y последнего события нажатия или перемещение.
    private var mLastDownOrMoveEventY = 0f

    // Изменить позицию на единицу при нажатии
    private var mChangeCurrentByLongPress: ChangeCurrentByLongPress? = null

    private var mDownYGlobal = 0f
    private var mDownY = 0f
    private var mCurrY = 0f
    private var mDividerTop = 0f    // Позиция верхнего разделителя.
    private var mDividerBottom = 0f // Позиция нижнего разделителя.
    private var mKeyPress = false
    private var mKeyScroll = false
    private var mMoveSpeed = 600

    private var mDisplayedValues = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    private var mItemHeight = 0            // Высота строки
    private var mRectClip = RectF()        // Область видимости текста
    private var mViewWidth = VIEW_WIDTH    // Ширина элемента
    private var mOffDividerX = DIVIDER_OFFSET  // Отступ от края постоянный
    private var mOffDividerY = 14          // Смещение разделителя по Y
    private var mHintHeight = 0f           // Высота подсказки
    private var mHintOffset = HINT_OFFSET  // Отступ подсказки
    private var mShownCount = 5            // Количество строк
    private var mValue = 0

    // Атрибуты
    private var mMax = 10
    private var mDividerColor = DIVIDER_COLOR
    private var mDividerHeight = DIVIDER_HEIGHT
    private var mDividerOffset = 0 // Отступ разделителя от края доп.
    private var mFadingExtent = 0.7f

    private var mTextColor = TEXT_COLOR
    private var mTextColorSel = TEXT_COLOR_SEL
    private var mTextSize = TEXT_SIZE.toFloat()
    private var mTextSizeSel = TEXT_SIZE_SEL.toFloat()
    private var mHintText: String? = null
    private var mHintTextColor = TEXT_COLOR
    private var mHintTextSize = HINT_SIZE.toFloat()
    private var mFontFamily: Typeface? = null
    private var mShowRows5 = SHOW_ROW_5
    private var mIntervalLongPress = INTERVAL_LONG_PRESS

    init {

        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        if (mMinShowIndex == -1 || mMaxShowIndex == -1) {
            updateValueForInit()
        }

        initPaints()
        setTextSize()
        setHint()
        setDividerOffset()
        initHandler()
    }

    // Кисти
    private fun initPaints() {
        mPaintText.color = mTextColor
        mPaintText.isAntiAlias = true
        mPaintText.textAlign = Paint.Align.CENTER
        mPaintText.typeface = mFontFamily

        mPaintDivider.color = mDividerColor
        mPaintDivider.isAntiAlias = true
        mPaintDivider.style = Paint.Style.STROKE
        mPaintDivider.strokeWidth = mDividerHeight.toFloat()

        mPaintHint.color = mHintTextColor
        mPaintHint.isAntiAlias = true
        mPaintHint.textAlign = Paint.Align.CENTER
        mPaintHint.textSize = mHintTextSize
    }

    var displayedValues: Array<String> = mDisplayedValues
        set(value) {
            field = value
            mMax = value.size
            mDisplayedValues = value
            mMaxShowIndex = mMax
        }

    private fun setDividerOffset() {
        mDividerOffset += mOffDividerX
    }

    var fadingExtent: Float = 0f
        set(value) {
            field = value
            mFadingExtent = value
        }

    var fontFamily: Typeface? = Typeface.DEFAULT
        set(value) {
            field = value
            mPaintText.typeface = value
        }

    var hintText: String = ""
        set(value) {
            field = value
            mHintText = value
            setHint()
        }

    var hintSize: Float = 0f
        set(value) {
            field = value
            mHintTextSize = value
            mPaintHint.textSize = value
            setHint()
        }

    var hintColor: Int = 0
        set(value) {
            field = value
            mPaintHint.color = value
        }

    private fun setHint() {
        mHintHeight = mHintTextSize + mHintOffset
    }

    var intervalLongPress: Int = 0
        set(value) {
            field = value
            mIntervalLongPress = value
        }

    var showHint: Boolean = true
        set(value) {
            field = value
            if (!value) mHintHeight = 0f
        }

    var showRows5: Boolean = true
        set(value) {
            field = value
            mShowRows5 = value
            setRowsCount()
            setPosDivider()
        }

    private fun setRowsCount() {
        mShownCount = if (mShowRows5) 5
        else 3
    }

    var textColor: Int = 0
        set(value) {
            field = value
            if (value != mTextColor) {
                mTextColor = value
                mPaintText.color = value
            }
        }

    var textColorSel: Int = 0
        set(value) {
            field = value
            if (value != mTextColorSel) {
                mTextColorSel = value
                mPaintText.color = value
            }
        }

    var textSize: Float = 0f
        set(value) {
            field = value
            if (value != mTextSize) {
                mTextSize = value
                mPaintText.textSize = value
                setTextSize()
            }
        }

    var textSizeSel: Float = 0f
        set(value) {
            field = value
            if (value != mTextSizeSel) {
                mTextSizeSel = value
                mPaintText.textSize = value
                setTextSize()
            }
        }

    private fun setTextSize() {
        mPaintText.textSize = mTextSizeSel
        mTextOffsetSel = getTextCenterYOffset(mPaintText.fontMetrics)
        mPaintText.textSize = mTextSize
        mTextOffset = getTextCenterYOffset(mPaintText.fontMetrics)
    }

    var value: Int
        get() = mPickedIndexRelativeToRow
        set(value) {
            mPickedIndexRelativeToRow = value
        }

    // Получить смещение текста по Y
    private fun getTextCenterYOffset(fontMetrics: Paint.FontMetrics?): Float {
        if (fontMetrics == null) {
            return 0f
        }
        return (abs((fontMetrics.top + fontMetrics.bottom).toDouble()) / 2).toFloat()
    }

    private fun initHandler() {
        mHandlerThread = HandlerThread("HandlerThread-For-Refreshing")
        mHandlerThread!!.start()

        mHandlerInNewThread = object : Handler(mHandlerThread!!.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    1 -> if (!mScroller.isFinished) {
                        mHandlerInNewThread!!.sendMessageDelayed(
                            getMsg(
                                1,
                                0,
                                0,
                                msg.obj
                            ), HANDLER_INTERVAL_REFRESH.toLong()
                        )
                    } else {
                        var duration = 0
                        val willPickIndex: Int
                        // Если скроллер закончился (не прокручивается), то отрегулируйте положение
                        if (mCurrDrawFirstItemY != 0) {
                            if (mCurrDrawFirstItemY < (-mItemHeight / 2)) {
                                // Отрегулируйте, чтобы прокрутить вверх
                                duration =
                                    (INTERVAL_REVISE.toFloat() * (mItemHeight + mCurrDrawFirstItemY) / mItemHeight).toInt()
                                mScroller.startScroll(
                                    0,
                                    mCurrDrawGlobalY,
                                    0,
                                    mItemHeight + mCurrDrawFirstItemY,
                                    duration * 3
                                )
                                willPickIndex =
                                    getWillPickIndexByGlobalY(mCurrDrawGlobalY + mItemHeight + mCurrDrawFirstItemY)
                            } else {
                                // Отрегулируйте, чтобы прокрутить вниз
                                duration =
                                    (INTERVAL_REVISE.toFloat() * (-mCurrDrawFirstItemY) / mItemHeight).toInt()
                                mScroller.startScroll(
                                    0,
                                    mCurrDrawGlobalY,
                                    0,
                                    mCurrDrawFirstItemY,
                                    duration * 3
                                )
                                willPickIndex =
                                    getWillPickIndexByGlobalY(mCurrDrawGlobalY + mCurrDrawFirstItemY)
                            }
                            postInvalidate()
                        } else {
                            // Получить индекс, который будет выбран
                            willPickIndex = getWillPickIndexByGlobalY(mCurrDrawGlobalY)
                        }
                        val changeMsg = getMsg(
                            2,
                            mPrevPickedIndex,
                            willPickIndex,
                            msg.obj
                        )
                        if (mRespondChangeInMainThread) {
                            mHandlerInMainThread!!.sendMessageDelayed(
                                changeMsg,
                                (duration * 2).toLong()
                            )
                        } else {
                            mHandlerInNewThread!!.sendMessageDelayed(
                                changeMsg,
                                (duration * 2).toLong()
                            )
                        }
                    }
                }
            }
        }
        mHandlerInMainThread = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    3 -> requestLayout()
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mHandlerThread == null || !mHandlerThread!!.isAlive) {
            initHandler()
        }
    }

    // Обновить значение для Init
    private fun updateValueForInit() {
        if (mMinShowIndex == -1) {
            mMinShowIndex = 0
        }
        if (mMaxShowIndex == -1) {
            mMaxShowIndex = mDisplayedValues.size
        }
    }

    // Получить индекс по индексу строки
    private fun getIndexByRowIndex(indexF: Int, size: Int): Int {
        var index = indexF
        if (size <= 0) {
            return 0
        }
        index %= size
        if (index < 0) {
            index += size
        }
        return index
    }

    // Один размер переработки
    private val oneRecycleSize: Int
        get() = mMaxShowIndex - mMinShowIndex

    // Выбранный индекс относительно строки
    private var mPickedIndexRelativeToRow: Int
        get() {
            val willPickIndex: Int = if (mCurrDrawFirstItemY != 0) {
                if (mCurrDrawFirstItemY < (-mItemHeight / 2)) {
                    getWillPickIndexByGlobalY(mCurrDrawGlobalY + mItemHeight + mCurrDrawFirstItemY)
                } else {
                    getWillPickIndexByGlobalY(mCurrDrawGlobalY + mCurrDrawFirstItemY)
                }
            } else {
                getWillPickIndexByGlobalY(mCurrDrawGlobalY)
            }
            return willPickIndex
        }
        set(pickedIndexToRaw) {
            if (mMinShowIndex > -1) {
                if (pickedIndexToRaw in mMinShowIndex..mMaxShowIndex) {
                    mPrevPickedIndex = pickedIndexToRaw
                    correctPositionByDefaultValue(
                        pickedIndexToRaw - mMinShowIndex,
                    )
                    postInvalidate()
                }
            }
        }

    // Возвращает индекс относительно mDisplayedValues 0
    private fun getWillPickIndexByGlobalY(globalY: Int): Int {
        if (mItemHeight == 0) {
            return 0
        }
        val willPickIndex = globalY / mItemHeight + mShownCount / 2
        val index = getIndexByRowIndex(
            willPickIndex, oneRecycleSize
        )
        if (index in 0..<oneRecycleSize) {
            return index + mMinShowIndex
        } else {
            throw IllegalArgumentException(
                ("getWillPickIndexByGlobalY illegal index : " + index
                        + " getOneRecycleSize() : " + oneRecycleSize)
            )
        }
    }

    // Выбранный индекс по умолчанию относительно показанной части
    private fun correctPositionByDefaultValue(defaultPickedIndex: Int) {
        mCurrDrawFirstItemIndex = defaultPickedIndex - (mShownCount - 1) / 2
        mCurrDrawFirstItemIndex = getIndexByRowIndex(mCurrDrawFirstItemIndex, oneRecycleSize)
        if (mItemHeight == 0) {
            mCurrentItemIndexEffect = true
        } else {
            mCurrDrawGlobalY = mCurrDrawFirstItemIndex * mItemHeight
            mInScrollingPickedOldValue = mCurrDrawFirstItemIndex + mShownCount / 2
            mInScrollingPickedOldValue %= oneRecycleSize
            if (mInScrollingPickedOldValue < 0) {
                mInScrollingPickedOldValue += oneRecycleSize
            }
            mInScrollingPickedNewValue = mInScrollingPickedOldValue
            calculateFirstItemParameterByGlobalY()
        }
    }

    // Плавное изменение цвета текста
    private fun getEvaluateColor(fraction: Float, startColor: Int, endColor: Int): Int {
        val color = ColorUtils.blendARGB(startColor, endColor, fraction)
        return color
    }

    // Плавное изменение размера текста
    private fun getEvaluateSize(fraction: Float, startSize: Float, endSize: Float): Float {
        return startSize + (endSize - startSize) * fraction
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mKeyScroll = false
        if (!isEnabled) {
            return false
        }
        mVelocityTracker.addMovement(event)
        mCurrY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mKeyPress = true
                mHandlerInNewThread!!.removeMessages(1)
                stopScrolling()
                scrollOnPress(event) // Прокрутить при нажатии
                mDownY = mCurrY
                mDownYGlobal = mCurrDrawGlobalY.toFloat()
                parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val spanY = mDownY - mCurrY
                if (abs(spanY) > 20) {
                    mKeyPress = false
                    mCurrDrawGlobalY = (mDownYGlobal + spanY).toInt()
                    calculateFirstItemParameterByGlobalY() // 1
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeChangeCurrentByOneFromLongPress() // Удалить команду изменения текущего значения
                if (mKeyPress) {
                    selectScrollDirection(event.y.toInt()) // Выбрать направление прокрутки
                } else {
                    countVelocityTracker()
                }
                mHandlerInNewThread!!.sendMessageDelayed(getMsg(), 0)
            }
        }
        return true
    }

    // Остановить прокрутку
    private fun stopScrolling() {
        if (!mScroller.isFinished) {
            mScroller.startScroll(0, mScroller.currY, 0, 0, 1)
            mScroller.abortAnimation()
            postInvalidate()
        }
    }

    // Прокрутить при нажатии
    private fun scrollOnPress(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        if (!mKeyPress)
            return false

        val action = event.action and MotionEvent.ACTION_MASK
        if (action != MotionEvent.ACTION_DOWN) {
            return false
        }

        removeAllCallbacks()
        parent.requestDisallowInterceptTouchEvent(true)

        mLastDownEventY = event.y
        mLastDownOrMoveEventY = mLastDownEventY

        if (mLastDownEventY < mDividerTop)
            postChangeCurrentByOneFromLongPress(false)
        else if ((mLastDownEventY > mDividerBottom))
            postChangeCurrentByOneFromLongPress(true)
        return true
    }

    // Удалить все ожидающие обратные вызовы из очереди сообщений
    private fun removeAllCallbacks() {
        if (mChangeCurrentByLongPress != null) {
            removeCallbacks(mChangeCurrentByLongPress) // Штатный
        }
    }

    // Команда на изменение текущего значения на единицу
    private fun postChangeCurrentByOneFromLongPress(
        increment: Boolean,
        delayMillis: Long = ViewConfiguration.getLongPressTimeout().toLong()
    ) {
        if (!mKeyPress) return
        if (mChangeCurrentByLongPress == null) {
            mChangeCurrentByLongPress = ChangeCurrentByLongPress()
        } else {
            removeCallbacks(mChangeCurrentByLongPress)
        }
        mChangeCurrentByLongPress!!.setStep(increment)
        postDelayed(mChangeCurrentByLongPress, delayMillis)
    }

    // Команда изменения текущего значения при длительном нажатии на единицу
    private inner class ChangeCurrentByLongPress : Runnable {
        private var mIncrement = false

        fun setStep(increment: Boolean) { // Короткое нажатие
            mIncrement = increment
        }

        override fun run() {
            if (mKeyPress) {
                changeValueByOne(mIncrement)
                postDelayed(this, mIntervalLongPress.toLong())
            }
        }
    }

    // Изменить текущее значение на единицу
    private fun changeValueByOne(increment: Boolean) {
        val duration = DURATION_PRESS
        val dy = if (increment) mItemHeight
        else -mItemHeight
        mScroller.startScroll(0, mCurrDrawGlobalY, 0, dy, duration)
        postInvalidate()
    }

    // Остановить команду изменения текущего значения на единицу
    private fun removeChangeCurrentByOneFromLongPress() {
        if (mChangeCurrentByLongPress != null) {
            removeCallbacks(mChangeCurrentByLongPress)
        }
    }

    // Выбрать направление прокрутки
    private fun selectScrollDirection(eventY: Int) {
        if (eventY < mDividerTop)
            changeValueByOne(false)
        else if (eventY > mDividerBottom)
            changeValueByOne(true)
    }

    // Переместить и получить выбранное значение
    private fun calculateFirstItemParameterByGlobalY() {
        mCurrDrawFirstItemIndex =
            floor((mCurrDrawGlobalY.toFloat() / mItemHeight).toDouble()).toInt()
        mCurrDrawFirstItemY = -(mCurrDrawGlobalY - mCurrDrawFirstItemIndex * mItemHeight)

        if (mListener != null && !mKeyScroll) {
            mInScrollingPickedNewValue = if (-mCurrDrawFirstItemY > mItemHeight / 2) {
                mCurrDrawFirstItemIndex + 1 + mShownCount / 2
            } else {
                mCurrDrawFirstItemIndex + mShownCount / 2
            }
            mInScrollingPickedNewValue %= oneRecycleSize
            if (mInScrollingPickedNewValue < 0) {
                mInScrollingPickedNewValue += oneRecycleSize
            }
            if (mInScrollingPickedOldValue != mInScrollingPickedNewValue) {
                mValue = mInScrollingPickedNewValue
                mListener!!.invoke(mValue)
            }
            mInScrollingPickedOldValue = mInScrollingPickedNewValue
        }
    }

    // Скорость трекера
    private fun countVelocityTracker() {
        mVelocityTracker.computeCurrentVelocity(mMoveSpeed) // 600
        val velocityY = (mVelocityTracker.yVelocity * mFriction).toInt()
        if (abs(velocityY.toDouble()) > mMinVelocity) {
            mScroller.fling(
                0,
                mCurrDrawGlobalY,
                0,
                -velocityY,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Int.MAX_VALUE
            )
            invalidate()
        }
        mVelocityTracker.clear()
    }

    // Фиксировать прокрутку
    override fun computeScroll() {
        if (mItemHeight == 0) {
            return
        }
        if (mScroller.computeScrollOffset()) {
            mCurrDrawGlobalY = mScroller.currY
            calculateFirstItemParameterByGlobalY()
            postInvalidate()
        }
    }

    private fun getMsg(): Message {
        return getMsg(1, 0, 0, null)
    }

    private fun getMsg(what: Int, arg1: Int, arg2: Int, obj: Any?): Message {
        val msg = Message.obtain()
        msg.what = what
        msg.arg1 = arg1
        msg.arg2 = arg2
        msg.obj = obj
        return msg
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawText(canvas)
        drawDivider(canvas)
        drawHint(canvas)
    }

    // Текст
    private fun drawText(canvas: Canvas) {
        val xc = width / 2f
        var index: Int
        var textColor: Int
        var textSize: Float
        var textOffsetY: Float // Смещение текста по Y
        var fraction = 0f

        for (i in 0 until mShownCount + 1) {
            // Y первого показанного элемента
            val y = (mCurrDrawFirstItemY + mItemHeight * i).toFloat() + mHintHeight

            index =
                getIndexByRowIndex(mCurrDrawFirstItemIndex + i, oneRecycleSize)

            when (i) {
                mShownCount / 2 -> { // Центр
                    fraction = (mItemHeight + mCurrDrawFirstItemY).toFloat() / mItemHeight
                    textColor = getEvaluateColor(fraction, mTextColor, mTextColorSel)
                    textSize = getEvaluateSize(fraction, mTextSize, mTextSizeSel)
                    textOffsetY = getEvaluateSize(fraction, mTextOffset, mTextOffsetSel)
                }

                mShownCount / 2 + 1 -> { // Нижний
                    textColor = getEvaluateColor(1 - fraction, mTextColor, mTextColorSel)
                    textSize = getEvaluateSize(1 - fraction, mTextSize, mTextSizeSel)
                    textOffsetY = getEvaluateSize(1 - fraction, mTextOffset, mTextOffsetSel)
                }

                else -> { // Верхний
                    textColor = mTextColor
                    textSize = mTextSize
                    textOffsetY = mTextOffset
                }
            }

            mPaintText.color = textColor
            mPaintText.textSize = textSize

            if (index in 0..<oneRecycleSize) {
                val strValue: String = mDisplayedValues[index + mMinShowIndex]

                val yt = y + mItemHeight / 2 + textOffsetY
                val fh = mHintHeight.toInt()

                // Рисовать текст
                if (fh != 0) { // Обрезать подсказку
                    canvas.save()
                    canvas.clipRect(mRectClip)
                    canvas.drawText(strValue, xc, yt, mPaintText)
                    canvas.restore()
                } else
                    canvas.drawText(strValue, xc, yt, mPaintText)
            }
        }
    }

    // Разделитель
    private fun drawDivider(canvas: Canvas) {
        val x1 = mDividerOffset.toFloat()
        val x2 = width - mDividerOffset.toFloat()
        var y = mDividerTop + mOffDividerY
        canvas.drawLine(x1, y, x2, y, mPaintDivider)
        y = mDividerBottom - mOffDividerY
        canvas.drawLine(x1, y, x2, y, mPaintDivider)
    }

    // Подсказка
    private fun drawHint(canvas: Canvas) {
        if (mHintText == null) {
            return
        }
        val x = width / 2f
        val y = mHintHeight - mHintOffset
        canvas.drawText(mHintText!!, x, y, mPaintHint)
    }

    // Инициализировать затухающие края
    private fun initializeFadingEdges() {
        isVerticalFadingEdgeEnabled = true
        val len = (height - mHintHeight - mItemHeight) / 2
        setFadingEdgeLength(len.toInt())
    }

    override fun getTopFadingEdgeStrength(): Float {
        return mFadingExtent
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return mFadingExtent
    }

    override fun isPaddingOffsetRequired(): Boolean {
        return true
    }

    override fun getTopPaddingOffset(): Int {
        return mHintHeight.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        mItemHeight = ITEM_HEIGHT

        if (mHintText == null) mHintHeight = 0f

        val wC = mViewWidth + (mOffDividerX * 2)
        val hC = mItemHeight * mShownCount + mHintHeight.toInt()

        setMeasuredDimension(
            resolveSize(wC, widthMeasureSpec),
            resolveSize(hC, heightMeasureSpec)
        )
    }

    // Положение разделителей
    private fun setPosDivider() {
        mDividerTop = (height - mItemHeight + mHintHeight) / 2
        mDividerBottom = mDividerTop + mItemHeight
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mViewWidth = w - mOffDividerX * 2
        mItemHeight = (h - mHintHeight.toInt()) / mShownCount

        mRectClip = RectF(0f, mHintHeight, width.toFloat(), h.toFloat())

        // Положение разделителей
        setPosDivider()

        // Затухание краев
        initializeFadingEdges()

        // Вычислить индекс первого видимого элемента
        var defaultValue = 0
        if (oneRecycleSize > 1) {
            defaultValue = if (mHasInit) {
                value
            } else if (mCurrentItemIndexEffect) {
                mCurrDrawFirstItemIndex + (mShownCount - 1) / 2
            } else {
                0
            }
        }
        correctPositionByDefaultValue(defaultValue)
        mHasInit = true
    }

    // Слушатель изменения значения
    fun setOnChangeListener(listener: OnValueChangeListener) {
        mListener = listener
    }

}