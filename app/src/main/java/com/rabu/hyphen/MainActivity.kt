package com.rabu.hyphen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rabu.hyphen.ui.screen.OwnershipTransferScreen
import com.rabu.hyphen.ui.theme.HyphenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HyphenTheme {
                OwnershipTransferScreen()
            }
        }
    }
}
