package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.util.Log
import android.widget.Toast

class ConfigParams (
    context: Context?
) {
    //Open database and read params
    private var mDatabase: MidandpadDB = MidandpadDB (context)

    companion object {
        const val MODULE: String = "midandpad"
        const val RUN_MODE = 0
        const val EDIT_MODE = 1
        const val TOASTLENGTH = Toast.LENGTH_SHORT
    }
    var mMinPress: Float = 0f
    var mMaxPress: Float = 1f
    /*var mMinPressTime: Int = 0
    var mMaxPressTime: Int = 10*/
    var mFixedVelocity: Int = 100
    var mIsFixedVelocity: Boolean = true
    var mDefaultChannel: UByte = 0x09.toUByte ()
    var mPPQ: Int = 120
    var mCurrPresetName: String = "default"
    var mMode = RUN_MODE
    var mCurrPreset: Long = 0
    init {
        mCurrPreset = mDatabase.getCurrPreset();
        loadPreset (mCurrPreset)
    }

    private var mPressSlope: Float = 0f
    private var mPressOrigin: Float = 0f

    /*private var mPressTimeSlope: Float = 0f
    private var mPressTimeOrigin: Float = 0f*/
    fun computePressureparams (){
        if (mIsFixedVelocity)
            return
        mPressSlope = (127f - 10f)/(mMaxPress-mMinPress)
        mPressOrigin = 10f - mPressSlope*mMinPress

        /*mPressTimeSlope = (127f - 10f)/(mMaxPress*mMaxPressTime - mMinPress*mMinPressTime)
        mPressTimeOrigin = 10f - mPressTimeSlope*mMinPress*mMinPressTime*/
    }

    fun getVelocity (press: Float): Int {
        if (mIsFixedVelocity)
            return mFixedVelocity

        var vel = mPressSlope*press + mPressOrigin

        if (vel > 127f)
            vel = 127f
        else if (vel < 0f)
            vel = 0f

        return vel.toInt()
    }

    /*fun getVelocity (press: Float, presstime: Long): Int{
        if (mIsFixedVelocity)
            return mFixedVelocity
        var vel = mPressTimeSlope*press*presstime + mPressTimeOrigin
        if (vel > 127f)
            vel = 127f
        else if (vel < 0f)
            vel = 0f
        return vel.toInt()
    }*/
    
    fun loadPreset (presetId: Long){
        mCurrPreset = presetId
        mDatabase.getPresetParams (mCurrPreset, this)
    }
    public fun configButtons (buttons: Array<Array<EventButton>>){
        mDatabase.configButtons (mCurrPreset, buttons)
    }

    public fun configBars(bars: Array<CCBar>) {
        mDatabase.configBars (mCurrPreset, bars)
    }

    public fun getPresets (): ArrayList<PresetItem> {
        return mDatabase.getPresets ()
    }

    fun deletePreset (id: Long){
        mDatabase.deletePreset (id)
    }

    fun renamePreset (id: Long, newname: String){
        mDatabase.renamePreset (id, newname)
    }

    fun checkPresetName (name: String): Boolean{
        return mDatabase.checkPresetName(name)
    }

    fun savePreset (buttons: Array<Array<EventButton>>, bars: Array<CCBar>){
        mDatabase.savePreset (mCurrPreset, buttons, bars);
    }

    fun savePresetAs (name: String, buttons: Array<Array<EventButton>>, bars: Array<CCBar>):
    Boolean{
        val now = mCurrPreset
        try {
            mCurrPreset = mDatabase.savePresetAs(name, buttons, bars)
            if (mCurrPreset != -1L)
                return true
            mCurrPreset = now
            return false
        }
        catch (e: Exception){
            mCurrPreset = now
            Log.v(ConfigParams.MODULE, "Exception saving new preset")
            Log.v(ConfigParams.MODULE, e.toString())
            return false
        }
    }

}