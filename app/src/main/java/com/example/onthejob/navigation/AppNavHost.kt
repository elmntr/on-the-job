package com.example.onthejob.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.onthejob.ui.screens.*
import com.example.onthejob.ui.newentry.NewEntryScreen
@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(Route.LogFeed)

    Scaffold(
        bottomBar = {
            val current = backStack.lastOrNull()
            if (current in Route.bottomNavItems) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current == Route.LogFeed,
                        onClick = { if (current != Route.LogFeed) { backStack.clear(); backStack.add(Route.LogFeed) } },
                        icon = { Icon(Icons.Filled.List, contentDescription = "Log feed") },
                        label = { Text("Log") },
                    )
                    NavigationBarItem(
                        selected = current == Route.Calendar,
                        onClick = { if (current != Route.Calendar) { backStack.clear(); backStack.add(Route.Calendar) } },
                        icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendar") },
                        label = { Text("Calendar") },
                    )
                    NavigationBarItem(
                        selected = current == Route.TimeTracking,
                        onClick = { if (current != Route.TimeTracking) { backStack.clear(); backStack.add(Route.TimeTracking) } },
                        icon = { Icon(Icons.Filled.Schedule, contentDescription = "Time tracking") },
                        label = { Text("Time") },
                    )
                }
            }
        }
    ) { padding ->
        NavDisplay(
            modifier = Modifier.padding(padding),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<Route.LogFeed> {
                    LogFeedScreen(
                        onNewEntry = { backStack.add(Route.NewEntry) },
                        onOpenEntry = { id -> backStack.add(Route.EntryDetail(id)) },
                    )
                }
                entry<Route.Calendar> { CalendarScreen() }
                entry<Route.TimeTracking> { TimeTrackingScreen() }
                entry<Route.NewEntry> { NewEntryScreen(onBack = { backStack.removeLastOrNull() }) }
                entry<Route.EntryDetail> { key ->
                    EntryDetailScreen(entryId = key.entryId, onBack = { backStack.removeLastOrNull() })
                }
                entry<Route.PdfExport> { PdfExportScreen(onBack = { backStack.removeLastOrNull() }) }
            },
        )
    }
}