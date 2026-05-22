package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Categories
import com.example.data.CategoryPreset
import com.example.data.Transaction
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.launch

fun getChineseMonthName(): String {
    return try {
        val sdf = SimpleDateFormat("M月帳單", Locale.TAIWAN)
        sdf.format(Date())
    } catch (e: Exception) {
        "本月帳單"
    }
}

fun getFormattedToday(): String {
    return try {
        val sdf = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.TAIWAN)
        sdf.format(Date())
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Fastfood" -> Icons.Default.Fastfood
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Movie" -> Icons.Default.Movie
        "Lightbulb" -> Icons.Default.Lightbulb
        "MedicalServices" -> Icons.Default.MedicalServices
        "AttachMoney" -> Icons.Default.AttachMoney
        "TrendingUp" -> Icons.Default.TrendingUp
        "Category" -> Icons.Default.Category
        else -> Icons.Default.Category
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = DecimalFormat("#,##0.##")
    return formatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TransactionViewModel) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val netBalance by viewModel.netBalance.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val categoryStats by viewModel.categoryStatistics.collectAsStateWithLifecycle()

    val currentFilterType by viewModel.filterType.collectAsStateWithLifecycle()
    val currentFilterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var monthlyBudget by remember { mutableStateOf(20000.0) } // Default budget NT$ 20,000
    var currentTab by remember { mutableStateOf("首頁") }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Scaffold(
        bottomBar = {
            ElegantBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    when (tab) {
                        "首頁" -> {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                        "明細" -> {
                            coroutineScope.launch {
                                // Index of FilterSection/Transactions
                                listState.animateScrollToItem(4)
                            }
                        }
                        "圖表" -> {
                            coroutineScope.launch {
                                if (categoryStats.isNotEmpty()) {
                                    listState.animateScrollToItem(3)
                                } else {
                                    listState.animateScrollToItem(1) // Fallback to hero
                                }
                            }
                        }
                        "我的" -> {
                            showSettingsDialog = true
                        }
                    }
                },
                onAddClick = { showAddDialog = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Index 0: Header block matching HTML flex design
            item {
                HeaderBlock(onAvatarClick = { showSettingsDialog = true })
            }

            // Index 1: Net Balance Hero Card (Segmented, Rounded 32.dp, Border, Custom Style)
            item {
                BalanceHeroCard(
                    netBalance = netBalance,
                    income = totalIncome,
                    expense = totalExpense,
                    monthlyBudget = monthlyBudget,
                    onBudgetClick = { showBudgetDialog = true }
                )
            }

            // Index 2: Quick Action Grid corresponding to the premium HTML mockup buttons!
            item {
                QuickActionGridRow(
                    onAnalysisClick = {
                        currentTab = "圖表"
                        coroutineScope.launch {
                            if (categoryStats.isNotEmpty()) {
                                listState.animateScrollToItem(3)
                            } else {
                                listState.animateScrollToItem(1)
                            }
                        }
                    },
                    onCategoryClick = {
                        currentTab = "明細"
                        coroutineScope.launch {
                            listState.animateScrollToItem(4)
                        }
                    },
                    onBudgetClick = {
                        showBudgetDialog = true
                    },
                    onSettingsClick = {
                        showSettingsDialog = true
                    }
                )
            }

            // Index 3: Category Expense Distribution Chart Card
            if (categoryStats.isNotEmpty()) {
                item {
                    CategoryDistributionCard(categoryStats = categoryStats)
                }
            }

            // Index 4: Filter Control Section
            item {
                FilterSection(
                    selectedType = currentFilterType,
                    onTypeSelected = { viewModel.setFilterType(it) },
                    selectedCategory = currentFilterCategory,
                    onCategorySelected = { viewModel.setFilterCategory(it) }
                )
            }

            // Transactions List
            if (transactions.isEmpty()) {
                item {
                    EmptyStatePlaceholder()
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        onDeleteClick = { viewModel.deleteTransaction(tx) }
                    )
                }
            }

            // Spacer on bottom to avoid any overlap with Bottom Bar
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, type, category, note ->
                viewModel.addTransaction(amount, type, category, note)
                showAddDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        ElegantSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onLoadSample = {
                viewModel.seedSampleTransactions()
                showSettingsDialog = false
            },
            onClearAll = {
                viewModel.clearAllTransactions()
                showSettingsDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        ElegantBudgetDialog(
            currentBudget = monthlyBudget,
            onDismiss = { showBudgetDialog = false },
            onSave = { budget ->
                monthlyBudget = budget
                showBudgetDialog = false
            }
        )
    }
}

@Composable
fun HeaderBlock(onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = getChineseMonthName(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE3E2E6),
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = getFormattedToday(),
                fontSize = 13.sp,
                color = Color(0xFF939094)
            )
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFD0BCFF))
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "用戶",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF381E72)
            )
        }
    }
}

