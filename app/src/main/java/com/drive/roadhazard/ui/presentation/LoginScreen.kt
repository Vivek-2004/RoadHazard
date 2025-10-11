package com.drive.roadhazard.ui.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drive.roadhazard.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun handleLogin() {
        isLoading = true
        coroutineScope.launch {
            viewModel.signIn(email, password)
            delay(4000)
            if (viewModel.jwt.isNotEmpty()) {
                onLoginSuccess()
            } else {
                Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    fun handleRegister() {
        isLoading = true
        coroutineScope.launch {
            viewModel.signUp(email, password, name, phoneNumber)
            delay(3000)
            if (viewModel.isRegisterSuccess) {
                Toast.makeText(context, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                isRegistering = false
            } else {
                Toast.makeText(context, "User Registration Failed", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF2196F3),
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedLabelColor = Color(0xFF2196F3),
        unfocusedLabelColor = Color(0xFF757575),
        cursorColor = Color(0xFF2196F3),
        focusedTextColor = Color(0xFF212121),
        unfocusedTextColor = Color(0xFF424242)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegistering) "Register" else "Sign In",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        if (isRegistering) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = Color.Black
            )
        } else {
            Button(
                onClick = {
                    if (isRegistering) {
                        handleRegister()
                    } else {
                        handleLogin()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text(
                    text = if (isRegistering) "Register" else "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        TextButton(
            onClick = {
                Log.e("EventRepository", "Toggle Register Button Clicked")
                isRegistering = !isRegistering
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = if (isRegistering) "Already have an account? Login" else "Don't have an account? Register",
                color = Color(0xFF2196F3)
            )
        }
    }
}