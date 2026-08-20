package com.android.tskmgr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.tskmgr.ui.PerformanceViewModel
import com.android.tskmgr.ui.ProcessViewModel
import com.android.tskmgr.ui.SettingsViewModel
import com.android.tskmgr.ui.StorageViewModel
import com.android.tskmgr.ui.screens.PerformanceScreen
import com.android.tskmgr.ui.screens.ProcessesScreen
import com.android.tskmgr.ui.screens.SettingsScreen
import com.android.tskmgr.ui.screens.StorageScreen
import com.android.tskmgr.ui.theme.TskMgrTheme

private data class Tab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TskMgrTheme {
                TaskManagerApp()
            }
        }
    }
}

@Composable
private fun TaskManagerApp() {
    val tabs = listOf(
        Tab("Processes", Icons.AutoMirrored.Filled.ViewList),
        Tab("Performance", Icons.Default.Memory),
        Tab("Storage", Icons.Default.Folder),
        Tab("Settings", Icons.Default.Settings),
    )
    var selected by rememberSaveable { mutableIntStateOf(0) }

    val processVm: ProcessViewModel = viewModel()
    val perfVm: PerformanceViewModel = viewModel()
    val storageVm: StorageViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selected) {
            0 -> Box(contentModifier.fillMaxSize()) { ProcessesScreen(processVm) }
            1 -> Box(contentModifier.fillMaxSize()) { PerformanceScreen(perfVm) }
            2 -> Box(contentModifier.fillMaxSize()) { StorageScreen(storageVm) }
            else -> Box(contentModifier.fillMaxSize()) { SettingsScreen(settingsVm) }
        }
    }
}
