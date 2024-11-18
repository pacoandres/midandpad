package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceInfo.PortInfo
import android.os.Message
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.collection.emptyLongSet
import androidx.constraintlayout.widget.ConstraintLayout

class MidiConfig (context: Context):ConstraintLayout (context) {

    private val mNDevicesOut: TextView
    private val mOpenButton: Button
    private val mCancelButton: Button
    private val mSelectDeviceOut: Spinner
    private val mSelectDeviceOutAdapter: ArrayAdapter<String>
    private val mSelectOutPort: Spinner
    private val mSelectOutPortAdapter: ArrayAdapter<Int>

    private val mNDevicesIn: TextView
    private val mSelectDeviceIn: Spinner
    private val mSelectDeviceInAdapter: ArrayAdapter<String>
    private val mSelectInPort: Spinner
    private val mSelectInPortAdapter: ArrayAdapter<Int>
    private val mSelectChannel: Spinner
    private val mSelectChannelAdapter: ArrayAdapter<Int>
    private val mPPQ: EditText

    companion object {
        private const val NOPORT = "-----"
    }

    private val mDevicesOut: HashMap<String, MidiDeviceInfo> = HashMap<String, MidiDeviceInfo> ()
    private val mDevicesIn: HashMap<String, MidiDeviceInfo> = HashMap<String, MidiDeviceInfo> ()
    init {
        inflate(context, R.layout.midiconfig, this)
        mNDevicesOut = findViewById(R.id.ndevicesout)
        mNDevicesIn = findViewById(R.id.ndevicesin)
        mOpenButton = findViewById(R.id.opendevice)
        mCancelButton = findViewById(R.id.cancelcalib)
        mSelectDeviceOut = findViewById(R.id.selectdeviceout)
        mSelectDeviceOutAdapter = ArrayAdapter<String>(context, R.layout.comboview)
        mSelectOutPort = findViewById(R.id.selectporto)
        mSelectOutPortAdapter = ArrayAdapter<Int>(context, R.layout.combonumberview)
        mSelectOutPortAdapter.setDropDownViewResource(R.layout.dropdowncombonumberview)
        mSelectDeviceIn = findViewById(R.id.selectdevicein)
        mSelectDeviceInAdapter = ArrayAdapter<String>(context, R.layout.comboview)
        mSelectInPort = findViewById(R.id.selectporti)
        mSelectInPortAdapter = ArrayAdapter<Int>(context, R.layout.combonumberview)
        mSelectInPortAdapter.setDropDownViewResource(R.layout.dropdowncombonumberview)
        mSelectChannel = findViewById(R.id.selectchannel)
        mSelectChannelAdapter = ArrayAdapter<Int>(context, R.layout.combonumberview)
        mSelectChannelAdapter.setDropDownViewResource(R.layout.dropdowncombonumberview)
        mPPQ = findViewById(R.id.ppq)



        mCancelButton.setOnClickListener { _: View ->
            onCancelClick()
        }


        mOpenButton.setOnClickListener { _: View ->
            onOpenClick ()
        }
        mSelectDeviceOut.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected (parent: AdapterView<*>?, v: View?,
                                                pos: Int, id: Long){
                setCurrDeviceOut (mDevicesOut[mSelectDeviceOutAdapter.getItem(pos)]!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                mSelectOutPortAdapter.clear()
                mSelectOutPort.isEnabled = false
            }
        }

        mSelectDeviceIn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected (parent: AdapterView<*>?, v: View?,
                                         pos: Int, id: Long){
                if (pos == 0){
                    mSelectInPortAdapter.clear()
                    return
                }
                setCurrDeviceIn (mDevicesIn[mSelectDeviceInAdapter.getItem(pos)]!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                mSelectInPortAdapter.clear ()
                mSelectInPort.isEnabled = false

            }
        }

        /*MainActivity.m_midi.m_devices.forEach { midiDeviceInfo: MidiDeviceInfo ->
            var name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (name == null)
                name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            if (name == null)
                name = resources.getString(R.string.unknowdevice)
            m_devices[name] = midiDeviceInfo
        }*/
        mSelectOutPort.isEnabled = false
        mSelectChannel.isEnabled = false

        for (i in 0..15){
            mSelectChannelAdapter.add(i+1)
        }
        mSelectDeviceOut.adapter = mSelectDeviceOutAdapter
        mSelectDeviceIn.adapter = mSelectDeviceInAdapter
        mSelectChannel.adapter = mSelectChannelAdapter
        mSelectOutPort.adapter = mSelectOutPortAdapter
        mSelectInPort.adapter = mSelectInPortAdapter
        updateChannelPPQ()
        update()

        findViewById<TextView>(R.id.appouttitle).underlined()
        findViewById<TextView>(R.id.appintitle).underlined()
    }
    fun updateChannelPPQ (){
        mSelectChannel.setSelection(MainActivity.mConfigParams.mDefaultChannel.toInt())
        mPPQ.text = MainActivity.mConfigParams.mPPQ.toEditable()
    }
    fun update (){
        mDevicesOut.clear()
        mSelectDeviceOutAdapter.clear()
        MainActivity.mMidi.mDevicesOut.forEach { midiDeviceInfo: MidiDeviceInfo ->
            var name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (name == null)
                name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            if (name == null)
                name = resources.getString(R.string.unknowdevice)
            mDevicesOut[name] = midiDeviceInfo
        }
        mDevicesOut.forEach { device ->
                mSelectDeviceOutAdapter.add(device.key)
        }
        mNDevicesOut.text = mDevicesOut.count().toString()

        mDevicesIn.clear ()
        mSelectDeviceInAdapter.clear()
        mSelectDeviceInAdapter.add (NOPORT)
        MainActivity.mMidi.mDevicesIn.forEach { midiDeviceInfo: MidiDeviceInfo ->
            var name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (name == null)
                name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            if (name == null)
                name = resources.getString(R.string.unknowdevice)
            mDevicesIn[name] = midiDeviceInfo
        }
        mDevicesIn.forEach { device ->
            mSelectDeviceInAdapter.add(device.key)
        }
        mNDevicesIn.text = mDevicesIn.count().toString()

    }
    private fun onCancelClick() {
        (context as MainActivity).goBack()
    }

