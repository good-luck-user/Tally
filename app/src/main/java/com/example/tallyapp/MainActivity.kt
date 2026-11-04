package com.example.tallyapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import com.example.tallyapp.notification.NotificationAccess
import com.example.tallyapp.ui.TransactionsScreen
import com.example.tallyapp.ui.theme.MytallyTheme

/** Main activity with Compose UI for transactions screen */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查通知权限
        if (NotificationAccess.isGranted(this)) {
            NotificationAccess.reconnectIfGranted(this)
            Log.i("MytallyNotification", "Notification access is granted; waiting for notifications.")

            // 设置 Compose UI
            setContent {
                MytallyTheme {
                    TransactionsScreen()
                }
            }
        } else {
            Toast.makeText(this, R.string.notification_access_required, Toast.LENGTH_LONG).show()
            NotificationAccess.openSettings(this)
            finish()
        }
    }
}
