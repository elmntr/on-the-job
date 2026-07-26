package com.example.onthejob.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.onthejob.ui.screens.*
import com.example.onthejob.ui.newentry.NewEntryScreen
import com.example.onthejob.ui.logfeed.LogFeedScreen
import com.example.onthejob.ui.theme.Amber
import com.example.onthejob.ui.theme.CondFontFamily
import com.example.onthejob.ui.theme.Ink
import com.example.onthejob.ui.theme.Ink2
import com.example.onthejob.ui.theme.Paper
import com.example.onthejob.ui.calendar.CalendarScreen

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(Route.LogFeed)

    // Bottom nav treated as another dark surface (same family as HoursCard),
    // rather than left on Material3 defaults — DESIGN.md doesn't cover this
    // component explicitly, so this pulls only from existing tokens: Ink
    // background, Amber for the selected state, Paper at reduced opacity for
    // unselected (mirrors HoursCard's own secondary-text treatment).
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Amber,
        selectedTextColor = Amber,
        indicatorColor = Ink2,
        unselectedIconColor = Paper.copy(alpha = 0.55f),
        unselectedTextColor = Paper.copy(alpha = 0.55f),
    )

    @Composable
    fun NavLabel(text: String) {
        Text(
            text = text,
            fontFamily = CondFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
        )
    }

    Scaffold(
        bottomBar = {
            val current = backStack.lastOrNull()
            if (current in Route.bottomNavItems) {
                NavigationBar(containerColor = Ink) {
                    NavigationBarItem(
                        selected = current == Route.LogFeed,
                        onClick = { if (current != Route.LogFeed) { backStack.clear(); backStack.add(Route.LogFeed) } },
                        icon = { Icon(Icons.Filled.List, contentDescription = "Log feed") },
                        label = { NavLabel("Log") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = current == Route.Calendar,
                        onClick = { if (current != Route.Calendar) { backStack.clear(); backStack.add(Route.Calendar) } },
                        icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendar") },
                        label = { NavLabel("Calendar") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = current == Route.TimeTracking,
                        onClick = { if (current != Route.TimeTracking) { backStack.clear(); backStack.add(Route.TimeTracking) } },
                        icon = { Icon(Icons.Filled.Schedule, contentDescription = "Time tracking") },
                        label = { NavLabel("Time") },
                        colors = navItemColors,
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
                entry<Route.Calendar> {
                    CalendarScreen(onOpenEntry = { id -> backStack.add(Route.EntryDetail(id)) })
                }
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