package com.comp350sec001group2.ekhoexplorer.ui.loginscreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    // Changed `remember` to `rememberSaveable` so login details survive
    // a configuration change (screen rotation during logging in)
    // i.e. `remember` is lost when the Activity is recreated
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var creatingAccount by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (creatingAccount) "Create account" else "Sign in",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (creatingAccount) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (creatingAccount) {
                        if (password != confirmPassword) {
                            error = "Passwords don't match"
                        } else {
                            AuthManager.signUp(email, password) { err ->
                                if (err == null) onLoggedIn() else error = err
                            }
                        }
                    } else {
                        AuthManager.signIn(email, password) { err ->
                            if (err == null) onLoggedIn() else error = err
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (creatingAccount) "Create account" else "Sign in") }

            TextButton(
                onClick = {
                    creatingAccount = !creatingAccount
                    error = null
                    confirmPassword = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (creatingAccount) "Already have an account? Sign in" else "New here? Create an account")
            }
        }
    }
}
