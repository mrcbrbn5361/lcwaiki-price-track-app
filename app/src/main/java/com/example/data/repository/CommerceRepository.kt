package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class CommerceRepository(
    private val database: AppDatabase,
    private val userDao: UserDao,
    private val productDao: ProductDao,
    private val watchlistDao: WatchlistDao,
    private val alertDao: AlertDao,
    private val notificationDao: NotificationDao,
    private val priceHistoryDao: PriceHistoryDao
) {
    // Flows
    val primaryUser: Flow<User?> = userDao.getPrimaryUser()
    val allProducts: Flow<List<Product>> = productDao.getAllProductsFlow()
    val categories: Flow<List<String>> = productDao.getAllCategoriesFlow()
    val brands: Flow<List<String>> = productDao.getAllBrandsFlow()

    fun getWatchlists(userId: Long): Flow<List<Watchlist>> = watchlistDao.getWatchlistsForUserFlow(userId)
    fun getProductsForWatchlist(watchlistId: Long): Flow<List<Product>> = watchlistDao.getProductsForWatchlistFlow(watchlistId)
    fun getProductCountForWatchlist(watchlistId: Long): Flow<Int> = watchlistDao.getProductCountForWatchlistFlow(watchlistId)
    fun isProductInWatchlist(watchlistId: Long, productId: Long): Flow<Boolean> = watchlistDao.isProductInWatchlistFlow(watchlistId, productId)

    fun getAlertsForUser(userId: Long): Flow<List<Alert>> = alertDao.getAlertsForUserFlow(userId)
    fun getAlertsForProduct(productId: Long): Flow<List<Alert>> = alertDao.getAlertsForProductFlow(productId)
    fun getNotificationsForUser(userId: Long): Flow<List<Notification>> = notificationDao.getNotificationsForUserFlow(userId)
    fun getPriceHistoryForProduct(productId: Long): Flow<List<PriceHistory>> = priceHistoryDao.getPriceHistoryForProductFlow(productId)

    // User Operations
    suspend fun getOrInitializeUser(): User = withContext(Dispatchers.IO) {
        val user = userDao.getPrimaryUser().firstOrNull()
        if (user != null) {
            return@withContext user
        } else {
            val defaultUser = User(email = "miracbirben@gmail.com", name = "Miraç Birben")
            val id = userDao.insertUser(defaultUser)
            return@withContext defaultUser.copy(id = id)
        }
    }

    // Product Operations
    suspend fun addProduct(product: Product): Long = withContext(Dispatchers.IO) {
        val id = productDao.insertProduct(product)
        // Insert initial price history
        priceHistoryDao.insertPriceHistory(
            PriceHistory(productId = id, price = product.currentPrice, createdAt = System.currentTimeMillis() - 86400000 * 3)
        )
        priceHistoryDao.insertPriceHistory(
            PriceHistory(productId = id, price = product.currentPrice, createdAt = System.currentTimeMillis())
        )
        id
    }

    suspend fun updateProductNotes(productId: Long, notes: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId)
        if (product != null) {
            productDao.updateProduct(product.copy(userNotes = notes))
        }
    }

    suspend fun updateProductPriceAndEvaluateAlerts(productId: Long, newPrice: Double, availability: String? = null) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val oldPrice = product.currentPrice
        if (oldPrice == newPrice && (availability == null || availability == product.availability)) return@withContext

        // Update product object
        val prevPrice = if (newPrice != oldPrice) oldPrice else product.previousPrice
        val discountPercent = if (prevPrice > 0) ((prevPrice - newPrice) / prevPrice * 100).coerceAtLeast(0.0) else 0.0

        val updatedProduct = product.copy(
            currentPrice = newPrice,
            previousPrice = prevPrice,
            discountPercent = discountPercent,
            availability = availability ?: product.availability,
            updatedAt = System.currentTimeMillis()
        )
        productDao.updateProduct(updatedProduct)

        // Insert new price history if price changed
        if (newPrice != oldPrice) {
            priceHistoryDao.insertPriceHistory(
                PriceHistory(productId = productId, price = newPrice, createdAt = System.currentTimeMillis())
            )
        }

        // Evaluate rules-based alerts!
        val alerts = alertDao.getAlertsForProduct(productId)
        for (alert in alerts) {
            if (alert.status != "ACTIVE") continue

            var triggerAlert = false
            var message = ""

            when (alert.type) {
                "PRICE_BELOW" -> {
                    if (newPrice <= alert.targetValue) {
                        triggerAlert = true
                        message = "Price of ${product.name} fell to ${String.format("%.2f", newPrice)} TL (Target below: ${String.format("%.2f", alert.targetValue)} TL)"
                    }
                }
                "PRICE_ABOVE" -> {
                    if (newPrice >= alert.targetValue) {
                        triggerAlert = true
                        message = "Price of ${product.name} increased to ${String.format("%.2f", newPrice)} TL (Target above: ${String.format("%.2f", alert.targetValue)} TL)"
                    }
                }
                "DISCOUNT_PERCENT" -> {
                    if (discountPercent >= alert.targetValue) {
                        triggerAlert = true
                        message = "${product.name} is now discounted by ${String.format("%.1f", discountPercent)}% (Target: ${String.format("%.1f", alert.targetValue)}%)"
                    }
                }
                "STOCK_AVAILABLE" -> {
                    if (availability == "In Stock" && product.availability != "In Stock") {
                        triggerAlert = true
                        message = "${product.name} is back in stock!"
                    }
                }
                "STOCK_RUNNING_LOW" -> {
                    if (availability == "Running Low" && product.availability != "Running Low") {
                        triggerAlert = true
                        message = "${product.name} is running extremely low on stock!"
                    }
                }
            }

            if (triggerAlert) {
                // Update alert status
                alertDao.updateAlert(alert.copy(status = "TRIGGERED"))

                // Send In-App Push Notification
                notificationDao.insertNotification(
                    Notification(
                        userId = alert.userId,
                        title = "Alert Triggered!",
                        message = message,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Watchlist Operations
    suspend fun createWatchlist(userId: Long, name: String): Long = withContext(Dispatchers.IO) {
        watchlistDao.insertWatchlist(Watchlist(userId = userId, name = name))
    }

    suspend fun renameWatchlist(watchlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val watchlist = watchlistDao.getWatchlistById(watchlistId)
        if (watchlist != null) {
            watchlistDao.updateWatchlist(watchlist.copy(name = newName))
        }
    }

    suspend fun deleteWatchlist(watchlistId: Long) = withContext(Dispatchers.IO) {
        val watchlist = watchlistDao.getWatchlistById(watchlistId)
        if (watchlist != null) {
            watchlistDao.deleteWatchlist(watchlist)
        }
    }

    suspend fun addProductToWatchlist(watchlistId: Long, productId: Long) = withContext(Dispatchers.IO) {
        watchlistDao.insertWatchlistItem(WatchlistItem(watchlistId = watchlistId, productId = productId))
    }

    suspend fun removeProductFromWatchlist(watchlistId: Long, productId: Long) = withContext(Dispatchers.IO) {
        watchlistDao.removeProductFromWatchlist(watchlistId, productId)
    }

    // Alert Operations
    suspend fun createAlert(alert: Alert): Long = withContext(Dispatchers.IO) {
        alertDao.insertAlert(alert)
    }

    suspend fun deleteAlert(alertId: Long) = withContext(Dispatchers.IO) {
        val alert = alertDao.getAlertById(alertId)
        if (alert != null) {
            alertDao.deleteAlert(alert)
        }
    }

    suspend fun pauseOrResumeAlert(alertId: Long, currentStatus: String) = withContext(Dispatchers.IO) {
        val newStatus = if (currentStatus == "ACTIVE") "PAUSED" else "ACTIVE"
        alertDao.updateAlertStatus(alertId, newStatus)
    }

    // Notification Operations
    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun clearNotifications(userId: Long) = withContext(Dispatchers.IO) {
        notificationDao.clearNotificationsForUser(userId)
    }

    // Seeding Database
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existing = productDao.getAllProductsSec()
        if (existing.isNotEmpty()) {
            Log.d("CommerceRepository", "Database already seeded with ${existing.size} products.")
            return@withContext
        }

        Log.d("CommerceRepository", "Seeding database with default values...")
        val userId = getOrInitializeUser().id

        // Create initial products
        val sampleProducts = listOf(
            Product(1, "MacBook Pro M3 Max", "Electronics", "Apple", 79999.0, 84999.0, 5.8, "In Stock", "https://picsum.photos/id/180/300/200", "https://apple.com"),
            Product(2, "iPhone 15 Pro Max", "Electronics", "Apple", 64999.0, 69999.0, 7.1, "In Stock", "https://picsum.photos/id/160/300/200", "https://apple.com"),
            Product(3, "Samsung Odyssey G9 49\"", "Electronics", "Samsung", 34999.0, 39999.0, 12.5, "Running Low", "https://picsum.photos/id/1/300/200", "https://samsung.com"),
            Product(4, "Sony WH-1000XM5 ANC", "Electronics", "Sony", 11999.0, 12999.0, 7.6, "In Stock", "https://picsum.photos/id/3/300/200", "https://sony.com"),
            Product(5, "PlayStation 5 Slim 1TB", "Electronics", "Sony", 21999.0, 21999.0, 0.0, "Out of Stock", "https://picsum.photos/id/96/300/200", "https://playstation.com"),
            Product(6, "Philips L'OR Barista Pro", "Home", "Philips", 4499.0, 5499.0, 18.1, "In Stock", "https://picsum.photos/id/30/300/200", "https://philips.com"),
            Product(7, "Dyson V15 Detect Complete", "Home", "Dyson", 28999.0, 28999.0, 0.0, "In Stock", "https://picsum.photos/id/142/300/200", "https://dyson.com"),
            Product(8, "Nike Air Max 270 Black", "Fashion", "Nike", 4199.0, 4999.0, 16.0, "In Stock", "https://picsum.photos/id/21/300/200", "https://nike.com"),
            Product(9, "Adidas Ultraboost Light", "Fashion", "Adidas", 5299.0, 5299.0, 0.0, "Running Low", "https://picsum.photos/id/103/300/200", "https://adidas.com"),
            Product(10, "Stanley Quencher Tumbler 1.18L", "Sports", "Stanley", 1699.0, 1999.0, 15.0, "In Stock", "https://picsum.photos/id/36/300/200", "https://stanley1913.com")
        )

        for (product in sampleProducts) {
            val pId = productDao.insertProduct(product)

            // Seed historical price points over last 3 weeks to draw realistic charts
            val points = if (product.currentPrice == product.previousPrice) {
                listOf(
                    product.currentPrice * 1.05,
                    product.currentPrice * 1.02,
                    product.currentPrice
                )
            } else {
                listOf(
                    product.previousPrice,
                    product.previousPrice * 1.03,
                    product.previousPrice,
                    product.currentPrice
                )
            }

            points.forEachIndexed { index, price ->
                priceHistoryDao.insertPriceHistory(
                    PriceHistory(
                        productId = pId,
                        price = price,
                        createdAt = System.currentTimeMillis() - (points.size - 1 - index) * 7 * 86400000
                    )
                )
            }
        }

        // Create default watchlists
        val techId = watchlistDao.insertWatchlist(Watchlist(userId = userId, name = "Premium Tech List"))
        val homeId = watchlistDao.insertWatchlist(Watchlist(userId = userId, name = "My Home Wishlist"))

        // Add some items to watchlist
        watchlistDao.insertWatchlistItem(WatchlistItem(watchlistId = techId, productId = 1)) // MacBook
        watchlistDao.insertWatchlistItem(WatchlistItem(watchlistId = techId, productId = 2)) // iPhone
        watchlistDao.insertWatchlistItem(WatchlistItem(watchlistId = techId, productId = 4)) // Sony XM5
        watchlistDao.insertWatchlistItem(WatchlistItem(watchlistId = homeId, productId = 6)) // Philips Coffee Maker
        watchlistDao.insertWatchlistItem(WatchlistItem(watchlistId = homeId, productId = 7)) // Dyson

        // Create active alerts
        alertDao.insertAlert(Alert(userId = userId, productId = 1, type = "PRICE_BELOW", targetValue = 75000.0, status = "ACTIVE"))
        alertDao.insertAlert(Alert(userId = userId, productId = 3, type = "STOCK_AVAILABLE", targetValue = 0.0, status = "ACTIVE"))
        alertDao.insertAlert(Alert(userId = userId, productId = 4, type = "DISCOUNT_PERCENT", targetValue = 10.0, status = "ACTIVE"))
        alertDao.insertAlert(Alert(userId = userId, productId = 7, type = "PRICE_BELOW", targetValue = 25000.0, status = "ACTIVE"))

        // Pre-create some alerts that have already been triggered
        alertDao.insertAlert(Alert(userId = userId, productId = 2, type = "PRICE_BELOW", targetValue = 66000.0, status = "TRIGGERED"))

        // Create notifications
        notificationDao.insertNotification(Notification(userId = userId, title = "Welcome to Commerce Tracker Pro!", message = "You are currently signed in as Miraç Birben. Biometric security is ready to toggle on.", createdAt = System.currentTimeMillis() - 3600000 * 24))
        notificationDao.insertNotification(Notification(userId = userId, title = "Price Alert Triggered!", message = "Price of iPhone 15 Pro Max fell to 64,999.00 TL (Previous: 69,999.00 TL)", createdAt = System.currentTimeMillis() - 3600000 * 2))
        notificationDao.insertNotification(Notification(userId = userId, title = "Weekly Price Summary", message = "Weekly report: 3 tracking watchlists active. Dyson V15 price stayed stable.", createdAt = System.currentTimeMillis() - 3600000 * 12))
    }

    // Analytics Calculations
    suspend fun getAnalyticsSummary(userId: Long): Map<String, Any> = withContext(Dispatchers.IO) {
        val products = productDao.getAllProductsSec()
        val watchlists = watchlistDao.getWatchlistsForUser(userId)
        val alerts = alertDao.getAlertsForUser(userId)
        val notifications = notificationDao.getNotificationsForUser(userId)

        var totalWatchlistItems = 0
        for (w in watchlists) {
            totalWatchlistItems += watchlistDao.getProductsForWatchlist(w.id).size
        }

        val activeAlerts = alerts.filter { it.status == "ACTIVE" }
        val triggeredAlerts = alerts.filter { it.status == "TRIGGERED" }

        // Find average discount percentage
        val discountedProducts = products.filter { it.discountPercent > 0.0 }
        val avgDiscount = if (discountedProducts.isNotEmpty()) discountedProducts.map { it.discountPercent }.average() else 0.0

        // Find category-wise tracking counts
        val categoryCounts = products.groupBy { it.category }.mapValues { it.value.size }

        // Find price alert performance: triggered vs active
        val alertPerformanceRatio = if (alerts.isNotEmpty()) (triggeredAlerts.size.toDouble() / alerts.size.toDouble() * 100) else 100.0

        return@withContext mapOf(
            "totalProducts" to products.size,
            "savedProducts" to totalWatchlistItems,
            "activeAlerts" to activeAlerts.size,
            "triggeredAlerts" to triggeredAlerts.size,
            "priceDropCount" to discountedProducts.size,
            "avgDiscount" to avgDiscount,
            "categoryCounts" to categoryCounts,
            "alertPerformanceRatio" to alertPerformanceRatio,
            "notificationCount" to notifications.size
        )
    }
}
