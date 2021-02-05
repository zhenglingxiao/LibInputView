package com.zlxrx.library.inputview.demo

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.seewo.eclass.library.inputview.demo.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        inputView.addHeaderIcon(R.drawable.ic_account, marginStart = 8)
        inputView.addHeaderIcon(R.drawable.ic_info, marginEnd = 8)
        inputView.addPasswordIcon(R.drawable.ic_eye_selector, 8, 8) {
            Toast.makeText(this, "password visible: $it", Toast.LENGTH_SHORT).show()
        }
        inputView.addClearIcon(R.drawable.ic_clean_selector, marginEnd = 8) {
            Toast.makeText(this, "clean", Toast.LENGTH_SHORT).show()
        }
    }
}
