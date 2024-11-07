package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout

class MainScreen (context: Context): ConstraintLayout(context) {




    private val mEditButton:ImageButton
    private val mMidiButton:ImageButton
    private val mSaveButton:ImageButton
    private val mSaveAsButton:ImageButton
    private val mRunButton:ImageButton
    private val mExploreButton:ImageButton
    private val mCalibrationButton: ImageButton

    private val mHaveMIDIcheck: CheckBox
    private val mHaveClockCheck:CheckBox



    companion object{
        public const val BUTTONROWS = 3
        public const val BUTTONCOLS = 4
        public const val CONTROLSCOUNT = 4
        private lateinit var mEventButtons: Array<Array<EventButton>>
        private lateinit var mControlBars: Array<CCBar>
        fun clockTick (){

                for (arr in mEventButtons) {
                    for (b in arr) {

                                b.tick()
                    }
                }
        }
    }

    init {
        inflate(getContext(), R.layout.activity_main, this)
        mEditButton = findViewById(R.id.buttonedit)
        mEditButton.setOnClickListener { _: View ->
            onEditClick()
        }

        mMidiButton= findViewById(R.id.buttonmidi)
        mMidiButton.setOnClickListener { _: View ->
            onMidiClick()
        }

        mSaveButton = findViewById(R.id.buttonsave)
        mSaveButton.setOnClickListener {_: View->
            onSaveClick ()
        }
        mSaveAsButton = findViewById(R.id.buttonsaveas)
        mSaveAsButton.setOnClickListener {_: View->
            onSaveAsClick ()
        }
        mRunButton = findViewById(R.id.buttonrun)
        mRunButton.setOnClickListener { _: View ->
            onRunClick()
        }

        mExploreButton = findViewById(R.id.buttonexplore)
        mExploreButton.setOnClickListener { _: View->
            onExploreClick ()
        }

        mCalibrationButton = findViewById(R.id.buttoncalib)
        mCalibrationButton.setOnClickListener {_: View->
            onCalibrationClick ()
        }


        mHaveMIDIcheck = findViewById(R.id.havemidi)
        mHaveMIDIcheck.isChecked = false
        mHaveClockCheck = findViewById(R.id.haveclock)
        mHaveClockCheck.isChecked = false


        mEventButtons = arrayOf(
            arrayOf(findViewById(R.id.button11) as EventButton,
                findViewById (R.id.button12) as EventButton,
                findViewById(R.id.button13) as EventButton,
                findViewById(R.id.button14) as EventButton),
            arrayOf(findViewById(R.id.button21) as EventButton,
                findViewById (R.id.button22) as EventButton,
                findViewById(R.id.button23) as EventButton,
                findViewById(R.id.button24) as EventButton),
            arrayOf(findViewById(R.id.button31) as EventButton,
                findViewById (R.id.button32) as EventButton,
                findViewById(R.id.button33) as EventButton,
                findViewById(R.id.button34) as EventButton))
        mEditButton.setBackgroundResource(android.R.drawable.btn_default)
        for (i in 0..BUTTONROWS - 1){
            for (j in 0..BUTTONCOLS - 1)
                mEventButtons[i][j].mNumber = (i+1)*10 + j+1
        }
        mControlBars = arrayOf(findViewById(R.id.ctrl1) as CCBar,
            findViewById(R.id.ctrl2) as CCBar,
            findViewById(R.id.ctrl3) as CCBar,
            findViewById(R.id.ctrl4) as CCBar)



        mControlBars[0].setLabelWidget(findViewById(R.id.label1))
        mControlBars[1].setLabelWidget(findViewById(R.id.label2))
        mControlBars[2].setLabelWidget(findViewById(R.id.label3))
        mControlBars[3].setLabelWidget(findViewById(R.id.label4))
        for (i in 0..CONTROLSCOUNT -1){
            mControlBars[i].mNumber = i+1
        }
        configControls()
    }
    fun configControls (){
        MainActivity.mConfigParams.configButtons(mEventButtons)
        MainActivity.mConfigParams.configBars (mControlBars)
    }
    private fun onMidiClick() {

        /*if ((context as MainActivity).m_midiconfig == null)
            context.m_midiconfig = MidiConfig (this, context)*/
        (context as MainActivity).changeView((context as MainActivity).mMidiConfig)
    }

    private fun onEditClick (){
        mEditButton.setBackgroundResource(com.google.android.material.R.color.design_default_color_secondary)
        mRunButton.setBackgroundResource(android.R.drawable.btn_default)
        MainActivity.mConfigParams.mMode = ConfigParams.EDIT_MODE
    }

    private fun onRunClick (){
        mRunButton.setBackgroundResource(com.google.android.material.R.color.design_default_color_secondary)
        mEditButton.setBackgroundResource(android.R.drawable.btn_default)
        MainActivity.mConfigParams.mMode = ConfigParams.RUN_MODE
    }

    private fun onExploreClick (){
        val exploreScreen = (context as MainActivity).mExploreScreen
        exploreScreen.updateData()
        (context as MainActivity).changeView(exploreScreen)
    }

    private fun onSaveClick (){
        MainActivity.mConfigParams.savePreset(mEventButtons, mControlBars)
        Toast.makeText(context, resources.getString(R.string.spresetsaved)
            .replace(ReplaceLabels.PRESETNAMELABEL, MainActivity.mConfigParams.mCurrPresetName),
            ConfigParams.TOASTLENGTH).show()
    }

    private fun onSaveAsClick (){
        val dlgTitle = context.resources.getString(R.string.srenamepreset)
        //val textlayout: TextInputLayout = TextInputLayout(context)
        val textinput = EditText (context)
        //textlayout.addView(textinput)
        AlertDialog.Builder (context, androidx.appcompat.R.style.AlertDialog_AppCompat)
            .setTitle(dlgTitle)
            //.setView(textlayout)
            .setView(textinput)
            .setPositiveButton(R.string.sok){
                    dialog: DialogInterface, _->
                val newname: String = textinput.text.toString().trim()
                if (newname == ""){
                    return@setPositiveButton
                }
                if (MainActivity.mConfigParams.checkPresetName(newname)){
                    val msg = context.resources.getString(R.string.spresetnameexists)
                        .replace(ReplaceLabels.PRESETNAMELABEL, newname)

                    AlertDialog.Builder (context, androidx.appcompat.R.style.AlertDialog_AppCompat)
                        .setTitle(R.string.sduplicatedname)
                        .setMessage(msg)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.sok, null)
                        .show()
                    dialog.dismiss()
                    return@setPositiveButton
                }
                MainActivity.mConfigParams.savePresetAs(newname, mEventButtons, mControlBars)
                dialog.dismiss()
                Toast.makeText(context, resources.getString(R.string.spresetsaved)
                    .replace(ReplaceLabels.PRESETNAMELABEL, newname),
                    ConfigParams.TOASTLENGTH).show()
            }
            .setNegativeButton(R.string.scancel){_,_->
                return@setNegativeButton
            }
            .show()
    }
    private fun onCalibrationClick (){
        (context as MainActivity).changeView((context as MainActivity).mTouchCalibration)
    }

    fun haveMIDI (status: Boolean){
        mHaveMIDIcheck.isChecked = status
    }

    fun haveClock (status: Boolean){
        mHaveClockCheck.isChecked = status
    }


}