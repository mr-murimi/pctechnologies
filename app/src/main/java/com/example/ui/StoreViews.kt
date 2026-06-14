package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CartItem
import com.example.data.PaymentOrder
import com.example.data.Product
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Helper function to format prices in Kenyan Shillings
fun formatKsh(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
    // Ensure we show KSh neatly
    return formatter.format(amount).replace("KES", "Ksh")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreMainScreen(viewModel: StoreViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "PC Tech Kenya Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { viewModel.navigateTo(Screen.Storefront) }
                        )
                        Column(
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pctechkenya.com"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback if no web handler found
                                }
                            }
                        ) {
                            Text(
                                text = "PC TECH KENYA",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = 1.2.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "pctechkenya.com",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Visit Website",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Orders history action
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.OrderHistory) },
                        modifier = Modifier.testTag("nav_orders_history_btn")
                    ) {
                        Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = "Order History")
                    }

                    // Admin dashboard action
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                        modifier = Modifier.testTag("nav_admin_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admin Area", tint = MaterialTheme.colorScheme.tertiary)
                    }

                    // Shopping Cart action
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(text = cartCount.toString())
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 12.dp, top = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.ShoppingCart) }
                            .testTag("nav_cart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Shopping Cart",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    is Screen.Storefront -> StorefrontScreen(viewModel)
                    is Screen.ProductDetails -> ProductDetailsScreen(viewModel, screen.productId)
                    is Screen.ShoppingCart -> ShoppingCartScreen(viewModel)
                    is Screen.Checkout -> CheckoutScreen(viewModel)
                    is Screen.OrderHistory -> OrderHistoryScreen(viewModel)
                    is Screen.AdminDashboard -> AdminDashboardScreen(viewModel)
                    is Screen.EditProduct -> EditProductScreen(viewModel, screen.productId)
                }
            }

            // Global STK Push Overlay dialog
            val showStk by viewModel.showStkSimulationDialog.collectAsState()
            val totalAmt by viewModel.cartTotal.collectAsState()
            val phoneNum by viewModel.mpesaPhone.collectAsState()

            if (showStk) {
                MpesaStkSimulationDialog(
                    amount = totalAmt,
                    phoneNumber = phoneNum,
                    onDismiss = { viewModel.dismissStkDialog() },
                    onConfirm = { pin -> viewModel.confirmMpesaPinAndComplete(pin) }
                )
            }

            // Global Loading Indicator for Payment
            val processing by viewModel.paymentProcessing.collectAsState()
            if (processing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF64748B).copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Securing Payment Gateway...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Connecting with Safaricom API & bank portal to process order securely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: STOREFRONT
