package com.almag.audioshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almag.audioshelf.ui.ShelfAppUi
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.theme.ShelfTheme

class MainActivity : ComponentActivity() {

    private val vm: ShelfViewModel by viewModels { factory((application as ShelfApp)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ShelfTheme { ShelfAppUi(vm) }
        }
    }

    companion object {
        fun factory(app: ShelfApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { ShelfViewModel(app.repository) }
        }
    }
}
