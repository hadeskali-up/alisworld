package com.alisworld.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alisworld.app.data.HistoryItem
import com.alisworld.app.ui.theme.GreenProfit
import com.alisworld.app.ui.theme.GreenSoft
import com.alisworld.app.ui.theme.RedLoss
import com.alisworld.app.ui.theme.RedSoft
import com.alisworld.app.ui.viewmodels.HistoryUiState
import com.alisworld.app.ui.viewmodels.HistoryViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Closed trades", fontWeight = FontWeight.Bold)
                        Text("Realised P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { IconButton(onClick = viewModel::loadHistory) { Icon(Icons.Default.Refresh, "Refresh closed trades") } }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isLoading),
            onRefresh = viewModel::loadHistory,
            modifier = Modifier.padding(paddingValues)
        ) {
            when {
                uiState.error != null -> HistoryError(uiState.error ?: "Unable to load history", viewModel::loadHistory)
                uiState.items.isEmpty() && !uiState.isLoading -> HistoryEmpty()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { HistoryInsights(uiState, viewModel::selectSymbol) }
                    items(uiState.filteredItems, key = { it.ticket }) { HistoryCard(it) }
                    if (uiState.filteredItems.isEmpty()) item { FilterEmpty(uiState.selectedSymbol) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryInsights(state: HistoryUiState, onSelectSymbol: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PnlMetric("DAILY P&L", formatPnl(state.dailyPnl), Modifier.weight(1f))
            PnlMetric("ALL-TIME P&L", formatPnl(state.allTimePnl), Modifier.weight(1f))
        }
        PnlMetric(
            label = if (state.selectedSymbol == null) "THIS LIST P&L" else "${state.selectedSymbol} P&L",
            value = formatPnl(state.filteredListPnl),
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = state.selectedSymbol ?: "All symbols",
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter symbol") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("All symbols") }, onClick = { onSelectSymbol(null); expanded = false })
                state.symbols.forEach { symbol ->
                    DropdownMenuItem(text = { Text(symbol) }, onClick = { onSelectSymbol(symbol); expanded = false })
                }
            }
        }
        Text(
            "Daily P&L uses Malaysia time (UTC+8). All-time P&L is calculated from stored realised trades.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PnlMetric(label: String, value: String, modifier: Modifier) {
    val positive = !value.startsWith("-")
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (positive) GreenProfit else RedLoss)
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    val isProfit = item.profit >= 0
    val pnlColor = if (isProfit) GreenProfit else RedLoss
    val pnlSurface = if (isProfit) GreenSoft else RedSoft
    val directionColor = if (item.type.equals("buy", true)) Color(0xFF1976D2) else Color(0xFFE66A22)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(pnlSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, null, tint = pnlColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(item.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = directionColor, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(directionColor.copy(alpha = 0.10f)).padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text("${"%.2f".format(Locale.US, item.volume)} lots  •  ${formatClosedTime(item.closeTime)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPnl(item.profit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = pnlColor)
                Text("CLOSED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatPnl(value: Double) = String.format(Locale.US, "%s%.2f USD", if (value >= 0) "+" else "", value)
private fun formatClosedTime(value: String): String = runCatching { LocalDateTime.parse(value.take(19)).format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.US)) }.getOrDefault(value)

@Composable private fun FilterEmpty(symbol: String?) { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No closed trades for ${symbol ?: "this filter"}") } }
@Composable private fun HistoryEmpty() { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(14.dp)); Text("No closed trades yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("Closed positions and their realised P&L will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun HistoryError(message: String, retry: () -> Unit) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(message, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(14.dp)); Button(onClick = retry) { Text("Try again") } } } }
