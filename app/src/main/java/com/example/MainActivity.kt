package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

// Custom Indian Rupee Formatter with Lakh/Crore grouping (en-IN)
fun formatRupee(value: Double): String {
    val isNegative = value < 0
    val absValue = Math.abs(value)
    val longVal = absValue.toLong()
    val decimal = String.format(Locale.US, "%.2f", absValue - longVal).substring(1) // e.g. ".50"
    val valStr = longVal.toString()
    
    val formatted = if (valStr.length <= 3) {
        valStr
    } else {
        val lastThree = valStr.substring(valStr.length - 3)
        val remaining = valStr.substring(0, valStr.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(",")
                count = 0
            }
        }
        sb.reverse().toString() + "," + lastThree
    }
    val sign = if (isNegative) "-" else ""
    return "$sign₹$formatted$decimal"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: FactoryViewModel = viewModel()) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val feedbackMsg by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(feedbackMsg) {
        if (feedbackMsg != null) {
            kotlinx.coroutines.delay(4000)
            RepositoryProvider.feedbackMessage.value = null
        }
    }
    
    if (!isLoggedIn) {
        AuthScreen(viewModel, lang)
    } else {
        var currentTab by remember { mutableStateOf(0) }
        val tabs = listOf(
            Loc.t("dashboard", lang) to Icons.Default.Dashboard,
            Loc.t("stock_ledger", lang) to Icons.Default.Inventory,
            Loc.t("directory", lang) to Icons.Default.ContactPhone,
            Loc.t("expenses", lang) to Icons.Default.ReceiptLong,
            Loc.t("staff_attendance", lang) to Icons.Default.Groups,
            Loc.t("ai_cfo", lang) to Icons.Default.Psychology
        )

        Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = KajuCream,
        topBar = {
            Column {
                // Header Panel with Brand Identity and Language Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            val y = size.height - strokeWidth / 2
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(KajuGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SMI",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Loc.t("app_title", lang),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }

                        // Header Actions (Profile & Overflow Options Menu)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var showProfileDialog by remember { mutableStateOf(false) }
                            var showMenu by remember { mutableStateOf(false) }
                            val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

                            // Profile button matching screenshot
                            IconButton(
                                onClick = { showProfileDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Profile",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 3-dot overflow menu
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (lang == Language.EN) "Sync with Cloud" else "క్లౌడ్‌తో సమకాలీకరించు") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.syncWithSupabase()
                                        },
                                        leadingIcon = {
                                            if (isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = KajuGold,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudSync,
                                                    contentDescription = "Sync",
                                                    tint = KajuGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (lang == Language.EN) "Change Language" else "భాష మార్చండి") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.toggleLanguage()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = "Language",
                                                tint = KajuGold,
                                                modifier = Modifier.size(20.dp)
                                             )
                                        },
                                        trailingIcon = {
                                            Text(
                                                text = if (lang == Language.EN) "TE" else "EN",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberGray
                                            )
                                        }
                                    )
                                }
                            }

                            if (showProfileDialog) {
                                val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
                                AlertDialog(
                                    onDismissRequest = { showProfileDialog = false },
                                    title = { Text(text = if (lang == Language.EN) "User Profile" else "వినియోగదారు ప్రొఫైల్") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(text = "Email: ${userEmail ?: "sujeethsaiarigela@gmail.com"}", fontWeight = FontWeight.Medium)
                                            Text(text = "Role: Administrator", color = AmberGray)
                                        }
                                    },
                                    confirmButton = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    showProfileDialog = false
                                                    viewModel.signOutFromSupabase()
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = RedPending)
                                            ) {
                                                Text(if (lang == Language.EN) "Sign Out" else "లాగ్ అవుట్")
                                            }
                                            TextButton(onClick = { showProfileDialog = false }) {
                                                Text("OK")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Scrollable tab switcher for 6 sections to ensure perfect visual presentation
                ScrollableTabRow(
                    selectedTabIndex = currentTab,
                    containerColor = Color.White,
                    contentColor = KajuGold,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = KajuGold,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            selectedContentColor = KajuGold,
                            unselectedContentColor = AmberGray
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content area supporting smooth crossfade
            when (currentTab) {
                0 -> DashboardView(viewModel, lang, onNavigateToCfo = { currentTab = 5 })
                1 -> InventoryView(viewModel, lang)
                2 -> DirectoryView(viewModel, lang)
                3 -> ExpensesView(viewModel, lang)
                4 -> StaffAttendanceView(viewModel, lang)
                5 -> CfoAdvisorView(viewModel, lang)
            }

            // Top overlay Feedback Banner (Slide-in simulation)
            AnimatedVisibility(
                visible = feedbackMsg != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PistachioGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth(0.9f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feedbackMsg ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
    }
}

// ==========================================
// VIEW 1: DASHBOARD
// ==========================================
@Composable
fun DashboardView(viewModel: FactoryViewModel, lang: Language, onNavigateToCfo: () -> Unit) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val stockLogs by viewModel.stockLogs.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val staff by viewModel.staff.collectAsStateWithLifecycle()

    val financials = remember(stockLogs, expenses, staff) {
        viewModel.getTimelineFinancials(stockLogs, expenses, staff)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High density financial metrics (2x2 grid in standard columnar wrapping)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metric 1: Cash In
                    DashboardMetricCard(
                        title = Loc.t("total_cash_in", lang),
                        value = formatRupee(financials.revenue),
                        icon = Icons.Default.TrendingUp,
                        color = PistachioGreen,
                        bgColor = PistachioLight,
                        modifier = Modifier.weight(1f)
                    )

                    // Metric 2: Net Margin
                    val marginPct = if (financials.revenue > 0) {
                        (financials.netProfit / financials.revenue) * 100
                    } else 0.0
                    DashboardMetricCard(
                        title = Loc.t("net_margin", lang),
                        value = String.format(Locale.US, "%.1f%%", marginPct),
                        icon = if (financials.netProfit >= 0) Icons.Default.CheckCircle else Icons.Default.TrendingDown,
                        color = if (financials.netProfit >= 0) PistachioGreen else RedPending,
                        bgColor = if (financials.netProfit >= 0) PistachioLight else RedLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metric 3: Total Expenses
                    DashboardMetricCard(
                        title = Loc.t("timeline_expenses", lang),
                        value = formatRupee(financials.totalExpenses),
                        icon = Icons.Default.TrendingDown,
                        color = RedPending,
                        bgColor = RedLight,
                        modifier = Modifier.weight(1f)
                    )

                    // Metric 4: Net Profit
                    DashboardMetricCard(
                        title = Loc.t("period_net_profit", lang),
                        value = formatRupee(financials.netProfit),
                        icon = Icons.Default.Savings,
                        color = KajuGold,
                        bgColor = KajuGoldLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Custom Visual Chart (Canvas drawing representing operations and budget)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("recent_sales_chart", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Compose Canvas Visual representation of finances
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Draw Grid Lines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = (h / gridLines) * i
                                drawLine(
                                    color = CardBorder.copy(alpha = 0.5f),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Calculate comparative bars for: Raw Material Cost, General Expenses, Staff Salaries, and Revenue
                            val maxVal = maxOf(financials.revenue, financials.rawMaterialCost, financials.otherExpenses, financials.proratedStaffOverhead, 1.0)
                            val scale = (h * 0.75f) / maxVal

                            val labels = listOf("Revenue", "Raw Mat", "Other Exp", "Staff")
                            val vals = listOf(financials.revenue, financials.rawMaterialCost, financials.otherExpenses, financials.proratedStaffOverhead)
                            val colors = listOf(PistachioGreen, KajuGold, RedPending, AmberGray)

                            val barWidth = w / 10f
                            val space = w / 5f

                            for (i in vals.indices) {
                                val barHeight = (vals[i] * scale).toFloat()
                                val x = space * (i + 1) - barWidth / 2

                                // Draw modern rounded rectangular bars
                                drawRect(
                                    color = colors[i],
                                    topLeft = Offset(x, h - barHeight),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }
                    }

                    // Legend representing the Canvas Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ChartLegendItem("Rev", PistachioGreen)
                        ChartLegendItem("Raw", KajuGold)
                        ChartLegendItem("Exp", RedPending)
                        ChartLegendItem("Staff", AmberGray)
                    }
                }
            }
        }

        // Active Stock summaries listing (Raw Material vs Finished Good allocations)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("stock_summary", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (products.isEmpty()) {
                        Text(
                            text = Loc.t("no_entries", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGray
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            products.forEach { product ->
                                val isRaw = product.classification == "Raw Material"
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isRaw) KajuGold else PistachioGreen)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = product.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = ChocolateBrown
                                            )
                                        }
                                        Text(
                                            text = "${product.currentStockKg} ${Loc.t("kg_symbol", lang)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ChocolateBrown
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Custom high-density stock allocation progress indicator
                                    val maxCapacity = 2000.0 // representation max
                                    val pct = (product.currentStockKg / maxCapacity).coerceIn(0.0, 1.0).toFloat()
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(KajuCream)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(pct)
                                                .background(if (isRaw) KajuGold else PistachioGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = AmberGray,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = ChocolateBrown
            )
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGray)
    }
}

