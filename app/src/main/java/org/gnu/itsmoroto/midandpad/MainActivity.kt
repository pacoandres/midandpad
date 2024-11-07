/***
 *TODO:

 */


package org.gnu.itsmoroto.midandpad


import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.fixedRateTimer
import kotlin.system.exitProcess
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Runnable

class MainActivity : AppCompatActivity(), Runnable {

    companion object {
        lateinit var mConfigParams: ConfigParams
        lateinit var mMidi: MidiHelper
    }
    enum class AppEvents {
        MIDICHANGES,
        MIDIOPEN,
        MIDICLOSE,
        MIDICLOCK,
        TIMERCLOCK,
        START,
        DEBUG
    }
    private var mPreviousView: View? = null
    lateinit var mMsgHandler:Handler
    private lateinit var mMainScreen: MainScreen
    lateinit var mMidiConfig: MidiConfig
    lateinit var mExploreScreen: ExploreScreen
    lateinit var mButtonConfigScreen: BtnConfigScreen
    lateinit var mBarConfigScreen: BarConfigScreen
    lateinit var mTouchCalibration: TouchCalibration
    private var mCurrentView: View? = null
    private lateinit var mContainer: FrameLayout
    private var mClockCount: Int = 0
    private var mClockSecond: Int = 0
    private val CLOCKTIMERPERIOD:Long = 500L

    private lateinit var mSplashScreen: Splash

    private lateinit var mClockTimer: Timer
    private val mBackCallback = object : OnBackPressedCallback(enabled = true) { //Enabled. False to disable
        override fun handleOnBackPressed() {
            goBack()
        }
    }
    private fun exit (){
        finish()
        System.exit(1)
    }

    private fun setHandler () {
        mMsgHandler = Handler(mainLooper, Handler.Callback { msg:Message->
            when  (msg.obj){

                AppEvents.MIDICHANGES->{
                    updateMidi ()
                }
                AppEvents.MIDIOPEN->{
                    haveMIDI (true)
                }
                AppEvents.MIDICLOSE->{
                    haveMIDI (false)
                }
                AppEvents.MIDICLOCK->{
                    mClockCount++
                }
                AppEvents.TIMERCLOCK->{
                    if (mClockSecond != mClockCount)
                        haveClock (true)
                    else
                        haveClock (false)
                    mClockSecond = mClockCount
                }
                AppEvents.START->{
                    //mContainer.removeView(mSplashScreen)
                    supportFragmentManager.beginTransaction().remove(mSplashScreen)
                        .commit()
                    doAfter ()

                }
                AppEvents.DEBUG->{
                    Log.i(ConfigParams.MODULE, "Debug loop message")
                }
                else ->
                    return@Callback false
            }

            return@Callback true
        })


    }

    private fun doAfter (){ //things needs run mainloop after splash
        mTouchCalibration = TouchCalibration(this)
        mButtonConfigScreen = BtnConfigScreen(this)
        mBarConfigScreen = BarConfigScreen(this)
        mExploreScreen = ExploreScreen(this)
        mMainScreen = MainScreen(this)
        mMidiConfig = MidiConfig(this)
        mMidiConfig.updateChannelPPQ()
        onBackPressedDispatcher.addCallback(this, mBackCallback)
        changeView (mMainScreen)
    }
    private fun initialize (){
        val context = this
        runBlocking {
            launch {
                delay(1000L) //For testing purposes
                mConfigParams = ConfigParams(context)
                mMidi = MidiHelper(context)
                //changeView (mMainScreen)
                val m = Message.obtain()
                m.obj = AppEvents.START
                mMsgHandler.sendMessage(m)

                mClockTimer = fixedRateTimer("clock timer", false, 0,
                    CLOCKTIMERPERIOD){
                    val mes = Message.obtain()
                    mes.obj = AppEvents.TIMERCLOCK
                    mMsgHandler.sendMessage(mes)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.maincontainer)
        mContainer = findViewById(R.id.container)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)){
            AlertDialog.Builder (this, androidx.appcompat.R.style.AlertDialog_AppCompat)
                .setTitle("Error")
                .setMessage(R.string.snomidi)
                .setPositiveButton(R.string.sok) {_,_->
                    exit()

                }
                .setIcon(android.R.drawable.stat_notify_error)
                .setOnDismissListener { _ ->
                    exit()
                }
                .show()
            return
        }
        setHandler()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        mSplashScreen = Splash ()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, mSplashScreen).commit()
        /*mSplashScreen = layoutInflater.inflate(R.layout.splashscreen, null) as ConstraintLayout
        mContainer.addView(mSplashScreen)*/
        Thread (this).start()
        val m = Message.obtain()
        m.obj = AppEvents.DEBUG
        mMsgHandler.sendMessage(m)
    }

    fun changeView (v:View?){
        if (v == null)
            return
        if (mCurrentView != null)
            mContainer.removeView(mCurrentView)
        if (v != mMainScreen) {
            mPreviousView = mCurrentView
            //mBackCallback.isEnabled = true
        }
        else {
            mPreviousView = null
            //mBackCallback.isEnabled = false //Back to default handler
        }
        mContainer.addView(v)
        mCurrentView = v
    }

    fun goBack (){
        if (mPreviousView != null)
            changeView(mPreviousView)
        else {
            mMidi.closeDevice()
            exitProcess(0)
        }
    }

    fun updateMidi (){
        mMidiConfig.update()
    }

    fun configControls (){
        mMainScreen.configControls()
    }
    fun haveMIDI (status: Boolean){
        mMainScreen.haveMIDI(status)
    }

    fun haveClock (status: Boolean){
        mMainScreen.haveClock(status)
    }

    fun updateChannelPPQ (){
        mMidiConfig.updateChannelPPQ()
    }

    override fun run() {
        initialize()
    }
}