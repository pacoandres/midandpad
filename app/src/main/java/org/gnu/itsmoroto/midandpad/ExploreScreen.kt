package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputLayout

class ExploreScreen(context: Context): ConstraintLayout(context)  {
    private var mData: ArrayList<PresetItem> = ArrayList<PresetItem> ()
    private val mList: ListView
    private val mListAdapter: ArrayAdapter<PresetItem>
    private val mLoadButton: Button
    private val mDeleteButton: Button
    private val mRenameButton: Button
    private val mCancelButton: Button
    private val mBackupButton: Button
    private val mRestoreButton: Button

    private var mSelectedIndex: Int = -1
    private var mSelectedView:View? = null
    init {
        inflate(context, R.layout.exploreview, this)
        mLoadButton = findViewById(R.id.explorebuttonload)
        mLoadButton.setOnClickListener { _: View ->
            onLoad ()
        }


        mDeleteButton = findViewById(R.id.explorebuttondelete)
        mDeleteButton.setOnClickListener { _: View ->
            onDelete ()
        }
        mRenameButton = findViewById(R.id.explorebuttonrename)
        mRenameButton.setOnClickListener { _: View ->
            onRename ()
        }
        mList = findViewById(R.id.presetlist)
        mListAdapter = ArrayAdapter<PresetItem>(context, R.layout.comboview)
        mList.adapter = mListAdapter
        mList.onItemClickListener = AdapterView.OnItemClickListener{ _, view, position, _->
            if (mSelectedIndex != -1)
                mSelectedView!!.isSelected = false
            mSelectedIndex = position
            mSelectedView = view
        }

        mBackupButton = findViewById(R.id.explorebuttonbackup)
        mBackupButton.setOnClickListener { _:View ->
            (context as MainActivity).doBackup()
        }

        mRestoreButton = findViewById(R.id.explorebuttonrestore)
        mRestoreButton.setOnClickListener { _:View ->
            (context as MainActivity).doRestore()
        }

        mCancelButton = findViewById(R.id.explorebuttoncancel)
        mCancelButton.setOnClickListener { _:View ->
            (context as MainActivity).goBack()
        }

        updateData()
    }

    fun updateData (){
        mData.clear()
        mListAdapter.clear()
        mData = MainActivity.mConfigParams.getPresets()
        mListAdapter.addAll(mData)
        mListAdapter.notifyDataSetChanged()
    }

    fun onLoad (){
        if (mSelectedIndex == -1)
            return
        val sel: PresetItem = mList.getItemAtPosition(mSelectedIndex) as PresetItem
        MainActivity.mConfigParams.loadPreset(sel.id.toLong())
        (context as MainActivity).updateChannelPPQ()
        (context as MainActivity).configControls()
        Toast.makeText(context, resources.getString(R.string.spresetloaded
        ).replace(ReplaceLabels.PRESETNAMELABEL, MainActivity.mConfigParams.mCurrPresetName),
            ConfigParams.TOASTLENGTH).show()
        (context as MainActivity).goBack()
    }

    fun onDelete (){
        if (mSelectedIndex == -1)
            return
        val sel: PresetItem = mList.getItemAtPosition(mSelectedIndex) as PresetItem
        if (sel.id == 1L){
            Toast.makeText(context, resources.getString(R.string.sdelfactory),
                ConfigParams.TOASTLENGTH).show()
            return;
        }
        val dlgTitle = context.resources.getString(R.string.sdeletepreset) + " " +
                sel.name + "?"
        if (sel.id == MainActivity.mConfigParams.mCurrPreset) {
            Toast.makeText(context, resources.getString(R.string.sdelcurrent),
                ConfigParams.TOASTLENGTH).show()
            return;
        }
        AlertDialog.Builder (context, androidx.appcompat.R.style.AlertDialog_AppCompat)
            .setTitle(dlgTitle)
            .setMessage(dlgTitle)
            .setPositiveButton(R.string.sok) {_,_->
                mList.setItemChecked(mSelectedIndex, false)
                MainActivity.mConfigParams.deletePreset (sel.id)
                mData.remove(sel)
                updateData()
            }
            .setNegativeButton(R.string.scancel){_,_->
                return@setNegativeButton
            }
            .show()

    }
    fun onRename (){
        if (mSelectedIndex == -1)
            return
        val sel: PresetItem = mList.getItemAtPosition(mSelectedIndex) as PresetItem
        val dlgTitle = context.resources.getString(R.string.srenamepreset) + " " +
                sel.name
        if (sel.id == 1L){
            Toast.makeText(context, resources.getString(R.string.srenamefactory),
                ConfigParams.TOASTLENGTH).show()
            return;
        }
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
                    showErrorDialog(context, resources.getString(R.string.sduplicatedname),
                        msg)
                    dialog.dismiss()
                    return@setPositiveButton
                }
                MainActivity.mConfigParams.renamePreset (sel.id, newname)
                updateData()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.scancel){_,_->
                return@setNegativeButton
            }
            .show()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView != this || visibility != VISIBLE)
            return
        updateData()
    }

}