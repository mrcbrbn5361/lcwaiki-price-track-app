package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getPrimaryUser(): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsSec(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductByIdFlow(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT DISTINCT category FROM products")
    fun getAllCategoriesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM products")
    fun getAllBrandsFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products")
    suspend fun clearProducts()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlists WHERE userId = :userId")
    fun getWatchlistsForUserFlow(userId: Long): Flow<List<Watchlist>>

    @Query("SELECT * FROM watchlists WHERE userId = :userId")
    suspend fun getWatchlistsForUser(userId: Long): List<Watchlist>

    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun getWatchlistById(id: Long): Watchlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: Watchlist): Long

    @Update
    suspend fun updateWatchlist(watchlist: Watchlist)

    @Delete
    suspend fun deleteWatchlist(watchlist: Watchlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItem): Long

    @Query("DELETE FROM watchlist_items WHERE watchlistId = :watchlistId AND productId = :productId")
    suspend fun removeProductFromWatchlist(watchlistId: Long, productId: Long)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun deleteWatchlistItem(id: Long)

    @Query("""
        SELECT p.* FROM products p 
        INNER JOIN watchlist_items wi ON p.id = wi.productId 
        WHERE wi.watchlistId = :watchlistId
    """)
    fun getProductsForWatchlistFlow(watchlistId: Long): Flow<List<Product>>

    @Query("""
        SELECT p.* FROM products p 
        INNER JOIN watchlist_items wi ON p.id = wi.productId 
        WHERE wi.watchlistId = :watchlistId
    """)
    suspend fun getProductsForWatchlist(watchlistId: Long): List<Product>

    @Query("SELECT COUNT(*) FROM watchlist_items WHERE watchlistId = :watchlistId")
    fun getProductCountForWatchlistFlow(watchlistId: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE watchlistId = :watchlistId AND productId = :productId)")
    fun isProductInWatchlistFlow(watchlistId: Long, productId: Long): Flow<Boolean>
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE userId = :userId ORDER BY id DESC")
    fun getAlertsForUserFlow(userId: Long): Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE userId = :userId")
    suspend fun getAlertsForUser(userId: Long): List<Alert>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertById(id: Long): Alert?

    @Query("SELECT * FROM alerts WHERE productId = :productId")
    fun getAlertsForProductFlow(productId: Long): Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE productId = :productId")
    suspend fun getAlertsForProduct(productId: Long): List<Alert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert): Long

    @Update
    suspend fun updateAlert(alert: Alert)

    @Delete
    suspend fun deleteAlert(alert: Alert)

    @Query("UPDATE alerts SET status = :status WHERE id = :id")
    suspend fun updateAlertStatus(id: Long, status: String)

    @Query("DELETE FROM alerts")
    suspend fun clearAlerts()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUserFlow(userId: Long): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getNotificationsForUser(userId: Long): List<Notification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearNotificationsForUser(userId: Long)
}

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY createdAt ASC")
    fun getPriceHistoryForProductFlow(productId: Long): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY createdAt ASC")
    suspend fun getPriceHistoryForProduct(productId: Long): List<PriceHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(history: PriceHistory): Long

    @Query("DELETE FROM price_history WHERE productId = :productId")
    suspend fun clearPriceHistoryForProduct(productId: Long)
}
