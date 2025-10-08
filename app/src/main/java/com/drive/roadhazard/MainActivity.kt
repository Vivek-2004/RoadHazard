package com.drive.roadhazard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.drive.roadhazard.ui.presentation.MyApp
import com.drive.roadhazard.ui.theme.RoadHazardTheme
import com.drive.roadhazard.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionResult(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.BLACK, Color.BLACK)
        )

        requestPermissions()

        val sharedPref = getSharedPreferences("Road", Context.MODE_PRIVATE)
        val prefEditor = sharedPref.edit()

        if (!sharedPref.contains("isLoggedIn")) {
            prefEditor.putBoolean("isLoggedIn", false)
            prefEditor.apply()
        }

        setContent {
            RoadHazardTheme {
                MyApp(viewModel = viewModel)
//                val isLoggedIn by remember { mutableStateOf(sharedPref.getBoolean("isLoggedIn",false)) }
//                if(isLoggedIn) {
//                    MyApp(viewModel = viewModel)
//                } else {
//                    LoginScreen(
//                        prefEditor = prefEditor,
//                        onLoginSuccess = { username ->
//                            viewModel.onLoginSuccess(username)
//                        }
//                    )
//                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
        )

        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.onPermissionResult(true)
        }
    }
}