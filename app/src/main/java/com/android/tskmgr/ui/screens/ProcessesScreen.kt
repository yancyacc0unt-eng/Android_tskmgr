package com.android.tskmgr.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.tskmgr.data.PermissionsHelper
import com.android.tskmgr.data.ProcessInfo
import com.android.tskmgr.data.RecentAppInfo
import com.android.tskmgr.data.SystemMetrics
import com.android.tskmgr.ui.ProcessViewModel
import java.util.concurrent.TimeUnit

@Composable
fun ProcessesScreen(viewModel: ProcessViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbar) {
        val msg = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    Box(Modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn {
                item {
                    InfoBanner()
                }
                item {
                    SectionHeader("Running processes (${state.processes.size})")
                }
                items(state.processes, key = { "${it.pid}-${it.processName}" }) { process ->
                    ProcessRow(process = process, onKill = { viewModel.killProcess(process) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                item {
                    SectionHeader("Recently used apps (${state.recentApps.size})")
                }
                if (!state.hasUsageAccess) {
                    item {
                        UsageAccessHint(onOpenSettings = { PermissionsHelper.openUsageAccessSettings(context) })
                    }
                }
                // Key includes lastUsedMillis so two entries of the same package
                // (should be deduped in the repository, but be safe) can never
                // collide and crash the LazyColumn with a duplicate-key exception.
                items(state.recentApps, key = { "${it.packageName}-${it.lastUsedMillis}" }) { app ->
                    RecentAppRow(app)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun InfoBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
    ) {
        Text(
            text = "Android 12+ hides most processes from non-root apps. The list below shows what the system exposes, plus recently used apps when usage access is granted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
    )
}

@Composable
private fun UsageAccessHint(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Usage access is off. Enable it to see active and recently used apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun ProcessRow(process: ProcessInfo, onKill: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(process.appIcon, Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(process.appLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = "${process.processName}  ·  ${process.importance}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(process.pid.toString(), Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
        Text(
            text = SystemMetrics.formatBytes(process.memoryKb * 1024),
            Modifier.width(88.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onKill),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Stop ${process.appLabel}",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(7.dp),
            )
        }
    }
}

@Composable
private fun RecentAppRow(app: RecentAppInfo) {
    val statusText = when {
        app.isRunning -> "Running"
        app.isActive -> "Active"
        else -> "Background"
    }
    val statusColor = when {
        app.isRunning -> MaterialTheme.colorScheme.primary
        app.isActive -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.appIcon, Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.appLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = "${app.packageName}  ·  ${formatLastUsed(app.lastUsedMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
        )
    }
}

@Composable
private fun AppIcon(icon: android.graphics.drawable.Drawable?, modifier: Modifier = Modifier) {
    if (icon != null) {
        val bmp = remember(icon) {
            try {
                icon.toBitmap(96, 96).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = modifier)
        } else {
            Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
        }
    } else {
        Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
    }
}

private fun formatLastUsed(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} min ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} h ago"
        else -> "${diff / TimeUnit.DAYS.toMillis(1)} d ago"
    }
}
