package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.CommerceApplication
import com.example.data.model.*
import com.example.data.repository.CommerceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommerceViewModel(private val repository: CommerceRepository) : ViewModel() {

    // Active screen index (0: Home, 1: Products, 2: Watchlists, 3: Alerts, 4: Profile)
    private val _currentScreen = MutableStateFlow(0)
    val currentScreen: StateFlow<Int> = _currentScreen.asStateFlow()

    fun setCurrentScreen(index: Int) {
        _currentScreen.value = index
    }

    // Secondary navigation for Product Details (null means no product selected)
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
        if (product != null) {
            // Trigger load of price history for selected product
            viewModelScope.launch {
                repository.getPriceHistoryForProduct(product.id).collect { history ->
                    _priceHistory.value = history
                }
            }
        }
    }

    fun updateProductNotes(productId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateProductNotes(productId, notes)
            // Refresh currently selected product
            if (_selectedProduct.value?.id == productId) {
                _selectedProduct.value = _selectedProduct.value?.copy(userNotes = notes)
            }
        }
    }

    // Price History holder for selected product
    private val _priceHistory = MutableStateFlow<List<PriceHistory>>(emptyList())
    val priceHistory: StateFlow<List<PriceHistory>> = _priceHistory.asStateFlow()

    // Preferences and Security states
    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAuthenticationMockActive = MutableStateFlow(false)
    val isAuthenticationMockActive: StateFlow<Boolean> = _isAuthenticationMockActive.asStateFlow()

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    fun setAuthenticationMockActive(active: Boolean) {
        _isAuthenticationMockActive.value = active
    }

    // Active User
    val currentUser = repository.primaryUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // All products stream
    val allProducts = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Categories and Brands lists
    val categories = repository.categories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val brands = repository.brands.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search, Filter, Sort State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand = _selectedBrand.asStateFlow()

    private val _minPriceFilter = MutableStateFlow<Double?>(null)
    val minPriceFilter = _minPriceFilter.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow<Double?>(null)
    val maxPriceFilter = _maxPriceFilter.asStateFlow()

    private val _filterOnlyDiscounted = MutableStateFlow(false)
    val filterOnlyDiscounted = _filterOnlyDiscounted.asStateFlow()

    private val _filterOnlyAvailable = MutableStateFlow(false)
    val filterOnlyAvailable = _filterOnlyAvailable.asStateFlow()

    private val _sortBy = MutableStateFlow("Lowest Price") // "Lowest Price", "Highest Price", "Newest", "Most Discounted"
    val sortBy = _sortBy.asStateFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(cat: String?) { _selectedCategory.value = cat }
    fun setSelectedBrand(brand: String?) { _selectedBrand.value = brand }
    fun setMinPrice(price: Double?) { _minPriceFilter.value = price }
    fun setMaxPrice(price: Double?) { _maxPriceFilter.value = price }
    fun setOnlyDiscounted(only: Boolean) { _filterOnlyDiscounted.value = only }
    fun setOnlyAvailable(only: Boolean) { _filterOnlyAvailable.value = only }
    fun setSortBy(sort: String) { _sortBy.value = sort }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedBrand.value = null
        _minPriceFilter.value = null
        _maxPriceFilter.value = null
        _filterOnlyDiscounted.value = false
        _filterOnlyAvailable.value = false
        _sortBy.value = "Lowest Price"
    }

    // Filtered Products stream
    val filteredProducts = combine(
        allProducts, searchQuery, selectedCategory, selectedBrand,
        minPriceFilter, maxPriceFilter, filterOnlyDiscounted, filterOnlyAvailable, sortBy
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val products = array[0] as List<Product>
        val query = array[1] as String
        val cat = array[2] as String?
        val brand = array[3] as String?
        val minVal = array[4] as Double?
        val maxVal = array[5] as Double?
        val discOnly = array[6] as Boolean
        val availOnly = array[7] as Boolean
        val sort = array[8] as String

        var list = products

        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) || it.brand.contains(query, ignoreCase = true) }
        }
        if (cat != null) {
            list = list.filter { it.category == cat }
        }
        if (brand != null) {
            list = list.filter { it.brand == brand }
        }
        if (minVal != null) {
            list = list.filter { it.currentPrice >= minVal }
        }
        if (maxVal != null) {
            list = list.filter { it.currentPrice <= maxVal }
        }
        if (discOnly) {
            list = list.filter { it.discountPercent > 0.0 }
        }
        if (availOnly) {
            list = list.filter { it.availability == "In Stock" || it.availability == "Running Low" }
        }

        when (sort) {
            "Lowest Price" -> list.sortedBy { it.currentPrice }
            "Highest Price" -> list.sortedByDescending { it.currentPrice }
            "Newest" -> list.sortedByDescending { it.updatedAt }
            "Most Discounted" -> list.sortedByDescending { it.discountPercent }
            else -> list
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Watchlist States
    val watchlists = currentUser.flatMapLatest { user ->
        if (user != null) repository.getWatchlists(user.id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedWatchlist = MutableStateFlow<Watchlist?>(null)
    val selectedWatchlist = _selectedWatchlist.asStateFlow()

    fun selectWatchlist(watchlist: Watchlist?) {
        _selectedWatchlist.value = watchlist
    }

    val productsInSelectedWatchlist = selectedWatchlist.flatMapLatest { wl ->
        if (wl != null) repository.getProductsForWatchlist(wl.id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createWatchlist(name: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.createWatchlist(user.id, name)
        }
    }

    fun renameWatchlist(watchlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameWatchlist(watchlistId, newName)
            if (_selectedWatchlist.value?.id == watchlistId) {
                _selectedWatchlist.value = _selectedWatchlist.value?.copy(name = newName)
            }
        }
    }

    fun deleteWatchlist(watchlistId: Long) {
        viewModelScope.launch {
            repository.deleteWatchlist(watchlistId)
            if (_selectedWatchlist.value?.id == watchlistId) {
                _selectedWatchlist.value = null
            }
        }
    }

    fun addProductToWatchlist(watchlistId: Long, productId: Long) {
        viewModelScope.launch {
            repository.addProductToWatchlist(watchlistId, productId)
        }
    }

    fun removeProductFromWatchlist(watchlistId: Long, productId: Long) {
        viewModelScope.launch {
            repository.removeProductFromWatchlist(watchlistId, productId)
        }
    }

    // Alerts Systems
    val alerts = currentUser.flatMapLatest { user ->
        if (user != null) repository.getAlertsForUser(user.id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createAlert(productId: Long, type: String, targetValue: Double) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val alert = Alert(
                userId = user.id,
                productId = productId,
                type = type,
                targetValue = targetValue,
                status = "ACTIVE"
            )
            repository.createAlert(alert)
        }
    }

    fun pauseOrResumeAlert(alertId: Long, currentStatus: String) {
        viewModelScope.launch {
            repository.pauseOrResumeAlert(alertId, currentStatus)
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }

    // Notifications systems
    val notifications = currentUser.flatMapLatest { user ->
        if (user != null) repository.getNotificationsForUser(user.id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearNotifications() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.clearNotifications(user.id)
        }
    }

    // Analytics calculations summary flow that recomputes when products, watchlists, or alerts change
    val analyticsSummary = combine(allProducts, watchlists, alerts, notifications) { _, _, _, _ ->
        val user = currentUser.value
        if (user != null) {
            repository.getAnalyticsSummary(user.id)
        } else {
            emptyMap()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Dynamic price simulator function to trigger rules-based alerts & show real updates
    fun simulatePriceChange(productId: Long, newPrice: Double, availability: String? = null) {
        viewModelScope.launch {
            repository.updateProductPriceAndEvaluateAlerts(productId, newPrice, availability)
            // Re-select products so details updates
            _selectedProduct.value = repository.allProducts.firstOrNull()?.find { it.id == productId }
        }
    }

    fun simulateDailyDiscounts() {
        viewModelScope.launch {
            val products = allProducts.value
            if (products.isNotEmpty()) {
                // Pick 2 random products and discount them by 20%
                val electronics = products.filter { it.category == "Electronics" }
                val homes = products.filter { it.category == "Home" }

                if (electronics.isNotEmpty()) {
                    val p = electronics.random()
                    val discPrice = p.currentPrice * 0.8 // 20% off
                    repository.updateProductPriceAndEvaluateAlerts(p.id, discPrice)
                }

                if (homes.isNotEmpty()) {
                    val p = homes.random()
                    val discPrice = p.currentPrice * 0.75 // 25% off
                    repository.updateProductPriceAndEvaluateAlerts(p.id, discPrice)
                }
            }
        }
    }

    init {
        // Run database seeder on initialization
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            repository.getOrInitializeUser()
        }
    }

    // Companion factory for manual dependency injection via AppContainer
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as CommerceApplication
                return CommerceViewModel(application.container.repository) as T
            }
        }
    }
}
