package com.example.data.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.CommerceRepository

class AppContainer(private val context: Context) {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val repository: CommerceRepository by lazy {
        CommerceRepository(
            database = database,
            userDao = database.userDao(),
            productDao = database.productDao(),
            watchlistDao = database.watchlistDao(),
            alertDao = database.alertDao(),
            notificationDao = database.notificationDao(),
            priceHistoryDao = database.priceHistoryDao()
        )
    }
}
