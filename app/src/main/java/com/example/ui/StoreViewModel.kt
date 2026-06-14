package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CartItem
import com.example.data.PaymentOrder
import com.example.data.Product
import com.example.data.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// Navigation destinations
sealed interface Screen {
    object Storefront : Screen
    data class ProductDetails(val productId: Int) : Screen
    object ShoppingCart : Screen
    object Checkout : Screen
    object OrderHistory : Screen
    object AdminDashboard : Screen
    data class EditProduct(val productId: Int?) : Screen // null means "Add Product"
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "pctechkenya_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val repository: ProductRepository by lazy {
        ProductRepository(database)
    }

    // Initialize database seed on creation
    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Screen State Navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Storefront)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation Backstack (simple implementation)
    private val backstack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        backstack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (backstack.isNotEmpty()) {
            _currentScreen.value = backstack.removeAt(backstack.size - 1)
        } else {
            _currentScreen.value = Screen.Storefront
        }
    }

    // Filter/Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Reactive products list filtered by search and category
    val products: StateFlow<List<Product>> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategory
    ) { allProds, query, category ->
        allProds.filter { prod ->
            val matchesCategory = category == "All" || prod.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isEmpty() || 
                    prod.name.contains(query, ignoreCase = true) || 
                    prod.description.contains(query, ignoreCase = true) ||
                    prod.specifications.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Unfiltered raw products for admin inventory
    val rawProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allOrders: StateFlow<List<PaymentOrder>> = repository.allOrders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cart derived totals
    val cartTotal: StateFlow<Double> = repository.cartItems
        .map { items -> items.sumOf { it.productPrice * it.quantity } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val cartCount: StateFlow<Int> = repository.cartItems
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Selected product for details or edit
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    fun selectProductById(productId: Int) {
        viewModelScope.launch {
            _selectedProduct.value = repository.getProductById(productId)
        }
    }

    fun clearSelectedProduct() {
        _selectedProduct.value = null
    }

    // Cart Operations
    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, quantity)
        }
    }

    fun updateCartQuantity(cartItemId: Int, qty: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, qty)
        }
    }

    fun removeCartItem(cartItemId: Int) {
        viewModelScope.launch {
            repository.removeCartItem(cartItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    // Checkout / Payment Gateway Simulation State
    private val _paymentMethod = MutableStateFlow("M-PESA") // "M-PESA" or "CARD"
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    // Form inputs
    private val _mpesaPhone = MutableStateFlow("")
    val mpesaPhone: StateFlow<String> = _mpesaPhone.asStateFlow()

    private val _cardHolderName = MutableStateFlow("")
    val cardHolderName: StateFlow<String> = _cardHolderName.asStateFlow()

    private val _cardNumber = MutableStateFlow("")
    val cardNumber: StateFlow<String> = _cardNumber.asStateFlow()

    private val _cardExpiry = MutableStateFlow("")
    val cardExpiry: StateFlow<String> = _cardExpiry.asStateFlow()

    private val _cardCvv = MutableStateFlow("")
    val cardCvv: StateFlow<String> = _cardCvv.asStateFlow()

    // Payment validation errors
    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    // STK popup simulator visibility
    private val _showStkSimulationDialog = MutableStateFlow(false)
    val showStkSimulationDialog: StateFlow<Boolean> = _showStkSimulationDialog.asStateFlow()

    private val _paymentProcessing = MutableStateFlow(false)
    val paymentProcessing: StateFlow<Boolean> = _paymentProcessing.asStateFlow()

    private val _lastCreatedOrder = MutableStateFlow<PaymentOrder?>(null)
    val lastCreatedOrder: StateFlow<PaymentOrder?> = _lastCreatedOrder.asStateFlow()

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        _paymentError.value = null
    }

    fun updateMpesaPhone(phone: String) {
        _mpesaPhone.value = phone
    }

    fun updateCardHolder(name: String) {
        _cardHolderName.value = name
    }

    fun updateCardNumber(num: String) {
        _cardNumber.value = num.filter { it.isDigit() }
    }

    fun updateCardExpiry(exp: String) {
        _cardExpiry.value = exp
    }

    fun updateCardCvv(cvv: String) {
        _cardCvv.value = cvv.filter { it.isDigit() }
    }

    // Perform Checkout flow
    fun startPaymentFlow() {
        _paymentError.value = null

        val currentCartList = cartItems.value
        if (currentCartList.isEmpty()) {
            _paymentError.value = "Your shopping cart is empty."
            return
        }

        val total = cartTotal.value

        if (_paymentMethod.value == "M-PESA") {
            val phone = _mpesaPhone.value.trim()
            // Validate Kenyan Safaricom Mobile formatting: 
            // e.g., 0712345678, 0112345678, 254712345678, +254712345678
            val phonePattern = "^(07|01|2547|2541|\\+2547|\\+2541)\\d{8}$".toRegex()
            if (!phonePattern.matches(phone)) {
                _paymentError.value = "Please enter a valid Kenyan Safaricom phone number (e.g. 0712345678)."
                return
            }

            // Valid phone: trigger M-Pesa STK push simulation dialog
            _showStkSimulationDialog.value = true
        } else {
            // Validate Card inputs
            if (_cardHolderName.value.trim().isEmpty()) {
                _paymentError.value = "Please enter Cardholder Name."
                return
            }
            if (_cardNumber.value.length < 15) {
                _paymentError.value = "Please enter a valid Credit Card Number."
                return
            }
            if (!_cardExpiry.value.contains("/")) {
                _paymentError.value = "Please enter expiry date (MM/YY)."
                return
            }
            if (_cardCvv.value.length < 3) {
                _paymentError.value = "Please enter a valid CVV."
                return
            }

            // Simulate card processing directly
            processCreditCardPayment(total)
        }
    }

    // Dismiss simulation popup manually
    fun dismissStkDialog() {
        _showStkSimulationDialog.value = false
    }

    // Confirms payment code (e.g., Simulated PIN Entry from Safaricom ToolKit Pop-up)
    fun confirmMpesaPinAndComplete(simulatedPin: String) {
        _showStkSimulationDialog.value = false
        _paymentProcessing.value = true

        viewModelScope.launch {
            // Emulate secure verification delay
            delay(2500)

            val total = cartTotal.value
            val cartList = cartItems.value
            val itemsSummary = cartList.joinToString(", ") { "${it.quantity}x ${it.productName}" }

            // Generate an authentic Safaricom-like receipt code (e.g., RBA198C29K)
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val digits = "0123456789"
            val prefix = (1..3).map { alphabet.random() }.joinToString("")
            val midSec = (1..5).map { digits.random() }.joinToString("")
            val suffix = alphabet.random().toString() + digits.random().toString()
            val mpesaTxCode = "$prefix$midSec$suffix"

            val success = repository.placeOrder(
                totalAmount = total,
                paymentMethod = "M-PESA",
                phoneNumber = _mpesaPhone.value,
                transactionCode = mpesaTxCode,
                itemsSummary = itemsSummary
            )

            if (success) {
                // Clear state
                _mpesaPhone.value = ""
                // Set last created order summary
                _lastCreatedOrder.value = repository.allOrders.first().firstOrNull()
                _paymentProcessing.value = false
                navigateTo(Screen.OrderHistory)
            } else {
                _paymentError.value = "Order placement action failed."
                _paymentProcessing.value = false
            }
        }
    }

    private fun processCreditCardPayment(total: Double) {
        _paymentProcessing.value = true
        viewModelScope.launch {
            delay(3000) // Processing simulation

            val cartList = cartItems.value
            val itemsSummary = cartList.joinToString(", ") { "${it.quantity}x ${it.productName}" }
            val truncatedCard = _cardNumber.value.takeLast(4)
            val cardTxCode = "CRD-" + UUID.randomUUID().toString().take(8).uppercase()

            val success = repository.placeOrder(
                totalAmount = total,
                paymentMethod = "CARD",
                cardHolder = _cardHolderName.value,
                lastFour = truncatedCard,
                transactionCode = cardTxCode,
                itemsSummary = itemsSummary
            )

            if (success) {
                _cardHolderName.value = ""
                _cardNumber.value = ""
                _cardExpiry.value = ""
                _cardCvv.value = ""
                _lastCreatedOrder.value = repository.allOrders.first().firstOrNull()
                _paymentProcessing.value = false
                navigateTo(Screen.OrderHistory)
            } else {
                _paymentError.value = "Credit card processing connection failed. Try again."
                _paymentProcessing.value = false
            }
        }
    }

    // Admin Panel Database Inventory Management Operations
    fun addOrUpdateProduct(
        id: Int?, // if null, insert, if not null, update
        name: String,
        category: String,
        price: Double,
        description: String,
        specifications: String,
        stockCount: Int,
        lowStockThreshold: Int
    ) {
        viewModelScope.launch {
            if (id == null) {
                val newProd = Product(
                    name = name,
                    category = category,
                    price = price,
                    description = description,
                    specifications = specifications,
                    stockCount = stockCount,
                    lowStockThreshold = lowStockThreshold
                )
                repository.insertProduct(newProd)
            } else {
                val existing = repository.getProductById(id)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name,
                        category = category,
                        price = price,
                        description = description,
                        specifications = specifications,
                        stockCount = stockCount,
                        lowStockThreshold = lowStockThreshold
                    )
                    repository.updateProduct(updated)
                }
            }
            navigateTo(Screen.AdminDashboard)
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.deleteProductById(productId)
        }
    }
}
