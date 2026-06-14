package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val imageUrl: String = "",
    val stockCount: Int,
    val specifications: String = "",
    val lowStockThreshold: Int = 5
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val productPrice: Double,
    val productCategory: String,
    val quantity: Int
)

@Entity(tableName = "payment_orders")
data class PaymentOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderDate: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val status: String, // "PENDING", "COMPLETED", "FAILED"
    val paymentMethod: String, // "M-PESA", "CARD"
    val phoneNumber: String = "", // For M-Pesa
    val cardHolder: String = "", // For Card payments
    val lastFour: String = "", // For Card payments
    val transactionCode: String, // Generated e.g., MPESA-XX-YY
    val itemsSummary: String // Concise description of items ordered
)