// -------------------------------------------------------------
@Composable
fun StorefrontScreen(viewModel: StoreViewModel) {
    val search by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val filteredProducts by viewModel.products.collectAsState()

    val categories = listOf("All", "Laptops", "Custom PCs", "RAM & Parts", "Storage", "Graphics Cards", "Accessories")

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Header banner for PC Tech Kenya
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BUILD YOUR DREAM PC",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "High performance computer parts, custom gaming rigs, and office tech.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("store_search_input"),
            placeholder = { Text("Search laptops, graphics cards, custom rigs...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        // Categories selector row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(cat) },
                    label = { Text(text = cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_$cat")
                )
            }
        }

        // Product list section
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No items matched",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No computer gear matched your request",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search filters or check back later for stock refresh.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Adaptive Grid based on screens width
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .testTag("products_grid"),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { item ->
                    ProductCard(product = item, onProductSelect = {
                        viewModel.selectProductById(item.id)
                        viewModel.navigateTo(Screen.ProductDetails(item.id))
                    }, onAddToCart = {
                        viewModel.addToCart(item, 1)
                    })
                }

                // Promotional banner for pctechkenya.com that spans columns
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val localContext = LocalContext.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pctechkenya.com"))
                                    localContext.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback if browser fails
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ready to Custom-Build?",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Visit the official pctechkenya.com marketplace to design bespoke configurations, find expert repair services, and track shipping direct to your door.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pctechkenya.com"))
                                        localContext.startActivity(intent)
                                    } catch (e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Visit Website", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Visit Site Link",
                                        modifier = Modifier.size(12.dp)
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onProductSelect: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(265.dp)
            .clickable { onProductSelect() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Category Badge & Quick Visual mock representer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (product.category.lowercase(Locale.ROOT)) {
                        "laptops" -> Icons.Default.LaptopMac
                        "custom pcs" -> Icons.Default.Computer
                        "ram & parts" -> Icons.Default.Memory
                        "storage" -> Icons.Default.SdStorage
                        "graphics cards" -> Icons.Default.DeveloperBoard
                        else -> Icons.Default.Cable
                    },
                    contentDescription = product.category,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Small Stock Pill Indicator
                val isLowStock = product.stockCount <= product.lowStockThreshold
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (product.stockCount == 0) "OUT OF STOCK" else if (isLowStock) "LOW STOCK: ${product.stockCount}" else "STOCK: ${product.stockCount}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Details
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = product.specifications,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatKsh(product.price),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )

                if (product.stockCount > 0) {
                    IconButton(
                        onClick = onAddToCart,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("add_to_cart_quick_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Add to Cart Quick",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Sold Out", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: PRODUCT DETAILS
// -------------------------------------------------------------
@Composable
fun ProductDetailsScreen(viewModel: StoreViewModel, productId: Int) {
    val selectedProductState by viewModel.selectedProduct.collectAsState()

    // Query on enter
    LaunchedEffect(productId) {
        viewModel.selectProductById(productId)
    }

    val product = selectedProductState

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var orderQty by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
            }
            Text(
                text = "Product Details",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                // Product Mock Image Showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (product.category.lowercase(Locale.ROOT)) {
                            "laptops" -> Icons.Default.LaptopMac
                            "custom pcs" -> Icons.Default.Computer
                            "ram & parts" -> Icons.Default.Memory
                            "storage" -> Icons.Default.SdStorage
                            "graphics cards" -> Icons.Default.DeveloperBoard
                            else -> Icons.Default.Cable
                        },
                        contentDescription = "PC Tech Visual Representation",
                        modifier = Modifier.size(90.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand category badge
                SuggestionChip(
                    onClick = {},
                    label = { Text(product.category) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Title
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Price tag
                Text(
                    text = formatKsh(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Stock Info Alert
                val isLowStock = product.stockCount <= product.lowStockThreshold
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLowStock) MaterialTheme.colorScheme.error else Color(0xFF4CAF50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (product.stockCount == 0) "Sold Out" else if (isLowStock) "Low Stock Warning" else "In Stock",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "  ${product.stockCount} items available in Warehouses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Detailed Specifications
                Text(
                    text = "System Specifications",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = product.specifications.ifEmpty { "N/A" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "Product Overview",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = product.description.ifEmpty { "No overview description has been entered for this equipment." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Action panel at bottom: Quantity Selector & Add to Cart Complete button
        if (product.stockCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, Color.Gray, RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (orderQty > 1) orderQty-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = orderQty.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(
                        onClick = { if (orderQty < product.stockCount) orderQty++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Button(
                    onClick = {
                        viewModel.addToCart(product, orderQty)
                        viewModel.navigateTo(Screen.ShoppingCart)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(48.dp)
                        .testTag("add_to_cart_detail_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add To Basket", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Sold out / Restocking", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: SHOPPING CART
// -------------------------------------------------------------
@Composable
fun ShoppingCartScreen(viewModel: StoreViewModel) {
    val cartList by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartTotal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Storefront) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
            }
            Text(
                text = "Your Basket",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (cartList.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearCart() }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cartList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Empty Basket",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your shopping basket is empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Explore pc parts and systems, and add items to begin.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Storefront) },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Browse Storefront", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartList) { cartItem ->
                    CartItemRow(item = cartItem, viewModel = viewModel)
                }
            }

            // Cart Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                        Text(formatKsh(subtotal), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Local KRA Taxes & VAT included:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("16.00%", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount Due:", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = formatKsh(subtotal),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.navigateTo(Screen.Checkout) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("checkout_proceed_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proceed To Secure Checkout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: StoreViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon layout
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.productCategory.lowercase(Locale.ROOT)) {
                        "laptops" -> Icons.Default.LaptopMac
                        "custom pcs" -> Icons.Default.Computer
                        "ram & parts" -> Icons.Default.Memory
                        "storage" -> Icons.Default.SdStorage
                        "graphics cards" -> Icons.Default.DeveloperBoard
                        else -> Icons.Default.Cable
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Description
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.productName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatKsh(item.productPrice) + " each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }

            // Multiplier/Qty
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity - 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Less", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity + 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "More", modifier = Modifier.size(16.dp))
                }
            }

            IconButton(
                onClick = { viewModel.removeCartItem(item.id) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove item")
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: SECURE PAYMENT GATEWAY (CHECKOUT)
// -------------------------------------------------------------
@Composable
fun CheckoutScreen(viewModel: StoreViewModel) {
    val totalAmt by viewModel.cartTotal.collectAsState()
    val checkPaymentMethod by viewModel.paymentMethod.collectAsState()
    val phone by viewModel.mpesaPhone.collectAsState()

    // Credit Card values
    val holderName by viewModel.cardHolderName.collectAsState()
    val cardNo by viewModel.cardNumber.collectAsState()
    val expiry by viewModel.cardExpiry.collectAsState()
    val cvv by viewModel.cardCvv.collectAsState()

    val errorMsg by viewModel.paymentError.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.ShoppingCart) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Secure payment gateway",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                // Secure Header info
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Secured Connection",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                "256-Bit SSL Secured Encryption",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "PCI-DSS certified secure payment transaction processing console.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Balance Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TOTAL ORDER PAYMENT DUE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatKsh(totalAmt),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment selector tabs
                Text(
                    text = "Select Secure Payment Method",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val mpesaSelected = checkPaymentMethod == "M-PESA"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (mpesaSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setPaymentMethod("M-PESA") }
                            .testTag("tab_mpesa_payment"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (mpesaSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Safaricom M-Pesa",
                                fontWeight = FontWeight.Bold,
                                color = if (mpesaSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    val cardSelected = checkPaymentMethod == "CARD"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (cardSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setPaymentMethod("CARD") }
                            .testTag("tab_card_payment"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = if (cardSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Credit/Debit Card",
                                fontWeight = FontWeight.Bold,
                                color = if (cardSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment form validation inputs
                if (checkPaymentMethod == "M-PESA") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Safaricom M-Pesa Express (STK Push)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Enter your registered M-Pesa phone number. Upon click, Safaricom will push an interactive PIN dialogue immediately to your smartphone screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { viewModel.updateMpesaPhone(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mpesa_phone_field"),
                                label = { Text("M-Pesa Phone Number") },
                                placeholder = { Text("e.g. 0712345678") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Supports Safaricom 07xx/01xx and 254xx formats.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Visa, MasterCard, or Amex Payment",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "E2E Secure card processing utilizing Luhn validations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            OutlinedTextField(
                                value = holderName,
                                onValueChange = { viewModel.updateCardHolder(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("cardholder_field"),
                                label = { Text("Cardholder Full Name") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = cardNo,
                                onValueChange = { viewModel.updateCardNumber(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("cardnumber_field"),
                                label = { Text("Credit Card Number") },
                                leadingIcon = { Icon(imageVector = Icons.Default.CreditCard, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = expiry,
                                    onValueChange = { viewModel.updateCardExpiry(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("expiry_field"),
                                    label = { Text("Expiry (MM/YY)") },
                                    placeholder = { Text("MM/YY") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { viewModel.updateCardCvv(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cvv_field"),
                                    label = { Text("CVV Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Error feedback block
                if (errorMsg != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error notification",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Checkout click trigger
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.startPaymentFlow()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pay_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (checkPaymentMethod == "M-PESA") "Authorize Safaricom STK Push" else "Pay Securely ${formatKsh(totalAmt)}",
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// SIMULATOR STK push overlay pop up dialog matching Safaricom SIM ToolKit UX
@Composable
fun MpesaStkSimulationDialog(
    amount: Double,
    phoneNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pinValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(4.dp), // Retro square dialog typical of old SIM Toolkit
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFFECEFF1))
                    .padding(16.dp)
            ) {
                // Retro header
                Text(
                    text = "SIM ToolKit",
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider(color = Color.LightGray)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Do you want to pay Ksh ${String.format("%,.2f", amount)} to PC TECH KENYA?",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter 4-Digit M-Pesa PIN:",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = pinValue,
                    onValueChange = {
                        if (it.length <= 4) pinValue = it.filter { ch -> ch.isDigit() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("mpesa_sim_pin_field"),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.DarkGray,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("mpesa_cancel_btn")
                    ) {
                        Text(
                            "CANCEL",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE53935)
                        )
                    }

                    TextButton(
                        onClick = {
                            if (pinValue.length == 4) {
                                focusManager.clearFocus()
                                onConfirm(pinValue)
                            }
                        },
                        enabled = pinValue.length == 4,
                        modifier = Modifier.testTag("mpesa_send_btn")
                    ) {
                        Text(
                            "SEND",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (pinValue.length == 4) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 5: TRANSACTIONS AND ORDERS COMPLETED LIST
// -------------------------------------------------------------
@Composable
fun OrderHistoryScreen(viewModel: StoreViewModel) {
    val orders by viewModel.allOrders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Storefront) }) {
                Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
            }
            Text(
                text = "Secure Payment History",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No receipts",
                        modifier = Modifier.size(70.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recorded transactions found",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Any orders secure-verified on the system will compile records here.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    OrderHistoryCard(order = order)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.Storefront) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Back To Shopping", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrderHistoryCard(order: PaymentOrder) {
    val formatter = remember { SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(order.orderDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (order.paymentMethod == "M-PESA") "M-PESA REFERENCE" else "SECURE CARD REFERENCE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = order.transactionCode,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2E7D32))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VERIFIED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            // Body
            Text(
                text = "Items Purchased:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = order.itemsSummary,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = "Completed: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                // Total paid
                Text(
                    text = formatKsh(order.totalAmount),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Secure tag info footer
            if (order.paymentMethod == "M-PESA") {
                Text(
                    text = "M-Pesa Sender phone: ${order.phoneNumber.take(5)}***${order.phoneNumber.takeLast(2)}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Processed to Cardholder: ${order.cardHolder} (ending in ** ${order.lastFour})",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 6: PRODUCT INVENTORY ADMINISTRATION DASHBOARD
// -------------------------------------------------------------
@Composable
fun AdminDashboardScreen(viewModel: StoreViewModel) {
    val items by viewModel.rawProducts.collectAsState()
    var filterLowStockOnly by remember { mutableStateOf(false) }

    val lowStockItems = items.filter { it.stockCount <= it.lowStockThreshold }
    val activeItemsList = if (filterLowStockOnly) lowStockItems else items

    // Quantitative metrics
    val totalStockUnits = items.sumOf { it.stockCount }
    val totalInventoryValuation = items.sumOf { it.price * it.stockCount }
    val lowStockCount = lowStockItems.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Storefront) }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close admin cockpit")
            }
            Text(
                text = "Inventory Control Cockpit",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quantitative Inventory Dashboard tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Assets", fontSize = 11.sp, color = Color.Gray)
                    Text("$totalStockUnits Units", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
            }

            Card(
                modifier = Modifier.weight(1.3f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Stock Valuation", fontSize = 11.sp, color = Color.Gray)
                    Text(formatKsh(totalInventoryValuation), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { filterLowStockOnly = !filterLowStockOnly },
                colors = CardDefaults.cardColors(
                    containerColor = if (lowStockCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Low Alerts", fontSize = 11.sp, color = if (lowStockCount > 0) MaterialTheme.colorScheme.error else Color.Gray)
                    Text(
                        "$lowStockCount items",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Toggle low stock only state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (filterLowStockOnly) "Showing: Low Stock Alerts" else "Showing: All Computer Stock",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { filterLowStockOnly = !filterLowStockOnly }
            ) {
                Text(
                    text = "Alerts Only",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Switch(
                    checked = filterLowStockOnly,
                    onCheckedChange = { filterLowStockOnly = it },
                    modifier = Modifier.scale(0.85f).testTag("low_stock_switch")
                )
            }
        }

        // Administrator lists
        if (activeItemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No items are currently matching high/low thresholds.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("admin_inventory_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeItemsList) { product ->
                    AdminProductRow(product = product, viewModel = viewModel)
                }
            }
        }

        // Action FAB triggers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(Screen.EditProduct(null)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_add_product_btn"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Computer Stock Item", fontWeight = FontWeight.Bold)
            }
        }
    }
}



@Composable
fun AdminProductRow(product: Product, viewModel: StoreViewModel) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.category + "  |  " + formatKsh(product.price),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    // Edit button
                    IconButton(
                        onClick = {
                            viewModel.selectProductById(product.id)
                            viewModel.navigateTo(Screen.EditProduct(product.id))
                        },
                        modifier = Modifier.testTag("admin_edit_prod_${product.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Product", tint = Color.Gray)
                    }

                    // Delete button with safety warning popup
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("admin_delete_prod_${product.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stock Count visual horizontal status bar representing stock level vs threshold
            val isLowStock = product.stockCount <= product.lowStockThreshold
            val healthPercent = if (product.stockCount == 0) 0f else (product.stockCount.toFloat() / 20f).coerceAtMost(1.0f)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Stock: ${product.stockCount} / Min warning threshold: ${product.lowStockThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLowStock) MaterialTheme.colorScheme.error else Color.Gray,
                    fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )

                // stock rating tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (product.stockCount == 0) Color.Gray.copy(alpha = 0.5f)
                            else if (isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (product.stockCount == 0) "OUT" else if (isLowStock) "LOW" else "OKAY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.stockCount == 0) Color.Gray else if (isLowStock) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { healthPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isLowStock) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }

    // Double validation popup dialog to prevent accidental inventory deletions
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion?") },
            text = { Text("Are you sure you want to delete '${product.name}' permanently from the PCTechKenya catalog? This operation cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        showDeleteConfirmDialog = false
                    },
                    modifier = Modifier.testTag("admin_confirm_delete_btn")
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("admin_cancel_delete_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SCREEN 7: INVENTORY ADD / EDIT FORM
// -------------------------------------------------------------
@Composable
fun EditProductScreen(viewModel: StoreViewModel, productId: Int?) {
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    // Trigger lookup if product is edit-mode
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.selectProductById(productId)
        } else {
            viewModel.clearSelectedProduct()
        }
    }

    val isEditMode = productId != null
    val targetProduct = if (isEditMode) selectedProduct else null

    // Prefill states
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Laptops") }
    var priceStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var specs by remember { mutableStateOf("") }
    var stockCountStr by remember { mutableStateOf("") }
    var warningLimitStr by remember { mutableStateOf("5") }

    var formError by remember { mutableStateOf<String?>(null) }

    // Synchronize states on loading targetProduct
    LaunchedEffect(targetProduct) {
        if (targetProduct != null) {
            name = targetProduct.name
            category = targetProduct.category
            priceStr = targetProduct.price.toString()
            description = targetProduct.description
            specs = targetProduct.specifications
            stockCountStr = targetProduct.stockCount.toString()
            warningLimitStr = targetProduct.lowStockThreshold.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.AdminDashboard) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
            }
            Text(
                text = if (isEditMode) "Edit Computer product" else "Add Stock Computer equipment",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Label / Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("form_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Selector Category Dropdown (Simple manual row list for portability)
                Text(
                    "Product Category *",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                val categoriesList = listOf("Laptops", "Custom PCs", "RAM & Parts", "Storage", "Graphics Cards", "Accessories")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categoriesList) { cat ->
                        val isSelected = cat == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("form_chip_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (KSh) *") },
                        modifier = Modifier.weight(1.2f).testTag("form_price"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = stockCountStr,
                        onValueChange = { stockCountStr = it },
                        label = { Text("Available Stock *") },
                        modifier = Modifier.weight(1f).testTag("form_stock"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = warningLimitStr,
                    onValueChange = { warningLimitStr = it },
                    label = { Text("Low Stock Alert Threshold *") },
                    modifier = Modifier.fillMaxWidth().testTag("form_threshold"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = specs,
                    onValueChange = { specs = it },
                    label = { Text("System Specifications") },
                    placeholder = { Text("e.g. i7 CPU, 16GB RAM, RTX 3050") },
                    modifier = Modifier.fillMaxWidth().testTag("form_specs"),
                    singleLine = false,
                    maxLines = 2
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Overview Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("form_description"),
                    singleLine = false,
                    maxLines = 3
                )

                // Error UI
                if (formError != null) {
                    Text(
                        text = formError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp).testTag("form_error")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val parsedPrice = priceStr.toDoubleOrNull()
                        val parsedStock = stockCountStr.toIntOrNull()
                        val parsedWarn = warningLimitStr.toIntOrNull()

                        if (name.trim().isEmpty() || parsedPrice == null || parsedStock == null || parsedWarn == null) {
                            formError = "Please fill in all starred properties with corresponding values correctly"
                        } else {
                            formError = null
                            viewModel.addOrUpdateProduct(
                                id = productId,
                                name = name,
                                category = category,
                                price = parsedPrice,
                                description = description,
                                specifications = specs,
                                stockCount = parsedStock,
                                lowStockThreshold = parsedWarn
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("form_save_btn"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditMode) "Save Changes" else "Publish Inventory Gear", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
