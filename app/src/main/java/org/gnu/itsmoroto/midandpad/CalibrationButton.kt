package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView

class CalibrationButton: androidx.appcompat.widget.AppCompatButton {
    private var mPressLabel: TextView? = null
    private var mPressPrefix: Int? = 0
    /*private var mTimeLabel: TextView? = null
    private var mTimePrefix: Int?= 0*/

    constructor(context: Context): super (context) {
        addOnClick ()
    }
    constructor(context: Context, attributeSet: AttributeSet): super (context, attributeSet) {
        addOnClick ()
    }
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int):
            super (context, attributeSet, defStyleAttr)
    {
        addOnClick ()
            }
    /*constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int):
            super (context, attributeSet, defStyleAttr, defStyleRes){

            }*/


    var mPress: Float = 0f
    var mTime: Int = 0

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when (event?.action){
            MotionEvent.ACTION_DOWN ->{
                mTime = event.eventTime.toInt()
                mPress = event.getPressure()
            }
            /*MotionEvent.ACTION_MOVE ->{
                val p: Float = event.getPressure()
                //b.count += p
                Log.v(ConfigParams.module, "Presion mantengo " + p.toString())
            }*/
            MotionEvent.ACTION_UP ->{
                mTime = (event.eventTime - mTime).toInt()
            }
            MotionEvent.ACTION_OUTSIDE ->{
                mTime = (event.eventTime - mTime).toInt()
            }
        }
        return super.onTouchEvent(event) //May force to false
    }

    fun setLabels (presslabel: TextView, /*timelabel: TextView,*/ pressprefix: Int/*,
                   timeprefix: Int*/){
        mPressLabel = presslabel
        mPressPrefix = pressprefix
        /*mTimeLabel = timelabel
        mTimePrefix = timeprefix*/
    }
    private fun addOnClick (){
        setOnClickListener { _->
            if (mPressLabel != null /*&& mTimeLabel != null*/) {
                val texts = StringBuilder()
                texts.clear()
                texts.append(context.getString(mPressPrefix!!), " ", mPress)
                mPressLabel!!.text = texts

                /*texts.clear()
                texts.append(context.getString(mTimePrefix!!), " ", m_time)
                mTimeLabel!!.text = texts*/
            }
        }
    }
}