@Composable
fun QuickActionGridRow(
    onAnalysisClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val actions = listOf(
            Triple("📊", "分析", onAnalysisClick),
            Triple("🏷️", "類別", onCategoryClick),
            Triple("📅", "預算", onBudgetClick),
            Triple("⚙️", "設置", onSettingsClick)
        )
        actions.forEach { (emoji, label, onClick) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1F1F1F))
                    .clickable { onClick() }
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = emoji, fontSize = 21.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFFC9C5D0),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BalanceHeroCard(
    netBalance: Double,
    income: Double,
    expense: Double,
    monthlyBudget: Double,
    onBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("balance_hero_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header Row of the card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill "本月結餘" in styling
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF381E72))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "本月結餘",
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Money Unit label
                Text(
                    text = "TWD",
                    color = Color(0xFF939094),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Total Big Balance text
            val prefix = if (netBalance >= 0) "NT$ " else "-NT$ "
            Text(
                text = "$prefix${formatCurrency(Math.abs(netBalance))}",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp
            )
            
            // Budget details if set
            if (monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                val percentage = if (monthlyBudget > 0) (expense / monthlyBudget * 100).coerceIn(0.0, 100.0) else 0.0
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "預算進度 (${String.format(Locale.getDefault(), "%.1f", percentage)}%)",
                            fontSize = 11.sp,
                            color = Color(0xFF939094)
                        )
                        Text(
                            text = "NT$ ${formatCurrency(expense)} / ${formatCurrency(monthlyBudget)}",
                            fontSize = 11.sp,
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (percentage / 100f).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (percentage > 90) ColorExpense else Color(0xFFD0BCFF),
                        trackColor = Color(0xFF333333)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
            Spacer(modifier = Modifier.height(18.dp))
            
            // Income / Expense Dual metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "收入",
                        color = Color(0xFF939094),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${formatCurrency(income)}",
                        color = ColorIncome,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color(0xFF333333))
                        .align(Alignment.CenterVertically)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp)
                ) {
                    Text(
                        text = "支出",
                        color = Color(0xFF939094),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${formatCurrency(expense)}",
                        color = ColorExpense,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDistributionCard(categoryStats: List<CategoryStat>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_distribution_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "支出結構分析",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE3E2E6)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment progress bar representing expense percentages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
            ) {
                if (categoryStats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF333333))
                    )
                } else {
                    categoryStats.forEach { stat ->
                        val color = Color(android.graphics.Color.parseColor(stat.category.colorHex))
                        Box(
                            modifier = Modifier
                                .weight(if (stat.percentage > 0) stat.percentage else 0.01f)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom compact stats rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryStats.take(4).forEach { stat ->
                    val color = Color(android.graphics.Color.parseColor(stat.category.colorHex))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stat.category.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFC9C5D0)
                            )
                        }
                        Row {
                            Text(
                                text = "NT$ ${formatCurrency(stat.totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", stat.percentage)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF939094)
                            )
                        }
                    }
                }
                if (categoryStats.size > 4) {
                    val remainingSum = categoryStats.drop(4).sumOf { it.totalAmount }
                    val remainingPercent = categoryStats.drop(4).map { it.percentage }.sum()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "其他支出",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFC9C5D0)
                            )
                        }
                        Row {
                            Text(
                                text = "NT$ ${formatCurrency(remainingSum)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", remainingPercent)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF939094)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val types = listOf("全部", "支出", "收入")
    val uniqueCategories = listOf("全部") + Categories.presets.map { it.displayName }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Flat segment type picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1F1F1F))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            types.forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onTypeSelected(type) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF939094),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Horizontal Category Filter Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(uniqueCategories) { cat ->
                val isSelected = selectedCategory == cat
                val preset = Categories.presets.firstOrNull { it.displayName == cat }
                val chipColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color(0xFF1F1F1F)
                }
                val textColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    Color(0xFF939094)
                }

                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onCategorySelected(cat) },
                    color = chipColor,
                    shape = CircleShape,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (preset != null) {
                            Icon(
                                imageVector = getCategoryIcon(preset.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF939094)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (cat == "全部") {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF939094)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDeleteClick: () -> Unit
) {
    val category = Categories.getCategory(transaction.categoryName)
    val categoryColor = Color(android.graphics.Color.parseColor(category.colorHex))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with solid round circle background #352F3D
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF352F3D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = transaction.categoryName,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text column: Note + Category & Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (transaction.note.isNotBlank()) transaction.note else category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE3E2E6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF939094)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF939094).copy(alpha = 0.5f))
                        )
                    }
                    Text(
                        text = transaction.getFormattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF939094).copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount Column (Color coded Expense Red / Income Green)
            val displaySign = if (transaction.isExpense) "-" else "+"
            val amountColor = if (transaction.isExpense) ColorExpense else ColorIncome

            Text(
                text = "$displaySign$ ${formatCurrency(transaction.amount)}",
                color = amountColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Quick Delete Button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("delete_transaction_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "刪除記錄",
                    tint = Color(0xFF939094).copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
            .testTag("empty_state_placeholder"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            // Draw a subtle creative geometry denoting "no transactions"
            val width = size.width
            val height = size.height
            val color = Color.Gray.copy(alpha = 0.25f)
            
            drawCircle(color = color, radius = width / 2f)
        }
        
        Spacer(modifier = Modifier.height(18.dp))
        
        Text(
            text = "尚無記賬記錄",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "點選右下方「+」號，立即開始記錄你的第一筆收支吧！",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var selectedCategoryPreset by remember { mutableStateOf<CategoryPreset?>(null) }
    
    val focusManager = LocalFocusManager.current

    // Dynamically filter categories depending on Selected Type
    val activeCategories = remember(selectedType) {
        if (selectedType == "INCOME") {
            Categories.presets.filter { it.isDefaultIncome }
        } else {
            Categories.presets.filter { !it.isDefaultIncome }
        }
    }

    // Auto update default category if type changes
    LaunchedEffect(selectedType) {
        selectedCategoryPreset = activeCategories.firstOrNull()
    }

    // Smart Auto Category suggestion helper based on Note keyword matching
    LaunchedEffect(noteStr) {
        if (noteStr.isNotBlank() && selectedType == "EXPENSE") {
            // Match custom search keywords
            val matched = findCategoryByKeywords(noteStr)
            if (matched != null) {
                selectedCategoryPreset = matched
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_transaction_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "新增記賬記錄",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Type Toggle (Expense / Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val types = listOf(
                        "EXPENSE" to "支出",
                        "INCOME" to "收入"
                    )
                    types.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        val activeColor = if (typeKey == "EXPENSE") ColorExpense else ColorIncome
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) activeColor else Color.Transparent)
                                .clickable { selectedType = typeKey }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Amount Text Field (Large visual style)
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        // Clean input
                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                            amountStr = input
                        }
                    },
                    label = { Text("金額") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = "貨幣")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input_field")
                )

                // Item description / Note Text Field
                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("備註 / 說明") },
                    placeholder = { 
                        if (selectedType == "EXPENSE") "例如：午餐、車資、日常購物" else "例如：薪水、利息"
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "描述")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input_field")
                )

                // Smart hint for detection
                if (selectedType == "EXPENSE") {
                    val suggestion = findCategoryByKeywords(noteStr)
                    if (suggestion != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "自動偵測",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "智慧解析：自動為您匹配「${suggestion.displayName}」分類",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Category Selection list Title
                Text(
                    text = "選擇收支分類",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Horizontally Scrollable list of category options to select
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(activeCategories) { category ->
                        val isSelected = selectedCategoryPreset?.id == category.id
                        val color = Color(android.graphics.Color.parseColor(category.colorHex))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategoryPreset = category }
                                .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(8.dp)
                                .width(64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) color else color.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = category.displayName,
                                    tint = if (isSelected) Color.White else color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Cancel / Save Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_add_button")
                    ) {
                        Text("取消")
                    }

                    val saveEnabled = amountStr.isNotBlank() && amountStr.toDoubleOrNull() != null && selectedCategoryPreset != null
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            val category = selectedCategoryPreset?.displayName ?: "其他雜項"
                            onSave(amount, selectedType, category, noteStr)
                        },
                        enabled = saveEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") ColorExpense else ColorIncome
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_add_button")
                    ) {
                        Text("儲存")
                    }
                }
            }
        }
    }
}