    private fun setCurrDeviceOut (deviceInfo: MidiDeviceInfo){
        mSelectOutPortAdapter.clear()

        deviceInfo.ports.forEach { port->
            if (port.type == PortInfo.TYPE_INPUT){
                mSelectOutPortAdapter.add(port.portNumber)
            }
        }
        mSelectOutPort.isEnabled = true


        mSelectChannel.isEnabled = true
    }

    private fun setCurrDeviceIn (deviceInfo: MidiDeviceInfo){
        mSelectInPortAdapter.clear()

        deviceInfo.ports.forEach { port->
            if (port.type == PortInfo.TYPE_OUTPUT){
                mSelectInPortAdapter.add(port.portNumber)
            }
        }
        mSelectInPort.isEnabled = true
    }

    private fun onOpenClick () {
        val seldeviceout = mDevicesOut[mSelectDeviceOut.selectedItem]

        val seldevicein: MidiDeviceInfo? =
            if (mSelectDeviceIn.selectedItem != NOPORT)
                mDevicesIn[mSelectDeviceIn.selectedItem]
            else
                null


        val selporto = if (mSelectOutPort.selectedItem != null) mSelectOutPort.selectedItem as Int
            else -1



        val selporti = if (mSelectInPort.selectedItem != null) mSelectInPort.selectedItem as Int
            else -1

        val defchannel = if (mSelectChannel.selectedItem != null) mSelectChannel.selectedItem as Int
            else -1

        val msg = Message.obtain()
        if (selporto != -1 && seldeviceout != null && defchannel != -1
        ) {
            if (seldevicein == null || selporti == -1) {
                AlertDialog.Builder(context, androidx.appcompat.R.style.AlertDialog_AppCompat)
                    .setTitle(R.string.snomidiintitle)
                    .setMessage(R.string.snomidiinmsg)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.sok) { _, _ ->
                        MainActivity.mMidi.openDeviceOut(seldeviceout, selporto)

                        MainActivity.mConfigParams.mDefaultChannel = (defchannel - 1).toUByte()
                        msg.obj = MainActivity.AppEvents.ONMIDIOPEN
                        (context as MainActivity).mMsgHandler.sendMessage(msg)
                    }
                    .setNegativeButton(R.string.scancel) { _, _ ->
                        return@setNegativeButton
                    }
                    .show()
            }
            else {
                MainActivity.mMidi.openDeviceOut(seldeviceout, selporto)
                MainActivity.mMidi.openDeviceIn(seldevicein, selporti)
                MainActivity.mConfigParams.mDefaultChannel = (defchannel - 1).toUByte()
                msg.obj = MainActivity.AppEvents.ONMIDIOPEN
                (context as MainActivity).mMsgHandler.sendMessage(msg)
            }
        } else {
            AlertDialog.Builder(context, androidx.appcompat.R.style.AlertDialog_AppCompat)
                .setTitle("Error")
                .setMessage(R.string.serrorfulfillment)
                .setPositiveButton(R.string.sok) { _, _ ->
                    return@setPositiveButton
                }
                .setIcon(android.R.drawable.stat_notify_error)
                .show()
            return
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView != this)
            return
        if (visibility != VISIBLE)
            return
        update()
    }
}