package com.example.launcher.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography
import com.example.core.theme.sciFiHudBackground
import com.example.launcher.model.AppItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerDialog(
    palette: CyberColorPalette,
    glowIntensity: Float,
    allApps: List<AppItem>,
    visibleApps: List<AppItem>,
    favoriteApps: List<AppItem>,
    hiddenApps: List<AppItem>,
    searchQuery: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onUpdateAppCustomization: (packageName: String, customName: String?, isHidden: Boolean, isLocked: Boolean, isFavorite: Boolean, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedAppForOptions by remember { mutableStateOf<AppItem?>(null) }
    var renameDialogOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val categories = listOf("All", "Favorites", "Media", "Social", "Tools", "Games", "Hidden")

    val displayedApps = when (selectedCategory) {
        "All" -> visibleApps
        "Favorites" -> favoriteApps
        "Hidden" -> hiddenApps
        else -> visibleApps.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }.filter {
        if (searchQuery.isBlank()) true
        else it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .sciFiHudBackground(palette, glowIntensity)
                .testTag("app_drawer_screen"),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APPLICATIONS MATRIX",
                            style = CyberTypography.titleLarge,
                            color = palette.primaryCyan,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${displayedApps.size} Subsystems Indexed",
                            style = CyberTypography.labelSmall,
                            color = palette.secondaryAccent,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .background(palette.surfaceDark, CircleShape)
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("close_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "Search Neural Index...",
                            style = CyberTypography.bodyMedium,
                            color = palette.primaryCyan.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = palette.primaryCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = palette.primaryCyan
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryCyan,
                        unfocusedBorderColor = palette.primaryCyan.copy(alpha = 0.35f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = palette.secondaryAccent
                    ),
                    textStyle = CyberTypography.bodyMedium,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surfaceDark.copy(alpha = 0.7f))
                        .testTag("drawer_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) palette.primaryCyan.copy(alpha = 0.25f)
                                    else palette.surfaceDark.copy(alpha = 0.7f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) palette.primaryCyan else palette.primaryCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .combinedClickable(onClick = { onCategoryChange(cat) })
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("category_$cat")
                        ) {
                            Text(
                                text = cat.uppercase(),
                                style = CyberTypography.labelSmall,
                                color = if (isSelected) palette.primaryCyan else Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("app_grid")
                ) {
                    items(displayedApps, key = { it.packageName }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onAppClick(app) },
                                    onLongClick = {
                                        selectedAppForOptions = app
                                        renameText = app.appName
                                    }
                                )
                                .testTag("app_item_${app.packageName}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                palette.surfaceDark,
                                                Color(0xFF040810)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        if (app.isLocked) palette.secondaryAccent
                                        else palette.primaryCyan.copy(alpha = 0.45f * glowIntensity),
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                if (app.icon != null) {
                                    val bmp = remember(app.packageName) { app.icon.toBitmap(96, 96) }
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = app.appName,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }

                                // Lock Badge
                                if (app.isLocked) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(16.dp)
                                            .background(palette.secondaryAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color.Black,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                // Favorite Badge
                                if (app.isFavorite) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .size(14.dp)
                                            .background(palette.primaryCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Favorite",
                                            tint = Color.Black,
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = app.appName,
                                style = CyberTypography.bodyMedium,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // App Long-Press Options Dialog
    selectedAppForOptions?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForOptions = null },
            title = {
                Text(
                    text = "SUBSYSTEM CONFIG: ${app.appName.uppercase()}",
                    style = CyberTypography.titleMedium,
                    color = palette.primaryCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Rename
                    Button(
                        onClick = {
                            renameDialogOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = palette.primaryCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rename Subsystem", color = Color.White, style = CyberTypography.bodyMedium)
                    }

                    // Toggle Lock
                    Button(
                        onClick = {
                            onUpdateAppCustomization(
                                app.packageName,
                                app.appName,
                                app.isHidden,
                                !app.isLocked,
                                app.isFavorite,
                                app.category
                            )
                            selectedAppForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.secondaryAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            if (app.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = palette.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (app.isLocked) "Unlock Application" else "Lock With Security PIN",
                            color = Color.White,
                            style = CyberTypography.bodyMedium
                        )
                    }

                    // Toggle Hide
                    Button(
                        onClick = {
                            onUpdateAppCustomization(
                                app.packageName,
                                app.appName,
                                !app.isHidden,
                                app.isLocked,
                                app.isFavorite,
                                app.category
                            )
                            selectedAppForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = palette.primaryCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (app.isHidden) "Unhide from Matrix" else "Hide from Matrix",
                            color = Color.White,
                            style = CyberTypography.bodyMedium
                        )
                    }

                    // Toggle Favorite
                    Button(
                        onClick = {
                            onUpdateAppCustomization(
                                app.packageName,
                                app.appName,
                                app.isHidden,
                                app.isLocked,
                                !app.isFavorite,
                                app.category
                            )
                            selectedAppForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            if (app.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                            contentDescription = null,
                            tint = palette.primaryCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                            color = Color.White,
                            style = CyberTypography.bodyMedium
                        )
                    }

                    // System Info
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            selectedAppForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = palette.primaryCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System App Details", color = Color.White, style = CyberTypography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForOptions = null }) {
                    Text("Close", color = palette.primaryCyan, style = CyberTypography.labelSmall)
                }
            },
            containerColor = palette.backgroundDark
        )
    }

    // Rename Sub-Dialog
    if (renameDialogOpen && selectedAppForOptions != null) {
        val app = selectedAppForOptions!!
        AlertDialog(
            onDismissRequest = { renameDialogOpen = false },
            title = {
                Text("RENAME SUBSYSTEM", style = CyberTypography.titleMedium, color = palette.primaryCyan)
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryCyan,
                        unfocusedBorderColor = palette.primaryCyan.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = CyberTypography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onUpdateAppCustomization(
                                app.packageName,
                                renameText.trim(),
                                app.isHidden,
                                app.isLocked,
                                app.isFavorite,
                                app.category
                            )
                        }
                        renameDialogOpen = false
                        selectedAppForOptions = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryCyan)
                ) {
                    Text("Save", color = Color.Black, style = CyberTypography.labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogOpen = false }) {
                    Text("Cancel", color = Color.Gray, style = CyberTypography.labelSmall)
                }
            },
            containerColor = palette.backgroundDark
        )
    }
}
