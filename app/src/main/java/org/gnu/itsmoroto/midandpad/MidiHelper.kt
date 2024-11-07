package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiManager.DeviceCallback
import android.os.Build
import android.os.Looper
import android.os.Handler
import android.media.midi.MidiManager.OnDeviceOpenedListener
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Message
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Type

class MidiHelper(context: Context): DeviceCallback(),
 OnDeviceOpenedListener{
    enum class EventTypes(val stringId:Int): TypeLabel {
        EVENT_NOTE (R.string.snote),
        EVENT_CONTROL (R.string.scc),
        EVENT_PROGRAM (R.string.sprogram),
        EVENT_CHORD (R.string.schord);
        override fun getLabelId (): Int{
            return stringId
        }
    }

    enum class BarTypes (val stringId: Int, val cc: Int = 0): TypeLabel {
        BAR_CC (R.string.scc),
        BAR_BEND (R.string.sbend, -1);

        override fun getLabelId(): Int {
            return stringId
        }
    }

    enum class NOTE_TIME (val multiplier: Int, val label: Int, val imageId: Int): TypeLabel{
        WHOLE (-4, R.string.swholenote, R.drawable.wholenote),
        HALF (-2, R.string.shalfnote, R.drawable.halfnote),
        QUARTER (-1, R.string.squarternote, R.drawable.quarternote),
        EIGHTH (2, R.string.seigthnote, R.drawable.eighthnote),
        SIXTEENTH (4, R.string.ssixteenthnote, R.drawable.sixteenthnote),
        THIRTY_SECOND (8, R.string.sthridtysecondnote, R.drawable.thirty_secondnote);

        override fun getLabelId(): Int {
            return label
        }

    }
    //For triplets should divide by 3 an multiply by 2



    private val mTickMutex = Mutex () //Can't find thread safety for sending

     companion object {
         const val MAXMIDIVALUE: Int = 0x7F
         val MIDINOTES = arrayOf<String>(
         "C-2", "C#-2", "D-2", "D#-2", "E-2", "F-2", "F#-2", "G-2", "G#-2", "A-2", "A#-2",
         "B-2", "C-1", "C#-1", "D-1", "D#-1",
         "E-1", "F-1", "F#-1", "G-1", "G#-1", "A-1", "A#-1", "B-1", "C0", "C#0",
         "D0", "D#0", "E0", "F0", "F#0", "G0", "G#0", "A0",
         "A#0", "B0", "C1", "C#1", "D1", "D#1", "E1",
         "F1", "F#1", "G1", "G#1", "A1", "A#1", "B1", "C2", "C#2", "D2",
         "D#2", "E2", "F2",
         "F#2", "G2", "G#2", "A2", "A#2", "B2", "C3", "C#3", "D3", "D#3",
         "E3", "F3", "F#3", "G3", "G#3", "A3", "A#3", "B3", "C4",
         "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4",
         "A#4", "B4", "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5", "G5",
         "G#5", "A5", "A#5", "B5", "C6", "C#6", "D6", "D#6", "E6", "F6",
         "F#6", "G6", "G#6", "A6", "A#6", "B6", "C7", "C#7", "D7",
         "D#7", "E7", "F7", "F#7", "G7", "G#7", "A7", "A#7", "B7", "C8",
         "C#8", "D8", "D#8", "E8", "F8", "F#8", "G8"
     )
         val STATUS_COMMAND_MASK:UByte = 0xF0.toUByte ()
         val  STATUS_CHANNEL_MASK:UByte = 0x0F.toUByte ()

         // Channel voice messages.
         val STATUS_NOTE_OFF:UByte =  0x80.toUByte ()
         val STATUS_NOTE_ON:UByte =  0x90.toUByte ()
         val STATUS_POLYPHONIC_AFTERTOUCH:UByte =  0xA0.toUByte ()
         val STATUS_CONTROL_CHANGE:UByte =  0xB0.toUByte ()
         val STATUS_PROGRAM_CHANGE:UByte =  0xC0.toUByte ()
         val STATUS_CHANNEL_PRESSURE:UByte =  0xD0.toUByte ()
         val STATUS_PITCH_BEND:UByte =  0xE0.toUByte ()

         //System common messages.
         val STATUS_SYSTEM_EXCLUSIVE:UByte =  0xF0.toUByte ()
         val STATUS_MIDI_TIME_CODE:UByte =  0xF1.toUByte ()
         val STATUS_SONG_POSITION:UByte =  0xF2.toUByte ()
         val STATUS_SONG_SELECT:UByte =  0xF3.toUByte ()
         val STATUS_TUNE_REQUEST:UByte =  0xF6.toUByte ()
         val STATUS_END_SYSEX:UByte =  0xF7.toUByte ()

         //Real time messages
         val STATUS_TIMING_CLOCK:UByte =  0xF8.toUByte ()
         val STATUS_START:UByte =  0xFA.toUByte ()
         val STATUS_CONTINUE:UByte =  0xFB.toUByte ()
         val STATUS_STOP:UByte =  0xFC.toUByte ()
         val STATUS_ACTIVE_SENSING:UByte =  0xFE.toUByte ()
         val STATUS_RESET:UByte =  0xFF.toUByte ()

         fun getNoteTime (id: Int): NOTE_TIME?{
             for (n in NOTE_TIME.entries){
                 if (n.ordinal == id)
                     return n
             }
             return null
         }
     }

    /*private class Receiver (m:MainActivity): MidiReceiver() {
        private val mMain = m
        @OptIn(ExperimentalUnsignedTypes::class)
        override fun onSend (msg: ByteArray, offset: Int, count: Int, timestamp: Long){
            val umsg = msg.toUByteArray()
            if (umsg[offset] != STATUS_TIMING_CLOCK)
                return
            //Is tick. There are 24 ticks per quarter note
            MainScreen.clockTick()
            val m = Message.obtain()
            m.obj = MainActivity.AppEvents.MIDICLOCK
            mMain.mMsgHandler.sendMessage(m)
        }
    }*/


    val mDevices: ArrayList<MidiDeviceInfo> = ArrayList<MidiDeviceInfo>()
    private var mMIDIManager: MidiManager? = null
    private var mOutPort: MidiInputPort? = null
    private var mInPort: MidiOutputPort? = null
    private var mDevice: MidiDevice? = null
    private var mNPortSend: Int = -1
    private var mNPortRecv: Int = -1
    private var mNConnect: Int = -1
    private val mMain = context as MainActivity
    init {

        mMIDIManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        if (mMIDIManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val mididevices:Set<MidiDeviceInfo> = mMIDIManager!!.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
                for (device in mididevices){
                    mDevices.add(device)
                }
                mMIDIManager!!.registerDeviceCallback(
                    MidiManager.TRANSPORT_MIDI_BYTE_STREAM,
                    { r: Runnable? -> Handler(Looper.getMainLooper()).post(r!!) },
                    this
                )
            } else {
                val mididevices:Array<MidiDeviceInfo> = mMIDIManager!!.devices
                for (device in mididevices){
                    mDevices.add(device)
                }
                mMIDIManager!!.registerDeviceCallback(this, null)
            }
        }

    }

    override fun onDeviceAdded(deviceinfo: MidiDeviceInfo?) {
        super.onDeviceAdded(deviceinfo)
        if (deviceinfo != null) {
            if (deviceinfo.inputPortCount > 0) {
                mDevices.add(deviceinfo)
                val m = Message.obtain()
                m.obj = MainActivity.AppEvents.MIDICHANGES
                mMain.mMsgHandler.sendMessage(m)
            }
        }
    }

    override fun onDeviceRemoved(deviceinfo: MidiDeviceInfo?) {
        super.onDeviceRemoved(deviceinfo)

        if (mDevices.indexOf(deviceinfo) == mNConnect) {
            mDevice!!.close()
            mDevice = null
        }
        else {
            val m = Message.obtain()
            m.obj = MainActivity.AppEvents.MIDICHANGES
            mMain.mMsgHandler.sendMessage(m)
        }
        mDevices.remove(deviceinfo)
    }

    fun closeDevice (){
        val m = Message.obtain()
        m.obj = MainActivity.AppEvents.MIDICLOSE
        mMain.mMsgHandler.sendMessage(m)
        if (mInPort != null){
            mInPort!!.close()
            mInPort = null
        }
        if (mOutPort != null){
            mOutPort!!.close()
            mOutPort = null
        }
        if (mDevice != null) {
            mDevice!!.close()
            mDevice = null
        }
    }
    fun openDevice (deviceinfo: MidiDeviceInfo, nportsend:Int, nportrecv: Int){
        closeDevice()
        if (deviceinfo.inputPortCount < 1 || deviceinfo.outputPortCount < 1)
            return
        if (mMIDIManager == null)
            return
        closeDevice()
        mNPortSend = nportsend
        mNPortRecv = nportrecv
        mMIDIManager!!.openDevice(deviceinfo, this, null)
    }


    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onDeviceOpened(device: MidiDevice) {
        mNConnect = mDevices.indexOf(device.info)
        mDevice = device
        mOutPort = device.openInputPort(mNPortSend)
        mInPort = device.openOutputPort(mNPortRecv)
        //mInPort!!.connect(Receiver (mMain))
        mInPort!!.connect(object : MidiReceiver (){
            override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
                if (STATUS_TIMING_CLOCK in msg!!.toUByteArray()){
                    MainScreen.clockTick()
                    val m = Message.obtain()
                    m.obj = MainActivity.AppEvents.MIDICLOCK
                    mMain.mMsgHandler.sendMessage(m)
                }
                //Log.i(ConfigParams.MODULE, "MIDI input ${count} bytes long")

            }

            /*override fun send(msg: ByteArray?, offset: Int, count: Int) {
                super.send(msg, offset, count)
                if (STATUS_TIMING_CLOCK in msg!!.toUByteArray()){
                    MainScreen.clockTick()
                    val m = Message.obtain()
                    m.obj = MainActivity.AppEvents.MIDICLOCK
                    mMain.mMsgHandler.sendMessage(m)
                }
            }

            override fun send(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
                super.send(msg, offset, count, timestamp)
                send (msg, offset, count)
            }*/
            override fun flush() {
                super.flush()
            }

            override fun onFlush() {
                super.onFlush()
            }

        })
        val m = Message.obtain()
        m.obj = MainActivity.AppEvents.MIDIOPEN
        mMain.mMsgHandler.sendMessage(m)
    }

    fun haveConnection (): Boolean{
        return (mDevice != null && mOutPort != null)
    }

    fun send (msg: ByteArray) {
        if (mOutPort == null) {
            Log.e (ConfigParams.MODULE, "Sending message to no midi device")
            return
        }
        runBlocking {
            launch {
                coroutineScope {
                    mTickMutex.withLock {
                        mOutPort!!.send(msg, 0, msg.size, System.nanoTime())
                    }
                }
            }
        }
    }


}