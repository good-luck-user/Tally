package com.example.tallyapp.ui.item

import com.example.tallyapp.data.Item

data class ItemDetailsUiState(
    val messages: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
