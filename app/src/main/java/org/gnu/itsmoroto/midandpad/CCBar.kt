package org.gnu.itsmoroto.midandpad

import abak.tr.com.boxedverticalseekbar.BoxedVertical
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView

class CCBar : BoxedVertical, BoxedVertical.OnValuesChangeListener {
    private lateinit var mLabel: TextView
    constructor(context: Context?): super (context){
        setOnClick()
    }
    constructor(context: Context?, attributeSet: AttributeSet): super (context, attributeSet){
        setOnClick ()
    }



    var mNumber: Int = 0
    var mName: String = ""
    var mChannel: Int = MidandpadDB.DEFAULT_CHANNEL
    private var mControl: Int = 0 //Special controls, like pitch bend are negatives.
    var mRZ: Boolean = false
    var mZeroPos: Int = 0
    private var mCurrpos: Int = 0
    var misEdit: Boolean = false
    public fun setLabelWidget (label:TextView){
        mLabel = label
    }

    public fun setName (name: String){
        mLabel.text = name
        mName = name
    }

    private fun setOnClick (){
        setOnBoxedPointsChangeListener(this)
    }

    companion object {
        const val DO_NOTHING = -2
        const val PITCH_BEND = -1 //zero on msb 64 and lsb 0 (msb and lsb are 7bit bytes)
        const val PITCHCENTER = 0x2000
        const val PITCHCENTERU: UByte = 0x40U
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onPointsChanged(boxedPoints: BoxedVertical?, points: Int) {
        if (MainActivity.mConfigParams.mMode == ConfigParams.RUN_MODE) {
            var channel: UByte = MainActivity.mConfigParams.mDefaultChannel
            if (mChannel != MidandpadDB.DEFAULT_CHANNEL)
                channel = mChannel.toUByte()
            if (mControl != PITCH_BEND) {
                val command: UByte = MidiHelper.STATUS_CONTROL_CHANGE or channel
                val msg = ubyteArrayOf(mControl.toUByte(), points.toUByte()
                )
                MainActivity.mMidi.send(command, msg.toByteArray())
            }
            else {
                //Log.v (ConfigParams.MODULE, "BAR: ${points and 0x7F}, ${points ushr 7}")
                val lsb = (points and 0x7F).toUByte()
                val msb = (points ushr 7).toUByte()
                val command: UByte = MidiHelper.STATUS_PITCH_BEND or channel
                val msg = ubyteArrayOf(lsb, msb)
                MainActivity.mMidi.send(command, msg.toByteArray())
            }
        }
    }

    override fun onStartTrackingTouch(boxedPoints: BoxedVertical?) {
        if (boxedPoints != null)
            mCurrpos = boxedPoints.value
        return
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onStopTrackingTouch(boxedPoints: BoxedVertical?) {
        if (MainActivity.mConfigParams.mMode == ConfigParams.EDIT_MODE
            && !misEdit) {
            misEdit = true
            editMe()
            /*if (boxedPoints != null)
                boxedPoints.value = m_currpos*/
        }
        else {
            if (mControl == PITCH_BEND){
                val channel = if (mChannel != MidandpadDB.DEFAULT_CHANNEL) mChannel.toUByte()
                    else MainActivity.mConfigParams.mDefaultChannel
                val command: UByte = MidiHelper.STATUS_PITCH_BEND or channel
                val msg = ubyteArrayOf(0U, PITCHCENTERU)
                MainActivity.mMidi.send(command, msg.toByteArray())
                value = PITCHCENTER.toInt()
            }
            else if (mRZ) {
                val channel = if (mChannel != MidandpadDB.DEFAULT_CHANNEL) mChannel.toUByte()
                    else MainActivity.mConfigParams.mDefaultChannel

                val command: UByte = MidiHelper.STATUS_CONTROL_CHANGE or channel
                val msg = ubyteArrayOf(mControl.toUByte(), mZeroPos.toUByte()
                )
                MainActivity.mMidi.send(command, msg.toByteArray())
                value = mZeroPos
            }

        }
    }

    fun editMe () {
        if (MainActivity.mConfigParams.mMode == ConfigParams.EDIT_MODE){
            (context as MainActivity).mBarConfigScreen.setBar(this)
            (context as MainActivity).changeView((context as MainActivity).mBarConfigScreen)
        }
    }


    fun setControl (control: Int) {
        if (control != PITCH_BEND){
            max = 0x7F
        }
        else {
            max = 0x3FFF
            value = PITCHCENTER
        }
        mControl = control
    }

    fun getControl (): Int{
        return mControl
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (MainActivity.mConfigParams.mMode == ConfigParams.EDIT_MODE &&
            event!!.action != MotionEvent.ACTION_UP)
            return true

        return super.onTouchEvent(event)
    }
}