package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog


class ButtonProgramCfg (context: Context) : PropertiesView (context) {

    private val mButtonProgram: EditText
    init {
        // Inflate the layout for this fragment
        inflate(context, R.layout.button_program, this)
        mButtonProgram = findViewById(R.id.buttonprogram)
    }

    override fun setProps(btn: EventButton) {
        mButtonProgram.text = btn.mNoteNumber.toEditable()
    }

    override fun getProps(btn: EventButton): Boolean {
        val programnumber = mButtonProgram.text.toString().toInt()
        if (programnumber < 0 || programnumber > MidiHelper.MAXMIDIVALUE){
            val msg = context.getString(R.string.svalexceedmessage).replace(ReplaceLabels.FIELDLABEL,
                context.getString(R.string.sprogram)).replace(ReplaceLabels.VALUELABEL,
                MidiHelper.MAXMIDIVALUE.toString())
            showErrorDialog(context, resources.getString(R.string.smidivalexceeded), msg)
            return false
        }
        btn.mNoteNumber = programnumber
        return true
    }

}