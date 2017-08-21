package piuk.blockchain.android.ui.customviews

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView

import java.util.ArrayList

import piuk.blockchain.android.R
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.annotations.Thunk

class NumericKeyboard : LinearLayout, View.OnClickListener {

    private var viewList: ArrayList<EditText>? = null
    private var decimalSeparator = "."
    @Thunk internal var numpad: TableLayout
    @Thunk internal var callback: NumericKeyboardCallback? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_numeric_keyboard, this, true)

        numpad = findViewById(R.id.numericPad)
        numpad.findViewById<View>(R.id.button1).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button2).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button3).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button4).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button5).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button6).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button7).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button8).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button9).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button10).setOnClickListener(this)
        numpad.findViewById<View>(R.id.button0).setOnClickListener(this)
        numpad.findViewById<View>(R.id.buttonDeleteBack).setOnClickListener(this)
        numpad.findViewById<View>(R.id.buttonDone).setOnClickListener(this)

        viewList = ArrayList()
    }

    fun enableOnView(view: EditText) {

        if (!viewList!!.contains(view)) viewList!!.add(view)

        view.setTextIsSelectable(true)
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val view1 = (context as Activity).currentFocus
                if (view1 != null) {
                    val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(view1.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                }
                setNumpadVisibility(View.VISIBLE)
            }
        }
        view.setOnClickListener { v ->
            (numpad.findViewById<View>(R.id.decimal_point) as TextView).text = decimalSeparator
            setNumpadVisibility(View.VISIBLE)
        }
    }

    fun setCallback(callback: NumericKeyboardCallback) {
        this.callback = callback
    }

    fun setNumpadVisibility(@ViewUtils.Visibility visibility: Int) {
        if (visibility == View.VISIBLE) {
            showKeyboard()
        } else {
            hideKeyboard()
        }
    }

    private fun showKeyboard() {
        if (!isVisible) {
            val bottomUp = AnimationUtils.loadAnimation(context, R.anim.bottom_up)
            startAnimation(bottomUp)
            bottomUp.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    // No-op
                }

                override fun onAnimationEnd(animation: Animation) {
                    if (callback != null) callback!!.onKeypadOpenCompleted()
                }

                override fun onAnimationRepeat(animation: Animation) {
                    // No-op
                }
            })
            visibility = View.VISIBLE
            if (callback != null) callback!!.onKeypadOpen()
        }
    }

    private fun hideKeyboard() {
        if (isVisible) {
            val topDown = AnimationUtils.loadAnimation(context, R.anim.top_down)
            startAnimation(topDown)
            visibility = View.GONE
            if (callback != null) callback!!.onKeypadClose()
        }
    }

    override fun onClick(v: View) {

        var pad: String? = ""
        when (v.id) {
            R.id.button10 -> pad = decimalSeparator
            R.id.buttonDeleteBack -> {
                deleteFromFocusedView()
                return
            }
            R.id.buttonDone -> setNumpadVisibility(View.GONE)
            else -> pad = v.tag.toString().substring(0, 1)
        }

        // Append tapped #
        if (pad != null) {
            appendToFocusedView(pad)
        }
    }

    private fun appendToFocusedView(pad: String) {
        for (view in viewList!!) {
            if (view.hasFocus()) {

                //Don't allow multiple decimals
                if (pad == decimalSeparator && view.text.toString().contains(decimalSeparator))
                    continue

                val startSelection = view.selectionStart
                val endSelection = view.selectionEnd
                if (endSelection - startSelection > 0) {
                    val selectedText = view.text.toString().substring(startSelection, endSelection)
                    view.setText(view.text.toString().replace(selectedText, pad))
                } else {
                    view.append(pad)
                }

                if (view.text.length > 0) {
                    view.post { view.setSelection(view.text.toString().length) }
                }
            }
        }
    }

    private fun deleteFromFocusedView() {
        for (view in viewList!!) {
            if (view.hasFocus() && view.text.length > 0) {
                view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
    }

    fun setDecimalSeparator(passedDecimalSeparator: String) {
        decimalSeparator = passedDecimalSeparator
    }

    val isVisible: Boolean
        get() = visibility == View.VISIBLE
}