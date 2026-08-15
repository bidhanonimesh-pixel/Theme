package com.example.launcher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography
import com.example.launcher.model.AppItem

@Composable
fun AppLockPromptDialog(
    app: AppItem,
    palette: CyberColorPalette,
    onVerifyPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = palette.secondaryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "RESTRICTED SUBSYSTEM",
                    style = CyberTypography.titleMedium,
                    color = palette.secondaryAccent
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "${app.appName} is secured with Stark Defense protocols. Enter authorization PIN to proceed.",
                    style = CyberTypography.bodyMedium,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6) pin = it
                        isError = false
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isError) Color.Red else palette.primaryCyan,
                        unfocusedBorderColor = if (isError) Color.Red else palette.primaryCyan.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = CyberTypography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_lock_pin_input")
                )
                if (isError) {
                    Text(
                        text = "INVALID PIN - ACCESS REJECTED",
                        style = CyberTypography.labelSmall,
                        color = Color(0xFFFF1744),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onVerifyPin(pin)
                    if (!success) isError = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryCyan),
                modifier = Modifier.testTag("submit_app_lock_pin")
            ) {
                Text("Unlock", color = Color.Black, style = CyberTypography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray, style = CyberTypography.labelSmall)
            }
        },
        containerColor = palette.surfaceDark
    )
}
