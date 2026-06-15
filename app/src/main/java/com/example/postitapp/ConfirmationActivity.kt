package com.example.postitapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.postitapp.data.TodoRepository
import com.example.postitapp.theme.PostItAppTheme
import com.example.postitapp.widget.EXTRA_ITEM_ID

class ConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        if (itemId == null) {
            finish()
            return
        }

        val repository = TodoRepository.getInstance(applicationContext)
        val item = repository.todoItems.value.find { it.id == itemId }
        if (item == null) {
            finish()
            return
        }

        setContent {
            PostItAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Eliminar tarea", color = Color.White) },
                        text = { Text("¿Deseas eliminar la tarea \"${item.text}\"?", color = Color.LightGray) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    repository.deleteItem(itemId)
                                    finish()
                                }
                            ) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) {
                                Text("Cancelar", color = Color.Gray)
                            }
                        },
                        containerColor = Color(0xFF1E1E24)
                    )
                }
            }
        }
    }
}
