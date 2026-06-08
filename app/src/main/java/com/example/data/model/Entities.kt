package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val brand: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val discountPercent: Double,
    val availability: String, // e.g. "In Stock", "Out of Stock", "Running Low"
    val imageUrl: String,
    val productUrl: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val userNotes: String = ""
) : Serializable {
    val isDiscounted: Boolean
        get() = discountPercent > 0.0
}

@Entity(
    tableName = "watchlists",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Watchlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String
) : Serializable

@Entity(
    tableName = "watchlist_items",
    foreignKeys = [
        ForeignKey(
            entity = Watchlist::class,
            parentColumns = ["id"],
            childColumns = ["watchlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["watchlistId"]),
        Index(value = ["productId"])
    ]
)
data class WatchlistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val watchlistId: Long,
    val productId: Long
) : Serializable

@Entity(
    tableName = "alerts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["productId"])
    ]
)
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val productId: Long,
    val type: String, // "PRICE_BELOW", "PRICE_ABOVE", "DISCOUNT_PERCENT", "STOCK_AVAILABLE", "STOCK_RUNNING_LOW", "DAILY_SUMMARY", "WEEKLY_SUMMARY", "MONTHLY_SUMMARY"
    val targetValue: Double, // target price or percentage threshold
    val status: String // "ACTIVE", "PAUSED", "TRIGGERED", "ARCHIVED"
) : Serializable

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"])]
)
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val price: Double,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
