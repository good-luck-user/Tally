package com.example.tallyapp.ui.item

import android.app.Application
import com.example.tallyapp.logging.TallyLog as Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallyapp.data.AppDataContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository by lazy { AppDataContainer(application).itemsRepository }
    private val mutableState = MutableStateFlow(ItemDetailsUiState())
    val uiState = mutableState.asStateFlow()
    private var loadJob: Job? = null

    /** 打开页面或点击重新加载时调用；Room 后续变更会自动更新列表。 */
    fun loadMessages() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = null)
            try {
                Log.i("TallyApp", "开始加载数据库消息")
                repository.getAllItemsStream().collect { messages ->
                    mutableState.value = ItemDetailsUiState(messages = messages, isLoading = false)
                    Log.i("TallyApp", "数据库消息加载成功：${messages.size} 条")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    error = "无法加载消息，请点击重新加载。",
                )
                Log.i("TallyApp", "数据库消息加载失败", exception)
            }
        }
    }
}