// ==========================================
// VIEW 2: STOCK & INVENTORY LEDGER
// ==========================================
@Composable
fun InventoryView(viewModel: FactoryViewModel, lang: Language) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val stockLogs by viewModel.stockLogs.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showLogMovementDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High density visual header control panel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = KajuGold),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_product_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Loc.t("add_product", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showLogMovementDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PistachioGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("log_stock_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Loc.t("receive_stock", lang).split(" ")[0] + "/" + Loc.t("ship_stock", lang).split(" ")[0], fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Stock list
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("stock_ledger", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (products.isEmpty()) {
                        Text(
                            text = Loc.t("no_entries", lang),
                            color = AmberGray,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            products.forEach { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KajuCream)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            color = ChocolateBrown,
                                            fontSize = 15.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (product.classification == "Raw Material") KajuGold.copy(alpha = 0.15f) else PistachioGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (product.classification == "Raw Material") Loc.t("raw_material", lang) else Loc.t("finished_good", lang),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (product.classification == "Raw Material") KajuGold else PistachioGreen
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${Loc.t("cost_price", lang).split(" ")[0]}: ${formatRupee(product.costPricePerKg)}",
                                                fontSize = 11.sp,
                                                color = AmberGray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${product.currentStockKg} ${Loc.t("kg_symbol", lang)}",
                                                fontWeight = FontWeight.Black,
                                                color = ChocolateBrown,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${Loc.t("initial_stock", lang).split(" ")[0]}: ${product.initialStockKg} ${Loc.t("kg_symbol", lang)}",
                                                fontSize = 11.sp,
                                                color = AmberGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteProduct(product) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RedPending,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Movements Ledger
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("recent_movements", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (stockLogs.isEmpty()) {
                        Text(
                            text = Loc.t("no_entries", lang),
                            color = AmberGray,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            stockLogs.forEach { log ->
                                val isInbound = log.type == "Inbound"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isInbound) PistachioLight.copy(alpha = 0.4f) else KajuCream)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isInbound) PistachioGreen.copy(alpha = 0.2f) else KajuGold.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isInbound) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isInbound) PistachioGreen else KajuGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = log.productName,
                                                fontWeight = FontWeight.Bold,
                                                color = ChocolateBrown,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${if (isInbound) Loc.t("supplier", lang) else Loc.t("customer", lang)}: ${log.contactName}",
                                                fontSize = 11.sp,
                                                color = AmberGray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${if (isInbound) "+" else "-"}${log.quantityKg} ${Loc.t("kg_symbol", lang)}",
                                                fontWeight = FontWeight.Black,
                                                color = if (isInbound) PistachioGreen else RedPending,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = formatRupee(log.quantityKg * log.pricePerKg),
                                                fontSize = 11.sp,
                                                color = AmberGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteStockLog(log) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RedPending,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Form: Add Product
    if (showAddProductDialog) {
        var prodName by remember { mutableStateOf("") }
        var classification by remember { mutableStateOf("Raw Material") } // or "Finished Good"
        var initialStockStr by remember { mutableStateOf("") }
        var costPriceStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text(text = Loc.t("add_product", lang), fontWeight = FontWeight.Bold, color = ChocolateBrown) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text(text = Loc.t("name", lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        singleLine = true
                    )

                    Column {
                        Text(text = Loc.t("classification", lang), fontSize = 12.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            FilterChip(
                                selected = classification == "Raw Material",
                                onClick = { classification = "Raw Material" },
                                label = { Text(Loc.t("inbound_raw", lang)) }
                            )
                            FilterChip(
                                selected = classification == "Finished Good",
                                onClick = { classification = "Finished Good" },
                                label = { Text(Loc.t("outbound_finished", lang)) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = initialStockStr,
                        onValueChange = { initialStockStr = it },
                        label = { Text(text = Loc.t("initial_stock", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("initial_stock_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text(text = Loc.t("cost_price", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cost_price_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initStock = initialStockStr.toDoubleOrNull() ?: 0.0
                        val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
                        if (prodName.isNotBlank() && initStock >= 0 && costPrice >= 0) {
                            viewModel.addProduct(prodName, classification, initStock, costPrice)
                            showAddProductDialog = false
                        } else {
                            viewModel.showFeedback(Loc.t("error_fields", lang))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KajuGold),
                    modifier = Modifier.testTag("save_product_button")
                ) {
                    Text(text = Loc.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text(text = Loc.t("cancel", lang), color = AmberGray)
                }
            }
        )
    }

    // Modal Form: Log Stock Inbound/Outbound
    if (showLogMovementDialog) {
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var quantityStr by remember { mutableStateOf("") }
        var customPriceStr by remember { mutableStateOf("") }
        var selectedContact by remember { mutableStateOf<Contact?>(null) }

        // Filter contact lists based on selection
        val eligibleProducts = products
        val eligibleContacts = contacts

        var logType by remember { mutableStateOf("Inbound") } // "Inbound" or "Outbound"

        // Rule Enforcements:
        // - Inbound display only products classified as "Raw Material" + require active Supplier
        // - Outbound display only products classified as "Finished Good" + require active Customer
        val filteredProducts = if (logType == "Inbound") {
            eligibleProducts.filter { it.classification == "Raw Material" }
        } else {
            eligibleProducts.filter { it.classification == "Finished Good" }
        }

        val filteredContacts = if (logType == "Inbound") {
            eligibleContacts.filter { it.classification == "Supplier" }
        } else {
            eligibleContacts.filter { it.classification == "Customer" }
        }

        AlertDialog(
            onDismissRequest = { showLogMovementDialog = false },
            title = { Text(text = if (logType == "Inbound") Loc.t("receive_stock", lang) else Loc.t("ship_stock", lang), fontWeight = FontWeight.Bold, color = ChocolateBrown) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                logType = "Inbound"
                                selectedProduct = null
                                selectedContact = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (logType == "Inbound") PistachioGreen else KajuCream,
                                contentColor = if (logType == "Inbound") Color.White else ChocolateBrown
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = Loc.t("receive_stock", lang).split(" ")[0])
                        }

                        Button(
                            onClick = {
                                logType = "Outbound"
                                selectedProduct = null
                                selectedContact = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (logType == "Outbound") KajuGold else KajuCream,
                                contentColor = if (logType == "Outbound") Color.White else ChocolateBrown
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = Loc.t("ship_stock", lang).split(" ")[0])
                        }
                    }

                    Text(
                        text = if (logType == "Inbound") Loc.t("inbound_desc", lang) else Loc.t("outbound_desc", lang),
                        fontSize = 11.sp,
                        color = AmberGray
                    )

                    // Product Selector
                    Column {
                        Text(text = Loc.t("select_product", lang), fontSize = 12.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = selectedProduct?.name ?: Loc.t("select_product", lang))
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                filteredProducts.forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text(prod.name) },
                                        onClick = {
                                            selectedProduct = prod
                                            customPriceStr = prod.costPricePerKg.toString()
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Contact Selector
                    Column {
                        Text(text = if (logType == "Inbound") Loc.t("select_supplier", lang) else Loc.t("select_customer", lang), fontSize = 12.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                        var expandedContact by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedContact = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = selectedContact?.name ?: (if (logType == "Inbound") Loc.t("active_supplier", lang) else Loc.t("active_customer", lang)))
                            }
                            DropdownMenu(
                                expanded = expandedContact,
                                onDismissRequest = { expandedContact = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                filteredContacts.forEach { con ->
                                    DropdownMenuItem(
                                        text = { Text(con.name) },
                                        onClick = {
                                            selectedContact = con
                                            expandedContact = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Quantity Entry
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text(text = Loc.t("quantity", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Price per KG Entry
                    OutlinedTextField(
                        value = customPriceStr,
                        onValueChange = { customPriceStr = it },
                        label = { Text(text = Loc.t("price_per_kg", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prod = selectedProduct
                        val con = selectedContact
                        val qty = quantityStr.toDoubleOrNull() ?: 0.0
                        val prc = customPriceStr.toDoubleOrNull() ?: 0.0
                        if (prod != null && con != null && qty > 0 && prc > 0) {
                            viewModel.logStockMovement(
                                productId = prod.id,
                                productName = prod.name,
                                classification = prod.classification,
                                type = logType,
                                quantity = qty,
                                pricePerKg = prc,
                                contactId = con.id,
                                contactName = con.name
                            )
                            showLogMovementDialog = false
                        } else {
                            viewModel.showFeedback(Loc.t("error_fields", lang))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (logType == "Inbound") PistachioGreen else KajuGold)
                ) {
                    Text(text = Loc.t("submit", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogMovementDialog = false }) {
                    Text(text = Loc.t("cancel", lang), color = AmberGray)
                }
            }
        )
    }
}

// ==========================================
// VIEW 3: SUPPLIERS & CUSTOMERS DIRECTORY
// ==========================================
@Composable
fun DirectoryView(viewModel: FactoryViewModel, lang: Language) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showAddContactDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Button(
                onClick = { showAddContactDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KajuGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_contact_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = Loc.t("add_contact", lang), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("contacts_directory", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (contacts.isEmpty()) {
                        Text(
                            text = Loc.t("no_entries", lang),
                            color = AmberGray,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            contacts.forEach { contact ->
                                val isSupplier = contact.classification == "Supplier"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KajuCream)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Bold,
                                            color = ChocolateBrown,
                                            fontSize = 15.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSupplier) KajuGold.copy(alpha = 0.15f) else PistachioGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isSupplier) Loc.t("supplier", lang) else Loc.t("customer", lang),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSupplier) KajuGold else PistachioGreen
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = contact.location,
                                                fontSize = 11.sp,
                                                color = AmberGray
                                            )
                                        }
                                        Text(
                                            text = contact.phoneNumber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ChocolateBrown.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val context = LocalContext.current
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${contact.phoneNumber}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Call",
                                                tint = PistachioGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteContact(contact) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RedPending
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Form: Add Contact
    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var classification by remember { mutableStateOf("Supplier") } // or "Customer"
        var phone by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text(text = Loc.t("add_contact", lang), fontWeight = FontWeight.Bold, color = ChocolateBrown) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = Loc.t("name", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Column {
                        Text(text = Loc.t("classification", lang), fontSize = 12.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            FilterChip(
                                selected = classification == "Supplier",
                                onClick = { classification = "Supplier" },
                                label = { Text(Loc.t("supplier", lang)) }
                            )
                            FilterChip(
                                selected = classification == "Customer",
                                onClick = { classification = "Customer" },
                                label = { Text(Loc.t("customer", lang)) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(text = Loc.t("phone", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(text = Loc.t("location", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            viewModel.addContact(name, classification, phone, location)
                            showAddContactDialog = false
                        } else {
                            viewModel.showFeedback(Loc.t("error_fields", lang))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KajuGold)
                ) {
                    Text(text = Loc.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text(text = Loc.t("cancel", lang), color = AmberGray)
                }
            }
        )
    }
}

// ==========================================
// VIEW 4: EXPENSES & OUTFLOWS LEDGER
// ==========================================
@Composable
fun ExpensesView(viewModel: FactoryViewModel, lang: Language) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Settled", "Pending"

    val filteredExpenses = when (selectedFilter) {
        "Settled" -> expenses.filter { it.status == "Settled" }
        "Pending" -> expenses.filter { it.status == "Pending" }
        else -> expenses
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Button(
                onClick = { showAddExpenseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KajuGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_expense_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = Loc.t("add_expense", lang), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontal status filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Settled", "Pending").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val label = when (filter) {
                        "Settled" -> Loc.t("settled", lang)
                        "Pending" -> Loc.t("pending", lang)
                        else -> Loc.t("all", lang)
                    }
                    Button(
                        onClick = { selectedFilter = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) KajuGold else Color.White,
                            contentColor = if (isSelected) Color.White else ChocolateBrown
                        ),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (isSelected) KajuGold else CardBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expense rows
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("expenses_ledger", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredExpenses.isEmpty()) {
                        Text(
                            text = Loc.t("no_entries", lang),
                            color = AmberGray,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredExpenses.forEach { exp ->
                                val isSettled = exp.status == "Settled"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KajuCream)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = exp.title,
                                            fontWeight = FontWeight.Bold,
                                            color = ChocolateBrown,
                                            fontSize = 14.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSettled) PistachioLight else RedLight)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isSettled) Loc.t("settled", lang) else Loc.t("pending", lang),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSettled) PistachioGreen else RedPending
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = Loc.t(exp.category.lowercase(), lang),
                                                fontSize = 11.sp,
                                                color = AmberGray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatRupee(exp.amount),
                                            fontWeight = FontWeight.Black,
                                            color = ChocolateBrown,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteExpense(exp) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RedPending,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Form: Add Expense
    if (showAddExpenseDialog) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Settled") } // "Settled" or "Pending"
        var category by remember { mutableStateOf("Utilities") } // Utilities, Packaging, Fuel, Others

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text(text = Loc.t("add_expense", lang), fontWeight = FontWeight.Bold, color = ChocolateBrown) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(text = Loc.t("expense_title", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(text = Loc.t("amount", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Column {
                        Text(text = Loc.t("status", lang), fontSize = 12.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            FilterChip(
                                selected = status == "Settled",
                                onClick = { status = "Settled" },
                                label = { Text(Loc.t("settled", lang)) }
                            )
                            FilterChip(
                                selected = status == "Pending",
                                onClick = { status = "Pending" },
                                label = { Text(Loc.t("pending", lang)) }
                            )
                        }
                    }

                    Column {
                        Text(text = Loc.t("category", lang), fontSize = 12.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Utilities", "Packaging", "Fuel", "Others").forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(Loc.t(cat.lowercase(), lang)) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amount > 0) {
                            viewModel.addExpense(title, amount, status, category)
                            showAddExpenseDialog = false
                        } else {
                            viewModel.showFeedback(Loc.t("error_fields", lang))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KajuGold)
                ) {
                    Text(text = Loc.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text(text = Loc.t("cancel", lang), color = AmberGray)
                }
            }
        )
    }
}

// ==========================================
// VIEW 5: STAFF REGISTRY & DAILY ATTENDANCE
// ==========================================
@Composable
fun StaffAttendanceView(viewModel: FactoryViewModel, lang: Language) {
    val staffList by viewModel.staff.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceForSelectedDate.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDateStr.collectAsStateWithLifecycle()

    var showAddStaffDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High density visual registration
        item {
            Button(
                onClick = { showAddStaffDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KajuGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_staff_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = Loc.t("add_staff", lang), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Daily Attendance Control with previous/next date micro-interactions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Loc.t("attendance_checklist", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Date switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(KajuCream)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                // Simple previous day
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = sdf.parse(selectedDate) ?: Date()
                                val cal = Calendar.getInstance()
                                cal.time = date
                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                viewModel.selectedDateStr.value = sdf.format(cal.time)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev")
                        }

                        Text(
                            text = selectedDate,
                            fontWeight = FontWeight.Bold,
                            color = ChocolateBrown,
                            fontSize = 15.sp
                        )

                        IconButton(
                            onClick = {
                                // Simple next day
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = sdf.parse(selectedDate) ?: Date()
                                val cal = Calendar.getInstance()
                                cal.time = date
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                viewModel.selectedDateStr.value = sdf.format(cal.time)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (staffList.isEmpty()) {
                        Text(
                            text = Loc.t("no_entries", lang),
                            color = AmberGray,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            staffList.forEach { member ->
                                val record = attendanceList.find { it.staffId == member.id }
                                val isPresent = record?.isPresent ?: false // defaults to Absent/Not Present

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KajuCream)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = member.name,
                                            fontWeight = FontWeight.Bold,
                                            color = ChocolateBrown,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${member.role} • ${formatRupee(member.monthlySalary)}/${Loc.t("month", lang)}",
                                            fontSize = 11.sp,
                                            color = AmberGray
                                        )
                                    }

                                    // Touch target toggles (micro-interaction visual changes)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { viewModel.toggleAttendance(member.id, true) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPresent) PistachioGreen else Color.White,
                                                contentColor = if (isPresent) Color.White else AmberGray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .border(1.dp, if (isPresent) PistachioGreen else CardBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            Text(text = Loc.t("present", lang).split(" ")[0], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.toggleAttendance(member.id, false) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!isPresent) RedPending else Color.White,
                                                contentColor = if (!isPresent) Color.White else AmberGray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .border(1.dp, if (!isPresent) RedPending else CardBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            Text(text = Loc.t("absent", lang).split(" ")[0], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteStaff(member) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RedPending,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Form: Add Staff
    if (showAddStaffDialog) {
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("") }
        var salaryStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = { Text(text = Loc.t("add_staff", lang), fontWeight = FontWeight.Bold, color = ChocolateBrown) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = Loc.t("name", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(text = Loc.t("role", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = salaryStr,
                        onValueChange = { salaryStr = it },
                        label = { Text(text = Loc.t("monthly_salary", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sal = salaryStr.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && sal > 0) {
                            viewModel.addStaff(name, role, sal)
                            showAddStaffDialog = false
                        } else {
                            viewModel.showFeedback(Loc.t("error_fields", lang))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KajuGold)
                ) {
                    Text(text = Loc.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) {
                    Text(text = Loc.t("cancel", lang), color = AmberGray)
                }
            }
        )
    }
}

// ==========================================
// VIEW 6: AI CFO ADVISOR & INTERACTIVE AUDIT
// ==========================================
@Composable
fun CfoAdvisorView(viewModel: FactoryViewModel, lang: Language) {
    val logs by viewModel.stockLogs.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val staffList by viewModel.staff.collectAsStateWithLifecycle()

    val timeline by viewModel.cfoTimeline.collectAsStateWithLifecycle()

    // Force timeline to "Custom Range" for exact day-to-day selection
    LaunchedEffect(Unit) {
        viewModel.cfoTimeline.value = "Custom Range"
    }

    val financials = remember(timeline, logs, expenses, staffList, viewModel.cfoStartDate.value, viewModel.cfoEndDate.value) {
        viewModel.getTimelineFinancials(logs, expenses, staffList)
    }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Day to Day Selection Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (lang == Language.EN) "Select Report Period (Day-to-Day)" else "నివేదిక సమయం ఎంచుకోండి (రోజువారీ)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChocolateBrown
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Loc.t("start_date", lang), fontSize = 11.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            CustomDatePickerButton(
                                timestamp = viewModel.cfoStartDate.value,
                                onDateSelected = { viewModel.cfoStartDate.value = it }
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Loc.t("end_date", lang), fontSize = 11.sp, color = AmberGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            CustomDatePickerButton(
                                timestamp = viewModel.cfoEndDate.value,
                                onDateSelected = { viewModel.cfoEndDate.value = it }
                            )
                        }
                    }
                }
            }
        }

        // Live On-screen financial preview of the selected period
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = KajuGoldLight),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (lang == Language.EN) "Selected Period Preview" else "ఎంచుకున్న కాలవ్యవధి ప్రివ్యూ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KajuGold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Operations Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = if (lang == Language.EN) "Raw Bought" else "కొనుగోలు", fontSize = 11.sp, color = AmberGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = String.format("%,.1f Kgs", financials.totalKgsBought), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChocolateBrown)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = if (lang == Language.EN) "Cashew Sold" else "అమ్మకాలు", fontSize = 11.sp, color = AmberGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = String.format("%,.1f Kgs", financials.totalKgsSold), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChocolateBrown)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Financial metrics table
                    @Composable
                    fun FinancialRow(label: String, value: Double, isAdd: Boolean? = null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, fontSize = 13.sp, color = ChocolateBrown)
                            val prefix = if (isAdd == true) "+" else if (isAdd == false) "-" else ""
                            Text(
                                text = "$prefix${formatRupee(value)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAdd == true) PistachioGreen else if (isAdd == false) RedPending else ChocolateBrown
                            )
                        }
                    }

                    FinancialRow(if (lang == Language.EN) "Sales Revenue" else "అమ్మకాల ఆదాయం", financials.revenue, isAdd = true)
                    FinancialRow(if (lang == Language.EN) "Raw Material Cost" else "ముడిసరుకు ఖర్చు", financials.rawMaterialCost, isAdd = false)
                    FinancialRow(if (lang == Language.EN) "Staff Salaries (Prorated)" else "సిబ్బంది జీతాలు", financials.proratedStaffOverhead, isAdd = false)
                    FinancialRow(if (lang == Language.EN) "Other Operational Expenses" else "ఇతర కార్యాచరణ ఖర్చు", financials.otherExpenses, isAdd = false)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.EN) "Period Net Profit" else "ఆ కాల నికర లాభం",
                            fontWeight = FontWeight.Bold,
                            color = ChocolateBrown,
                            fontSize = 15.sp
                        )
                        Text(
                            text = formatRupee(financials.netProfit),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (financials.netProfit >= 0) PistachioGreen else RedPending
                        )
                    }
                }
            }
        }

        // Beautiful "Generate PDF Report" button
        item {
            Button(
                onClick = {
                    generateFinancialPdf(
                        context = context,
                        startDate = viewModel.cfoStartDate.value,
                        endDate = viewModel.cfoEndDate.value,
                        financials = financials,
                        transactions = logs,
                        expenses = expenses
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PistachioGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_report_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (lang == Language.EN) "Download & View PDF Report" else "PDF నివేదికను డౌన్‌లోడ్ చేసి చూడండి",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

fun generateFinancialPdf(
    context: android.content.Context,
    startDate: Long,
    endDate: Long,
    financials: TimelineFinancials,
    transactions: List<StockTransaction>,
    expenses: List<Expense>
) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    // A4 standard: 595 x 842 points
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#4A2A18") // ChocolateBrown
        textSize = 20f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val subtitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 10f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val headingPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#1B5E20") // Dark green accent
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val boldTextPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val regularTextPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }

    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
    }

    var y = 60f

    // Document Header
    canvas.drawText("SRI MALLIKARJUNA INDUSTRIES", 297.5f, y, titlePaint)
    y += 18f
    canvas.drawText("Cashew Processing & Trading Factory", 297.5f, y, subtitlePaint)
    y += 18f
    
    val goldSubtitlePaint = android.graphics.Paint(subtitlePaint).apply {
        color = android.graphics.Color.parseColor("#C5A059")
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("AI CFO FINANCIAL AUDIT & STATEMENT", 297.5f, y, goldSubtitlePaint)
    y += 18f

    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
    val dateRangeStr = "Period: ${sdf.format(java.util.Date(startDate))} to ${sdf.format(java.util.Date(endDate))}"
    canvas.drawText(dateRangeStr, 297.5f, y, subtitlePaint)
    y += 25f

    // Double line divider
    canvas.drawLine(40f, y, 555f, y, linePaint)
    canvas.drawLine(40f, y + 2f, 555f, y + 2f, linePaint)
    y += 25f

    // Section 1: Operational Volumes
    canvas.drawText("1. OPERATIONAL VOLUMES (KGs)", 40f, y, headingPaint)
    y += 20f

    fun drawRow(label: String, valStr: String) {
        canvas.drawText(label, 50f, y, regularTextPaint)
        canvas.drawText(valStr, 400f, y, boldTextPaint)
        y += 20f
    }

    drawRow("Total Raw Cashew Material Purchased:", String.format("%,.2f Kgs", financials.totalKgsBought))
    drawRow("Total Finished Cashew Product Sold:", String.format("%,.2f Kgs", financials.totalKgsSold))

    y += 10f
    canvas.drawLine(40f, y, 555f, y, linePaint)
    y += 25f

    // Section 2: Financial Performance
    canvas.drawText("2. FACTORY FINANCIAL SUMMARY (INR)", 40f, y, headingPaint)
    y += 20f

    fun drawFinancialRow(label: String, amount: Double, isExpense: Boolean) {
        canvas.drawText(label, 50f, y, regularTextPaint)
        val prefix = if (isExpense) "(-) " else "(+) "
        val valStr = prefix + String.format("Rs. %,.2f", amount)
        canvas.drawText(valStr, 400f, y, boldTextPaint)
        y += 20f
    }

    drawFinancialRow("Gross Sales Revenue:", financials.revenue, isExpense = false)
    drawFinancialRow("Raw Cashew Material Purchase Cost:", financials.rawMaterialCost, isExpense = true)
    drawFinancialRow("Staff Salaries & Payroll (Prorated):", financials.proratedStaffOverhead, isExpense = true)
    drawFinancialRow("Other Factory Operational Expenses:", financials.otherExpenses, isExpense = true)

    y += 5f
    canvas.drawLine(50f, y, 500f, y, linePaint)
    y += 18f

    // Total expenses row
    canvas.drawText("Total Operating Expenses:", 50f, y, boldTextPaint)
    canvas.drawText(String.format("Rs. %,.2f", financials.totalExpenses), 400f, y, boldTextPaint)
    y += 25f

    // Draw highlighted rectangle for Net Profit/Loss
    val rectPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.FILL
        color = if (financials.netProfit >= 0) {
            android.graphics.Color.parseColor("#E8F5E9") // Light Green
        } else {
            android.graphics.Color.parseColor("#FFEBEE") // Light Red
        }
    }
    val rectStrokePaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        color = if (financials.netProfit >= 0) {
            android.graphics.Color.parseColor("#4CAF50")
        } else {
            android.graphics.Color.parseColor("#F44336")
        }
        strokeWidth = 1.5f
    }
    
    // Draw box
    canvas.drawRect(40f, y, 555f, y + 45f, rectPaint)
    canvas.drawRect(40f, y, 555f, y + 45f, rectStrokePaint)

    // Text inside box
    val boxTitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val boxValuePaint = android.graphics.Paint().apply {
        color = if (financials.netProfit >= 0) {
            android.graphics.Color.parseColor("#2E7D32") // Darker green
        } else {
            android.graphics.Color.parseColor("#C62828") // Darker red
        }
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    canvas.drawText("NET PERIOD EARNINGS (PROFIT / LOSS):", 60f, y + 26f, boxTitlePaint)
    canvas.drawText(String.format("Rs. %,.2f", financials.netProfit), 380f, y + 26f, boxValuePaint)
    
    y += 75f

    // Section 3: AI CFO Remarks
    canvas.drawText("3. AI CFO SYSTEM AUDIT REMARKS", 40f, y, headingPaint)
    y += 20f

    val remarksText = "During this audit period, Sri Mallikarjuna Industries successfully managed raw cashew purchases of " +
            String.format("%,.2f Kgs", financials.totalKgsBought) + " and fulfilled finished cashew orders of " +
            String.format("%,.2f Kgs.", financials.totalKgsSold) + " The factory operated with a total gross revenue of " +
            String.format("Rs. %,.2f", financials.revenue) + " against total operational expenses of " +
            String.format("Rs. %,.2f", financials.totalExpenses) + ", culminating in a net " +
            (if (financials.netProfit >= 0) "profit" else "loss") + " of " +
            String.format("Rs. %,.2f.", Math.abs(financials.netProfit)) + " The operating profit margin for this period stands at " +
            (if (financials.revenue > 0) String.format("%.2f", (financials.netProfit / financials.revenue) * 100.0) else "0.00") +
            "%. This report represents an official factory audit statement processed on " +
            sdf.format(java.util.Date()) + "."

    val wrappedPaint = android.graphics.Paint(regularTextPaint).apply {
        textSize = 10f
    }
    
    val endY = drawParagraph(canvas, remarksText, 50f, y, wrappedPaint, 490f, 15f)
    
    // Draw signature lines at the bottom
    var sigY = 730f
    canvas.drawLine(50f, sigY, 180f, sigY, linePaint)
    canvas.drawLine(400f, sigY, 530f, sigY, linePaint)
    sigY += 15f
    
    val sigPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 9f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    canvas.drawText("Factory Manager", 50f, sigY, sigPaint)
    canvas.drawText("Authorized CFO Signee", 400f, sigY, sigPaint)

    // Document Footer
    canvas.drawLine(40f, 790f, 555f, 790f, linePaint)
    canvas.drawText("Sri Mallikarjuna Industries • Confidential AI CFO System Report", 297.5f, 810f, subtitlePaint)

    pdfDocument.finishPage(page)

    try {
        val file = java.io.File(context.cacheDir, "financial_report.pdf")
        val outputStream = java.io.FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = android.content.Intent.createChooser(intent, "Open Financial Report")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun drawParagraph(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    startY: Float,
    paint: android.graphics.Paint,
    maxWidth: Float,
    lineHeight: Float
): Float {
    var y = startY
    val words = text.split(" ")
    var currentLine = ""
    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val width = paint.measureText(testLine)
        if (width > maxWidth) {
            canvas.drawText(currentLine, x, y, paint)
            y += lineHeight
            currentLine = word
        } else {
            currentLine = testLine
        }
    }
    if (currentLine.isNotEmpty()) {
        canvas.drawText(currentLine, x, y, paint)
        y += lineHeight
    }
    return y
}

@Composable
fun CustomDatePickerButton(
    timestamp: Long,
    onDateSelected: (Long) -> Unit
) {
    // Elegant stable year-month-day drop-down selectors
    val cal = remember(timestamp) {
        Calendar.getInstance().apply { timeInMillis = timestamp }
    }

    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        Text(text = sdf.format(cal.time), fontSize = 12.sp)
    }

    if (showPicker) {
        var day by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
        var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) } // 0..11
        var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(text = "Select Date", fontWeight = FontWeight.Bold, color = ChocolateBrown) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Day dropdown equivalent
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Day", fontSize = 11.sp, color = AmberGray)
                        var dayExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { dayExpanded = true }) {
                                Text(day.toString())
                            }
                            DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                                for (d in 1..31) {
                                    DropdownMenuItem(text = { Text(d.toString()) }, onClick = { day = d; dayExpanded = false })
                                }
                            }
                        }
                    }

                    // Month dropdown equivalent
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = "Month", fontSize = 11.sp, color = AmberGray)
                        var monthExpanded by remember { mutableStateOf(false) }
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        Box {
                            OutlinedButton(onClick = { monthExpanded = true }) {
                                Text(months[month])
                            }
                            DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                                months.forEachIndexed { idx, m ->
                                    DropdownMenuItem(text = { Text(m) }, onClick = { month = idx; monthExpanded = false })
                                }
                            }
                        }
                    }

                    // Year dropdown equivalent
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = "Year", fontSize = 11.sp, color = AmberGray)
                        var yearExpanded by remember { mutableStateOf(false) }
                        val years = (2020..2028).toList()
                        Box {
                            OutlinedButton(onClick = { yearExpanded = true }) {
                                Text(year.toString())
                            }
                            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                                years.forEach { y ->
                                    DropdownMenuItem(text = { Text(y.toString()) }, onClick = { year = y; yearExpanded = false })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCal = Calendar.getInstance()
                        newCal.set(Calendar.YEAR, year)
                        newCal.set(Calendar.MONTH, month)
                        newCal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(newCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                        onDateSelected(newCal.timeInMillis)
                        showPicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KajuGold)
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = "Cancel", color = AmberGray)
                }
            }
        )
    }
}

// Simple and robust parser for bold tags and lists inside markdown
@Composable
fun MarkdownText(markdown: String) {
    val lines = markdown.split("\n")
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    Text(
                        text = trimmed.removePrefix("###").trim(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ChocolateBrown,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("##") -> {
                    Text(
                        text = trimmed.removePrefix("##").trim(),
                        style = MaterialTheme.typography.titleLarge,
                        color = KajuGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("#") -> {
                    Text(
                        text = trimmed.removePrefix("#").trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = KajuGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val content = trimmed.substring(1).trim()
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyLarge, color = KajuGold, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseBoldText(content),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChocolateBrown
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseBoldText(trimmed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChocolateBrown,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun parseBoldText(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val parts = text.split("**")
    for (i in parts.indices) {
        if (i % 2 == 1) {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ChocolateBrown)) {
                append(parts[i])
            }
        } else {
            builder.append(parts[i])
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun AuthScreen(viewModel: FactoryViewModel, lang: Language) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KajuCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .shadow(8.dp, shape = RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(KajuGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SMI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif
                    )
                }

                Text(
                    text = "Sri Mallikarjuna Industries",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChocolateBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = if (lang == Language.EN) 
                        "Real-time Enterprise Resource Planning & Financial Ledger"
                    else "రియల్ టైమ్ ఎంటర్‌ప్రైజ్ రిసోర్స్ ప్లానింగ్ & ఫైనాన్షియల్ లెడ్జర్",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (lang == Language.EN) "Sign In" else "సైన్ ఇన్",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChocolateBrown,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (lang == Language.EN) "Email Address" else "ఈమెయిల్ చిరునామా") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(18.dp), tint = KajuGold)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KajuGold,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = KajuGold
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (lang == Language.EN) "Password" else "పాస్‌వర్డ్") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password", modifier = Modifier.size(18.dp), tint = KajuGold)
                    },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KajuGold,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = KajuGold
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                )

                authError?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RedLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = RedPending, modifier = Modifier.size(16.dp))
                            Text(
                                text = err,
                                color = RedPending,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.signInWithSupabase(email, password)
                    },
                    enabled = !isAuthLoading && email.isNotEmpty() && password.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KajuGold,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_button")
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (lang == Language.EN) "Sign In" else "సైన్ ఇన్",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { viewModel.toggleLanguage() }
                ) {
                    Icon(Icons.Default.Language, contentDescription = "Language", tint = AmberGray, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (lang == Language.EN) "తెలుగు లో చూడండి" else "View in English",
                        fontSize = 12.sp,
                        color = AmberGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
