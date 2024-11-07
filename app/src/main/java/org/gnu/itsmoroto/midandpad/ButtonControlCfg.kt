package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class ButtonControlCfg (context: Context): PropertiesView (context) {

    private val mButtonControl: EditText

    private val mTypeSelect:Spinner
    private val mTypeAdapter: TypeLabelAdapter<EventButton.CONTROLOFFTYPES>

    private val mControlON: EditText
    private val mControlOFF: EditText

    private val mLabelControlOFF: TextView

    init{
        inflate(context, R.layout.button_control, this)
        mButtonControl = findViewById(R.id.buttoncontrol)


        mTypeSelect = findViewById(R.id.controltype)
        mTypeAdapter = TypeLabelAdapter<EventButton.CONTROLOFFTYPES> (context,
            R.layout.comboview, EventButton.CONTROLOFFTYPES.entries.toTypedArray())
        mTypeSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: View?,
                pos: Int, id: Long
            ) {
                if (mTypeAdapter.getItem(pos) != EventButton.CONTROLOFFTYPES.FIXED){
                    showOFF (VISIBLE)
                }
                else
                    showOFF (GONE)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }
        }
        mControlON = findViewById(R.id.buttoncontrolon)

        mLabelControlOFF = findViewById(R.id.labelcontroloff)
        mControlOFF = findViewById(R.id.buttoncontroloff)
        fillCombos ()
    }

    private fun fillCombos (){

        mTypeAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mTypeSelect.adapter = mTypeAdapter

    }

    private fun showOFF (vis: Int){
        mLabelControlOFF.visibility = vis
        mControlOFF.visibility = vis

    }
    override fun setProps(btn: EventButton) {
        mButtonControl.text = btn.mNoteNumber.toEditable()

        mTypeSelect.setSelection(btn.getmType().ordinal)

        mControlON.text = btn.mONValue.toEditable()
        mControlOFF.text = btn.mOFFValue.toEditable()

        if (btn.mControlOFF != EventButton.CONTROLOFFTYPES.FIXED){
            showOFF(GONE)
        }
        else {
            showOFF(VISIBLE)
        }

    }

    override fun getProps(btn: EventButton): Boolean {
        val controlnumber: Int = mButtonControl.text.toString().toInt()
        val controlon: Int = mControlON.text.toString().toInt()
        val controloff: Int = mControlOFF.text.toString().toInt()
        var controlwidget: View? = null
        var msg:String = ""
        if (controlnumber < 0 || controlnumber > MidiHelper.MAXMIDIVALUE) {
            msg = context.getString(R.string.svalexceedmessage).replace(
                ReplaceLabels.FIELDLABEL,
                context.getString(R.string.scc)
            ).replace(
                ReplaceLabels.VALUELABEL,
                MidiHelper.MAXMIDIVALUE.toString()
            )
            controlwidget = mButtonControl
        }
        else if (controlon < 0 || controlon > MidiHelper.MAXMIDIVALUE){
            msg = context.getString(R.string.svalexceedmessage).replace(
                ReplaceLabels.FIELDLABEL,
                context.getString(R.string.svalueon)
            ).replace(
                ReplaceLabels.VALUELABEL,
                MidiHelper.MAXMIDIVALUE.toString()
            )
            controlwidget = mControlON
        }
        else if (controloff < 0 || controloff > MidiHelper.MAXMIDIVALUE){
            msg = context.getString(R.string.svalexceedmessage).replace(
                ReplaceLabels.FIELDLABEL,
                context.getString(R.string.svalueoff)
            ).replace(
                ReplaceLabels.VALUELABEL,
                MidiHelper.MAXMIDIVALUE.toString()
            )
            controlwidget = mControlOFF
        }

        if (msg != ""){
            AlertDialog.Builder (context, androidx.appcompat.R.style.AlertDialog_AppCompat)
                .setTitle(R.string.smidivalexceeded)
                .setMessage(msg)
                .setPositiveButton(R.string.sok) {_,_->
                    return@setPositiveButton
                }
                .show()
            controlwidget!!.requestFocus()
            return false
        }
        btn.mNoteNumber = controlnumber
        btn.mControlOFF = mTypeSelect.selectedItem as EventButton.CONTROLOFFTYPES
        btn.mONValue = controlon
        btn.mOFFValue = controloff
        return true
    }
}