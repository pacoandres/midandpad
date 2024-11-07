package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout

class BtnConfigScreen(context: Context): ConstraintLayout(context)  {
    private val mNameText: EditText
    private val mTypeSelect: Spinner
    private val mChannelSelect: Spinner

    private val mPropsContainer: FrameLayout


    private var mCurrButton: EventButton? = null

    private val mTypeAdapter: TypeLabelAdapter<MidiHelper.EventTypes> =
        TypeLabelAdapter<MidiHelper.EventTypes>(context, R.layout.comboview,
            MidiHelper.EventTypes.entries.toTypedArray())

    private val mChannelAdapter: ArrayAdapter<String> = ArrayAdapter<String>(context,
        R.layout.comboview)

    private val mOKButton:AppCompatButton
    private val mCancelButton:AppCompatButton

    private var mCurrPropsView: PropertiesView? = null

    private val mTitle: TextView
    init {
        inflate(context, R.layout.buttonconfig, this)
        mTitle = findViewById(R.id.labelbuttonconfig)
        mNameText = findViewById(R.id.buttonname)
        mTypeSelect  = findViewById(R.id.buttontype)

        mChannelSelect = findViewById(R.id.buttonchannel)


        mOKButton = findViewById(R.id.buttonpropok)
        mCancelButton = findViewById(R.id.buttonpropcancel)
        mCancelButton.setOnClickListener { _: View ->
            onBack ()
        }
        mOKButton.setOnClickListener { _: View ->
            acceptChanges ()
        }

        mPropsContainer = findViewById(R.id.propscontainer)
        fillSpinners()
    }
    private fun fillSpinners() {

        mTypeAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mTypeSelect.adapter = mTypeAdapter
        setTypeChanger ()

        mChannelAdapter.add(resources.getString(R.string.sdefault))
        for (i in 1..16){
            mChannelAdapter.add (i.toString())
        }
        mChannelAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mChannelSelect.adapter = mChannelAdapter

    }
    private fun removePropsView (){
        if (mCurrPropsView != null){
            mPropsContainer.removeView(mCurrPropsView)
        }
        mCurrPropsView = null
    }

    private fun setPropsView (v: PropertiesView){
        if (mCurrPropsView != null)
            removePropsView()
        v.setProps(mCurrButton!!)
        mPropsContainer.addView(v)
        mCurrPropsView = v
    }

    private fun choosePropsView (type: MidiHelper.EventTypes){
        val propsView: PropertiesView = when (type){
            MidiHelper.EventTypes.EVENT_CONTROL->{
                ButtonControlCfg (context)
            }

            MidiHelper.EventTypes.EVENT_PROGRAM->{
                ButtonProgramCfg (context)
            }

            MidiHelper.EventTypes.EVENT_NOTE->{
                ButtonNoteCfg (context)
            }

            MidiHelper.EventTypes.EVENT_CHORD->{
                ButtonChordCfg (context)
            }
        }
        setPropsView(propsView)
    }

    private fun setTypeChanger (){
        mTypeSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected (parent: AdapterView<*>?, v: View?,
                                         pos: Int, id: Long){
                val selected: MidiHelper.EventTypes =
                    parent!!.getItemAtPosition(pos) as MidiHelper.EventTypes
                choosePropsView(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //Should not happen
            }
        }
    }


    private fun acceptChanges () {
        if (mCurrButton == null || mCurrPropsView == null) //Should not happen
            return
        val selected: MidiHelper.EventTypes = mTypeSelect.selectedItem as MidiHelper.EventTypes
        mCurrButton!!.mName = mNameText.text.toString()
        mCurrButton!!.setmType(selected)
        mCurrButton!!.mChannel = mChannelSelect.selectedItemPosition - 1
        if (!mCurrPropsView!!.getProps(mCurrButton!!))
            return
        onBack()
    }

    fun setButton (btn: EventButton){
        val title = StringBuilder ()
        title.append(context.getString(R.string.sbuttonconfig), " ",
            btn.mNumber/10, " ", btn.mNumber%10)
        mTitle.text = title
        mCurrButton = btn
        mNameText.text = btn.mName.toEditable()
        mTypeSelect.setSelection(btn.getmType().ordinal)
        mChannelSelect.setSelection(btn.mChannel + 1)
        choosePropsView(btn.getmType())
    }
    private fun onBack (){
        mCurrButton = null
        (context as MainActivity).goBack()
    }


}