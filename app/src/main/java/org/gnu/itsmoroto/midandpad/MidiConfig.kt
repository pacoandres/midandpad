package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceInfo.PortInfo
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout

class MidiConfig (context: Context):ConstraintLayout (context) {

    private val mNDevices: TextView
    private val mOpenButton: Button
    private val mCancelButton: Button
    private val mSelectDevice: Spinner
    private val mSelectDeviceAdapter: ArrayAdapter<String>
    private val mSelectOutPort: Spinner
    private val mSelectOutPortAdapter: ArrayAdapter<Int>
    private val mSelectInPort: Spinner
    private val mSelectInPortAdapter: ArrayAdapter<Int>
    private val mSelectChannel: Spinner
    private val mSelectChannelAdapter: ArrayAdapter<Int>
    private val mPPQ: EditText

    private val mDevices: HashMap<String, MidiDeviceInfo> = HashMap<String, MidiDeviceInfo> ()

    init {
        inflate(context, R.layout.midiconfig, this)
        mNDevices = findViewById(R.id.ndevices)
        mOpenButton = findViewById(R.id.opendevice)
        mCancelButton = findViewById(R.id.cancelcalib)
        mSelectDevice = findViewById(R.id.selectdevice)
        mSelectDeviceAdapter = ArrayAdapter<String>(context, R.layout.comboview)
        mSelectOutPort = findViewById(R.id.selectporto)
        mSelectOutPortAdapter = ArrayAdapter<Int>(context, R.layout.combonumberview)
        mSelectOutPortAdapter.setDropDownViewResource(R.layout.dropdowncombonumberview)
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
        mSelectDevice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected (parent: AdapterView<*>?, v: View?,
                                                pos: Int, id: Long){
                setCurrDevice (mDevices[mSelectDeviceAdapter.getItem(pos)]!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                mSelectOutPortAdapter.clear()
                mSelectInPortAdapter.clear ()
                mSelectOutPort.isEnabled = false
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
        mSelectDevice.adapter = mSelectDeviceAdapter
        mSelectChannel.adapter = mSelectChannelAdapter
        mSelectOutPort.adapter = mSelectOutPortAdapter
        mSelectInPort.adapter = mSelectInPortAdapter
        updateChannelPPQ()
        update()
    }
    fun updateChannelPPQ (){
        mSelectChannel.setSelection(MainActivity.mConfigParams.mDefaultChannel.toInt())
        mPPQ.text = MainActivity.mConfigParams.mPPQ.toEditable()
    }
    fun update (){
        mDevices.clear()
        mSelectDeviceAdapter.clear()
        MainActivity.mMidi.mDevices.forEach { midiDeviceInfo: MidiDeviceInfo ->
            var name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            if (name == null)
                name = midiDeviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            if (name == null)
                name = resources.getString(R.string.unknowdevice)
            mDevices[name] = midiDeviceInfo
        }
        mDevices.forEach { device ->
                mSelectDeviceAdapter.add(device.key)
        }
        mNDevices.text = mDevices.count().toString()
    }
    private fun onCancelClick() {
        (context as MainActivity).goBack()
    }

    private fun setCurrDevice (deviceInfo: MidiDeviceInfo){
        mSelectOutPortAdapter.clear()
        mSelectInPortAdapter.clear()
        deviceInfo.ports.forEach { port->
            if (port.type == PortInfo.TYPE_INPUT){
                mSelectOutPortAdapter.add(port.portNumber)
            }
        }
        mSelectOutPort.isEnabled = true

        deviceInfo.ports.forEach { port->
            if (port.type == PortInfo.TYPE_OUTPUT){
                mSelectInPortAdapter.add(port.portNumber)
            }
        }

        mSelectInPort.isEnabled = true

        mSelectChannel.isEnabled = true
    }

    private fun onOpenClick () {
        val seldevice = mDevices[mSelectDevice.selectedItem]
        val selporto = mSelectOutPort.selectedItem as Int

        val selporti = if (mSelectInPort.selectedItem != null) mSelectInPort.selectedItem as Int
            else -1
        val defchannel = mSelectChannel.selectedItem as Int
        if (selporto != -1 && seldevice != null && defchannel != -1 &&
            selporti != -1
        ) {
            MainActivity.mMidi.openDevice(seldevice, selporto, selporti)
            MainActivity.mConfigParams.mDefaultChannel = (defchannel - 1).toUByte()
        } else {
            AlertDialog.Builder(context, androidx.appcompat.R.style.AlertDialog_AppCompat)
                .setTitle("Error")
                .setMessage(R.string.serrorfulfillment)
                .setPositiveButton(R.string.sok) { _, _ ->
                    return@setPositiveButton
                }
                .setIcon(android.R.drawable.stat_notify_error)
                .show()
        }
        onCancelClick()
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