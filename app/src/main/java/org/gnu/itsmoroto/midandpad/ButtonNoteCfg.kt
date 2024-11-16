package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView

class ButtonNoteCfg (context: Context): PropertiesView (context) {

    private val mNoteSelect:Spinner
    private val mNoteAdapter: ArrayAdapter<String>


    private val mTypeSelect:Spinner
    private val mTypeAdapter: TypeLabelAdapter<EventButton.NOTEOFFTYPES>

    private val mLabelRollLength: TextView
    private val mRollLength:Spinner
    private val mRollLengthAdapter: TypeNoteAdapter

    private val mIsTriplet: CheckBox

    private val mClockDisclaimer: TextView
    private val mNoteToggle: CheckBox


    init {
        inflate (context, R.layout.button_note, this)

        mNoteSelect = findViewById(R.id.buttonnote)
        mNoteAdapter = ArrayAdapter<String>(context, R.layout.comboview,
            MidiHelper.MIDINOTES)

        mTypeSelect = findViewById(R.id.notebehaviour)
        mTypeAdapter = TypeLabelAdapter<EventButton.NOTEOFFTYPES> (context, R.layout.comboview,
            EventButton.NOTEOFFTYPES.entries.toTypedArray())

        mLabelRollLength = findViewById(R.id.labelrollduration)
        mRollLength = findViewById(R.id.rollduration)
        mRollLengthAdapter = TypeNoteAdapter(context, MidiHelper.NOTE_TIME.entries.toTypedArray())

        mIsTriplet = findViewById(R.id.istripletcheck)
        mClockDisclaimer = findViewById(R.id.noteclockdiscalimer)
        mNoteToggle = findViewById(R.id.isnotetoggle)
        fillCombos ()
    }

    private fun fillCombos (){

        mNoteAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mNoteSelect.adapter = mNoteAdapter

        mTypeAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mTypeSelect.adapter = mTypeAdapter
        mTypeSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: View?,
                pos: Int, id: Long
            ) {
                if (mTypeAdapter.getItem(pos) == null)
                    return
                if (mTypeAdapter.getItem(pos) != EventButton.NOTEOFFTYPES.ROLL){
                    showOFF (GONE)
                }
                else
                    showOFF (VISIBLE)

                showToggle(mTypeAdapter.getItem(pos)!!.hasToggle)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }
        }


        mRollLengthAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mRollLength.adapter = mRollLengthAdapter
    }
    private fun showToggle (show: Boolean){
        if (show)
            mNoteToggle.visibility = VISIBLE
        else
            mNoteToggle.visibility = GONE
    }
    private fun showOFF (vis: Int){
        mLabelRollLength.visibility = vis
        mRollLength.visibility = vis
        mIsTriplet.visibility = vis
        mClockDisclaimer.visibility = vis

    }
    override fun setProps(btn: EventButton) {
        mNoteSelect.setSelection(btn.mNoteNumber)

        mTypeSelect.setSelection((btn.mNoteOFF as EventButton.NOTEOFFTYPES).ordinal)

        mRollLength.setSelection(btn.mRollNote.ordinal)
        mIsTriplet.isChecked = btn.getIsTriplet()
        if (btn.mNoteOFF == EventButton.NOTEOFFTYPES.ROLL){
            showOFF(VISIBLE)
        }
        else {
            showOFF(GONE)
        }
        mNoteToggle.isChecked = btn.mNoteToggle
        showToggle(btn.mNoteOFF!!.hasToggle)

    }

    override fun getProps(btn: EventButton): Boolean {
        btn.mNoteNumber = mNoteSelect.selectedItemPosition
        btn.mNoteOFF = mTypeSelect.selectedItem as EventButton.NOTEOFFTYPES
        btn.mRollNote = mRollLength.selectedItem as MidiHelper.NOTE_TIME
        btn.setTriplet(mIsTriplet.isChecked)
        if (btn.mNoteOFF!!.hasToggle)
            btn.mNoteToggle = mNoteToggle.isChecked
        else
            btn.mNoteToggle = false
        return true
    }
}