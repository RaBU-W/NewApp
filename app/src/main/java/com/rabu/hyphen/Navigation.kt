package com.rabu.hyphen

import androidx.compose.runtime.Composable

import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import com.rabu.hyphen.ui.screen.AdminChange
import com.rabu.hyphen.ui.feature.Timer.Timer

@Composable
fun Navigation(){
    //Nav Controller
    val navController = rememberNavController()
    
    //Host 
    NavHost(navController = navController, startDestination = "Timer"){
        composable(route = "Timer"){
            Timer()
        }
        composable(route = "Home"){
            Home(navController)
        }
        composable(route = "About"){
            About(navController)
        }
        composable(route = "AdminChange"){
            AdminChange()
        }
    }
}

@Composable
fun Home(navController: NavController){
    Text("Hello yhe Navigation Se bni Home Screen He !!!")
    Button(
        onClick = {navController.navigate("About")}
    ){
        Text("Go About Page")
    }
}

@Composable
fun About(navController: NavController){
    Text("Hello Ye About Page he!!")
    Button(
        onClick = {navController.navigate("AdminChange")}
    ){
        Text("Go To Admin change Page")
    }
}