package com.plantia.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.MockDashboardData
import com.plantia.screens.home.components.DailyMissionCard
import com.plantia.screens.home.components.PlantSummaryCard
import com.plantia.screens.home.components.QuickAccessGrid
import com.plantia.screens.home.components.UserHeaderCard
import com.plantia.screens.home.components.WeatherAlertCard
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit = {}
) {
    val bottomNavItems = listOf(
        BottomNavItem("Inicio", Icons.Default.Home),
        BottomNavItem("Escanear", Icons.Default.PhotoCamera),
        BottomNavItem("Jardin", Icons.Default.Yard),
        BottomNavItem("Perfil", Icons.Default.Person)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inicio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AmberLight,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "480",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAlert
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = GreenPrimary,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "J",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 1) {
                                onNavigateToScan()
                            } else {
                                selectedTab = index
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = GreenPrimary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            UserHeaderCard(user = MockDashboardData.userSummary)

            WeatherAlertCard(
                alert = MockDashboardData.weatherAlert,
                onActionClick = {}
            )

            PlantSummaryCard(
                plant = MockDashboardData.plantSummary,
                onEnterGarden = {}
            )

            QuickAccessGrid(
                items = MockDashboardData.quickAccessItems,
                onItemClick = { item ->
                    if (item.id == "scan") {
                        onNavigateToScan()
                    }
                }
            )

            DailyMissionCard(mission = MockDashboardData.dailyMission)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
