package com.example.tmjtb.morsecode

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.example.tmjtb.morsecode.R.id.*

import kotlinx.android.synthetic.main.activity_main.*

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

        testButton.setOnClickListener {view ->
            appendTextAndScroll(inputText.text.toString());
            hidekeyboard();
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
                val scrollDelta = (layout!!.getLineBottom(line:outputText. getLineCount () - 1)
                -outputText.getScrollY() - outputText.getHeight())
                if (scrollDelta > 0)
                    outputText.scrollBy(x:0, scrollDelta)
            }
        }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(context:this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager - getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMedhotdManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken), flags:0)
    }

}