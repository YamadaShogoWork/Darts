package com.dartspractice.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dartspractice.app.ui.screens.AwardSettingsScreen
import com.dartspractice.app.ui.screens.DetailScreen
import com.dartspractice.app.ui.screens.GraphScreen
import com.dartspractice.app.ui.screens.HomeScreen
import com.dartspractice.app.ui.screens.RecordScreen
import com.dartspractice.app.ui.screens.ResultScreen
import com.dartspractice.app.ui.screens.SettingsScreen
import com.dartspractice.app.ui.theme.DartsColors
import com.dartspractice.app.ui.theme.DartsIcons

/** 下部ナビのタブ。モックアップどおり ホーム / グラフ / 履歴 の3つ。「履歴」は日別詳細を開く。 */
private enum class Tab(val label: String, val icon: ImageVector, val route: Any) {
    Home("ホーム", DartsIcons.Home, HomeRoute),
    Graph("グラフ", DartsIcons.Graph, GraphRoute),
    History("履歴", DartsIcons.History, DetailRoute()),
}

private fun NavDestination?.currentTab(): Tab? = when {
    this == null -> null
    hasRoute<HomeRoute>() -> Tab.Home
    hasRoute<GraphRoute>() -> Tab.Graph
    hasRoute<DetailRoute>() -> Tab.History
    else -> null // 記録入力・結果・設定は下部ナビを出さない
}

@Composable
fun DartsApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val tab = backStack?.destination.currentTab()

    Scaffold(
        containerColor = DartsColors.Background,
        bottomBar = { if (tab != null) BottomNav(tab) { nav.switchTab(it) } },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenRecord = { sessionId -> nav.navigate(RecordRoute(sessionId)) },
                    onOpenSettings = { nav.navigate(SettingsRoute) },
                    onOpenGraph = { nav.switchTab(Tab.Graph) },
                    onOpenDay = { date -> nav.navigate(DetailRoute(date)) },
                )
            }
            composable<GraphRoute> {
                GraphScreen(onOpenDay = { date -> nav.navigate(DetailRoute(date)) })
            }
            composable<DetailRoute> { entry ->
                DetailScreen(
                    date = entry.toRoute<DetailRoute>().date,
                    onBack = { nav.switchTab(Tab.Graph) },
                )
            }
            composable<RecordRoute> { entry ->
                RecordScreen(
                    sessionId = entry.toRoute<RecordRoute>().sessionId,
                    onShowResult = { sessionId ->
                        nav.navigate(ResultRoute(sessionId)) { popUpTo<RecordRoute> { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable<ResultRoute> { entry ->
                ResultScreen(
                    sessionId = entry.toRoute<ResultRoute>().sessionId,
                    onDone = { nav.popBackStack(HomeRoute, inclusive = false) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenAwards = { nav.navigate(AwardSettingsRoute) },
                )
            }
            composable<AwardSettingsRoute> {
                AwardSettingsScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(DartsColors.NavBar).navigationBarsPadding()) {
        HorizontalDivider(color = DartsColors.Border, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            Tab.entries.forEach { t ->
                val selected = t == current
                val color = if (selected) DartsColors.Accent else DartsColors.SubText
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(role = Role.Tab) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(t.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        t.label,
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
