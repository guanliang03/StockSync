/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ui.InventoryTopAppBar
import com.seis2.loanlit.R
import data.Item
import ui.AppViewModelProvider
import ui.item.formatedPrice
import ui.navigation.NavigationDestination
import ui.theme.InventoryTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Spacer
import java.text.DateFormat
import java.util.Date

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val titleRes = R.string.home
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToItemEntry: () -> Unit,
    navigateToItemUpdate: (Int) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val homeUiState by viewModel.homeUiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val itemList = homeUiState.itemList

    fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val csvHeader = "ID,Name,Category,Storage Location,Unit Price,Quantity,Discontinued,Remarks\n"
                    val csvContent = itemList.joinToString("\n") { item ->
                        "${item.id},\"${escapeCsv(item.name)}\",\"${escapeCsv(item.category)}\",\"${escapeCsv(item.location)}\",${item.price},${item.quantity},${item.discontinued},\"${escapeCsv(item.itemDetails)}\""
                    }
                    outputStream.write((csvHeader + csvContent).toByteArray())
                }
                Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val pdfDocument = AndroidPdfDocument()
                    val pageInfo = AndroidPdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = AndroidPaint()
                    
                    val titlePaint = AndroidPaint().apply {
                        color = AndroidColor.rgb(10, 37, 64)
                        textSize = 20f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    val textPaint = AndroidPaint().apply {
                        color = AndroidColor.BLACK
                        textSize = 10f
                        isAntiAlias = true
                    }
                    
                    // Draw Header
                    canvas.drawText("Smart Inventory Management Report", 30f, 50f, titlePaint)
                    canvas.drawText("Generated on: ${DateFormat.getDateTimeInstance().format(Date())}", 30f, 70f, textPaint.apply { color = AndroidColor.GRAY })
                    
                    // Draw Line
                    paint.color = AndroidColor.rgb(200, 200, 200)
                    paint.strokeWidth = 1f
                    canvas.drawLine(30f, 85f, 565f, 85f, paint)
                    
                    // Stats Summary
                    val totalItems = itemList.size
                    val totalStock = itemList.sumOf { it.quantity }
                    val totalValue = itemList.sumOf { it.price * it.quantity }
                    
                    textPaint.apply { color = AndroidColor.BLACK; isFakeBoldText = true; textSize = 11f }
                    canvas.drawText("Total Items: $totalItems", 30f, 110f, textPaint)
                    canvas.drawText("Total Stock: $totalStock", 200f, 110f, textPaint)
                    canvas.drawText("Total Value: $${String.format("%.2f", totalValue)}", 380f, 110f, textPaint)
                    
                    canvas.drawLine(30f, 125f, 565f, 125f, paint)
                    
                    // Draw Table Header
                    textPaint.apply { textSize = 10f; isFakeBoldText = true }
                    canvas.drawText("Name", 30f, 145f, textPaint)
                    canvas.drawText("Category", 180f, 145f, textPaint)
                    canvas.drawText("Location", 280f, 145f, textPaint)
                    canvas.drawText("Price", 380f, 145f, textPaint)
                    canvas.drawText("Stock", 450f, 145f, textPaint)
                    canvas.drawText("Status", 500f, 145f, textPaint)
                    
                    canvas.drawLine(30f, 155f, 565f, 155f, paint)
                    
                    textPaint.isFakeBoldText = false
                    var y = 175f
                    val rowHeight = 22f
                    val pageHeightLimit = 800f
                    
                    var pageNumber = 1
                    var currentPage = page
                    var currentCanvas = canvas
                    
                    itemList.forEach { item ->
                        if (y > pageHeightLimit) {
                            pdfDocument.finishPage(currentPage)
                            pageNumber++
                            val nextPageInfo = AndroidPdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                            currentPage = pdfDocument.startPage(nextPageInfo)
                            currentCanvas = currentPage.canvas
                            
                            // Title
                            currentCanvas.drawText("Smart Inventory Management Report (Cont.)", 30f, 50f, titlePaint)
                            currentCanvas.drawLine(30f, 65f, 565f, 65f, paint)
                            
                            // Table Header
                            textPaint.apply { isFakeBoldText = true }
                            currentCanvas.drawText("Name", 30f, 85f, textPaint)
                            currentCanvas.drawText("Category", 180f, 85f, textPaint)
                            currentCanvas.drawText("Location", 280f, 85f, textPaint)
                            currentCanvas.drawText("Price", 380f, 85f, textPaint)
                            currentCanvas.drawText("Stock", 450f, 85f, textPaint)
                            currentCanvas.drawText("Status", 500f, 85f, textPaint)
                            currentCanvas.drawLine(30f, 95f, 565f, 95f, paint)
                            
                            textPaint.isFakeBoldText = false
                            y = 115f
                        }
                        
                        val statusText = when {
                            item.discontinued -> "Discontinued"
                            item.quantity == 0 -> "Out of Stock"
                            item.quantity in 1..5 -> "Low Stock"
                            else -> "In Stock"
                        }
                        
                        val tName = if (item.name.length > 22) item.name.take(20) + ".." else item.name
                        val tCat = if (item.category.length > 15) item.category.take(13) + ".." else item.category
                        val tLoc = if (item.location.length > 15) item.location.take(13) + ".." else item.location
                        
                        currentCanvas.drawText(tName, 30f, y, textPaint)
                        currentCanvas.drawText(tCat, 180f, y, textPaint)
                        currentCanvas.drawText(tLoc, 280f, y, textPaint)
                        currentCanvas.drawText("$${String.format("%.2f", item.price)}", 380f, y, textPaint)
                        currentCanvas.drawText(item.quantity.toString(), 450f, y, textPaint)
                        currentCanvas.drawText(statusText, 500f, y, textPaint)
                        
                        y += rowHeight
                    }
                    
                    pdfDocument.finishPage(currentPage)
                    pdfDocument.writeTo(outputStream)
                    pdfDocument.close()
                }
                Toast.makeText(context, "PDF report exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InventoryTopAppBar(
                title = stringResource(HomeDestination.titleRes),
                canNavigateBack = true,
                scrollBehavior = scrollBehavior,
                navigateUp = navigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToItemEntry,
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(
                        end = WindowInsets.safeDrawing.asPaddingValues()
                            .calculateEndPadding(LocalLayoutDirection.current)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.item_entry_title)
                )
            }
        },
    ) { innerPadding ->
        HomeBody(
            itemList = homeUiState.itemList,
            onItemClick = navigateToItemUpdate,
            onExportCsv = { csvLauncher.launch("inventory_report_${System.currentTimeMillis()}.csv") },
            onExportPdf = { pdfLauncher.launch("inventory_report_${System.currentTimeMillis()}.pdf") },
            modifier = modifier.fillMaxSize(),
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun HomeBody(
    itemList: List<Item>,
    onItemClick: (Int) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(HomeTab.LIST) }
    var showLowOnly by remember { mutableStateOf(false) }

    // Quick low-stock calculation and filtered list toggle
    val lowStockCount = remember(itemList) { itemList.count { !it.discontinued && it.quantity in 1..5 } }

    val filteredItemList = remember(itemList, searchQuery, showLowOnly) {
        var list = itemList
        if (showLowOnly) {
            list = list.filter { !it.discontinued && it.quantity in 1..5 }
        } else if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    // Stats calculations
    val totalItems = itemList.size
    val totalStock = itemList.sumOf { it.quantity }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        ),
    ) {
        TabSelector(
            activeTab = activeTab,
            onTabSelected = { activeTab = it }
        )

        // Low-stock alert banner
        if (activeTab == HomeTab.LIST && lowStockCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$lowStockCount items low in stock",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!showLowOnly) {
                        Button(onClick = { showLowOnly = true }) {
                            Text(text = "Show low")
                        }
                    } else {
                        Button(onClick = { showLowOnly = false }) {
                            Text(text = "Show all")
                        }
                    }
                }
            }
        }

        if (activeTab == HomeTab.LIST) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search items, categories, locations...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Stats Cards Row (Only show if there are items)
            if (itemList.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Items Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Items",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = totalItems.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Total Stock Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Stock",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = totalStock.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (filteredItemList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (itemList.isEmpty()) 
                                stringResource(R.string.no_item_description) 
                            else 
                                "No inventory matches your search.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                InventoryList(
                    itemList = filteredItemList,
                    onItemClick = { onItemClick(it.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            InsightsTabContent(
                itemList = itemList,
                onExportCsv = onExportCsv,
                onExportPdf = onExportPdf,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InventoryList(
    itemList: List<Item>,
    onItemClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(items = itemList, key = { it.id }) { item ->
            InventoryItem(
                item = item,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { onItemClick(item) }
            )
        }
    }
}

enum class HomeTab {
    LIST, INSIGHTS
}

@Composable
fun TabSelector(
    activeTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf(
            HomeTab.LIST to stringResource(R.string.tab_list),
            HomeTab.INSIGHTS to stringResource(R.string.tab_insights)
        )
        tabs.forEach { (tab, label) ->
            val isSelected = activeTab == tab
            val bgCol = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val txtCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(bgCol, RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = txtCol
                )
            }
        }
    }
}

@Composable
private fun InsightsTabContent(
    itemList: List<Item>,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.analytics_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        // Donut Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                val categoryData = remember(itemList) {
                    itemList.filter { it.category.isNotBlank() }
                        .groupBy { it.category.trim() }
                        .mapValues { entry -> entry.value.sumOf { it.quantity } }
                        .filter { it.value > 0 }
                }

                if (categoryData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No categorized stock to display.\nAdd items with quantities and categories to see charts.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val totalStock = categoryData.values.sum()
                    val colorPalette = listOf(
                        Color(0xFF10B981), // Emerald
                        Color(0xFF06B6D4), // Cyan
                        Color(0xFF8B5CF6), // Violet
                        Color(0xFFEF4444), // Red/Rose
                        Color(0xFFF59E0B), // Amber
                        Color(0xFF3B82F6), // Blue
                        Color(0xFFEC4899), // Pink
                        Color(0xFF14B8A6), // Teal
                        Color(0xFF6366F1), // Indigo
                        Color(0xFF84CC16)  // Lime
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Drawing
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            Canvas(modifier = Modifier.size(120.dp)) {
                                var startAngle = -90f
                                val strokeWidthPx = 16.dp.toPx()
                                categoryData.entries.forEachIndexed { index, entry ->
                                    val sweepAngle = 360f * entry.value / totalStock
                                    val color = colorPalette[index % colorPalette.size]
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle + 2f,
                                        sweepAngle = sweepAngle - 4f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Stock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = totalStock.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Legend Column
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            categoryData.entries.take(5).forEachIndexed { index, entry ->
                                val color = colorPalette[index % colorPalette.size]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (entry.key.length > 10) entry.key.take(8) + ".." else entry.key,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${entry.value})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (categoryData.size > 5) {
                                Text(
                                    text = "+ ${categoryData.size - 5} more categories",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stock Status Distribution Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Stock Level Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val total = itemList.size
                if (total == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items in inventory to analyze.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val statusCounts = remember(itemList) {
                        val discontinuedCount = itemList.count { it.discontinued }
                        val activeItems = itemList.filter { !it.discontinued }
                        val outOfStockCount = activeItems.count { it.quantity == 0 }
                        val lowStockCount = activeItems.count { it.quantity in 1..5 }
                        val inStockCount = activeItems.count { it.quantity > 5 }
                        
                        listOf(
                            Triple("In Stock", inStockCount, Color(0xFF10B981)),
                            Triple("Low Stock", lowStockCount, Color(0xFFF59E0B)),
                            Triple("Out of Stock", outOfStockCount, Color(0xFFEF4444)),
                            Triple("Discontinued", discontinuedCount, Color(0xFF64748B))
                        )
                    }

                    statusCounts.forEach { (label, count, color) ->
                        val percentage = if (total > 0) count.toFloat() / total else 0f
                        val percentageText = "${(percentage * 100).toInt()}%"
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$count items ($percentageText)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = if (percentage > 0f) percentage else 0.001f)
                                        .height(8.dp)
                                        .background(color, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Reports Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.reports_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Export the entire product database to a standardized file format for auditing, sharing, or accounting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export CSV",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export PDF",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItem(
    item: Item,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title & Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Smart Status Pill (Dynamically determined based on stock count)
                val badgeBg: Color
                val badgeTxt: Color
                val badgeTextStr: String
                val badgeIcon: androidx.compose.ui.graphics.vector.ImageVector

                when {
                    item.discontinued -> {
                        badgeBg = Color(0xFFE2E8F0) // Slate 200
                        badgeTxt = Color(0xFF475569) // Slate 600
                        badgeTextStr = "Discontinued"
                        badgeIcon = Icons.Default.Close
                    }
                    item.quantity == 0 -> {
                        badgeBg = Color(0xFFFFE4E6) // Rose 100
                        badgeTxt = Color(0xFF9F1239) // Rose 800
                        badgeTextStr = "Out of Stock"
                        badgeIcon = Icons.Default.Close
                    }
                    item.quantity in 1..5 -> {
                        badgeBg = Color(0xFFFEF3C7) // Amber 100
                        badgeTxt = Color(0xFF92400E) // Amber 800
                        badgeTextStr = "Low Stock"
                        badgeIcon = Icons.Default.Warning
                    }
                    else -> {
                        badgeBg = Color(0xFFD1FAE5) // Emerald 100
                        badgeTxt = Color(0xFF065F46) // Emerald 800
                        badgeTextStr = "In Stock"
                        badgeIcon = Icons.Default.Check
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeTxt,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeTextStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeTxt
                    )
                }
            }

            // Category Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.borrower_name, item.category),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Metadata: Location, Price, Qty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Price Amount
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.5f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Price: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.formatedPrice(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Stock Quantity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(0.8f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.quantity} Qty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Remarks Section (Only visible if not blank)
            if (item.itemDetails.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Remarks / Description",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.itemDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeBodyPreview() {
    InventoryTheme {
        HomeBody(
            itemList = listOf(
                Item(1, "Game", 100.0, 20, "KPZ", "Ali", "ABC Game", false),
                Item(2, "Pen", 200.0, 3, "KIY", "Siti", "DEF pen", false),
                Item(3, "TV", 300.0, 0, "KUO", "Alan", "GHI TV", true)
            ),
            onItemClick = {},
            onExportCsv = {},
            onExportPdf = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeBodyEmptyListPreview() {
    InventoryTheme {
        HomeBody(
            itemList = listOf(),
            onItemClick = {},
            onExportCsv = {},
            onExportPdf = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InventoryItemPreview() {
    InventoryTheme {
        InventoryItem(
            Item(1, "Game", 100.0, 20, "KPZ", "Ali", "ABC Game", false),
        )
    }
}

