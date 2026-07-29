package com.alisworld.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alisworld.app.data.DashboardSummary
import com.alisworld.app.ui.theme.AlisPurple
import com.alisworld.app.ui.theme.AlisPurpleDark
import com.alisworld.app.ui.theme.GreenProfit
import com.alisworld.app.ui.theme.RedLoss
import com.alisworld.app.ui.viewmodels.DashboardViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AlisWorld", fontWeight = FontWeight.Bold)
                        Text("MT5 portfolio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh portfolio")
                    }
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isLoading),
            onRefresh = { viewModel.loadDashboard() },
            modifier = Modifier.padding(paddingValues)
        ) {
            when {
                uiState.error != null -> DashboardError(uiState.error ?: "Unable to load portfolio", viewModel::loadDashboard)
                uiState.summary != null -> DashboardContent(uiState.summary!!)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun DashboardContent(summary: DashboardSummary) {
    val positive = summary.dailyPnl >= 0
    val pnlColor = if (positive) GreenProfit else RedLoss
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Portfolio overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Live figures from your connected MT5 terminal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.background(Brush.linearGradient(listOf(AlisPurple, AlisPurpleDark))).padding(22.dp)
            ) {
                Text("LIVE FLOATING P&L", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(formatMoney(summary.dailyPnl, summary.currency), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%s%.2f%% today", if (summary.dailyPnlPct >= 0) "+" else "", summary.dailyPnlPct),
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeroMetric("OPEN POSITIONS", summary.openPositionsCount.toString())
                    HeroMetric("FLOATING", formatMoney(summary.openPositionsValue, summary.currency))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AccountMetric("Balance", formatMoney(summary.balance, summary.currency), Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
            AccountMetric("Equity", formatMoney(summary.equity, summary.currency), Icons.Default.ShowChart, Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Account health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                SummaryRow("Balance", formatMoney(summary.balance, summary.currency))
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                SummaryRow("Equity", formatMoney(summary.equity, summary.currency))
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                SummaryRow("Open P&L", formatMoney(summary.openPositionsValue, summary.currency), pnlColor)
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.68f), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

private fun formatMoney(value: Double, currency: String) = String.format(Locale.US, "%s%.2f %s", if (value >= 0) "+" else "", value, currency)

@Composable
private fun DashboardError(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(14.dp))
            Button(onClick = retry) { Text("Try again") }
        }
    }
}
