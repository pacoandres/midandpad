package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class TouchCalibration (context: Context): ConstraintLayout(context) {
    private val mIsFixed: CheckBox
    private val mFixedValue: EditText
    private val mMinButton: CalibrationButton
    private val mMaxButton: CalibrationButton

    private val mLabelMinPress: TextView
    private val mLabelMaxPress: TextView

    /*private val mLabelMinTime: TextView
    private val mLabelMaxTime: TextView*/

    private val mOKButton: Button
    private val mCancelButton: Button

    init {
        inflate(context, R.layout.touchcalibration, this)
        mIsFixed = findViewById(R.id.isfixedvelocity)
        mFixedValue = findViewById(R.id.fixedvelocity)
        mMinButton = findViewById(R.id.minvelocity)
        mMaxButton = findViewById(R.id.maxvelocity)
        mLabelMinPress = findViewById(R.id.labelminpress)
        mLabelMaxPress = findViewById(R.id.labelmaxpress)
        /*mLabelMinTime = findViewById(R.id.labelmintime)
        mLabelMaxTime = findViewById(R.id.labelmaxtime)*/

        mOKButton = findViewById(R.id.okcalib)
        mCancelButton = findViewById(R.id.cancelcalib)

        loadConfig ()

        mIsFixed.setOnCheckedChangeListener() { _, checked:Boolean ->
            isFixed(checked)
        }

        mOKButton.setOnClickListener { _->
            acceptChanges ()
        }

        mCancelButton.setOnClickListener { _->
            cancel ()
        }

        mMinButton.setLabels(mLabelMinPress/*, mLabelMinTime*/, R.string.sminpress/*,
            R.string.smintime*/)

        mMaxButton.setLabels(mLabelMaxPress/*, mLabelMaxTime*/, R.string.smaxpress/*,
            R.string.smaxtime*/)
        mFixedValue.isFocusableInTouchMode = true
    }

    fun loadConfig (){
        val texts = StringBuilder ()
        mIsFixed.isChecked = MainActivity.mConfigParams.mIsFixedVelocity
        texts.append(MainActivity.mConfigParams.mFixedVelocity)
        mFixedValue.text = texts.toString().toEditable()

        texts.clear()
        texts.append(context.getString(R.string.sminpress), " ", MainActivity.mConfigParams.mMinPress)
        mLabelMinPress.text = texts

        texts.clear()
        texts.append(context.getString(R.string.smaxpress), " ", MainActivity.mConfigParams.mMaxPress)
        mLabelMaxPress.text = texts

        /*texts.clear()
        texts.append(context.getString(R.string.smintime), " ", MainActivity.mConfigParams.mMinPressTime)
        mLabelMinTime.text = texts

        texts.clear()
        texts.append(context.getString(R.string.smaxtime), " ", MainActivity.mConfigParams.mMaxPressTime)
        mLabelMaxTime.text = texts*/
        isFixed (mIsFixed.isChecked)
    }

    private fun isFixed (checked: Boolean){
        if (checked){
            mFixedValue.isEnabled = true
            mMinButton.isEnabled = false
            mMaxButton.isEnabled = false
        }
        else {
            mFixedValue.isEnabled = false
            mMinButton.isEnabled = true
            mMaxButton.isEnabled = true
        }
    }

    private fun acceptChanges (){
        val cf = MainActivity.mConfigParams
        if (!mIsFixed.isChecked && (mMaxButton.mPress == mMinButton.mPress ))
            mIsFixed.isChecked = true
        
        cf.mIsFixedVelocity = mIsFixed.isChecked
        val velocity = mFixedValue.text.toString().toInt()
        if (velocity > MidiHelper.MAXMIDIVALUE){
            val msg = context.getString(R.string.svalexceedmessage).replace(ReplaceLabels.FIELDLABEL,
                context.getString(R.string.svelocity)).replace(ReplaceLabels.VALUELABEL,
                    MidiHelper.MAXMIDIVALUE.toString())
            showErrorDialog(context, resources.getString(R.string.smidivalexceeded),
                msg)
            mFixedValue.requestFocus()
            return
        }
        cf.mFixedVelocity = velocity
        cf.mMinPress = mMinButton.mPress
        cf.mMaxPress = mMaxButton.mPress

        /*cf.mMinPressTime = mMinButton.m_time
        cf.mMaxPressTime = mMaxButton.m_time*/
        cf.computePressureparams()
        cancel()
    }
    private fun cancel (){
        (context as MainActivity).goBack()
    }

}