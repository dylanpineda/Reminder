package com.example.postitapp.data

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: String,
    val text: String,
    val isChecked: Boolean
)
