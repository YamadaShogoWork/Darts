package com.dartspractice.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dartspractice.app.ui.navigation.DartsApp
import com.dartspractice.app.ui.theme.DartsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 端末がライトモードでも、上下のシステムバーは常にダーク表示(白いアイコン・黒い帯)にする
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            DartsTheme {
                DartsApp()
            }
        }
    }
}
