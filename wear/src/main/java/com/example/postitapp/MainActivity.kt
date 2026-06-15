package com.example.postitapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import android.app.RemoteInput
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.input.RemoteInputIntentHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.TodoRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TodoRepository.getInstance(this)

        setContent {
            MaterialTheme {
                Scaffold(
                    timeText = { TimeText() }
                ) {
                    val items by repository.todoItems.collectAsState()
                    WatchScreen(
                        items = items,
                        onToggle = { repository.toggleItem(it) },
                        onAdd = { repository.addItem(it) },
                        onDelete = { repository.deleteItem(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun WatchScreen(
    items: List<TodoItem>,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<TodoItem?>(null) }

    Dialog(
        showDialog = (itemToDelete != null),
        onDismissRequest = { itemToDelete = null }
    ) {
        Alert(
            title = { Text("Eliminar", textAlign = TextAlign.Center) },
            negativeButton = {
                Button(onClick = { itemToDelete = null }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cancelar"
                    )
                }
            },
            positiveButton = {
                Button(onClick = {
                    itemToDelete?.let { onDelete(it.id) }
                    itemToDelete = null
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar"
                    )
                }
            }
        ) {
            Text(
                text = itemToDelete?.text ?: "",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            val input = results?.getCharSequence("input_key")?.toString()
            if (!input.isNullOrBlank()) {
                onAdd(input)
            }
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(
                    text = "Post It Watch",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary
                )
            }
        }

        item {
            Chip(
                onClick = {
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    val remoteInputs = listOf(
                        RemoteInput.Builder("input_key")
                            .setLabel("Nueva tarea")
                            .build()
                      )
                      RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                      launcher.launch(intent)
                },
                label = { Text("Nueva tarea") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        if (items.isEmpty()) {
            item {
                Text(
                    text = "No hay tareas\nAgregalas aqui o en el telefono",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp)
                )
            }
        } else {
            items(items, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToggleChip(
                        checked = item.isChecked,
                        onCheckedChange = { onToggle(item.id) },
                        label = {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.body1.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                        },
                        toggleControl = {
                            ToggleChipDefaults.checkboxIcon(checked = item.isChecked)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { itemToDelete = item },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
