package com.example.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.absoluteValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.viewmodel.CommerceViewModel
import java.text.SimpleDateFormat
import java.util.*

val Alignment.Companion.CenterVertizontally: Alignment.Vertical
    get() = Alignment.CenterVertically


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommerceMainScaffold(viewModel: CommerceViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val isAuthMockActive by viewModel.isAuthenticationMockActive.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showBiometricAuthDialog by remember { mutableStateOf(false) }

    // On start, if biometric is enabled, show lock screen mock
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && !isAuthMockActive) {
            showBiometricAuthDialog = true
        }
    }

    if (showBiometricAuthDialog) {
        Dialog(onDismissRequest = { }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Biometric Lock Active",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Confirm fingerprint to access your premium commerce portfolios and watchlist profiles.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.setAuthenticationMockActive(true)
                            showBiometricAuthDialog = false
                            Toast.makeText(context, "Authentication Successful!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("biometric_auth_button")
                    ) {
                        Text("Simulate Unlock")
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("Home", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                    Triple("Search", Icons.Default.Search, Icons.Outlined.Search),
                    Triple("Watchlists", Icons.Default.Favorite, Icons.Outlined.FavoriteBorder),
                    Triple("Alerts", Icons.Default.NotificationsActive, Icons.Outlined.NotificationsNone),
                    Triple("Profile", Icons.Default.Person, Icons.Outlined.PersonOutline)
                )

                items.forEachIndexed { index, pair ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == index) pair.second else pair.third,
                                contentDescription = pair.first
                            )
                        },
                        label = { Text(pair.first, fontSize = 11.sp, fontWeight = if (currentScreen == index) FontWeight.Bold else FontWeight.Normal) },
                        selected = currentScreen == index,
                        onClick = {
                            viewModel.setCurrentScreen(index)
                            viewModel.selectProduct(null) // Reset detail view on tab switch
                        },
                        modifier = Modifier.testTag("nav_item_${pair.first.lowercase()}")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeContent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = selectedProduct to currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { (product, screenIndex) ->
                if (product != null) {
                    ProductDetailsScreen(
                        product = product,
                        viewModel = viewModel,
                        onBack = { viewModel.selectProduct(null) }
                    )
                } else {
                    when (screenIndex) {
                        0 -> HomeScreen(viewModel = viewModel)
                        1 -> ProductsScreen(viewModel = viewModel)
                        2 -> WatchlistsScreen(viewModel = viewModel)
                        3 -> AlertsScreen(viewModel = viewModel)
                        4 -> ProfileScreen(viewModel = viewModel)
                        else -> HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// 1. HOME SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: CommerceViewModel) {
    val analytics by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateWatchlistDialog by remember { mutableStateOf(false) }
    var watchlistNameInput by remember { mutableStateOf("") }

    var showQuickAlertProduct by remember { mutableStateOf<Product?>(null) }
    var showQuickAlertSetup by remember { mutableStateOf(false) }

    val activeProducts = analytics["totalProducts"] as? Int ?: 0
    val activeAlerts = analytics["activeAlerts"] as? Int ?: 0
    val priceDrops = analytics["priceDropCount"] as? Int ?: 0
    val avgDiscountValue = analytics["avgDiscount"] as? Double ?: 0.0

    if (showCreateWatchlistDialog) {
        Dialog(onDismissRequest = { showCreateWatchlistDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create Watchlist", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = watchlistNameInput,
                        onValueChange = { watchlistNameInput = it },
                        label = { Text("Watchlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("watchlist_name_field")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateWatchlistDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (watchlistNameInput.isNotBlank()) {
                                    viewModel.createWatchlist(watchlistNameInput.trim())
                                    watchlistNameInput = ""
                                    showCreateWatchlistDialog = false
                                    Toast.makeText(context, "Watchlist Created!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("watchlist_submit_button")
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    if (showQuickAlertSetup && showQuickAlertProduct != null) {
        QuickAlertSetupDialog(
            product = showQuickAlertProduct!!,
            viewModel = viewModel,
            onDismiss = {
                showQuickAlertSetup = false
                showQuickAlertProduct = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Branding Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertizontally,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Commerce Tracker Pro",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rule-Based Offline Price Engine",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = "Pro Icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Quick Statistics Section
        Text(
            text = "Quick Statistics",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Tracked Items",
                value = "$activeProducts",
                icon = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Active Alerts",
                value = "$activeAlerts",
                icon = Icons.Default.NotificationsActive,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Discounts Today",
                value = "$priceDrops",
                icon = Icons.Default.Percent,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Avg. Discount",
                value = "${String.format("%.1f", avgDiscountValue)}%",
                icon = Icons.Default.AutoGraph,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions Section
        Text(
            text = "Quick Actions",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.setCurrentScreen(1) }, // Switch to Search tab
                modifier = Modifier.weight(1f).testTag("quick_action_search"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Search Product", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showCreateWatchlistDialog = true },
                modifier = Modifier.weight(1f).testTag("quick_action_watchlist"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Watchlist", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (products.isNotEmpty()) {
                        showQuickAlertProduct = products.random()
                        showQuickAlertSetup = true
                    } else {
                        Toast.makeText(context, "No products tracking.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).testTag("quick_action_alert"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Trending Products Section
        val discountedProducts = products.filter { it.discountPercent > 0.0 }.sortedByDescending { it.discountPercent }
        if (discountedProducts.isNotEmpty()) {
            Text(
                text = "Trending Hot Deals",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(discountedProducts) { product ->
                    TrendingProductCard(product = product, onClick = { viewModel.selectProduct(product) })
                }
            }
        }

        // Recently Viewed Products Grid / Recent Activity Flow
        Text(
            text = "Tracked Catalogue List",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                products.take(4).forEach { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProduct(product) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertizontally,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                ImagePlaceholder(title = product.name)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${product.brand} • ${product.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${String.format("%.2f", product.currentPrice)} TL", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            if (product.discountPercent > 0) {
                                Text("-${String.format("%.1f", product.discountPercent)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }

        // Recent Notifications Feed
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertizontally,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Notifications feed",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (notifications.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearNotifications() }) {
                    Text("Clear All")
                }
            }
        }

        if (notifications.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No new notifications or alerts triggered.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                notifications.take(5).forEach { notification ->
                    NotificationListItem(
                        notification = notification,
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertizontally,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TrendingProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                ImagePlaceholder(title = product.name)
                // Hot deal tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xFFE53935), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "-${String.format("%.0f", product.discountPercent)}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = product.brand,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        fontFamily = FontFamily.SansSerif,
                        text = "${String.format("%.0f", product.currentPrice)} TL",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${String.format("%.0f", product.previousPrice)} TL",
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(notification: Notification, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CircleNotifications,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertizontally
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.createdAt)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Notification",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// Simple fallback component when AsyncImage fails or placeholder is preferred
@Composable
fun ImagePlaceholder(title: String, modifier: Modifier = Modifier.fillMaxSize()) {
    // Generate simple gradient based on title sum
    val colorIndex = title.hashCode().absoluteValue % 4
    val gradientColors = when (colorIndex) {
        0 -> listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA))
        1 -> listOf(Color(0xFFF3E5F5), Color(0xFFCE93D8))
        2 -> listOf(Color(0xFFFFF3E0), Color(0xFFFFB74D))
        else -> listOf(Color(0xFFE8F5E9), Color(0xFFA5D6A7))
    }
    Box(
        modifier = modifier.background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        val initials = if (title.length >= 2) title.substring(0, 2).uppercase() else title.uppercase()
        Text(
            text = initials,
            fontWeight = FontWeight.Black,
            color = Color.DarkGray,
            fontSize = 18.sp
        )
    }
}

// 2. PRODUCT SEARCH SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: CommerceViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrand.collectAsStateWithLifecycle()
    val minPrice by viewModel.minPriceFilter.collectAsStateWithLifecycle()
    val maxPrice by viewModel.maxPriceFilter.collectAsStateWithLifecycle()
    val onlyDiscounted by viewModel.filterOnlyDiscounted.collectAsStateWithLifecycle()
    val onlyAvailable by viewModel.filterOnlyAvailable.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

    var showFiltersDialog by remember { mutableStateOf(false) }

    var alertTargetProduct by remember { mutableStateOf<Product?>(null) }
    var showAddAlertDialog by remember { mutableStateOf(false) }

    var watchlistsProductSelect by remember { mutableStateOf<Product?>(null) }
    var showWatchlistSelectDialog by remember { mutableStateOf(false) }

    val watchlists by viewModel.watchlists.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (showAddAlertDialog && alertTargetProduct != null) {
        QuickAlertSetupDialog(
            product = alertTargetProduct!!,
            viewModel = viewModel,
            onDismiss = {
                showAddAlertDialog = false
                alertTargetProduct = null
            }
        )
    }

    if (showWatchlistSelectDialog && watchlistsProductSelect != null) {
        Dialog(onDismissRequest = { showWatchlistSelectDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Watchlist", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Add ${watchlistsProductSelect!!.name} to:", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (watchlists.isEmpty()) {
                        Text("No watchlists created. Create one from the Home/Watchlists screen.", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(watchlists) { watchlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addProductToWatchlist(watchlist.id, watchlistsProductSelect!!.id)
                                            showWatchlistSelectDialog = false
                                            watchlistsProductSelect = null
                                            Toast.makeText(context, "Added to ${watchlist.name}!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertizontally
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(watchlist.name, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showWatchlistSelectDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    if (showFiltersDialog) {
        Dialog(onDismissRequest = { showFiltersDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertizontally
                    ) {
                        Text("Filters & Sorting", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        IconButton(onClick = { showFiltersDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sort By Selection
                    Text("Sort By", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val sorts = listOf("Lowest Price", "Highest Price", "Newest", "Most Discounted")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sorts.forEach { option ->
                            FilterChip(
                                selected = sortBy == option,
                                onClick = { viewModel.setSortBy(option) },
                                label = { Text(option, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price Range input
                    Text("Price Range (TL)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minPrice?.toString() ?: "",
                            onValueChange = { viewModel.setMinPrice(it.toDoubleOrNull()) },
                            label = { Text("Min Price", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxPrice?.toString() ?: "",
                            onValueChange = { viewModel.setMaxPrice(it.toDoubleOrNull()) },
                            label = { Text("Max Price", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle Filters
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.setOnlyDiscounted(!onlyDiscounted) },
                        verticalAlignment = Alignment.CenterVertizontally,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Only Discounted Deals", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = onlyDiscounted, onCheckedChange = { viewModel.setOnlyDiscounted(it) })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.setOnlyAvailable(!onlyAvailable) },
                        verticalAlignment = Alignment.CenterVertizontally,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Only In Stock & Low", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = onlyAvailable, onCheckedChange = { viewModel.setOnlyAvailable(it) })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Brand Filters
                    Text("Select Brand", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedBrand == null,
                            onClick = { viewModel.setSelectedBrand(null) },
                            label = { Text("All Brands") }
                        )
                        brands.forEach { brand ->
                            FilterChip(
                                selected = selectedBrand == brand,
                                onClick = { viewModel.setSelectedBrand(brand) },
                                label = { Text(brand) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearFilters()
                                showFiltersDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                        Button(
                            onClick = { showFiltersDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App header
        Text(
            text = "Product Search Engine",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Bar & Filter Toggle Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search products, brands...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("product_search_bar"),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showFiltersDialog = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (selectedBrand != null || minPrice != null || maxPrice != null || onlyDiscounted || onlyAvailable)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).testTag("filter_dialog_trigger")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters"
                )
            }
        }

        // Category Chips Row (horizontal scroll)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.setSelectedCategory(null) },
                    label = { Text("All Categories") },
                    modifier = Modifier.testTag("category_chip_all")
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.setSelectedCategory(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("category_chip_${category.lowercase()}")
                )
            }
        }

        // Matching items label
        Text(
            text = "Matching Products (${filteredProducts.size})",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No tracking items fit this pattern.", fontWeight = FontWeight.Bold)
                    Text("Try relaxing search query or toggling discount filters.", fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { product ->
                    ProductCardItem(
                        product = product,
                        onViewDetails = { viewModel.selectProduct(product) },
                        onSaveProduct = {
                            watchlistsProductSelect = product
                            showWatchlistSelectDialog = true
                        },
                        onAddAlert = {
                            alertTargetProduct = product
                            showAddAlertDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCardItem(
    product: Product,
    onViewDetails: () -> Unit,
    onSaveProduct: () -> Unit,
    onAddAlert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .background(Color.White)
            ) {
                ImagePlaceholder(title = product.name)
                // Stock Availability Badge
                val badgeColor = when (product.availability) {
                    "In Stock" -> Color(0xFF4CAF50)
                    "Running Low" -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(badgeColor, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(product.availability, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Discount Badge on upper right
                if (product.discountPercent > 0.0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color(0xFFE53935), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${String.format("%.0f", product.discountPercent)}% OFF",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${product.brand} • ${product.category}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${String.format("%.0f", product.currentPrice)} TL",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    if (product.discountPercent > 0.0) {
                        Text(
                            text = "${String.format("%.0f", product.previousPrice)} TL",
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedIconButton(
                        onClick = onSaveProduct,
                        modifier = Modifier.size(32.dp).testTag("save_product_${product.id}"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Save Product", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }

                    OutlinedIconButton(
                        onClick = onAddAlert,
                        modifier = Modifier.size(32.dp).testTag("alert_product_${product.id}"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = "Add alert", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    }

                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier.weight(1f).height(32.dp).testTag("view_details_${product.id}"),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Info", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 3. WATCHLIST SYSTEM SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistsScreen(viewModel: CommerceViewModel) {
    val watchlists by viewModel.watchlists.collectAsStateWithLifecycle()
    val selectedWatchlist by viewModel.selectedWatchlist.collectAsStateWithLifecycle()
    val productsInSelectedWatchlist by viewModel.productsInSelectedWatchlist.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var watchlistName by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf<Watchlist?>(null) }
    var renameName by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create Watchlist", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = watchlistName,
                        onValueChange = { watchlistName = it },
                        label = { Text("Watchlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (watchlistName.isNotBlank()) {
                                viewModel.createWatchlist(watchlistName.trim())
                                watchlistName = ""
                                showCreateDialog = false
                                Toast.makeText(context, "Watchlist Created!", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Create") }
                    }
                }
            }
        }
    }

    if (showRenameDialog != null) {
        Dialog(onDismissRequest = { showRenameDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rename Watchlist", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_watchlist_field")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (renameName.isNotBlank()) {
                                viewModel.renameWatchlist(showRenameDialog!!.id, renameName.trim())
                                showRenameDialog = null
                                renameName = ""
                                Toast.makeText(context, "Watchlist Renamed!", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.testTag("rename_watchlist_submit")) { Text("Rename") }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertizontally,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Watchlists System",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_watchlist_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New List")
            }
        }

        if (watchlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No watchlists defined yet.", fontWeight = FontWeight.Bold)
                    Text("Create a watchlist group above to segment your monitoring.", fontSize = 12.sp)
                }
            }
        } else {
            // Horizontal rows of watchlists
            Text("Watchlist Groups", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlists) { wl ->
                    val isSelected = selectedWatchlist?.id == wl.id
                    Card(
                        modifier = Modifier
                            .width(168.dp)
                            .clickable {
                                if (isSelected) viewModel.selectWatchlist(null)
                                else viewModel.selectWatchlist(wl)
                            }
                            .testTag("watchlist_group_${wl.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertizontally
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    contentDescription = null
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            renameName = wl.name
                                            showRenameDialog = wl
                                        },
                                        modifier = Modifier.size(24.dp).testTag("rename_wl_${wl.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteWatchlist(wl.id)
                                            Toast.makeText(context, "Watchlist Deleted!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp).testTag("delete_wl_${wl.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(wl.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Active segment list", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            if (selectedWatchlist == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a watchlist group card to analyze items.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertizontally
                ) {
                    Text(
                        text = "Viewing: ${selectedWatchlist!!.name}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("(${productsInSelectedWatchlist.size} items)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (productsInSelectedWatchlist.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No items saved. Add monitoring alert details on Search tab.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                } else {
                    // Grouped view by standard categories (for categories organization)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val grouped = productsInSelectedWatchlist.groupBy { it.category }
                        grouped.forEach { (category, pList) ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(category, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            items(pList) { product ->
                                WatchlistItemRow(
                                    product = product,
                                    onClick = { viewModel.selectProduct(product) },
                                    onRemove = {
                                        viewModel.removeProductFromWatchlist(selectedWatchlist!!.id, product.id)
                                        Toast.makeText(context, "Removed from group!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistItemRow(product: Product, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("watchlist_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            ) {
                ImagePlaceholder(title = product.name)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${product.brand} • ${product.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertizontally) {
                    Text("${String.format("%.0f", product.currentPrice)} TL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    if (product.discountPercent > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE53935), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "-${String.format("%.0f", product.discountPercent)}%",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.testTag("remove_wl_item_${product.id}")) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove product", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 4. ALERT SYSTEM SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: CommerceViewModel) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "PAUSED", "TRIGGERED"

    val filteredAlerts = alerts.filter {
        when (activeTab) {
            "ACTIVE" -> it.status == "ACTIVE"
            "PAUSED" -> it.status == "PAUSED"
            "TRIGGERED" -> it.status == "TRIGGERED"
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Rules-Based Alerts",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Tab Selector Row
        TabRow(
            selectedTabIndex = when (activeTab) {
                "ACTIVE" -> 0
                "PAUSED" -> 1
                "TRIGGERED" -> 2
                else -> 0
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            val tabs = listOf("ACTIVE", "PAUSED", "TRIGGERED")
            tabs.forEachIndexed { index, title ->
                val count = alerts.count { it.status == title }
                Tab(
                    selected = activeTab == title,
                    onClick = { activeTab = title },
                    text = { Text("$title ($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("alert_tab_${title.lowercase()}")
                )
            }
        }

        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No alerts found in state $activeTab", fontWeight = FontWeight.Bold)
                    Text("Go details screen of any product to define alert rules.", fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAlerts) { alert ->
                    val product = products.find { it.id == alert.productId }
                    if (product != null) {
                        AlertCardItem(
                            alert = alert,
                            product = product,
                            onToggleStatus = { viewModel.pauseOrResumeAlert(alert.id, alert.status) },
                            onDelete = { viewModel.deleteAlert(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCardItem(
    alert: Alert,
    product: Product,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("alert_card_${alert.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (alert.type) {
                            "PRICE_BELOW" -> Icons.Default.TrendingDown
                            "PRICE_ABOVE" -> Icons.Default.TrendingUp
                            "DISCOUNT_PERCENT" -> Icons.Default.Percent
                            "STOCK_AVAILABLE" -> Icons.Default.Inventory
                            else -> Icons.Default.NotificationImportant
                        }
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(product.brand, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (alert.status != "TRIGGERED") {
                        IconButton(onClick = onToggleStatus, modifier = Modifier.size(32.dp).testTag("toggle_alert_${alert.id}")) {
                            Icon(
                                imageVector = if (alert.status == "ACTIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_alert_${alert.id}")) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertizontally
            ) {
                Column {
                    val alertDescriptionText = when (alert.type) {
                        "PRICE_BELOW" -> "Price goes BELOW ${String.format("%.0f", alert.targetValue)} TL"
                        "PRICE_ABOVE" -> "Price goes ABOVE ${String.format("%.0f", alert.targetValue)} TL"
                        "DISCOUNT_PERCENT" -> "Discount exceeds ${String.format("%.0f", alert.targetValue)}%"
                        "STOCK_AVAILABLE" -> "Product restocks \"In Stock\""
                        "STOCK_RUNNING_LOW" -> "Product stock decreases \"Running Low\""
                        else -> "Rule active"
                    }
                    Text("Trigger Rule", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(alertDescriptionText, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Current State", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("${String.format("%.0f", product.currentPrice)} TL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Summary timing tag indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val summaries = listOf("DS" to "Daily Summary", "WS" to "Weekly Summary", "MS" to "Monthly Summary")
                summaries.forEach { item ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(item.first, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// 5. PROFILE SCREEN (SECURITY, SETTINGS & SIMULATION)
@Composable
fun ProfileScreen(viewModel: CommerceViewModel) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isAuthMockActive by viewModel.isAuthenticationMockActive.collectAsStateWithLifecycle()
    val analytics by viewModel.analyticsSummary.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Tracker Profile",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // User profile Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp),
                        contentDescription = "User Photo"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = user?.name ?: "Miraç Birben", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = user?.email ?: "miracbirben@gmail.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("BASIC PRIVILEGE ACCOUNT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Encryption and Biometrics Security panel
        Text("Device Security & Shifting", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertizontally,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertizontally) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Biometric Security Toggle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Unlock using security fingerprint prompt", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = {
                            viewModel.toggleBiometric(it)
                            if (!it) viewModel.setAuthenticationMockActive(false)
                            Toast.makeText(context, if (it) "Fingerprint Mock System Enabled" else "Security fingerprint disabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("biometric_security_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertizontally,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Encrypted Local Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Database SQLite Room records protected", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SECURE", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isAuthMockActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAuthMockActive) "Session Authenticated" else "Session Locked - Restart app check rules",
                            color = if (isAuthMockActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Analytics Analytics summary card
        Text("Analytics Diagnostics Panel", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val ratio = analytics["alertPerformanceRatio"] as? Double ?: 100.0
                val triggered = analytics["triggeredAlerts"] as? Int ?: 0
                val notificationsCount = analytics["notificationCount"] as? Int ?: 0

                AnalyticsRow(label = "Database Tracked Products", value = "${analytics["totalProducts"] ?: 0}")
                Spacer(modifier = Modifier.height(8.dp))
                AnalyticsRow(label = "Users Saved Items Count", value = "${analytics["savedProducts"] ?: 0}")
                Spacer(modifier = Modifier.height(8.dp))
                AnalyticsRow(label = "Triggered Alert items", value = "$triggered")
                Spacer(modifier = Modifier.height(8.dp))
                AnalyticsRow(label = "Rules Engine Fire Ratio", value = "${String.format("%.1f", ratio)}%")

                Spacer(modifier = Modifier.height(12.dp))
                // Graphical presentation block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (ratio / 100.0).toFloat().coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Live Simulations Operations Center
        Text("Alerts Rules Engine Simulator", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "You can manually simulate immediate platform price fluctuations to evaluate the rules-based alerting system. Discount alerts and target lower-bound prices will instantly test true and post alerts notifications.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.simulateDailyDiscounts()
                        Toast.makeText(context, "Fluctuations simulated. Check Notification alerts!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("simulate_discounts_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Random -20% Price Changes")
                }
            }
        }
    }
}

@Composable
fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// 6. PRODUCT DETAILS SCREEN WITH HISTORY CHART VISUALS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: Product,
    viewModel: CommerceViewModel,
    onBack: () -> Unit
) {
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()
    val watchlists by viewModel.watchlists.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var userNotesText by remember(product.userNotes) { mutableStateOf(product.userNotes) }

    // Quick Alert rule sliders state
    var showAddAlertDialogState by remember { mutableStateOf(false) }

    // Simulation price change state
    var customSimulationPriceText by remember { mutableStateOf("") }
    var simulatedAvailability by remember { mutableStateOf(product.availability) }

    if (showAddAlertDialogState) {
        QuickAlertSetupDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { showAddAlertDialogState = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Navigation Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertizontally,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Product Diagnostics Details", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            IconButton(
                onClick = {
                    if (watchlists.isNotEmpty()) {
                        viewModel.addProductToWatchlist(watchlists.first().id, product.id)
                        Toast.makeText(context, "Added to ${watchlists.first().name}!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Create a watchlist group first.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("detail_favorite_trigger")
            ) {
                Icon(Icons.Default.Favorite, tint = MaterialTheme.colorScheme.primary, contentDescription = "Add watchlist")
            }
        }

        // Main description Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .background(Color.White)
                ) {
                    ImagePlaceholder(title = product.name)
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
                            Text("Brand: ${product.brand} • Category: ${product.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    when (product.availability) {
                                        "In Stock" -> Color(0xFF4CAF50)
                                        "Running Low" -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(product.availability, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertizontally
                    ) {
                        Column {
                            Text("Current Pricing Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${String.format("%.2f", product.currentPrice)} TL",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (product.discountPercent > 0.0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${String.format("%.2f", product.previousPrice)} TL",
                                        textDecoration = TextDecoration.LineThrough,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        if (product.discountPercent > 0.0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE53935), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", product.discountPercent)}% DISCOUNT",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // PRICE CHRONOLOGICAL CHART GRAPH (Canvas based)
        Text("Historical Pricing Trend (TL)", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        PriceHistoryChart(
            history = priceHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(bottom = 16.dp)
        )

        // PRICE TIMELINE ITEMS
        Text("Chronological Price Record Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (priceHistory.isEmpty()) {
                    Text("No pricing changes documented.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    priceHistory.reversed().take(3).forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertizontally) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(p.createdAt)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${String.format("%.2f", p.price)} TL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }

        // Saved product Alerts Management Block
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            Text("Product Active Rules Thresholds", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton(
                onClick = { showAddAlertDialogState = true },
                modifier = Modifier.testTag("launch_alert_setup")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Rule")
            }
        }

        val myAlerts = alerts.filter { it.productId == product.id }
        if (myAlerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No rules configured for this specific product.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                myAlerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertizontally) {
                            Icon(
                                imageVector = if (alert.status == "ACTIVE") Icons.Default.CircleNotifications else Icons.Default.PauseCircle,
                                tint = if (alert.status == "ACTIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (alert.type) {
                                    "PRICE_BELOW" -> "Rule: Price <= ${String.format("%.0f", alert.targetValue)} TL"
                                    "PRICE_ABOVE" -> "Rule: Price >= ${String.format("%.0f", alert.targetValue)} TL"
                                    "DISCOUNT_PERCENT" -> "Rule: Discount % >= ${String.format("%.0f", alert.targetValue)}%"
                                    "STOCK_AVAILABLE" -> "Rule: Stock Restores In Stock"
                                    else -> "Rule active"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (alert.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(alert.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (alert.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                    }
                }
            }
        }

        // USER NOTES SECTION
        Text("User Diagnostics Notes", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = userNotesText,
                    onValueChange = { userNotesText = it },
                    placeholder = { Text("Enter internal notes, purchase plans, or bargain target dates...") },
                    modifier = Modifier.fillMaxWidth().height(96.dp).testTag("product_notes_field"),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.updateProductNotes(product.id, userNotesText.trim())
                        Toast.makeText(context, "Product notes saved locally!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End).testTag("save_notes_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Notes")
                }
            }
        }

        // REAL TIME SIMULATION CENTER
        Text("Real-Time Price & Stock Simulator", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A rule-based environment allows live checking. Simulate changing this product's current valuation below, and observe watchlists, notifications, and active rule statuses trigger immediately.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customSimulationPriceText,
                    onValueChange = { customSimulationPriceText = it },
                    label = { Text("Simulated Pricing Value (TL)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("sim_price_field"),
                    placeholder = { Text("Current: ${String.format("%.2f", product.currentPrice)}") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Availability simulator Selection rows
                Text("Simulate Availability Status", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val states = listOf("In Stock", "Running Low", "Out of Stock")
                    states.forEach { statusOption ->
                        FilterChip(
                            selected = simulatedAvailability == statusOption,
                            onClick = { simulatedAvailability = statusOption },
                            label = { Text(statusOption, fontSize = 10.sp) },
                            modifier = Modifier.testTag("sim_avail_$statusOption")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val priceToSimulate = customSimulationPriceText.toDoubleOrNull() ?: product.currentPrice
                        viewModel.simulatePriceChange(product.id, priceToSimulate, simulatedAvailability)
                        customSimulationPriceText = ""
                        Toast.makeText(context, "Price rules updated dynamically!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sim_submit_btn")
                ) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Evaluate Simulation Model")
                }
            }
        }
    }
}

// PREMIUM CANVAS DRAWN CHART
@Composable
fun PriceHistoryChart(history: List<PriceHistory>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) {
        Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Insufficient pricing history points.", fontSize = 11.sp)
            }
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))) {
        val width = size.width
        val height = size.height
        val padding = 34.dp.toPx()

        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        val prices = history.map { it.price }
        val maxPrice = (prices.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val minPrice = (prices.minOrNull() ?: 0.0).coerceAtLeast(0.0)
        val priceRange = if (maxPrice != minPrice) maxPrice - minPrice else 1.0

        // Draw horizontal grid lines (3 steps)
        val steps = 3
        for (i in 0..steps) {
            val y = padding + (chartHeight / steps) * i
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Identify coordinates for historical paths
        val points = mutableListOf<Offset>()
        history.forEachIndexed { index, pricePoint ->
            val fractionIndex = if (history.size > 1) index.toDouble() / (history.size - 1) else 0.5
            val x = padding + (chartWidth * fractionIndex).toFloat()
            val yFraction = (pricePoint.price - minPrice) / priceRange
            val y = padding + (chartHeight * (1.0 - yFraction)).toFloat()
            points.add(Offset(x, y))
        }

        // Draw visual connecting path line
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            // Draw clean stroke line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw smooth gradient fill under charts path lines
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, padding + chartHeight)
                lineTo(points.first().x, padding + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                    startY = points.map { it.y }.minOrNull() ?: padding,
                    endY = padding + chartHeight
                )
            )
        }

        // Draw visual coordinate circles points
        points.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = point
            )
        }
    }
}

// QUICK ALERT SETUP COMPONENT DIALOG
@Composable
fun QuickAlertSetupDialog(
    product: Product,
    viewModel: CommerceViewModel,
    onDismiss: () -> Unit
) {
    var alertType by remember { mutableStateOf("PRICE_BELOW") } // "PRICE_BELOW", "PRICE_ABOVE", "DISCOUNT_PERCENT", "STOCK_AVAILABLE"
    var targetPriceText by remember { mutableStateOf("") }
    var targetDiscountText by remember { mutableStateOf("10") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Set Evaluation Rule", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(product.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(12.dp))

                // Rule Types row
                Text("Condition Pattern", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                val types = listOf(
                    Triple("PRICE_BELOW", "Price Below", Icons.Default.TrendingDown),
                    Triple("PRICE_ABOVE", "Price Above", Icons.Default.TrendingUp),
                    Triple("DISCOUNT_PERCENT", "Discount %", Icons.Default.Percent),
                    Triple("STOCK_AVAILABLE", "Restocks", Icons.Default.Inventory)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(types) { item ->
                        FilterChip(
                            selected = alertType == item.first,
                            onClick = { alertType = item.first },
                            label = { Text(item.second, fontSize = 10.sp) },
                            modifier = Modifier.testTag("rule_chip_${item.first.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (alertType) {
                    "PRICE_BELOW" -> {
                        OutlinedTextField(
                            value = targetPriceText,
                            onValueChange = { targetPriceText = it },
                            label = { Text("Notify when price falls below (TL)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("rule_price_below_field"),
                            placeholder = { Text("Current: ${String.format("%.0f", product.currentPrice)}") }
                        )
                    }
                    "PRICE_ABOVE" -> {
                        OutlinedTextField(
                            value = targetPriceText,
                            onValueChange = { targetPriceText = it },
                            label = { Text("Notify when price increases above (TL)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("rule_price_above_field"),
                            placeholder = { Text("Current: ${String.format("%.0f", product.currentPrice)}") }
                        )
                    }
                    "DISCOUNT_PERCENT" -> {
                        OutlinedTextField(
                            value = targetDiscountText,
                            onValueChange = { targetDiscountText = it },
                            label = { Text("Notify when discount percent exceeds (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("rule_discount_field")
                        )
                    }
                    "STOCK_AVAILABLE" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Condition active: Trigger notifications immediately if stock availability changes to \"In Stock\".",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = when (alertType) {
                                "PRICE_BELOW", "PRICE_ABOVE" -> targetPriceText.toDoubleOrNull() ?: product.currentPrice
                                "DISCOUNT_PERCENT" -> targetDiscountText.toDoubleOrNull() ?: 10.0
                                else -> 0.0
                            }
                            viewModel.createAlert(product.id, alertType, value)
                            onDismiss()
                            Toast.makeText(context, "Rule condition activated!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("rule_submit_btn")
                    ) {
                        Text("Add Alert")
                    }
                }
            }
        }
    }
}
