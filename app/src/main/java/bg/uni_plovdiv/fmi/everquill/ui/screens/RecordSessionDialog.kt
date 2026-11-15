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
import bg.uni_plovdiv.fmi.everquill.data.entity.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSessionDialog(
    books: List<Book>,
    onDismiss: () -> Unit,
    onRecordSession: (String, Int) -> Unit
) {
    val activeBooks = books.filter { it.status == bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus.READING || it.status == bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus.TO_READ }
    val targetBooks = if (activeBooks.isNotEmpty()) activeBooks else books

    var selectedBook by remember { mutableStateOf(targetBooks.firstOrNull()) }
    var pagesReadStr by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Reading Session") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (targetBooks.isEmpty()) {
                    Text("No books available. Please add a book first in your Library.")
                } else {
                    // Book dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBook?.title ?: "Select book",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Book") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            targetBooks.forEach { book ->
                                DropdownMenuItem(
                                    text = { Text("${book.title} (p. ${book.currentPage}/${book.totalPages})") },
                                    onClick = {
                                        selectedBook = book
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pagesReadStr,
                        onValueChange = { pagesReadStr = it; errorMessage = null },
                        label = { Text("Pages Read Today") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedBook != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val remaining = selectedBook!!.totalPages - selectedBook!!.currentPage
                        Text(
                            text = "Remaining pages in book: $remaining",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (targetBooks.isNotEmpty()) {
                Button(
                    onClick = {
                        val book = selectedBook
                        if (book == null) {
                            errorMessage = "Please select a book"
                            return@Button
                        }
                        val pages = pagesReadStr.toIntOrNull()
                        if (pages == null || pages <= 0) {
                            errorMessage = "Please enter a valid positive page count"
                            return@Button
                        }
                        val remaining = book.totalPages - book.currentPage
                        if (pages > remaining) {
                            errorMessage = "Pages read ($pages) exceeds remaining pages ($remaining)"
                            return@Button
                        }

                        onRecordSession(book.id, pages)
                        onDismiss()
                    }
                ) {
                    Text("Record & Earn XP")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
