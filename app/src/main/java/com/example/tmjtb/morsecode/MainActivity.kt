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
        else if (c == '-')
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
            override fun onPeriodicNotification(track : AudioTrack) {}
            override fun onMarkerReached(track : AudioTrack) {
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