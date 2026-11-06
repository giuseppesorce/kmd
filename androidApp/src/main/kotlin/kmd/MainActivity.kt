package kmd

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import kmd.shared.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
