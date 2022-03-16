package com.ndhzs.bottomsheettest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm = findViewById<FragmentContainerView>(R.id.fragment)

        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.replace(R.id.fragment, VpFragment())
        beginTransaction.commit()
    }
}