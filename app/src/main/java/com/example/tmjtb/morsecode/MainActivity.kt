package com.example.tmjtb.morsecode

// SAMPLE RATE VARIABLE HAS TO BE GLOBAL

import android.Manifest.permission.SEND_SMS
import kotlinx.android.synthetic.main.content_main.*
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.lang.Math.round
import java.util.*
import kotlin.concurrent.timerTask

val SAMPLE_RATE = 44100

class MainActivity : AppCompatActivity() {

    var prefs: SharedPreferences? = null

    val letToCodeDict : HashMap<String, String> = HashMap<String, String>()
    val codeToLetDict : HashMap<String, String> = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        outputText.movementMethod = ScrollingMovementMethod()
        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString().toUpperCase())
            hideKeyboard()
        }

        val jsonObj = loadMorseJSONFile()
        buildDicts(jsonObj)

        legendButton.setOnClickListener { view ->
            outputText.text = ""
            legendButton()
            hideKeyboard()
        }

        translateButton.setOnClickListener { _ ->
            outputText.text = ""
            val input = inputText.text.toString()

            appendTextAndScroll(input.toUpperCase())

            if (input.matches("(\\.|-|\\s/\\s|\\s)+".toRegex())) {
                val transMorse = translateMorse(input)
                appendTextAndScroll(transMorse.toUpperCase())
            }
            else {
                val transText = translateText(input)
                appendTextAndScroll(transText)
            }
            hideKeyboard()
        }

        playButton.setOnClickListener { _ ->
            val input = inputText.text.toString()
            playString(translateText(input),0)
        }

        prefs = getDefaultSharedPreferences(this.applicationContext)

        val morsePitch = prefs!!.getString("morse_pitch", "550").toInt()

        fab.setOnClickListener { view ->
            sendSMS("9312423238", outputText.text.toString())
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {

                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)

                return true

            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun appendTextAndScroll(text: String) {
        if (outputText != null) {
            outputText.append(text + "\n")
            val layout = outputText.getLayout()
            if (layout != null) {
                val scrollDelta = (layout!!.getLineBottom(outputText.getLineCount () - 1)
                -outputText.getScrollY() - outputText.getHeight())
                if (scrollDelta > 0)
                    outputText.scrollBy(0, scrollDelta)
            }
        }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun loadMorseJSONFile() : JSONObject {
        val filePath = "morse.json"

        val jsonStr = application.assets.open(filePath).bufferedReader().use{
            it.readText()
        }

        val jsonObj = JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1))

        return jsonObj
    }

    private fun buildDicts(Json: JSONObject) {

        for (k in Json.keys()) {
            var code: String = Json[k] as String

            letToCodeDict.put(k, code)
            codeToLetDict.put(code, k)

            Log.d("log", "$k: $code")
        }
    }

    private fun legendButton() {

        appendTextAndScroll("HERE ARE THE CODES")

        for (k in letToCodeDict.keys.sorted()) {
            appendTextAndScroll("${k.toUpperCase()}: ${letToCodeDict[k]}")
        }
    }

    private fun translateText(input : String) : String {

        var value = ""

        val lowerStr = input.toLowerCase()

        for (c in lowerStr) // Loop for checking all the input
        {
            // if space than explode
            if (c == ' ') value += "/ "
            else if (letToCodeDict.containsKey(c.toString())) value += "${letToCodeDict[c.toString()]} "
            else value += "? "
        }

        Log.d("log", "Morse: $value")

        return value

    }

    private fun translateMorse(input: String) : String {
        var value = ""

        val lowerStr = input.split("(\\s)+".toRegex())

        Log.d("log", "Split stirng: $lowerStr")

        for (item in lowerStr) {
            if (item == "/") value += " "
            else if (codeToLetDict.containsKey(item)) value += codeToLetDict[item]
            else value += "[NA]"
        }

        Log.d("log", "Text: $value")

        return value
    }

    fun playString(s:String, i: Int = 0) : Unit {
        if (i>s.length-1)
            return;
        var mDelay: Long = 0;

        var thenFun: () -> Unit = { ->
            this@MainActivity.runOnUiThread(java.lang.Runnable {playString(s, i+1)})
        }

        var c = s[i]
        Log.d("Log", "Processing pos: " + i + " char: [" + c + "]")
        if (c=='.')
            playDot(thenFun)
        else if (c=='-')
            playDash(thenFun)
        else if (c=='/')
            pause(6*dotLength, thenFun)
        else if (c==' ')
            pause(2*dotLength, thenFun)
    }

    val dotLength:Int = 50
    val dashLength:Int = dotLength*3

    val dotSoundBuffer: ShortArray = genSineWaveSoundBuffer(550.0, dotLength) //freq: 550.0
    val dashSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dashLength)

    fun playDash(onDone:()->Unit={}){
        Log.d("DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer,{->pause(dotLength, onDone)})
    }
    fun playDot(onDone: () -> Unit={}){
        Log.d("DEBUG", "playDot")
        playSoundBuffer(dotSoundBuffer,{ -> pause(dotLength, onDone)})
    }

    fun pause(durationMSec:Int, onDone: () -> Unit={}){
        Log.d("DEBUG", "pause: ${durationMSec}")
        Timer().schedule(timerTask { onDone()  }, durationMSec.toLong())
    }

    private fun genSineWaveSoundBuffer(frequency:Double, durationMSec: Int):ShortArray{
        val duration : Int = Math.round((durationMSec/1000.0) * SAMPLE_RATE).toInt()

        var mSound: Double
        val mBuffer = ShortArray(duration)
        for(i in 0 until duration) {
            mSound= Math.sin(2.0*Math.PI*i.toDouble()/(SAMPLE_RATE/frequency))
            mBuffer[i] = (mSound*java.lang.Short.MAX_VALUE).toShort()
        }
        return mBuffer
    }


    private fun playSoundBuffer (mBuffer: ShortArray, onDone: () -> Unit={ }) {
        var minBufferSize = SAMPLE_RATE / 10
        if (minBufferSize < mBuffer.size) {
            minBufferSize = minBufferSize + minBufferSize *
                    (Math.round(mBuffer.size.toFloat()) / minBufferSize.toFloat()).toInt()
        }

        val nBuffer = ShortArray(minBufferSize)
        for (i in nBuffer.indices) {
            if (i < mBuffer.size)
                nBuffer[i] = mBuffer[i]
            else
                nBuffer[i] = 0
        }

        val mAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM)

        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        mAudioTrack.setNotificationMarkerPosition(mBuffer.size)
        mAudioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track: AudioTrack){}
            override fun onMarkerReached(track: AudioTrack?) {
                Log.d("Log", "Audio track end of file reached...")
                mAudioTrack.stop()
                mAudioTrack.release()
                onDone()
            }
        })
        mAudioTrack.play()
        mAudioTrack.write(nBuffer, 0, minBufferSize)
    }

    private fun sendSMS(phoneNumber:String, message:String) {
        val sentPendingIntents = ArrayList<PendingIntent>()
        val deliveredPendingIntents = ArrayList<PendingIntent>()

        val sentPI = PendingIntent.getBroadcast( this, 0,
                Intent(this, SmsSentReceiver::class.java), 0)
        val deliveredPI = PendingIntent.getBroadcast(this , 0,
                Intent(this , SmsDeliveredReceiver::class.java), 0)

        val PERMISSION_REQUEST_CODE = 1
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            if ((checkSelfPermission(SEND_SMS) === PackageManager.PERMISSION_DENIED))
            {
                Log.d("permission", "permission denied to SEND_SMS - requesting it")
                val permissions = arrayOf<String>(SEND_SMS)
                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            }
        }


        try{
            val sms = SmsManager.getDefault()
            val mSMSMessage = sms.divideMessage(message)
            for (i in 0 until mSMSMessage.size)
            {
                sentPendingIntents.add(i, sentPI)
                deliveredPendingIntents.add(i, deliveredPI)
            }
            sms.sendMultipartTextMessage(phoneNumber, null, mSMSMessage,
                    sentPendingIntents, deliveredPendingIntents)
        }
        catch (e:Exception) {
            e.printStackTrace()
            Toast.makeText(getBaseContext(), "SMS sending failed...", Toast.LENGTH_SHORT).show()
        }
    }

    class SmsDeliveredReceiver: BroadcastReceiver() {
        override fun onReceive(context:Context, arg1:Intent) {
            when (getResultCode()) {
                Activity.RESULT_OK -> Toast.makeText(context, "SMS delivered", Toast.LENGTH_SHORT).show()
                Activity.RESULT_CANCELED -> Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class SmsSentReceiver:BroadcastReceiver() {
        override fun onReceive(context:Context, arg1:Intent) {
            when (getResultCode()) {
                Activity.RESULT_OK -> Toast.makeText(context, "SMS Sent", Toast.LENGTH_SHORT).show()
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(context, "SMS generic failure", Toast.LENGTH_SHORT)
                        .show()
                SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(context, "SMS no service", Toast.LENGTH_SHORT)
                        .show()
                SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(context, "SMS null PDU", Toast.LENGTH_SHORT).show()
                SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(context, "SMS radio off", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

