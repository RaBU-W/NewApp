package com.rabu.hyphen.ui.feature.Timer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    // veribles
    var FixedTime: Long by mutableStateOf(0L)
    var CurrentTime: Long by mutableStateOf(0L)
    var RemainingTime: Long by mutableStateOf(0L)

    // khali Hone Per Value Dalna
    init {
        if (CurrentTime == 0L) {
            CurrentTime = System.currentTimeMillis()
            FixedTime = System.currentTimeMillis() + 90000
            RemainingTime = FixedTime - CurrentTime
        }
    }

    // Timer for remaining time
    init {
        viewModelScope.launch {
            while (RemainingTime > 0) {
                RemainingTime = FixedTime - System.currentTimeMillis()
                delay(1000)
            }
        }
    }
}
