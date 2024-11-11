package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import java.util.Timer
import java.util.TimerTask


class EventButton : androidx.appcompat.widget.AppCompatButton {

    private class FlamPrimary(note: Int, vel:Int, channel: Int): TimerTask (){
        private val mNote = note
        private val mVel = vel
        private val mChannel = if (channel != -1) channel.toUByte()
            else MainActivity.mConfigParams.mDefaultChannel

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun run() {
            val command: UByte = MidiHelper.STATUS_NOTE_ON or mChannel
            val msg: Array<UByte> = arrayOf(mNote.toUByte(), mVel.toUByte())
            MainActivity.mMidi.send(command, msg.toUByteArray().toByteArray())
        }

    }

    private class RollNote(note: Int, vel:Int, channel: Int): TimerTask (){
        private val mNote = note
        private val mVel = vel
        private val mChannel = if (channel != -1) channel.toUByte()
        else MainActivity.mConfigParams.mDefaultChannel

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun run() {
            val command: UByte = MidiHelper.STATUS_NOTE_ON or mChannel
            val msg: Array<UByte> = arrayOf(mNote.toUByte(), mVel.toUByte())
            MainActivity.mMidi.send(command, msg.toUByteArray().toByteArray())
        }

    }

    /*constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int):
            super (context, attributeSet, defStyleAttr, defStyleRes){
                setOnClick()
            }*/


    /*public val EVENT_NOTE: Int = 0
    public val EVENT_CONTROL: Int = 1*/

    enum class NOTEOFFTYPES (val label: Int): TypeLabel{
        NOSENDOFF (R.string.snosendoff),
        SENDOFF (R.string.ssendoff),
        ROLL (R.string.sroll),
        FLAM (R.string.sflam);

        override fun getLabelId(): Int {
            return label
        }
    }

    enum class CONTROLOFFTYPES (val label: Int): TypeLabel{
        FIXED (R.string.sfixed),
        MOMENTARY (R.string.smomentary),
        TOGGLE (R.string.stoggle);
        override fun getLabelId(): Int {
            return label
        }
    }

    enum class CHORDOFFTYPES (val label:Int): TypeLabel {
        FULLOFF (R.string.sfulloff),
        FULLNOFF (R.string.sfullnoff),
        ARPEGGIOFF (R.string.sarpeggioff),
        ARPEGGIONOFF (R.string.sarpeggionoff);

        override fun getLabelId(): Int {
            return label
        }

    }


    var mNumber: Int = 0
    private var mTime: Long = 0
    private var mPress: Float = 0.0f
    private var mVel: Int = 0
    var mName: String = ""
    private var mType: MidiHelper.EventTypes = MidiHelper.EventTypes.EVENT_NOTE

    var mNoteNumber:Int = 0 //If EVENT_CONTROL
    //var m_sendOff:OFFTYPES = OFFTYPES.NOSENDOFF
    var mNoteOFF:NOTEOFFTYPES? = null
    var mControlOFF:CONTROLOFFTYPES? = null
    var mChordOFF:CHORDOFFTYPES? = null
    var mONValue:Int = 0
    var mOFFValue: Int = 0
    var mChannel: Int = MidandpadDB.DEFAULT_CHANNEL

    private var mClockTicks = 0
    private var mClicked: Boolean = false
    private val mOFFBG: Drawable = ResourcesCompat.getDrawable(resources,
        R.drawable.ccbutton_off_background, null)!!
    private val mONBG: Drawable = ResourcesCompat.getDrawable(resources,
        R.drawable.ccbutton_on_background, null)!!

    val mChordNotes = ArrayList<Int> ()
    var mRollNote: MidiHelper.NOTE_TIME = MidiHelper.NOTE_TIME.QUARTER
    private var mRollNoteTime: Int = 0
    private var mIsTriplet = false

