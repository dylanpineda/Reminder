package com.example.postitapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TodoRepository.getInstance(application)

    val uiState: StateFlow<MainScreenUiState> = repository.todoItems
        .map<List<TodoItem>, MainScreenUiState> { items -> MainScreenUiState.Success(items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainScreenUiState.Loading
        )

    fun addItem(text: String) {
        if (text.isNotBlank()) {
            repository.addItem(text.trim())
        }
    }

    fun toggleItem(id: String) {
        repository.toggleItem(id)
    }

    fun deleteItem(id: String) {
        repository.deleteItem(id)
    }

    fun editItem(id: String, newText: String) {
        if (newText.isNotBlank()) {
            repository.editItem(id, newText.trim())
        }
    }
}

sealed interface MainScreenUiState {
    data object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val items: List<TodoItem>) : MainScreenUiState
}
