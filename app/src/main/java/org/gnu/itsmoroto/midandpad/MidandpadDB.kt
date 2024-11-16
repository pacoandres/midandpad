package org.gnu.itsmoroto.midandpad

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class MidandpadDB//else throw exception
    (context: Context?) : SQLiteOpenHelper(
    context, "midandpaddb", null,
    BuildConfig.dbversion
) {

    private lateinit var mContext: Context
    private val mName = "midandpaddb"
    private val mVersion = BuildConfig.dbversion

    companion object {
        val DEFAULT_CHANNEL = -1
        val NOTE_SEPARATOR = ";"
        val VERSION1 = 1
    }

    init {
        if (context != null)
            mContext = context
    }



    override fun onCreate(db: SQLiteDatabase?) {
        if (db == null)
            return;

        var q = "CREATE TABLE Presets (presetId INTEGER PRIMARY KEY" +
                ",name TEXT" +
                ",minpress REAL" +
                ",maxpress REAL" +
                ",minpresstime INTEGER" +
                ",maxpresstime INTEGER" +
                ",defchannel INTEGER" +
                ",defppq INTEGER" +
                ",isfixedvel INTEGER" +
                ",fixedvel INTEGER" +
                ")"
        db.execSQL(q)
        q = "CREATE TABLE Buttons (ButtonId INTEGER PRIMARY KEY," +
                "presetId INTEGER" +
                ",number INTEGER" +
                ",name TEXT" +
                ",type INTEGER" +
                ",noteoff INTEGER" +
                ",note INTEGER" +
                ",controloff INTEGER" +
                ",valueon INTEGER" +
                ",valueoff INTEGER" +
                ",channel INTEGER" + //-1 default preset channel
                ",chordoff INTEGER" +
                ",chordnotes TEXT default NULL" +
                ",rollnotetime INTEGER default 0" + //The NOTETIME ordinal, not the ticks
                ",triplet INTEGER default 0" + //1 is triplet.
                ",notetoggle INTEGER default 0"
                ")"
        db.execSQL(q)
        q = "CREATE TABLE Bars (BarId INTEGER PRIMARY KEY" +//Aquí me quedo
                ",presetId INTEGER" +
                ",number INTEGER" +
                ",name TEXT" +
                ",control INTEGER" +
                ",rz INTEGER" +
                ",zeropos INTEGER default 0" +
                ",defval INTEGER" +
                ")"
        db.execSQL(q)

        q = "CREATE TABLE Current (id INTEGER, presetId INTEGER)"
        db.execSQL(q)
        fillTables (db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null)
            return;
        //dbversion is configured in build.gradle.kts(:app)
        if (oldVersion < mVersion){
            //Do stuff
            if (oldVersion < newVersion){
                db.execSQL("ALTER TABLE Buttons ADD COLUMN notetoggle INTEGER default 0");
            }
        }
    }
    private fun fillTables (db: SQLiteDatabase){
        db.beginTransaction()
        val values = ContentValues ()
        try {
            values.put("presetId", 1)
            values.put("name", "default")
            values.put("minpress", 0)
            values.put("maxpress", 1)
            values.put("minpresstime", 0)
            values.put("maxpresstime", 10)
            values.put("isfixedvel", 1)
            values.put("fixedvel", 100)
            values.put("defchannel", 9)
            values.put("defppq", 24) //It's the MIDI standard for PPQ
            val presetid = db.insertOrThrow("Presets", null, values)

            values.clear()
            values.put("id", 1)
            values.put("presetId", presetid)
            db.insertOrThrow("Current", null, values)
            insertButtons(db, presetid)
            insertBars (db, presetid)

            db.setTransactionSuccessful()
        }
        catch (e: Exception){
            /*Log.v(ConfigParams.MODULE, "Exception populating default data")
            Log.v(ConfigParams.MODULE, e.toString())*/
            val msg = mContext.getString(R.string.sdbgenericerror)
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
        }
        finally {
            db.endTransaction()
        }

    }

    private fun insertBars(db: SQLiteDatabase, presetid: Long) {
        val values = ContentValues ()
        try {
            values.put("presetId", presetid)
            values.put("number", 1)
            values.put("name", "Volume")
            values.put("control", 7)
            values.put("rz", 0)
            values.put("zeropos", 0)
            values.put("defval", 80)
            db.insertOrThrow("Bars", null, values)

            values.put("presetId", presetid)
            values.put("number", 2)
            values.put("name", "Expression")
            values.put("control", 11)
            values.put("rz", 0)
            values.put("zeropos", 0)
            values.put("defval", 127)
            db.insertOrThrow("Bars", null, values)

            values.put("presetId", presetid)
            values.put("number", 3)
            values.put("name", "Reverb")
            values.put("control", 91)
            values.put("rz", 0)
            values.put("zeropos", 0)
            values.put("defval", 10)
            db.insertOrThrow("Bars", null, values)

            values.put("presetId", presetid)
            values.put("number", 4)
            values.put("name", "Chorus")
            values.put("control", 93)
            values.put("rz", 0)
            values.put("zeropos", 0)
            values.put("defval", 0)
            db.insertOrThrow("Bars", null, values)
        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.sinserterror).replace(
                ReplaceLabels.TABLE, "Bars")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
            throw e
        }
    }

    private fun insertButtons (db: SQLiteDatabase, presetid: Long) {
        val values = ContentValues ()
        try {
            values.put ("presetId", presetid)
            values.put("number", 11)
            values.put("name", "Kick")
            values.put("type", MidiHelper.EventTypes.EVENT_NOTE.ordinal)
            values.put("noteoff", EventButton.NOTEOFFTYPES.SENDOFF.ordinal)
            values.put ("note", 35)
            values.put("controloff", EventButton.CONTROLOFFTYPES.FIXED.ordinal)
            values.put("valueon", 127)
            values.put("valueoff", 0)
            values.put("channel", DEFAULT_CHANNEL)
            values.put("chordoff", EventButton.CHORDOFFTYPES.FULLOFF.ordinal)
            values.put("chordnotes", "60;64;67")
            values.put("rollnotetime", MidiHelper.NOTE_TIME.SIXTEENTH.ordinal)
            values.put("triplet", 0)
            values.put("notetoggle", 0)
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 12)
            values.put("name", "Stick")
            values.put ("note", 37)
            values.put("chordnotes", "61;65;68")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 13)
            values.put("name", "Snare")
            values.put ("note", 38)
            values.put("chordnotes", "62;66;69")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 14)
            values.put("name", "Floor tom")
            values.put ("note", 41)
            values.put("chordnotes", "63;67;70")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 21)
            values.put("name", "Closed HH")
            values.put ("note", 42)
            values.put("chordnotes", "64;68;71")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 22)
            values.put("name", "Pedal HH")
            values.put ("note", 44)
            values.put("chordnotes", "65;69;72")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 23)
            values.put("name", "Open HH")
            values.put ("note", 46)
            values.put("chordnotes", "66;70;73")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 24)
            values.put("name", "Crash")
            values.put ("note", 49)
            values.put("chordnotes", "67;71;74")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 31)
            values.put("name", "Low tom")
            values.put ("note", 45)
            values.put("chordnotes", "68;72;75")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 32)
            values.put("name", "Mid tom")
            values.put ("note", 47)
            values.put("chordnotes", "69;73;76")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 33)
            values.put("name", "High tom")
            values.put ("note", 50)
            values.put("chordnotes", "70;74;77")
            db.insertOrThrow("Buttons", null, values)

            values.put("number", 34)
            values.put("name", "Ride")
            values.put ("note", 51)
            values.put("chordnotes", "71;75;78")
            db.insertOrThrow("Buttons", null, values)
        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.sinserterror).replace(
                ReplaceLabels.TABLE, "Buttons")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
            throw e
        }
    }

    public fun getCurrPreset (): Long {
        val db = readableDatabase
        //val db = writableDatabase
        val currpreset: Long
        val cursor = db.query("Current", arrayOf<String>("presetId"),
            "id=?", arrayOf("1"), null, null,
            null, "1")
        cursor.moveToFirst()
        currpreset = cursor.getLong(0)
        cursor.close()
        db.close()
        return currpreset
    }

    public fun getPresetParams(presetid: Long, configParams: ConfigParams) {
        val db = readableDatabase
        val cursor = db.query ("Presets", arrayOf("name", "minpress", "maxpress", "defchannel",
            "minpresstime", "maxpresstime", "isfixedvel", "fixedvel", "defppq"),
            "presetId=?", arrayOf(presetid.toString()), null, null,
            null, "1")
        cursor.moveToFirst()
        configParams.mCurrPresetName = cursor.getString(0)

        configParams.mMinPress = cursor.getFloat(1)

        configParams.mMaxPress = cursor.getFloat(2)
        configParams.mDefaultChannel = cursor.getInt(3).toUByte()
        /*configParams.mMinPressTime = cursor.getInt(4)
        configParams.mMaxPressTime = cursor.getInt(5)*/
        configParams.mIsFixedVelocity = (cursor.getInt(6) == 1)
        configParams.mFixedVelocity = cursor.getInt(7)
        configParams.mPPQ = cursor.getInt(8)
        cursor.close()
        db.close()
    }

    fun configButtons(presetid: Long, buttons: Array<Array<EventButton>>) {
        val db = readableDatabase
        val cursor = db.query ("Buttons",
            arrayOf("number", "name", "type", "noteoff", "note", "valueon", "valueoff", "channel",
                "chordnotes", "rollnotetime", "triplet", "controloff", "chordoff", "notetoggle"),
            "presetId=?", arrayOf(presetid.toString()), null, null,
            null, null)
        cursor.moveToFirst()
        do {
            val number = cursor.getInt(0)
            val row:Int = (number/10) - 1
            val col:Int = (number%10) - 1
            buttons[row][col].mNumber = number
            buttons[row][col].setName(cursor.getString(1))
            buttons[row][col].setmType(MidiHelper.EventTypes.entries[cursor.getInt(2)])
            buttons[row][col].mNoteOFF =EventButton.NOTEOFFTYPES.entries[cursor.getInt(3)]
            buttons[row][col].mNoteNumber = cursor.getInt(4)
            buttons[row][col].mONValue = cursor.getInt(5)
            buttons[row][col].mOFFValue = cursor.getInt(6)
            buttons[row][col].mChannel = cursor.getInt(7)
            buttons[row][col].setChordNotes (cursor.getString(8))
            buttons[row][col].mRollNote = MidiHelper.NOTE_TIME.entries[cursor.getInt(9)]
            buttons[row][col].setTriplet (cursor.getInt(10))
            buttons[row][col].mControlOFF = EventButton.CONTROLOFFTYPES.entries[cursor.getInt(11)]
            buttons[row][col].mChordOFF = EventButton.CHORDOFFTYPES.entries[cursor.getInt(12)]
            buttons[row][col].mNoteToggle = (cursor.getInt(13) == 1)
        }while (cursor.moveToNext())
        cursor.close()
        db.close()
    }

    fun configBars(presetid: Long, bars: Array<CCBar>) {
        val db = readableDatabase
        val cursor = db.query ("Bars",
            arrayOf("number", "name", "control", "rz", "zeropos", "defval"),
            "presetId=?", arrayOf(presetid.toString()), null, null,
            null, null)
        cursor.moveToFirst()
        do {
            val number = cursor.getInt(0) -1
            bars[number].mNumber = number + 1
            bars[number].setName(cursor.getString(1))
            bars[number].mRZ = (cursor.getInt(3) == 1)
            bars[number].mZeroPos = cursor.getInt(4)
            bars[number].value = cursor.getInt(5)
            bars[number].setControl(cursor.getInt(2))
        }while (cursor.moveToNext())
        cursor.close()
        db.close()
    }

    fun getPresets (): ArrayList<PresetItem> {
        val ret: ArrayList<PresetItem> = ArrayList<PresetItem>()
        val db = readableDatabase
        val cursor = db.query ("Presets", arrayOf("presetId", "name"), null,
            null, null, null, null, null)
        cursor.moveToFirst()
        do {
            ret.add(PresetItem(cursor.getLong(0), cursor.getString(1)))
        } while (cursor.moveToNext())
        cursor.close()
        db.close()
        return ret
    }

    fun deletePreset (id: Long){
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (db.delete("Presets", "presetId=?", arrayOf(id.toString())) != 0) {
                db.delete("Buttons", "presetId=?", arrayOf(id.toString()))
                db.delete("Bars", "presetId=?", arrayOf(id.toString()))
                db.setTransactionSuccessful()
            }else {
                val msg = mContext.getString(R.string.sdeleteerror).replace(
                    ReplaceLabels.TABLE, "Presets") + "\n" +
                        "Error in delete: preset ${id} not found"
                showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                    msg)
            }


        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.sdeleteerror).replace(
                ReplaceLabels.TABLE, "Presets, Buttons, Bars")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
        }
        finally {
            db.endTransaction()
            db.close()
        }
    }

    fun renamePreset (id: Long, newname: String){
        val db = writableDatabase
        val newvalues: ContentValues = ContentValues ()
        newvalues.put("name", newname)
        db.beginTransaction()
        try {
            if (db.update("Presets", newvalues,"presetId=?", arrayOf(id.toString())) != 0) {
                db.setTransactionSuccessful()
            }
            else {
                val msg = mContext.getString(R.string.supdateerror).replace(
                    ReplaceLabels.TABLE, "Presets") + "\n" +
                        "Error in update: preset ${id} not found"
                showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                    msg)

            }
        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.supdateerror).replace(
                ReplaceLabels.TABLE, "Presets")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
        }
        finally {
            db.endTransaction()
            db.close()
        }
    }

    fun checkPresetName (name: String): Boolean {
        val db = writableDatabase
        val cursor = db.query ("Presets", arrayOf("presetId", "name"), "name=?",
            arrayOf(name), null, null, null, null)
        val ret = (cursor.count >0)
        cursor.close()
        db.close()
        return ret
    }

    fun savePreset (id: Long, buttons: Array<Array<EventButton>>, bars: Array<CCBar>){
        val db = writableDatabase
        val newpresetvalues = ContentValues ()
        val cf = MainActivity.mConfigParams
        try {
            db.beginTransaction()
            newpresetvalues.put("minpress", cf.mMinPress)
            newpresetvalues.put("maxpress", cf.mMaxPress)
            /*newpresetvalues.put("minpresstime", cf.mMinPressTime)
            newpresetvalues.put("maxpresstime", cf.mMaxPressTime)*/
            newpresetvalues.put("defchannel", cf.mDefaultChannel.toInt())
            newpresetvalues.put("isfixedvel", if (cf.mIsFixedVelocity) 1 else 0)
            newpresetvalues.put("fixedvel", cf.mFixedVelocity)
            newpresetvalues.put("defppq", cf.mPPQ)
            if (db.update("Presets", newpresetvalues, "presetId=?",
                arrayOf(id.toString())
            ) != 0) {
                updateButtons(db, id, buttons)
                updateBars(db, id, bars)
                db.setTransactionSuccessful()
            }
            else {
                val msg = mContext.getString(R.string.supdateerror).replace(
                    ReplaceLabels.TABLE, "Presets") + "\n" +
                        "Error in update: preset ${id} not found"
                showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                    msg)
            }

        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.supdateerror).replace(
                ReplaceLabels.TABLE, "Presets")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
        }
        finally {
            db.endTransaction()
            db.close()
        }
    }
    private fun updateButtons (db: SQLiteDatabase, id: Long, buttons: Array<Array<EventButton>>){
        val newvalues = ContentValues ()
        try {
            for (i in 0..2){
                for (j in 0..3){
                    val b = buttons[i][j]
                    val number = (i+1)*10 + j+1
                    newvalues.put("name", b.mName)
                    newvalues.put("type", b.getmType().ordinal)
                    newvalues.put("noteoff", b.mNoteOFF!!.ordinal)
                    newvalues.put("note", b.mNoteNumber)
                    newvalues.put("controloff", b.mControlOFF!!.ordinal)
                    newvalues.put("valueon", b.mONValue)
                    newvalues.put("valueoff", b.mOFFValue)
                    newvalues.put("channel", b.mChannel)
                    newvalues.put("chordoff", b.mChordOFF!!.ordinal)
                    newvalues.put("chordnotes", b.getChordNotes())
                    newvalues.put("rollnotetime", b.mRollNote.ordinal)
                    newvalues.put("triplet", if (b.getIsTriplet()) 1 else 0)
                    newvalues.put("notetoggle", if (b.mNoteToggle) 1 else 0)
                    if (db.update("Buttons", newvalues, "presetId=? and number=?",
                        arrayOf(id.toString(), number.toString())) == 0){
                        throw SQLiteException ("Error in update button: $id, $number")
                    }
                }
            }
        }
        catch (e: Exception){
            throw e
        }
    }

    private fun updateBars (db: SQLiteDatabase, id: Long, bars: Array<CCBar>){
        val newvalues = ContentValues ()
        try {
            for (i in 0..3){
                val number = i + 1
                val b = bars[i]
                newvalues.put("name", b.mName)
                newvalues.put("control", b.getControl())
                newvalues.put("rz", b.mRZ)
                newvalues.put("zeropos", b.mZeroPos)
                newvalues.put("defval", b.value)
                if (db.update("Bars", newvalues, "presetId=? and number=?",
                    arrayOf(id.toString(), number.toString())) == 0
                    ){
                    throw SQLiteException ("Error in update bar: $id, $number")
                }
            }
        }
        catch (e: Exception){
            throw e
        }
    }

    fun savePresetAs (name: String, buttons: Array<Array<EventButton>>, bars: Array<CCBar>):Long{
        val db = writableDatabase
        var ret = -1L
        val values = ContentValues ()
        val cf = MainActivity.mConfigParams
        try {
            db.beginTransaction()
            values.put("name", name)
            values.put("minpress", cf.mMinPress)
            values.put("maxpress", cf.mMaxPress)
            /*values.put("minpresstime", cf.mMinPressTime)
            values.put("maxpresstime", cf.mMaxPressTime)*/
            values.put("defchannel", cf.mDefaultChannel.toInt())
            values.put("isfixedvel", if (cf.mIsFixedVelocity) 1 else 0)
            values.put("fixedvel", cf.mFixedVelocity)
            values.put ("defppq", cf.mPPQ)
            val presetid = db.insertOrThrow("Presets", null, values)
            if (presetid != -1L) {
                saveButtons(db, presetid, buttons)
                saveBars(db, presetid, bars)
                ret = presetid
                db.setTransactionSuccessful()
            }
            else {
                val msg = mContext.getString(R.string.sinserterror).replace(
                    ReplaceLabels.TABLE, "Presets") + "\n" +
                        "Unknown error inserting in Presets"
                showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                    msg)
            }
        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.sinserterror).replace(
                ReplaceLabels.TABLE, "Presets")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
        }
        finally {
            db.endTransaction()
            db.close()
        }
        return ret
    }

    private fun saveButtons (db: SQLiteDatabase, id: Long, buttons: Array<Array<EventButton>>){
        val values = ContentValues ()
        values.put("presetId", id)
        try {
            for (i in 0..2){
                for (j in 0..3){
                    val b = buttons[i][j]
                    val number = (i+1)*10 + j+1
                    values.put("number", number)
                    values.put("name", b.mName)
                    values.put("type", b.getmType().ordinal)
                    values.put("noteoff", b.mNoteOFF!!.ordinal)
                    values.put("note", b.mNoteNumber)
                    values.put("controloff", b.mControlOFF!!.ordinal)
                    values.put("valueon", b.mONValue)
                    values.put("valueoff", b.mOFFValue)
                    values.put("channel", b.mChannel)
                    values.put("chordoff", b.mChordOFF!!.ordinal)
                    values.put("chordnotes", b.getChordNotes())
                    values.put("rollnotetime", b.mRollNote.ordinal)
                    values.put("triplet", if (b.getIsTriplet()) 1 else 0)
                    values.put("notetoggle", if (b.mNoteToggle) 1 else 0)
                    db.insertOrThrow("Buttons", null, values)
                }
            }
        }
        catch (e: Exception){
            throw e
        }
    }

    private fun saveBars (db: SQLiteDatabase, id: Long, bars: Array<CCBar>){
        val values = ContentValues ()
        values.put("presetId", id)
        try {
            for (i in 0..3) {
                val b = bars[i]
                values.put("number", i + 1)
                values.put("name", b.mName)
                values.put("control", b.getControl())
                values.put("rz", b.mRZ)
                values.put("zeropos", b.mZeroPos)
                values.put("defval", b.value)
                db.insertOrThrow("Bars", null, values)
            }
        }
        catch (e: Exception){
            throw e
        }
    }

    fun saveCurrent (){
        val db = writableDatabase
        val cf = MainActivity.mConfigParams
        val values = ContentValues ()
        try {
            db.beginTransaction()
            values.put("presetId", cf.mCurrPreset)
            if (db.update("Current", values, "id = 1", null) == 0
            ){
                showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                    mContext.getString(R.string.supdateerror)
                        .replace(ReplaceLabels.TABLE, "Current"))
            }
            db.setTransactionSuccessful()
        }
        catch (e: Exception){
            val msg = mContext.getString(R.string.supdateerror)
                .replace(ReplaceLabels.TABLE, "Current")
            showErrorDialog(mContext, mContext.getString(R.string.sdatabaseerror),
                msg, e)
        }
        finally {
            db.endTransaction()
        }
    }
}