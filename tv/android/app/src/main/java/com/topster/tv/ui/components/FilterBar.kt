package com.topster.tv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topster.tv.api.models.MediaItem

/**
 * Filter options for media items
 */
data class FilterOptions(
    val type: MediaType? = null,
    val yearRange: YearRange? = null,
    val quality: List<String>? = null
    val hasYearOnly: Boolean? = null
)

/**
 * Year range for filtering
 */
data class YearRange(
    val min: Int? = null,
    val max: Int? = null
)

/**
 * Filter types for Android TV
 */
enum class MediaType {
    ALL, MOVIE, TV_SHOW
}

/**
 * Media quality options
 */
enum class MediaQuality(val displayName: String, val value: String) {
    HD("HD", "720p"),
    FHD("Full HD", "1080p"),
    UHD("4K", "2160p");

    companion object {
        val all = listOf(HD, FHD, UHD)
    }
}

/**
 * FilterBar component for Android TV
 * Based on CLI FilterBar but adapted for Compose TV
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    filters: FilterOptions,
    totalItems: Int,
    filteredItems: Int,
    onClear: (() -> Unit)? = null,
    onFilterChange: (FilterOptions) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(filters.type ?: MediaType.ALL) }
    var showYearFilter by remember { mutableStateOf(false) }
    var showQualityFilter by remember { mutableStateOf(false) }

    // Calculate active filters count
    val activeFiltersCount = remember(filters, selectedType) {
        var count = 0
        if (filters.type != null && filters.type != MediaType.ALL) count++
        if (filters.yearRange != null) count++
        if (filters.quality != null && filters.quality.isNotEmpty()) count++
        count
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Main filter toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters ${if (activeFiltersCount > 0) "($activeFiltersCount active)" else ""}",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (activeFiltersCount > 0) {
                    Button(
                        onClick = onClear ?: {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF4444),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Clear All", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F5FF),
                        contentColor = Color.White
                    )
                ) {
                    Text("⚙️ Configure", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Result count
        Text(
            text = "$filteredItems of $totalItems items",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Expandable filter options
        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                color = Color(0xFF1A1A2A),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Media type filter
                    Text(
                        text = "Media Type",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(MediaType.entries) { type ->
                            FilterChip(
                                label = type.name,
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    onFilterChange(filters.copy(type = type))
                                }
                            )
                        }
                    }

                    // Year filter
                    Text(
                        text = "Year Range",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            label = "All Years",
                            selected = filters.yearRange == null,
                            onClick = {
                                onFilterChange(filters.copy(yearRange = null))
                            }
                        )

                        FilterChip(
                            label = "Custom Range",
                            selected = filters.yearRange != null,
                            onClick = { showYearFilter = !showYearFilter }
                        )
                    }

                    if (showYearFilter && filters.yearRange != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Min year input
                            OutlinedTextField(
                                value = filters.yearRange?.min?.toString() ?: "",
                                onValueChange = { value ->
                                    val min = value.toIntOrNull()
                                    val currentRange = filters.yearRange ?: YearRange()
                                    onFilterChange(filters.copy(yearRange = currentRange.copy(min = min)))
                                },
                                label = { Text("Min Year", color = Color.White, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            // Max year input
                            OutlinedTextField(
                                value = filters.yearRange?.max?.toString() ?: "",
                                onValueChange = { value ->
                                    val max = value.toIntOrNull()
                                    val currentRange = filters.yearRange ?: YearRange()
                                    onFilterChange(filters.copy(yearRange = currentRange.copy(max = max)))
                                },
                                label = { Text("Max Year", color = Color.White, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Quality filter
                    Text(
                        text = "Quality",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(null) + MediaQuality.all) { quality ->
                            FilterChip(
                                label = quality?.displayName ?: "All Qualities",
                                selected = quality == null || filters.quality?.contains(quality?.value ?: ""),
                                onClick = {
                                    if (quality == null) {
                                        onFilterChange(filters.copy(quality = null))
                                    } else {
                                        val currentQualityList = filters.quality ?: emptyList()
                                        if (currentQualityList.contains(quality.value)) {
                                            onFilterChange(filters.copy(quality = currentQualityList - quality.value))
                                        } else {
                                            onFilterChange(filters.copy(quality = currentQualityList + quality.value))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter chip component for selection
 */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier,
        color = if (selected) Color(0xFF00F5FF) else Color(0xFF2A2A3A),
        shape = RoundedCornerShape(20.dp),
        border = if (!selected) {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        } else null
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
