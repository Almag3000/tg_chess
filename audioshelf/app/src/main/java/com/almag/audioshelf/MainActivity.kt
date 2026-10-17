package com.almag.audioshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text(com.almag.audioshelf.core.Text.books(com.almag.audioshelf.core.Seed.size)) }
    }
}
