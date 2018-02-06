package com.example.tmjtb.morsecode

import kotlinx.android.synthetic.main.content_main.*
import android.app.Activity
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager

import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.lang.Math.round
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        outputText.movementMethod = ScrollingMovementMethod()

        translateButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString())
            appendTextAndScroll(translate(inputText.text.toString()))
            hideKeyboard()
        }

        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString())
            hideKeyboard()
        }

        val jsonObj = loadMorseJSONObject()

        legendButton.setOnClickListener { view ->
            appendTextAndScroll(jsonObj.toString())
            hideKeyboard()
        }

        buildDicts(jsonObj)
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
            R.id.action_settings -> true
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

    fun loadMorseJSONObject() : JSONObject {
        val filePath = "morse.json";

        val jsonStr = application.assets.open(filePath).bufferedReader().use{
            it.readText()
        }

        val jsonObj = JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1));

        return jsonObj;
    }

    var letToCodeDict : HashMap<String, String> = HashMap()
    var CodeToLetDict : HashMap<String, String> = HashMap()

    fun buildDicts(Json: JSONObject) {

        for (k in Json.keys()) {
            var code = Json[k];

            letToCodeDict.set(k,code.toString())
            CodeToLetDict.set(code.toString(),k)

            Log.d("log", "$k: $code")
        }
    }

    fun legendButton() {

        appendTextAndScroll("HERE ARE THE CODES");

        for (k in letToCodeDict.keys.sorted()) {
            appendTextAndScroll("$k: ${letToCodeDict[k]}");
        }
    }

    fun translate(s:String) : String {

        var s2 = s.toLowerCase()
        var r = ""

        for (c in s2) {
            if (c == ' ')
                r += " / "
            else /*if*/ (letToCodeDict.containsKey(c.toString()))
                r += letToCodeDict.get(c.toString())
            /*else
                r += "?"*/
        }
        return r
    }

    fun playString(s:String, i: Int = 0) : Unit {

        if (i > s.length - 1)
            return;

        var mDelay: Long = 0;

        var thenFun: () -> Unit = { ->
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                playString(s, i+1)
            })
        }

        var c = s[i]
        Log.d("Log", "Processing psl: " + i + " char: [" + c + "]")

        if (c == '.')
            playDot(thenFun)
        else if
            playDash(thenFun)
        else if (c == '/')
            pause(6+dotLength, thenFun)
        else if (c == ' ')
            pause( 2+dotLength, thenFun)
    }

    val dotLength:Int = 50
    val dashLength:Int = dotLength * 3

    val dotSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dotLength)
    val dashSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dashLength)

    fun playDash(onDone : () -> Unit = {}) {
        Log.d("DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer, { -> pause(dotLength, onDone)})
    }

    fun playDot(onDone : () -> Unit = {}) {
        Log.d("DEBUG", "playDot")
        playSoundBuffer(dotSoundBuffer, { -> pause(dotLength, onDone)})
    }

    fun pause(durationMSec: Int, onDone : () -> Unit = {}){
        Log.d("DEBUG", "pause" + durationMSec)
        Timer().schedule(timerTask {
            onDone()
        }, durationMSec.toLong())
    }

    val SAMPLE_RATE = 44100

    private fun genSineWaveSoundBuffer(frequency: Double, durationMSec: Int) : ShortArray {
        val duration: Int = round((durationMSec / 1000.0) * SAMPLE_RATE).toInt()

        var mSound : Double
        val mBuffer = ShortArray(duration)
        for (i in 0 until duration) {
            mSound = Math.sin(2.0 * Math.PI * i.toDouble() / (SAMPLE_RATE / frequency))
            mBuffer[i] = (mSound * java.lang.Short.MAX_VALUE).toShort()
        }
        return mBuffer
    }

    private fun playSoundBuffer(mBuffer:ShortArray, onDone : () -> Unit = {}) {
        var minBufferSize = SAMPLE_RATE/10
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
            override fun onPeriodNotification(track: AudioTrack) {}
            override fun onMarkerReached(track: AudioTrack) {
                Log.d("Log", "Audio track end of file reached...")
                mAudioTrack.stop()
                mAudioTrack.release()
                onDone()
            }
        })
        mAudioTrack.play()
        mAudioTrack.write(nBuffer, 0, minBufferSize)
    }

}