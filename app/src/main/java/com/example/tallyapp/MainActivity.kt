/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.tallyapp

import android.os.Bundle
import com.example.tallyapp.logging.TallyLog as Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.tallyapp.ui.notification.NotificationAccess
import com.example.tallyapp.ui.AppScreen
import com.example.tallyapp.ui.AppViewModel
import com.example.tallyapp.ui.theme.UnscrambleTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.i("TallyApp", "tallyapp start")
        setContent {
            UnscrambleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val state by viewModel.uiState.collectAsState()
                    AppScreen(
                        state = state,
                        onSelectTab = viewModel::selectTab,
                        onSelectPage = viewModel::selectPage,
                        onOpenSettings = { NotificationAccess.openSettings(this@MainActivity) },
                        onReconnect = viewModel::reconnectNotifications,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForeground()
    }
}
