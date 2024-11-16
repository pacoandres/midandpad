package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView


class ButtonChordCfg (context: Context) : PropertiesView (context) {
    private val mChordType: Spinner
    private val mChordTypeAdapter: TypeLabelAdapter<EventButton.CHORDOFFTYPES>

    private val mLabelChordTime: TextView
    private val mChordTime: Spinner
    private val mChordTimeAdapter: TypeNoteAdapter

    private val mIsTriplet: CheckBox
    private val mContainer: LinearLayout

    private val mNote1: Spinner
    private val mNote2: Spinner
    private val mNote1Adapter: ArrayAdapter<String>
    private val mNote2Adapter: ArrayAdapter<String>
    private val mButtonPlus: Button

    private val mDisclaimer: TextView
    private val mNoteToggle: CheckBox

    private var mCurrBtn: EventButton? = null
    private val mNotes: LinkedHashMap<Button, NoteRow> = LinkedHashMap<Button, NoteRow>()
    init {
        // Inflate the layout for this fragment
        inflate(context, R.layout.button_chord, this)
        mChordType = findViewById(R.id.chordtype)
        mChordTypeAdapter = TypeLabelAdapter<EventButton.CHORDOFFTYPES> (context,
            R.layout.comboview, EventButton.CHORDOFFTYPES.entries.toTypedArray())
        mChordTypeAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mChordType.adapter = mChordTypeAdapter

        setChordTypeHandler ()

        mLabelChordTime = findViewById(R.id.labelchordtime)
        mChordTime = findViewById(R.id.chordtime)
        mChordTimeAdapter = TypeNoteAdapter (context)
        initNoteTimeAdapter()
        mChordTimeAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mChordTime.adapter = mChordTimeAdapter

        mIsTriplet = findViewById(R.id.istripletcheck)
        mContainer = findViewById(R.id.notescontainer)

        mNote1 = findViewById(R.id.note1)
        mNote1Adapter = ArrayAdapter<String> (context, R.layout.comboview,
            MidiHelper.MIDINOTES)
        mNote1Adapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mNote1.adapter = mNote1Adapter

        mNote2 = findViewById(R.id.note2)
        mNote2Adapter = ArrayAdapter<String> (context, R.layout.comboview,
            MidiHelper.MIDINOTES)
        mNote2Adapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mNote2.adapter = mNote2Adapter

        mButtonPlus = findViewById(R.id.buttonplus)
        mButtonPlus.setOnClickListener (::newNote)
        mDisclaimer = findViewById(R.id.chorddiscalimer)
        mNoteToggle = findViewById(R.id.ischordtoggle)
    }

    fun initNoteTimeAdapter () { //No roll in apeggio.
        for (t in MidiHelper.NOTE_TIME.entries){
            if (t == MidiHelper.NOTE_TIME.ROLL)
                continue
            mChordTimeAdapter.add(t)
        }
    }

    fun newNote (v: View?): NoteRow{
        val newnote = NoteRow (context)
        newnote.mButtonPlus.setOnClickListener{b->
            newNote(b)
        }
        newnote.mButtonMinus.setOnClickListener {b->
            removeNote(b)
        }
        if (mNotes.isEmpty()){
            mButtonPlus.visibility = GONE
        }
        else {
            mNotes.entries.last().value.hidePlus()
        }
        newnote.showPlus()
        mNotes[newnote.mButtonMinus] = newnote
        mContainer.addView(newnote)
        return newnote
    }

    fun removeNote (v: View){
        val removednote: NoteRow = mNotes[v as Button] ?: return
        //val index = mNotes.entries.indexOf(removednote)

        if (mNotes.entries.last().value == removednote && mNotes.count() > 1){
            mNotes.remove(v)
            mNotes.entries.last().value.showPlus()
        }
        else{
            mNotes.remove(v)
            if (mNotes.isEmpty())
                mButtonPlus.visibility = VISIBLE
        }
        mContainer.removeView(removednote)
    }

    fun showTime (show: Int){
        mLabelChordTime.visibility = show
        mChordTime.visibility = show
        mIsTriplet.visibility = show
        mDisclaimer.visibility = show
    }
    override fun setProps(btn: EventButton) {
        val chordtype = btn.mChordOFF!!
        mCurrBtn = btn
        mNotes.clear()
        mChordType.setSelection(chordtype.ordinal)
        if (chordtype in setOf(EventButton.CHORDOFFTYPES.ARPEGGIOFF,
            EventButton.CHORDOFFTYPES.ARPEGGIONOFF)){
            showTime(VISIBLE)
            if (btn.mRollNote != MidiHelper.NOTE_TIME.ROLL)
                mChordTime.setSelection(btn.mRollNote.ordinal)
            else //Comes from ROLL, we have no roll
                mChordTime.setSelection(MidiHelper.NOTE_TIME.QUARTER.ordinal)

            mIsTriplet.isChecked = btn.getIsTriplet()
        }
        else {
            showTime(GONE)
        }
        if (btn.mChordNotes.isEmpty()){
            //TODO ("This is a problem. Should not happen")
            return
        }
        mNoteToggle.isChecked = btn.mNoteToggle
        showToggle(btn.mNoteToggle)
        mNote1.setSelection(btn.mChordNotes[0])
        mNote2.setSelection(btn.mChordNotes[1])
        for (i in 2..btn.mChordNotes.count()-1){
            val newnote = newNote(null)
            newnote.setNote(btn.mChordNotes[i])
        }
    }

    override fun getProps(btn: EventButton):Boolean {
        val type: EventButton.CHORDOFFTYPES = mChordType.selectedItem as EventButton.CHORDOFFTYPES
        btn.mChordOFF = type
        if (type in setOf(EventButton.CHORDOFFTYPES.ARPEGGIOFF,
                EventButton.CHORDOFFTYPES.ARPEGGIONOFF)){
            btn.mRollNote = mChordTime.selectedItem as MidiHelper.NOTE_TIME
            btn.setTriplet(mIsTriplet.isChecked)
        }
        btn.mChordNotes.clear()
        btn.mChordNotes.add (mNote1.selectedItemPosition)
        btn.mChordNotes.add (mNote2.selectedItemPosition)
        if (type.hasToggle)
            btn.mNoteToggle = mNoteToggle.isChecked
        else
            btn.mNoteToggle = false
        for (note in mNotes){
            btn.mChordNotes.add(note.value.getNote())
        }
        return true
    }
    private fun showToggle (show: Boolean){
        mNoteToggle.visibility = if (show) VISIBLE else GONE
    }
    private fun setChordTypeHandler () {
        mChordType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: View?,
                pos: Int, id: Long
            ) {
                val selected: EventButton.CHORDOFFTYPES =
                    parent!!.getItemAtPosition(pos) as EventButton.CHORDOFFTYPES
                if (selected in setOf(
                        EventButton.CHORDOFFTYPES.ARPEGGIOFF,
                        EventButton.CHORDOFFTYPES.ARPEGGIONOFF
                    )
                ) {
                    showTime(VISIBLE)
                    if (mCurrBtn != null) {
                        mChordTime.setSelection(mCurrBtn!!.mRollNote.ordinal)
                        mIsTriplet.isChecked = mCurrBtn!!.getIsTriplet()
                    }
                } else {
                    showTime(GONE)
                }
                showToggle(selected.hasToggle)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }
        }
    }


}