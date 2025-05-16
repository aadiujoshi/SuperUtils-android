package com.example.superutils.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun RenameModal(
    initialText: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }

    // Find where the extension starts (last ".")
    val extensionIndex = initialText.lastIndexOf('.').takeIf { it > 0 } ?: initialText.length

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            Column {
                val textFieldValue = remember(initialText) {
                    TextFieldValue(
                        text = initialText,
                        selection = TextRange(0, extensionIndex)
                    )
                }
                var fieldState by remember { mutableStateOf(textFieldValue) }

                TextField(
                    value = fieldState,
                    onValueChange = { newValue ->
                        fieldState = newValue
                        text = newValue.text
                    },
                    singleLine = true,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRename(text) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Rename")
            }
        }
    )

    // Automatically focus the text field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
