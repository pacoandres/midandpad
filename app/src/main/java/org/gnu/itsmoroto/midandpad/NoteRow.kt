package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner

class NoteRow (context: Context): LinearLayout (context){
    private val mSelectNote: Spinner
    private val mSelectNoteAdapter: ArrayAdapter<String>
    val mButtonPlus: Button
    val mButtonMinus: Button

    init {
        inflate(context, R.layout.noterow, this)
        mSelectNote = findViewById(R.id.noten)
        mButtonPlus = findViewById(R.id.buttonplusn)
        mButtonMinus = findViewById(R.id.buttonminusn)
        mSelectNoteAdapter = ArrayAdapter<String>(context, R.layout.comboview,
            MidiHelper.MIDINOTES)
        mSelectNoteAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mSelectNote.adapter = mSelectNoteAdapter
    }

    fun hidePlus (){
        mButtonPlus.visibility = GONE
    }

    fun showPlus (){
        mButtonPlus.visibility = VISIBLE
    }

    fun getNote (): Int{
        return mSelectNote.selectedItemPosition
    }

    fun setNote (note: Int){
        mSelectNote.setSelection(note)
    }
}