// Helper to keywords matching
private fun findCategoryByKeywords(note: String): CategoryPreset? {
    val term = note.lowercase()
    return when {
        term.contains("飯") || term.contains("麵") || term.contains("吃") || term.contains("餐") ||
        term.contains("麥當勞") || term.contains("肯德基") || term.contains("咖喱") || term.contains("咖啡") ||
        term.contains("飲料") || term.contains("奶茶") || term.contains("牛排") || term.contains("便當") -> {
            Categories.presets.firstOrNull { it.id == "food" }
        }
        term.contains("買") || term.contains("衣服") || term.contains("鞋") || term.contains("購物") ||
        term.contains("超商") || term.contains("全家") || term.contains("網購") || term.contains("淘寶") ||
        term.contains("蝦皮") -> {
            Categories.presets.firstOrNull { it.id == "shopping" }
        }
        term.contains("捷運") || term.contains("車") || term.contains("公車") || term.contains("悠遊卡") ||
        term.contains("高鐵") || term.contains("計程車") || term.contains("加油") || term.contains("火車") -> {
            Categories.presets.firstOrNull { it.id == "transport" }
        }
        term.contains("影") || term.contains("歌") || term.contains("ktv") || term.contains("遊戲") ||
        term.contains("玩具") || term.contains("門票") || term.contains("遊樂") || term.contains("電影") ||
        term.contains("switch") || term.contains("steam") -> {
            Categories.presets.firstOrNull { it.id == "entertainment" }
        }
        term.contains("水") || term.contains("電") || term.contains("瓦斯") || term.contains("房租") ||
        term.contains("費") || term.contains("信箱") || term.contains("網路") || term.contains("第四台") -> {
            Categories.presets.firstOrNull { it.id == "utilities" }
        }
        term.contains("健") || term.contains("醫") || term.contains("藥") || term.contains("看病") ||
        term.contains("醫院") || term.contains("診所") || term.contains("口罩") || term.contains("保健") -> {
            Categories.presets.firstOrNull { it.id == "medical" }
        }
        else -> null
    }
}

