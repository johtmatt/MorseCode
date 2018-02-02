package com.example.tmjtb.morsecode

import kotlinx.android.synthetic.main.content_main.*
import android.app.Activity
import android.content.Context
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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        outputText.movementMethod = ScrollingMovementMethod();

        translateButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString());
            appendTextAndScroll(translate(inputText.text.toString()))
            hideKeyboard();
        }

        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString());
            //appendTextAndScroll(translate(inputText.text.toString()))
            hideKeyboard();
        }

        val jsonObj = loadMorseJSONObject()

        legendButton.setOnClickListener { view ->
            appendTextAndScroll(jsonObj.toString());
            hideKeyboard();
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

        // Don't forget to wire button under the onCreate function above
        // textView.append("HERE ARE THE CODES");

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
            else if (letToCodeDict.containsKey(c.toString()))
                r += letToCodeDict.get(c.toString())
            else
                r += "?"
        }

        return r
    }

}