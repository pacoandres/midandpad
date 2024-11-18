package org.gnu.itsmoroto.midandpad


import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log

import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.system.exitProcess
import kotlinx.coroutines.Runnable

class MainActivity : AppCompatActivity(), Runnable {


    companion object {
        lateinit var mConfigParams: ConfigParams
        lateinit var mMidi: MidiHelper
    }
    enum class AppEvents {
        MIDICHANGES,
        ONMIDIOPEN,
        MIDIOPEN,
        MIDICLOSE,
        MIDICLOCK,
        TIMERCLOCK,
        START,
        DEBUG,
        DOBACKUP,
        RESTOREFILE
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
                    //Crashes when orientation changes.
                    //May be this solves: https://codingtechroom.com/question/resolving-java-lang-illegalstateexception-fragmentmanager-has-been-destroyed
                    supportFragmentManager.beginTransaction().remove(mSplashScreen)
                        .commit()
                    doAfter ()

                }
                AppEvents.DEBUG->{
                    Log.i(ConfigParams.MODULE, "Debug loop message")
                }
                AppEvents.DOBACKUP->{
                    if (mBackupDirectory != null) {

                        Log.i(ConfigParams.MODULE, "Directory choosed ${mBackupDirectory}")
                        BackupDatabase.BackupToDir(this,
                            mBackupDirectory!!,
                            applicationInfo.dataDir/* + "/databases"*/)
                        mBackupDirectory = null
                    }
                    else
                        Log.i (ConfigParams.MODULE, "No backupdir")
                }
                AppEvents.RESTOREFILE ->{
                    if (mRestoreFile != null) {
                        BackupDatabase.RestoreDatabase (this, mRestoreFile!!, cacheDir,
                            applicationInfo.dataDir)
                        mRestoreFile = null
                        mExploreScreen.updateData()
                    }
                }
                AppEvents.ONMIDIOPEN -> {
                    //Comes from midiconfig
                    goBack()
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
        mMainScreen.setPresetName(mConfigParams.mCurrPresetName)
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
        checkPermissions()
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

    private fun exitMidAndPad (){
        mConfigParams.saveCurrent()
        mMidi.closeMidi()
        exitProcess(0)
    }

    private fun exitSave (){
        AlertDialog.Builder (this, androidx.appcompat.R.style.AlertDialog_AppCompat)
            .setTitle(R.string.sexitsavetitle)
            .setMessage(R.string.sexitsavemessage)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.syes) {_,_->
                mMainScreen.onSaveClick()
                exitMidAndPad()
            }
            .setNegativeButton(R.string.sno){_,_->
                exitMidAndPad()
            }
            .show()
    }
    fun goBack (){
        if (mPreviousView != null)
            changeView(mPreviousView)
        else {
            exitSave ()
        }
    }

    fun updateMidi (){
        mMidiConfig.update()
    }

    fun setPresetNameLabel (name: String){
        if (this::mMainScreen.isInitialized)
            mMainScreen.setPresetName(name)
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


    /**
     * For backup
     */
    private var mBackupDirectory: DocumentFile? = null
    private var mRestoreFile: DocumentFile? = null
    private val mBackupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if  (result.resultCode == RESULT_OK &&
                result.data != null && result.data!!.data != null) {
                val uri = result.data!!.data
                mBackupDirectory = DocumentFile.fromTreeUri(this, uri!!)
                val mes = Message.obtain()
                mes.obj = AppEvents.DOBACKUP
                mMsgHandler.sendMessage(mes)
            }
        }

    private val mRestoreLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if  (result.resultCode == RESULT_OK &&
                result.data != null) {
                val uri = result.data!!.data!!
                mRestoreFile = DocumentFile.fromSingleUri(this, uri)
                val mes = Message.obtain()
                mes.obj = AppEvents.RESTOREFILE
                mMsgHandler.sendMessage(mes)
            }
        }

    fun doBackup () {
        val chooser = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        mBackupLauncher.launch(chooser)

    }

    fun doRestore () {
        val chooser = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        mRestoreLauncher.launch(Intent.createChooser(chooser, "Seleccionar"))
    }

    val PERMISSIONSCODE = 100
    val mPermissionsArray = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private fun checkPermissions (){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(this, mPermissionsArray, PERMISSIONSCODE)
            }
        }
        /*else {
            if (!Environment.isExternalStorageManager())
                noBackup()
        }*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONSCODE){
            if (grantResults.count() == mPermissionsArray.count()) {
                for (p in grantResults) {
                    if (p != PackageManager.PERMISSION_GRANTED)
                        noBackup()
                }
            }
            else
                noBackup ()
        }
    }
    private fun noBackup (){
        showErrorDialog(this, getString(R.string.snobackuptitle),
            getString(R.string.snobackupmsg))
    }
}