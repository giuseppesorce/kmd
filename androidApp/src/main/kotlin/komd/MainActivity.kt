// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import komd.shared.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
