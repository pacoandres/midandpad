package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout

class BarConfigScreen(context: Context): ConstraintLayout(context) {
    private val mCancelButton: AppCompatButton
    private val mOKButton: AppCompatButton
    private val mTextName: EditText
    private val mBarType: Spinner
    private val mBarTypeAdapter: TypeLabelAdapter<MidiHelper.BarTypes>
    private val mLabelCC: TextView
    private val mTextCC: EditText
    private val mHasRZ: CheckBox
    private val mTextRZ: EditText
    private val mLabelRZ: TextView
    private val mTitle: TextView
    private val mSelectChannel: Spinner
    private var mCurrentBar: CCBar? = null
    private val mSelectChannelAdapter: ArrayAdapter<String> = ArrayAdapter<String>(context, R.layout.comboview)
    init {
        inflate(context, R.layout.barconfig, this)
        mCancelButton = findViewById(R.id.barpropcancel)
        mOKButton = findViewById(R.id.barpropok)
        mTextName = findViewById(R.id.barname)
        mLabelCC = findViewById(R.id.labelbarcc)
        mTextCC = findViewById(R.id.barcc)
        mHasRZ = findViewById(R.id.barrz)
        mTextRZ = findViewById(R.id.barzerovalue)
        mTitle = findViewById(R.id.labelbarconfig)
        mSelectChannel = findViewById(R.id.barchannel)
        mLabelRZ = findViewById(R.id.labelbarzerval)
        mOKButton.setOnClickListener { _: View ->
            acceptChanges ()
        }
        mCancelButton.setOnClickListener { _: View ->
            onBack ()
        }
        mHasRZ.setOnClickListener { _: View ->
            changeRZ ()
        }
        mBarType = findViewById(R.id.bartype)
        mBarTypeAdapter = TypeLabelAdapter<MidiHelper.BarTypes> (context,
            R.layout.comboview, MidiHelper.BarTypes.entries.toTypedArray())
        fillSpinner ()
    }

    private fun fillSpinner (){
        mSelectChannelAdapter.add(resources.getString(R.string.sdefault))
        for (i in 1..16){
            mSelectChannelAdapter.add (i.toString())
        }
        mSelectChannelAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mSelectChannel.adapter = mSelectChannelAdapter
        mBarTypeAdapter.setDropDownViewResource(R.layout.dropdowncomboview)
        mBarType.adapter = mBarTypeAdapter
        mBarType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long,
            ) {
                if (parent!!.getItemAtPosition(pos) == MidiHelper.BarTypes.BAR_BEND)
                    setCC(CCBar.PITCH_BEND)
                else {
                    if (mCurrentBar!!.getControl() != CCBar.PITCH_BEND)
                        setCC(mCurrentBar!!.getControl())
                    else
                        setCC (0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }

        }

    }
    private fun changeRZ (){
        if (mHasRZ.isChecked && mBarType.selectedItem != MidiHelper.BarTypes.BAR_BEND) {
            mLabelRZ.visibility = VISIBLE
            mTextRZ.visibility = VISIBLE
        }
        else {
            mTextRZ.visibility = GONE
            mLabelRZ.visibility = GONE
        }
    }

    private fun showCC (vis: Int) {
        mTextCC.visibility = vis
        mLabelCC.visibility = vis
        mHasRZ.visibility = vis
        mTextRZ.visibility = vis
        mLabelRZ.visibility = GONE
        changeRZ()
    }
    private fun setCC (cc: Int){
        if (cc == CCBar.PITCH_BEND){
            mBarType.setSelection(MidiHelper.BarTypes.BAR_BEND.ordinal)
            mTextCC.setText("0")
            showCC(GONE)
        }
        else {
            mBarType.setSelection(MidiHelper.BarTypes.BAR_CC.ordinal)
            mTextCC.setText(cc.toString())
            showCC(VISIBLE)
        }

    }
    fun setBar (bar: CCBar){
        mCurrentBar = bar
        mTextName.setText(bar.mName)
        mSelectChannel.setSelection(bar.mChannel+1)
        setCC(bar.getControl())
        mHasRZ.isChecked = bar.mRZ
        if (bar.mRZ)
            mTextRZ.setText(bar.mZeroPos.toString())
        val title: String = resources.getString(R.string.sbarconfig) + " " + bar.mNumber.toString()
        mTitle.text = title
        changeRZ()
    }

    private fun acceptChanges (){
        if (mCurrentBar == null)
            return

        mCurrentBar!!.setName(mTextName.text.toString())

        if (mBarType.selectedItem == MidiHelper.BarTypes.BAR_CC)
            mCurrentBar!!.setControl(mTextCC.text.toString().toInt())
        else
            mCurrentBar!!.setControl(CCBar.PITCH_BEND)
        mCurrentBar!!.mRZ = mHasRZ.isChecked
        mCurrentBar!!.mChannel =  mSelectChannel.selectedItemPosition - 1
        if (mHasRZ.isChecked) {
            var sval = mTextRZ.text.toString()
            if (sval == "")
                sval = "0"
            mCurrentBar!!.mZeroPos = sval.toInt()
        }
        onBack()
    }

    private fun onBack (){
        if (mCurrentBar != null)
            mCurrentBar!!.misEdit = false
        mCurrentBar = null
        (context as MainActivity).goBack()
    }
}