@Composable
fun ElegantBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1F1F))
            .drawBehind {
                drawLine(
                    color = Color(0xFF333333),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Floating Button (drawn absolute overflowing the bar)
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            modifier = Modifier
                .size(56.dp)
                .offset(y = (-18).dp)
                .testTag("add_transaction_fab_center"),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+", fontSize = 28.sp, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
        }

        // Row containing the 4 Navigation icons (with spacer in center)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: 首頁
            RowColumnTabItem(
                label = "首頁",
                iconStr = "🏠",
                isActive = currentTab == "首頁",
                onClick = { onTabSelected("首頁") }
            )
            // Tab 2: 明細
            RowColumnTabItem(
                label = "明細",
                iconStr = "📋",
                isActive = currentTab == "明細",
                onClick = { onTabSelected("明細") }
            )
            
            // Spacer for the center button
            Spacer(modifier = Modifier.width(56.dp))
            
            // Tab 3: 圖表
            RowColumnTabItem(
                label = "圖表",
                iconStr = "📊",
                isActive = currentTab == "圖表",
                onClick = { onTabSelected("圖表") }
            )
            // Tab 4: 我的
            RowColumnTabItem(
                label = "我的",
                iconStr = "⚙️",
                isActive = currentTab == "我的",
                onClick = { onTabSelected("我的") }
            )
        }
    }
}

@Composable
fun RowColumnTabItem(
    label: String,
    iconStr: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = iconStr,
            fontSize = 18.sp,
            color = if (isActive) Color(0xFFD0BCFF) else Color(0xFF939094)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) Color(0xFFD0BCFF) else Color(0xFF939094)
        )
    }
}

@Composable
fun ElegantSettingsDialog(
    onDismiss: () -> Unit,
    onLoadSample: () -> Unit,
    onClearAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "智慧設置與說明",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "本記賬本採用 Elegant Dark 尊爵雅黑設計，為您提供無負擔、精緻優雅的記賬體驗。您可以透過下方按鈕進行資料維護：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF939094),
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLoadSample,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF381E72),
                            contentColor = Color(0xFFD0BCFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("載入預設範例資料")
                    }

                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB4AB).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFFB4AB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除所有記賬明細")
                    }
                }

                HorizontalDivider(color = Color(0xFF333333))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("關閉", color = Color(0xFFD0BCFF))
                }
            }
        }
    }
}

@Composable
fun ElegantBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetStr by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "設定每月預算",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "設定預算有助於控制消費，在首頁卡片中將會即時顯示您的消費進度和賸餘比例。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF939094),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            budgetStr = input
                        }
                    },
                    label = { Text("預算金額 (NT$)", color = Color(0xFF939094)) },
                    placeholder = { Text("例如: 20000", color = Color(0xFF939094).copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = Color(0xFFD0BCFF),
                        cursorColor = Color(0xFFD0BCFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSave(0.0) }, // Reset budget
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                    ) {
                        Text("停用預算")
                    }

                    Button(
                        onClick = {
                            val value = budgetStr.toDoubleOrNull() ?: 0.0
                            onSave(value)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        )
                    ) {
                        Text("確認儲存")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color(0xFF939094))
                }
            }
        }
    }
}
