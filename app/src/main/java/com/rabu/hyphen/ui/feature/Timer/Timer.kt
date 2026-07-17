package com.rabu.hyphen.ui.feature.Timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TextField



@Composable
fun Timer() {

    var Second by remember {mutableStateOf("")}

    val TimerViewModelObject: TimerViewModel = viewModel()
    var FixedTime = TimerViewModelObject.FixedTime
    var RemainingTime = TimerViewModelObject.RemainingTime
    Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) { 
        Text("Fixed Time $FixedTime Remaining Time $RemainingTime")

        TextField(
            value = Second,
            onValueChange = {Second = it}
        )
 }
}
