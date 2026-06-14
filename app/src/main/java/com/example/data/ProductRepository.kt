package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProductRepository(private val db: AppDatabase) {
    private val productDao = db.productDao()
    private val cartDao = db.cartDao()
    private val orderDao = db.orderDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val cartItems: Flow<List<CartItem>> = cartDao.getCartItems()
    val allOrders: Flow<List<PaymentOrder>> = orderDao.getAllOrders()

    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun deleteProductById(id: Int) = productDao.deleteProductById(id)

    // Cart management
    suspend fun addToCart(product: Product, qty: Int = 1) {
        val existing = cartDao.getCartItemByProduct(product.id)
        if (existing != null) {
            cartDao.updateCartItem(existing.copy(quantity = existing.quantity + qty))
        } else {
            cartDao.insertCartItem(
                CartItem(
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price,
                    productCategory = product.category,
                    quantity = qty
                )
            )
        }
    }

    suspend fun updateCartQuantity(cartItemId: Int, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteCartItemById(cartItemId)
        } else {
            val items = cartDao.getCartItems().first()
            val item = items.find { it.id == cartItemId }
            if (item != null) {
                cartDao.updateCartItem(item.copy(quantity = quantity))
            }
        }
    }

    suspend fun removeCartItem(cartItemId: Int) = cartDao.deleteCartItemById(cartItemId)

    suspend fun clearCart() = cartDao.clearCart()

    // Checkout: Places order, updates product stock, and clears cart
    suspend fun placeOrder(
        totalAmount: Double,
        paymentMethod: String,
        phoneNumber: String = "",
        cardHolder: String = "",
        lastFour: String = "",
        transactionCode: String,
        itemsSummary: String
    ): Boolean {
        // Retrieve current cart contents
        val currentCart = cartItems.first()
        if (currentCart.isEmpty()) return false

        // Update product stock counts
        for (item in currentCart) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val newStock = (product.stockCount - item.quantity).coerceAtLeast(0)
                productDao.updateProduct(product.copy(stockCount = newStock))
            }
        }

        // Write the Order record
        val order = PaymentOrder(
            totalAmount = totalAmount,
            status = "COMPLETED",
            paymentMethod = paymentMethod,
            phoneNumber = phoneNumber,
            cardHolder = cardHolder,
            lastFour = lastFour,
            transactionCode = transactionCode,
            itemsSummary = itemsSummary
        )
        orderDao.insertOrder(order)

        // Clear cart
        cartDao.clearCart()
        return true
    }

    // Seed data helper
    suspend fun seedDatabaseIfEmpty() {
        // Query current products list directly (not as Flow to avoid suspension traps)
        val current = allProducts.first()
        if (current.isEmpty()) {
            val defaultProducts = listOf(
                Product(
                    name = "HP Victus 16 Gaming Laptop",
                    category = "Laptops",
                    price = 135000.00,
                    description = "High-performance processing unit designed for creators and gamers alike. Play games in high fidelity with outstanding thermal management.",
                    specifications = "i7-13700H, 16GB Fast DDR5 RAM, 512GB NVMe SSD, RTX 4050 6GB GDDR6 VRAM",
                    stockCount = 8,
                    lowStockThreshold = 3
                ),
                Product(
                    name = "Asus ROG Strix G15 Premium",
                    category = "Laptops",
                    price = 185000.00,
                    description = "Immersive AMD gaming machine with ROG Intelligent Cooling, metallic finish keyboard with Aura Sync, and crystal clear screen refresh rates.",
                    specifications = "Ryzen 9, 16GB RAM, 1TB Gen4 SSD, RTX 4060 8GB GDDR6",
                    stockCount = 4,
                    lowStockThreshold = 3
                ),
                Product(
                    name = "PC Tech Core i5 Builder Rig",
                    category = "Custom PCs",
                    price = 75000.00,
                    description = "Our best-selling mid-tier custom desktop tower, optimized for streaming, web design, workspace multitasking, and budget-friendly gaming.",
                    specifications = "Intel Core i5-12400F, 16GB RGB RAM, 512GB SSD, Nvidia GTX 1650 4GB, 550W Bronze PSU",
                    stockCount = 12,
                    lowStockThreshold = 4
                ),
                Product(
                    name = "PC Tech Ultimate Liquid Rig",
                    category = "Custom PCs",
                    price = 380000.00,
                    description = "Top of the line liquid-cooled custom computing powerhouse. Engineered for extreme workstation tasks and 4K uncompromised gameplay.",
                    specifications = "Core i9-14900K Liquid Cooler, 32GB High-Speed DDR5, 2TB SSD, NVIDIA RTX 4080 Super 16GB",
                    stockCount = 2,
                    lowStockThreshold = 1
                ),
                Product(
                    name = "Corsair Vengeance RGB Pro 16GB",
                    category = "RAM & Parts",
                    price = 9500.00,
                    description = "High performance overclocked DDR4 desktop memory designed to light up your build with stunning dynamic multi-zone RGB lighting.",
                    specifications = "2x8GB DDR4 3200MHz CL16 Desktop Memory Kit Dual Channel",
                    stockCount = 25,
                    lowStockThreshold = 8
                ),
                Product(
                    name = "Crucial P3 Plus 1TB NVMe SSD",
                    category = "Storage",
                    price = 11200.00,
                    description = "Unleash superfast Gen4 speeds in your storage system. Upgrade with Crucial for efficient software response, load operations, and writes.",
                    specifications = "PCIe Gen4 M.2 2280 NVMe, Sequential read up to 5000MB/s, Write 3600MB/s",
                    stockCount = 30,
                    lowStockThreshold = 6
                ),
                Product(
                    name = "Nvidia GeForce RTX 4070 Ti GPU",
                    category = "Graphics Cards",
                    price = 120000.00,
                    description = "Outstanding graphical processing power with advanced ray-tracing engines, DLSS 3 frame generation technology, and ultra-high frame counts.",
                    specifications = "12GB GDDR6X, MSI Gaming X Trio Custom Triple Fan Heat Sink",
                    stockCount = 3,
                    lowStockThreshold = 2
                ),
                Product(
                    name = "Logitech G502 Hero Gaming Mouse",
                    category = "Accessories",
                    price = 8500.00,
                    description = "Legendary gaming mouse designed with customized Hero sensor. Includes adjustable weight layout systems and tactile mechanical keys.",
                    specifications = "16K DPI High Accuracy Sensor, customizable RGB LEDs, 5 weights of 3.6g",
                    stockCount = 15,
                    lowStockThreshold = 5
                ),
                Product(
                    name = "Logitech G213 Prodigy Keyboard",
                    category = "Accessories",
                    price = 6800.00,
                    description = "Tactile, responsive gaming keyboard designed for responsive feedback. Durable, splash-resistant, and styled with multi-mode RGB backlight.",
                    specifications = "Responsive Mech-Dome Keys, Dedicated Multimedia Controls, Integrated Wrist Rest",
                    stockCount = 4,
                    lowStockThreshold = 5
                )
            )

            for (prod in defaultProducts) {
                productDao.insertProduct(prod)
            }
        }
    }
}
