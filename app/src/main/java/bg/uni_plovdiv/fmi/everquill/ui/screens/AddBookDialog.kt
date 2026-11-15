package bg.uni_plovdiv.fmi.everquill.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookDialog(
    onDismiss: () -> Unit,
    onAddBook: (String, String, Int, Int, BookStatus) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var totalPagesStr by remember { mutableStateOf("") }
    var currentPageStr by remember { mutableStateOf("0") }
    var selectedStatus by remember { mutableStateOf(BookStatus.TO_READ) }
    var expanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Book") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = null },
                    label = { Text("Book Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it; errorMessage = null },
                    label = { Text("Author") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = totalPagesStr,
                    onValueChange = { totalPagesStr = it; errorMessage = null },
                    label = { Text("Total Pages") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentPageStr,
                    onValueChange = { currentPageStr = it; errorMessage = null },
                    label = { Text("Current Page") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Status dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BookStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayName) },
                                onClick = {
                                    selectedStatus = status
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = title.trim()
                    val a = author.trim()
                    val tp = totalPagesStr.toIntOrNull() ?: 0
                    val cp = currentPageStr.toIntOrNull() ?: 0

                    if (t.isEmpty()) {
                        errorMessage = "Please enter a book title"
                        return@Button
                    }
                    if (a.isEmpty()) {
                        errorMessage = "Please enter an author"
                        return@Button
                    }
                    if (tp <= 0) {
                        errorMessage = "Total pages must be greater than 0"
                        return@Button
                    }
                    if (cp < 0 || cp > tp) {
                        errorMessage = "Current page must be between 0 and $tp"
                        return@Button
                    }

                    onAddBook(t, a, tp, cp, selectedStatus)
                    onDismiss()
                }
            ) {
                Text("Add Book")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