    private var mInRoll: Boolean = false
    private val MAXFLAMTIME: Long = 30
    private var mNotePosition: Int = 0 //For arpeggios
    private val ON = 1
    private val OFF = 0
    private var mFlamTimer: Timer? = null
    private var mRollTimer: Timer? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val ret = super.onTouchEvent(event)
        if (event == null)
            return ret

        run {//It's a workarround as break does not exists in kotlin
            val p: Float = event.pressure
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mInRoll = false
                    if (MainActivity.mConfigParams.mMode == ConfigParams.EDIT_MODE)
                        return@run
                    if (mType == MidiHelper.EventTypes.EVENT_NOTE) {

                        mVel = MainActivity.mConfigParams.getVelocity(p)
                        when (mNoteOFF) {
                            NOTEOFFTYPES.NOSENDOFF,
                           /* ->{
                                mTime = event.eventTime
                                mPress = p
                                return true
                            }*/
                            NOTEOFFTYPES.SENDOFF,
                            NOTEOFFTYPES.FLAM-> {

                                if (mNoteOFF == NOTEOFFTYPES.FLAM) {
                                    sendMidi(mVel/2, ON)
                                    mFlamTimer = Timer ()
                                    mFlamTimer!!.schedule(
                                        FlamPrimary (mNoteNumber, mVel, mChannel), MAXFLAMTIME)
                                }
                                else
                                    sendMidi (mVel, ON)
                                return true
                            }
                            NOTEOFFTYPES.ROLL -> {
                                if (mRollNote == MidiHelper.NOTE_TIME.ROLL){
                                    startRoll ()
                                    return true
                                }
                                mInRoll = true
                                mClockTicks = mRollNoteTime
                                return true
                            }

                            null -> {
                                return@run
                            }
                        }
                    } else if (mType == MidiHelper.EventTypes.EVENT_CONTROL) {

                        when (mControlOFF){
                            CONTROLOFFTYPES.FIXED,
                            CONTROLOFFTYPES.TOGGLE ->{
                                return true
                            }
                            CONTROLOFFTYPES.MOMENTARY -> {
                                sendMidi (0, ON)
                                return true
                            }
                            null->
                                return@run
                        }
                    } else if (mType == MidiHelper.EventTypes.EVENT_PROGRAM){
                        return super.onTouchEvent(event)
                    }
                    else if (mType == MidiHelper.EventTypes.EVENT_CHORD){
                        mVel = MainActivity.mConfigParams.getVelocity(p)
                        when (mChordOFF){
                            CHORDOFFTYPES.FULLNOFF,
                                 /*-> {
                                mTime = event.eventTime
                                mPress = p
                                return true
                            }*/
                            CHORDOFFTYPES.FULLOFF -> {

                                sendMidi (mVel, ON)
                                return true

                            }
                            CHORDOFFTYPES.ARPEGGIOFF,
                            CHORDOFFTYPES.ARPEGGIONOFF -> {
                                mInRoll = true
                                mNotePosition = 0
                                mClockTicks = mRollNoteTime
                                return true
                            }
                            null->return@run
                        }
                    }

                }
                /*MotionEvent.ACTION_MOVE ->{
                    val p: Float = event.getPressure()
                    //b.count += p
                    Log.v(ConfigParams.module, "Presion mantengo " + p.toString())
                }*/
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_OUTSIDE,
                    -> {
                    mInRoll = false
                    if (MainActivity.mConfigParams.mMode == ConfigParams.EDIT_MODE)
                        return@run
                    if (mType == MidiHelper.EventTypes.EVENT_NOTE) {

                        when (mNoteOFF) {
                            NOTEOFFTYPES.NOSENDOFF
                                -> {
                                /*val t = event.eventTime - mTime
                                mVel = MainActivity.mConfigParams.getVelocity(mPress, t)
                                sendMidi(mVel, ON)*/
                                return true

                            }

                            NOTEOFFTYPES.SENDOFF -> {
                                sendMidi(mVel, OFF)
                                return true
                            }

                            NOTEOFFTYPES.ROLL->{
                                if (mRollTimer != null){
                                    mRollTimer?.cancel()
                                    mRollTimer = null
                                }
                            }
                            NOTEOFFTYPES.FLAM ->
                                return true

                            null->return@run

                        }
                    } else if (mType == MidiHelper.EventTypes.EVENT_PROGRAM) {
                        sendMidi(0, ON)
                        return true
                    } else if (mType == MidiHelper.EventTypes.EVENT_CONTROL){

                        when (mControlOFF){
                            CONTROLOFFTYPES.TOGGLE->{
                                mClicked = !mClicked
                                sendMidi(0, if (mClicked) ON else OFF)
                                setClicked()
                                return true
                            }
                            CONTROLOFFTYPES.FIXED-> {
                                sendMidi(0, ON)
                                return true
                            }
                            CONTROLOFFTYPES.MOMENTARY ->{
                                sendMidi(0, OFF)
                                return true
                            }
                            null->return@run
                        }
                    } else if (mType == MidiHelper.EventTypes.EVENT_CHORD){

                        when (mChordOFF){
                            CHORDOFFTYPES.FULLOFF -> {
                                sendMidi(mVel, OFF)
                                return true
                            }
                            CHORDOFFTYPES.FULLNOFF -> {
                                /*val t = event.eventTime - mTime
                                mVel = MainActivity.mConfigParams.getVelocity(mPress, t)
                                sendMidi(mVel, ON)*/
                                return true
                            }
                            CHORDOFFTYPES.ARPEGGIOFF,
                            CHORDOFFTYPES.ARPEGGIONOFF ->{
                                return super.onTouchEvent(event)
                            }
                            null->return@run
                        }
                    }
                }
            }
        }
        return ret
    }


    constructor(context: Context): super (context) {
        initialize ()
    }
    constructor(context: Context, attributeSet: AttributeSet): super (context, attributeSet) {
        initialize ()
    }
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int):
            super (context, attributeSet, defStyleAttr)
    {
        initialize ()
    }

    fun initialize (){
        background = ResourcesCompat.getDrawable(resources,
            R.drawable.notebutton_background, null)
        setOnClickListener { _->
            onclick ()
        }
    }

    private fun onclick (){
        if (MainActivity.mConfigParams.mMode != ConfigParams.EDIT_MODE)
            return
        (context as MainActivity).mButtonConfigScreen.setButton(this)
        (context as MainActivity).changeView((context as MainActivity).mButtonConfigScreen)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun sendMidi (vel: Int, type: Int){
        val msg = ArrayList<UByte> ()
        val channel = if (mChannel != -1) mChannel.toUByte() else MainActivity.mConfigParams.mDefaultChannel
        var command: UByte = 0U
        when (mType) {
            MidiHelper.EventTypes.EVENT_NOTE -> {
                command = if (type == ON)
                    MidiHelper.STATUS_NOTE_ON or channel.toUByte()
                else
                    MidiHelper.STATUS_NOTE_OFF or channel.toUByte()

                msg.add(mNoteNumber.toUByte())
                msg.add(vel.toUByte())
            }

            MidiHelper.EventTypes.EVENT_CHORD -> {
                command = if (type == ON)
                    MidiHelper.STATUS_NOTE_ON or channel.toUByte()
                else
                    MidiHelper.STATUS_NOTE_OFF or channel.toUByte()

                for (note in mChordNotes) {
                    msg.add(note.toUByte())
                    msg.add(vel.toUByte())
                }
            }

            MidiHelper.EventTypes.EVENT_PROGRAM -> {
                command = MidiHelper.STATUS_PROGRAM_CHANGE or channel.toUByte()
                msg.add(mNoteNumber.toUByte())
            }
            MidiHelper.EventTypes.EVENT_CONTROL->{
                command = MidiHelper.STATUS_CONTROL_CHANGE or channel.toUByte()
                msg.add(mNoteNumber.toUByte())
                if (type == ON)
                    msg.add(mONValue.toUByte())
                else
                    msg.add(mOFFValue.toUByte())
            }
        }
        MainActivity.mMidi.send(command, msg.toUByteArray().toByteArray())
    }


    private fun setClicked (){
        if (mClicked)
            background = mONBG
        else
            background = mOFFBG
    }
    public fun setName (name: String){
        mName = name
        text = mName
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun tick (){
        if (!mInRoll) {
            return
        }

        if (mRollTimer != null)
            return

        val msg = ArrayList<UByte> ()
        val channel:UByte = if (mChannel > -1) mChannel.toUByte()
            else MainActivity.mConfigParams.mDefaultChannel
        var command: UByte = 0U
        if (mInRoll) {
            mClockTicks++
            if (mClockTicks < mRollNoteTime)
                return
            mClockTicks = 0

            if (mType == MidiHelper.EventTypes.EVENT_NOTE){ //It's roll
                command = MidiHelper.STATUS_NOTE_ON or channel
                msg.add(mNoteNumber.toUByte())
                msg.add(mVel.toUByte())
                MainActivity.mMidi.send(command, msg.toUByteArray().toByteArray())
                return
            }
            //It's arpeggio

            command = MidiHelper.STATUS_NOTE_ON or channel
            if (mChordOFF == CHORDOFFTYPES.ARPEGGIOFF){
                val note = if (mNotePosition == 0) mChordNotes.last()
                    else mChordNotes[mNotePosition - 1]
                msg.add(note.toUByte())
                msg.add(0U)
            }
            msg.add(mChordNotes[mNotePosition].toUByte())
            msg.add(mVel.toUByte())
            MainActivity.mMidi.send(command, msg.toUByteArray().toByteArray())
            mNotePosition++
            if (mNotePosition >= mChordNotes.count())
                mNotePosition = 0
        }

    }


    fun setChordNotes (snotes: String){ //is a ; separated
        val notes = snotes.split (MidandpadDB.NOTE_SEPARATOR)
        if (notes.isEmpty())
            return
        mChordNotes.clear()
        for (n in notes){
            try {
                if (n.isEmpty())
                    continue
                val note: Int = n.toInt()
                mChordNotes.add(note)
            }
            catch (e: Exception){ //Should not happen
                continue
            }

        }
    }

    fun getChordNotes (): String {
        return mChordNotes.joinToString(separator = MidandpadDB.NOTE_SEPARATOR)
    }
    fun setTriplet (istriplet: Int){
        setTriplet(if (istriplet == 1) true else false)
    }

    fun setTriplet (istriplet: Boolean){
        mRollNoteTime = if (mRollNote.multiplier < 0) {
            (-mRollNote.multiplier)*MainActivity.mConfigParams.mPPQ
        } else{
            MainActivity.mConfigParams.mPPQ/mRollNote.multiplier
        }
        if (istriplet)
            mRollNoteTime = mRollNoteTime * 2 /3

        mIsTriplet = istriplet

    }

    fun getIsTriplet (): Boolean{
        return mIsTriplet
    }

    fun getmType (): MidiHelper.EventTypes {
        return mType
    }

    fun setmType (type: MidiHelper.EventTypes) {
        val getd: (Int)-> Drawable? = {resid->
            ResourcesCompat.getDrawable(resources, resid, null)
        }
        mType = type
        when (type){
            MidiHelper.EventTypes.EVENT_NOTE -> {
                background = getd (R.drawable.notebutton_background)
            }
            MidiHelper.EventTypes.EVENT_CONTROL -> {
                background = getd (R.drawable.ccbutton_off_background)
            }
            MidiHelper.EventTypes.EVENT_PROGRAM -> {
                background = getd (R.drawable.programbutton_background)
            }
            MidiHelper.EventTypes.EVENT_CHORD -> {
                background = getd (R.drawable.chordbutton_background)
            }
        }
    }

    private fun startRoll (){
        mRollTimer = Timer ()
        mRollTimer!!.schedule(
            RollNote (mNoteNumber, mVel, mChannel), 0, MAXFLAMTIME)
    }

}