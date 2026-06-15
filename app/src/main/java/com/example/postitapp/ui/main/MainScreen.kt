package com.example.postitapp.ui.main

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.postitapp.data.TodoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(context.applicationContext as Application)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Post It List",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1E1E24)
                )
            )
        },
        containerColor = Color(0xFF121214)
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                MainScreenUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is MainScreenUiState.Success -> {
                    val items = (state as MainScreenUiState.Success).items
                    MainScreenContent(
                        items = items,
                        onToggle = { viewModel.toggleItem(it) },
                        onDelete = { viewModel.deleteItem(it) },
                        onAdd = { viewModel.addItem(it) },
                        onItemClick = onItemClick
                    )
                }
                is MainScreenUiState.Error -> {
                    Text(
                        "Error: ${(state as MainScreenUiState.Error).throwable.message}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    items: List<TodoItem>,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String) -> Unit,
    onItemClick: (NavKey) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<TodoItem?>(null) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar tarea", color = Color.White) },
            text = { Text("¿Deseas eliminar la tarea \"${itemToDelete?.text}\"?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDelete(it.id) }
                        itemToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.7f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay tareas pendientes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    TodoItemRow(
                        item = item,
                        onToggle = { onToggle(item.id) },
                        onDelete = { itemToDelete = item },
                        onItemClick = { /* No-op for now as there are no other NavKeys */ }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Nueva tarea...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                        text = ""
                    }
                }),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                        text = ""
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar tarea"
                )
            }
        }
    }
}

@Composable
fun TodoItemRow(
    item: TodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E24))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = item.text,
            modifier = Modifier.weight(1f),
            color = if (item.isChecked) Color.Gray else Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            )
        )

        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color(0xFFEF5350)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar tarea"
            )
        }
    }
}
