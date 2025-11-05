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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.drive.roadhazard.ui.presentation.LoginScreen
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

        val sharedPref = getSharedPreferences("Road", Context.MODE_PRIVATE)
        val prefEditor = sharedPref.edit()

        if (!sharedPref.contains("isLoggedIn")) {
            prefEditor.putBoolean("isLoggedIn", false)
            prefEditor.apply()
        }

        setContent {
            RoadHazardTheme {
                var isLoggedIn by remember {
                    mutableStateOf(sharedPref.getBoolean("isLoggedIn", false))
                }

                if (isLoggedIn) {
                    val _jwt: String? = sharedPref.getString("jwt", null)
                    if (!_jwt.isNullOrBlank()) {
                        viewModel.onLoginSuccess(_jwt)
                    }
                    MyApp(viewModel = viewModel)
                    requestPermissions()
                } else {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            if (viewModel.jwt.isNotEmpty()) {
                                prefEditor.putBoolean("isLoggedIn", true)
                                prefEditor.putString("jwt", viewModel.jwt)
                                prefEditor.apply()
                                isLoggedIn = true
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO
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